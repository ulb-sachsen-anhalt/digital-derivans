package de.ulb.digital.derivans.data;

import static de.ulb.digital.derivans.data.MetadataStore.NS_MODS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.mets.model.Mets;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.model.DescriptiveData;

/**
 * 
 * Builder for descriptive Metadata from METS/MODS
 * 
 * @author u.hartwig
 *
 */
class DescriptiveDataBuilder {

	private String urn = MetadataStore.UNKNOWN;

	private String person = MetadataStore.UNKNOWN;

	private String identifier = MetadataStore.UNKNOWN;

	private String title = MetadataStore.UNKNOWN;

	private String year = MetadataStore.UNKNOWN;

	private String accessCondition = MetadataStore.UNKNOWN;

	private MetadataHandler handler;
	
	private Mets mets;

	private static final Logger LOGGER = LogManager.getLogger(DescriptiveDataBuilder.class);

	/**
	 * Constructor if not METS/MODS available
	 */
	public DescriptiveDataBuilder() {
		this(null);
	}

	/**
	 * 
	 * Constructor with METS/MODS Model
	 * 
	 * @param mets
	 */
	public DescriptiveDataBuilder(Mets mets) {
		this.mets = mets;
	}


	DescriptiveDataBuilder urn() throws DigitalDerivansException {
		this.urn = getURN();
		return this;
	}

	DescriptiveDataBuilder person() throws DigitalDerivansException {
		this.person = getPerson();
		return this;
	}

	DescriptiveDataBuilder identifier() throws DigitalDerivansException {
		this.identifier = loadIdentifier();
		return this;
	}

	DescriptiveDataBuilder title() throws DigitalDerivansException {
		this.title = getTitle();
		return this;
	}

	DescriptiveDataBuilder access() throws DigitalDerivansException {
		accessCondition = getAccessCondition();
		return this;
	}

	DescriptiveDataBuilder year() throws DigitalDerivansException {
		year = getYear();
		return this;
	}

	DescriptiveData build() {
		DescriptiveData dd = new DescriptiveData();
		dd.setUrn(urn);
		dd.setIdentifier(identifier);
		dd.setTitle(title);
		dd.setYearPublished(year);
		dd.setPerson(person);
		dd.setLicense(Optional.of(accessCondition));
		LOGGER.debug("build data: '{}'", dd);
		return dd;
	}

	String getPerson() throws DigitalDerivansException {
		Element mods = handler.getPrimaryMods();
		if (mods != null) {
			List<Element> nameSubtrees = mods.getChildren("name", NS_MODS);

			// collect proper name relations
			Map<MARCRelator, List<Element>> properRelations = getDesiredRelations(nameSubtrees);
			if (properRelations.isEmpty()) {
				LOGGER.warn("found no proper related persons!");
				return MetadataStore.UNKNOWN;
			}

			// assume we have pbl's or aut's candidates
			if (properRelations.containsKey(MARCRelator.AUTHOR)) {
				return getSomeName(properRelations.get(MARCRelator.AUTHOR));
			} else if (properRelations.containsKey(MARCRelator.PUBLISHER)) {
				return getSomeName(properRelations.get(MARCRelator.PUBLISHER));
			}

		}
		return MetadataStore.UNKNOWN;
	}

	/**
	 * 
	 * Get some name for person-of-interest from MODS:
	 * 
	 * <ul>
	 * <li>if any mods:displayForm exists, get the text() from first element</li>
	 * <li>if not, search for mods:namePart elements and get the first with
	 * attribute "family"</li>
	 * </ul>
	 * 
	 * @param list
	 * @return
	 */
	private String getSomeName(List<Element> list) {
		for (Element e : list) {
			List<Element> displayers = e.getChildren("displayForm", NS_MODS);
			if (!displayers.isEmpty()) {
				return displayers.get(0).getTextNormalize();
			}
			for (Element f : e.getChildren("namePart", NS_MODS)) {
				if ("family".equals(f.getAttributeValue("type"))) {
					return f.getTextNormalize();
				}
			}
		}
		return MetadataStore.UNKNOWN;
	}

	private Map<MARCRelator, List<Element>> getDesiredRelations(List<Element> nameSubtrees) {
		Map<MARCRelator, List<Element>> map = new TreeMap<>();
		for (Element e : nameSubtrees) {
			for (Element f : e.getChildren("role", NS_MODS)) {
				for (Element g : f.getChildren("roleTerm", NS_MODS)) {
					if ("code".equals(g.getAttributeValue("type"))) {
						String code = g.getTextNormalize();
						MARCRelator rel = MARCRelator.forCode(code);
						switch (rel) {
						case AUTHOR:
						case PUBLISHER:
							LOGGER.debug("map '{}' as person", rel);
							List<Element> currList = new ArrayList<>();
							currList.add(e);
							map.merge(rel, currList, (prev, curr) -> {
								prev.addAll(curr);
								return prev;
							});
							break;
						default:
							LOGGER.debug("dont map '{}' as person", rel);
						}
					}
				}
			}
		}
		return map;
	}

