package de.ulb.digital.derivans.data.mets;

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

import de.ulb.digital.derivans.data.IMetadataStore;

/**
 * 
 * Encapsulate mods part of METS/MODS
 * 
 * @author u.hartwig
 * 
 */
public class MODS {

	private static final Logger LOGGER = LogManager.getLogger(MODS.class);

	private String id;

	private Element element;

	public MODS(String id, Element element) {
		this.id = id;
		this.element = element;
	}

	public String getId() {
		return this.id;
	}

	public String getPerson() {
		List<Element> nameSubtrees = this.element.getChildren("name", METS.NS_MODS);

		// collect proper name relations
		Map<MODSRelator, List<Element>> properRelations = getDesiredRelations(nameSubtrees);
		if (properRelations.isEmpty()) {
			LOGGER.warn("found no proper related persons!");
		}

		// assume we have pbl's or aut's candidates
		if (properRelations.containsKey(MODSRelator.AUTHOR)) {
			return MODS.getSomeName(properRelations.get(MODSRelator.AUTHOR));
		} else if (properRelations.containsKey(MODSRelator.PUBLISHER)) {
			return MODS.getSomeName(properRelations.get(MODSRelator.PUBLISHER));
		}
		return IMetadataStore.UNKNOWN;
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
	private static String getSomeName(List<Element> list) {
		for (Element e : list) {
			List<Element> displayers = e.getChildren("displayForm", METS.NS_MODS);
			if (!displayers.isEmpty()) {
				return displayers.get(0).getTextNormalize();
			}
			for (Element f : e.getChildren("namePart", METS.NS_MODS)) {
				if ("family".equals(f.getAttributeValue("type"))) {
					return f.getTextNormalize();
				}
			}
		}
		return IMetadataStore.UNKNOWN;
	}

	private static Map<MODSRelator, List<Element>> getDesiredRelations(List<Element> nameSubtrees) {
		Map<MODSRelator, List<Element>> map = new TreeMap<>();
		for (Element e : nameSubtrees) {
			for (Element f : e.getChildren("role", METS.NS_MODS)) {
				for (Element g : f.getChildren("roleTerm", METS.NS_MODS)) {
					if ("code".equals(g.getAttributeValue("type"))) {
						String code = g.getTextNormalize();
						MODSRelator rel = MODSRelator.forCode(code);
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

	public String getIdentifier() {
		List<Element> recordInfos = this.element.getChildren("recordInfo", METS.NS_MODS);
		Predicate<Element> sourceExists = e -> Objects.nonNull(e.getAttributeValue("source"));
		for (Element recordInfo : recordInfos) {
			List<Element> identifiers = recordInfo.getChildren("recordIdentifier", METS.NS_MODS);
			Optional<Element> optUrn = identifiers.stream().filter(sourceExists).findFirst();
			if (optUrn.isPresent()) {
				return optUrn.get().getTextTrim();
			}
		}
		return IMetadataStore.UNKNOWN;
	}

	public String getTitle() {
		Element titleInfo = this.element.getChild("titleInfo", METS.NS_MODS);
		if (titleInfo != null) {
			Element titleText = titleInfo.getChild("title", METS.NS_MODS);
			if (titleText != null) {
				return titleText.getTextNormalize();
			}
			Element subtitleText = this.element.getChild("subTitle", METS.NS_MODS);
			if (subtitleText != null) {
				return subtitleText.getTextNormalize();
			}
		} else {
			// if exists a relatedItem ...
			if (this.element.getChild("relatedItem", METS.NS_MODS) != null) {
				// ... then it must be a volume of something
				return "Band";
			}
		}
		return IMetadataStore.UNKNOWN;
	}

	public String getIdentifierURN() {
		List<Element> identifiers = this.element.getChildren("identifier", METS.NS_MODS);
		Predicate<Element> typeUrn = e -> e.getAttribute("type").getValue().equals("urn");
		Optional<Element> optUrn = identifiers.stream().filter(typeUrn).findFirst();
		if (optUrn.isPresent()) {
			return optUrn.get().getTextNormalize();
		}
		return IMetadataStore.UNKNOWN;
	}

	public String getAccessCondition() {
		Element cond = this.element.getChild("accessCondition", METS.NS_MODS);
		if (cond != null) {
			return cond.getTextNormalize();
		}
		return IMetadataStore.UNKNOWN;
	}

	public String getYearPublication() {
		PredicateEventTypePublication publicationEvent = new PredicateEventTypePublication();
		Optional<Element> optPubl = this.element.getChildren("originInfo", METS.NS_MODS).stream()
				.filter(publicationEvent)
				.findFirst();
		if (optPubl.isPresent()) {
			Element publ = optPubl.get();
			Element issued = publ.getChild("dateIssued", METS.NS_MODS);
			if (issued != null) {
				return issued.getTextNormalize();
			}
		}
		// Attribute 'eventType=publication' of node 'publication' is missing
		// so try to find/filter node less consistently
		Element oInfo = this.element.getChild("originInfo", METS.NS_MODS);
		if (oInfo != null) {
			Element issued = oInfo.getChild("dateIssued", METS.NS_MODS);
			if (issued != null) {
				return issued.getTextNormalize();
			}
		}
		return IMetadataStore.UNKNOWN;
	}

}
