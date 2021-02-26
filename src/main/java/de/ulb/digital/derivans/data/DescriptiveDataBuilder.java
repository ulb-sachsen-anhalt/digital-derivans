package de.ulb.digital.derivans.data;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.mets.model.Mets;

import de.ulb.digital.derivans.model.DescriptiveData;

class DescriptiveDataBuilder {

	private String urn = MetadataStore.UNKNOWN;

	private String author = MetadataStore.UNKNOWN;

	// watch out!
	private String identifier;

	private String title = MetadataStore.UNKNOWN;

	private String year = MetadataStore.UNKNOWN;

	private String accessCondition = MetadataStore.UNKNOWN;

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

	DescriptiveDataBuilder urn() {
		this.urn = getURN();
		return this;
	}

	DescriptiveDataBuilder author() {
		this.author = getAuthor();
		return this;
	}

	DescriptiveDataBuilder identifier() {
		this.identifier = loadIdentifier();
		return this;
	}

	DescriptiveDataBuilder title() {
		this.title = getTitle();
		return this;
	}

	DescriptiveDataBuilder access() {
		accessCondition = getAccessCondition();
		return this;
	}

	DescriptiveDataBuilder year() {
		year = getYear();
		return this;
	}

	DescriptiveData build() {
		DescriptiveData dd = new DescriptiveData();
		dd.setUrn(urn);
		dd.setAuthor(author);
		dd.setIdentifier(identifier);
		dd.setTitle(title);
		dd.setYearPublished(year);
		dd.setLicense(Optional.of(accessCondition));
		LOGGER.debug("build data: '{}'", dd);
		return dd;
	}

	String getAuthor() {
		Element mods = getPrimaryMods();
		if (mods != null) {
			Element nameSubtree = mods.getChild("name", MetadataStore.NS_MODS);
			if (nameSubtree != null) {
				Element displayElement = nameSubtree.getChild("displayForm", MetadataStore.NS_MODS);
				if (displayElement != null) {
					return displayElement.getTextTrim();
				}
			}
		}
		return MetadataStore.UNKNOWN;
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
	private String loadIdentifier() {
		Element mods = getPrimaryMods();
		if (mods != null) {
			Element recordInfo = mods.getChild("recordInfo", MetadataStore.NS_MODS);
			if (recordInfo != null) {
				Predicate<Element> sourceGvk = e -> e.getAttribute("source").getValue().equals("gvk-ppn");
				List<Element> identifiers = recordInfo.getChildren("recordIdentifier", MetadataStore.NS_MODS);
				Optional<Element> optUrn = identifiers.stream().filter(sourceGvk).findFirst();
				if (optUrn.isPresent()) {
					return optUrn.get().getTextTrim();
				} else {
					return extractAlternative(identifiers);
				}
			}
		}
		return null;
	}

	private String extractAlternative(List<Element> identifiers) {
		for (Element identifierElement : identifiers) {
			String sourceValue = identifierElement.getAttribute("source").getValue();
			if (sourceValue.toLowerCase().contains("ulb")) {
				return identifierElement.getTextTrim();
			}
		}
		return MetadataStore.UNKNOWN;
	}

	String getTitle() {
		Element mods = getPrimaryMods();
		if (mods != null) {
			Element titleInfo = mods.getChild("titleInfo", MetadataStore.NS_MODS);
			return titleInfo.getChild("title", MetadataStore.NS_MODS).getTextNormalize();
		}
		return MetadataStore.UNKNOWN;
	}

	String getURN() {
		Element mods = getPrimaryMods();
		if (mods != null) {
			List<Element> identifiers = mods.getChildren("identifier", MetadataStore.NS_MODS);
			Predicate<Element> typeUrn = e -> e.getAttribute("type").getValue().equals("urn");
			Optional<Element> optUrn = identifiers.stream().filter(typeUrn).findFirst();
			if (optUrn.isPresent()) {
				return optUrn.get().getTextNormalize();
			}
		}
		return MetadataStore.UNKNOWN;
	}

	String getAccessCondition() {
		Element mods = getPrimaryMods();
		if (mods != null) {
			Element cond = mods.getChild("accessCondition", MetadataStore.NS_MODS);
			if (cond != null) {
				return cond.getTextNormalize();
			}
		}
		return MetadataStore.UNKNOWN;
	}

	String getYear() {
		Element mods = getPrimaryMods();
		if (mods != null) {
			PredicateEventTypePublication publicationEvent = new PredicateEventTypePublication();
			Optional<Element> optPubl = mods.getChildren("originInfo", MetadataStore.NS_MODS).stream()
					.filter(publicationEvent).findFirst();
			if (optPubl.isPresent()) {
				Element publ = optPubl.get();
				Element issued = publ.getChild("dateIssued", MetadataStore.NS_MODS);
				if (issued != null) {
					return issued.getTextNormalize();
				}
			}
		}

		return MetadataStore.UNKNOWN;
	}

	private Element getPrimaryMods() {
		if (mets != null) {
			String dmdId = mets.getLogicalStructMap().getDivContainer().getDmdId();
			return mets.getDmdSecById(dmdId).getMdWrap().getMetadata();
		}
		return null;
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