	/**
	 * 
	 * Please note, this information is critical, that it cannot guess a default
	 * value and must be set to something meaningful.
	 * 
	 * If this setup fails due missing metadata, the identifier must be set later
	 * on. Therefore it returns null.
	 * 
	 * @return
	 */
	private String loadIdentifier() throws DigitalDerivansException {
		Element mods = handler.getPrimaryMods();
		if (mods == null) {
			return null;
		}
		List<Element> recordInfos = mods.getChildren("recordInfo", NS_MODS);
		Predicate<Element> sourceExists = e -> Objects.nonNull(e.getAttributeValue("source"));
		for (Element recordInfo : recordInfos) {
			List<Element> identifiers = recordInfo.getChildren("recordIdentifier", NS_MODS);
			Optional<Element> optUrn = identifiers.stream().filter(sourceExists).findFirst();
			if (optUrn.isPresent()) {
				return optUrn.get().getTextTrim();
			}
		}
		throw new DigitalDerivansException("found no valid recordIdentifier");
	}

	String getTitle() throws DigitalDerivansException {
		Element mods = handler.getPrimaryMods();
		if (mods != null) {
			Element titleInfo = mods.getChild("titleInfo", NS_MODS);
			if (titleInfo != null) {
				Element titleText = titleInfo.getChild("title", NS_MODS);
				if (titleText != null) {
					return titleText.getTextNormalize();
				} else {
					String msg = "Invalid MODS: missing title in titleInfo";
					LOGGER.error(msg);
					throw new DigitalDerivansException(msg);
				}
			} else {
				// if exists a relatedItem ...
				if(mods.getChild("relatedItem", NS_MODS) != null) {
					// ... then it must be a volume of something
					return "Band";
				}
			}
		}
		return MetadataStore.UNKNOWN;
	}

	String getURN() throws DigitalDerivansException {
		Element mods = handler.getPrimaryMods();
		if (mods != null) {
			List<Element> identifiers = mods.getChildren("identifier", NS_MODS);
			Predicate<Element> typeUrn = e -> e.getAttribute("type").getValue().equals("urn");
			Optional<Element> optUrn = identifiers.stream().filter(typeUrn).findFirst();
			if (optUrn.isPresent()) {
				return optUrn.get().getTextNormalize();
			}
		}
		return MetadataStore.UNKNOWN;
	}

	String getAccessCondition() throws DigitalDerivansException {
		Element mods = handler.getPrimaryMods();
		if (mods != null) {
			Element cond = mods.getChild("accessCondition", NS_MODS);
			if (cond != null) {
				return cond.getTextNormalize();
			}
		}
		return MetadataStore.UNKNOWN;
	}

	String getYear() throws DigitalDerivansException {
		Element mods = handler.getPrimaryMods();
		if (mods != null) {
			PredicateEventTypePublication publicationEvent = new PredicateEventTypePublication();
			Optional<Element> optPubl = mods.getChildren("originInfo", NS_MODS).stream().filter(publicationEvent)
					.findFirst();
			if (optPubl.isPresent()) {
				Element publ = optPubl.get();
				Element issued = publ.getChild("dateIssued", NS_MODS);
				if (issued != null) {
					return issued.getTextNormalize();
				}
			}
			// Attribute 'eventType=publication' of node 'publication' is missing
			// so try to find/filter node less consistently
			Element oInfo = mods.getChild("originInfo", NS_MODS);
			if (oInfo != null) {
				Element issued = oInfo.getChild("dateIssued", NS_MODS);
				if (issued != null) {
					return issued.getTextNormalize();
				}
			}
		}
		return MetadataStore.UNKNOWN;
	}

	public void setHandler(MetadataHandler handler) {
		this.handler = handler;

	}
}

/**
 * 
 * Predicate for filtering mods:originInfo[@eventType] elements
 * 
 * @author hartwig
 *
 */
class PredicateEventTypePublication implements Predicate<Element> {

	@Override
	public boolean test(Element el) {

		if (el.getAttribute("eventType") != null) {
			String val = el.getAttributeValue("eventType");
			return val.equalsIgnoreCase("publication");
		}
		return false;
	}

}

/**
 * 
 * Map MARC relator codes with enum
 * 
 * @author u.hartwig
 *
 */
enum MARCRelator {

	AUTHOR("aut"), 
	ASSIGNED_NAME("asn"), 
	CONTRIBUTOR("ctb"), 
	OTHER("oth"), 
	PUBLISHER("pbl"), 
	PRINTER("prt"),
	UNKNOWN("n.a.");

	private String code;

	private MARCRelator(String code) {
		this.code = code;
	}

	public static MARCRelator forCode(String code) {
		for (MARCRelator e : values()) {
			if (e.code.equals(code)) {
				return e;
			}
		}
		return UNKNOWN;
	}
}
