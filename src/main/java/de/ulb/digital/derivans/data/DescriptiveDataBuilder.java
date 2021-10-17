package de.ulb.digital.derivans.data;

import static de.ulb.digital.derivans.data.MetadataStore.NS_MODS;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;
import static java.util.stream.Collectors.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.mets.model.Mets;
import org.mycore.mets.model.sections.DmdSec;
import org.mycore.mets.model.struct.SmLink;

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

	private Mets mets;

	private Element primaryMods;

	private MetadataHandler handler;

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
		Element mods = getPrimaryMods();
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
		Element mods = getPrimaryMods();
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
		Element mods = getPrimaryMods();
		if (mods != null) {
			Element titleInfo = mods.getChild("titleInfo", NS_MODS);
			if (titleInfo != null) {
				String titleText = titleInfo.getChild("title", NS_MODS).getTextNormalize();
				if (titleText != null) {
					return titleText;
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
		Element mods = getPrimaryMods();
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
		Element mods = getPrimaryMods();
		if (mods != null) {
			Element cond = mods.getChild("accessCondition", NS_MODS);
			if (cond != null) {
				return cond.getTextNormalize();
			}
		}
		return MetadataStore.UNKNOWN;
	}

	String getYear() throws DigitalDerivansException {
		Element mods = getPrimaryMods();
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

	private Element getPrimaryMods() throws DigitalDerivansException {
		if (primaryMods != null) {
			return this.primaryMods;
		} else {
			if (mets != null) {
				String dmdId = getDescriptiveMetadataIdentifierOfInterest();
				DmdSec primaryDMD = mets.getDmdSecById(dmdId);
				if (primaryDMD == null) {
					throw new DigitalDerivansException("can't identify primary MODS section");
				}
				Iterable<Element> iter = primaryDMD.asElement().getDescendants(new ModsFilter());
				this.primaryMods = iter.iterator().next();;
			}
			return this.primaryMods;
		}
	}

	/**
	 * 
	 * Resolve identifier for descriptive section backwards.
	 * Starting from Physical Top-Level container of MAX filegroup,
	 * pick the most reasonable link and use this to identify the
	 * proper logical structure.
	 * 
	 * TODO review mets-model
	 * conversions necessary since mets-model doesn't read
	 * properly all Element's Attributes like "DMDID"
	 * 
	 * @return
	 */
	private String getDescriptiveMetadataIdentifierOfInterest() {
		// get most probably link from structMapping section
		String logId = getLogicalLinkFromStructMapping();
		
		// get logical divisions as raw elements (see above)
		List<Element> logSubcontainers = handler.requestLogicalSubcontainers();
		
		for (Element e: logSubcontainers) {
			if (e.getAttributeValue("ID").equals(logId)) {
				return e.getAttributeValue("DMDID");
			}
		}
		return null;
	}
	
	/**
	 * 
	 * If the structMap links contain a mapping to "physroot", 
	 * then this is considered to link the logical struct's
	 * corresponding to the main logical structure (VLS-like)
	 * OR
	 * count strctMap-links from logical to physical, and identify the 
	 * one containing most links / as many links as there are physical
	 * containers (Kitodo2-like)
	 * 
	 * In both cases, follow the link's origin to the logical map. 
	 * 
	 * @return
	 */
	private String getLogicalLinkFromStructMapping() {
		List<SmLink> smLinksToPhysRoot = mets.getStructLink().getSmLinkByTo("physroot");
		if (!smLinksToPhysRoot.isEmpty()) {
			return smLinksToPhysRoot.get(0).getFrom();
		} else {
			List<SmLink> links = mets.getStructLink().getSmLinks();
			// map SmLinks to their @from-count
			Map<String, Integer> mapToN = links.stream().collect(groupingBy(SmLink::getFrom, summingInt(n -> 1)));
			// get identifier of SmLink with MAX counts
			var optMaxLink = mapToN.entrySet().stream().max(Comparator.comparingInt(Entry::getValue));
			if (optMaxLink.isPresent()) {
				var maxMapping = optMaxLink.get();
				return maxMapping.getKey();
			}
		}
		return null;
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
