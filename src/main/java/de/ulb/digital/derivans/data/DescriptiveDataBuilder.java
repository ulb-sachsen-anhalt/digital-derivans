package de.ulb.digital.derivans.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.mets.model.Mets;
import org.mycore.mets.model.files.File;
import org.mycore.mets.model.files.FileGrp;
import org.mycore.mets.model.sections.DmdSec;
import org.mycore.mets.model.struct.LogicalDiv;
import org.mycore.mets.model.struct.LogicalStructMap;
import org.mycore.mets.model.struct.PhysicalStructMap;
import org.mycore.mets.model.struct.SmLink;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.model.DescriptiveData;

import static de.ulb.digital.derivans.data.MetadataStore.*; 

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

	DescriptiveDataBuilder urn() {
		this.urn = getURN();
		return this;
	}

	DescriptiveDataBuilder person() {
		this.person = getPerson();
		return this;
	}

	DescriptiveDataBuilder identifier() throws DigitalDerivansException {
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
		dd.setIdentifier(identifier);
		dd.setTitle(title);
		dd.setYearPublished(year);
		dd.setPerson(person);
		dd.setLicense(Optional.of(accessCondition));
		LOGGER.debug("build data: '{}'", dd);
		return dd;
	}

	String getPerson() {
		Element mods = getPrimaryMods();
		if (mods != null) {
			List<Element> nameSubtrees = mods.getChildren("name", NS_MODS);

			// collect proper name relations
			Map<MARCRelator, List<Element>> properRelations = getDesiredRelations(nameSubtrees);
			if(properRelations.isEmpty()) {
				LOGGER.warn("found no proper related persons!");
				return MetadataStore.UNKNOWN;
			}

			// assume we have pbl's or aut's candidates
			if(properRelations.containsKey(MARCRelator.AUTHOR)) {
				return getSomeName(properRelations.get(MARCRelator.AUTHOR));
			} else if(properRelations.containsKey(MARCRelator.PUBLISHER)) {
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
	 * 	<li>if any mods:displayForm exists, get the text() from first element</li>
	 * 	<li>if not, search for mods:namePart elements and get the first with attribute "family"</li>
	 * </ul>
	 * 
	 * @param list
	 * @return
	 */
	private String getSomeName(List<Element> list) {
		for(Element e : list) {
			List<Element> displayers = e.getChildren("displayForm", NS_MODS);
			if(!displayers.isEmpty()) {
				return displayers.get(0).getTextNormalize();
			}
			for(Element f : e.getChildren("namePart", NS_MODS)) {
				if("family".equals(f.getAttributeValue("type"))) {
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
		for(Element recordInfo : recordInfos) {
			List<Element> identifiers = recordInfo.getChildren("recordIdentifier", NS_MODS);
			Optional<Element> optUrn = identifiers.stream().filter(sourceExists).findFirst();
			if (optUrn.isPresent()) {
				return optUrn.get().getTextTrim();
			}
		}
		throw new DigitalDerivansException("found no valid recordIdentifier");
	}

	String getTitle() {
		Element mods = getPrimaryMods();
		if (mods != null) {
			Element titleInfo = mods.getChild("titleInfo", NS_MODS);
			if (titleInfo != null) {
				return titleInfo.getChild("title", NS_MODS).getTextNormalize();
			}
			// take care of host title (kitodo2)
			// TODO: do some handler stuff
			// currently, mods:relatedItem is *not* handled by mets-model
		}
		return MetadataStore.UNKNOWN;
	}

	String getURN() {
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

	String getAccessCondition() {
		Element mods = getPrimaryMods();
		if (mods != null) {
			Element cond = mods.getChild("accessCondition", NS_MODS);
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
			Optional<Element> optPubl = mods.getChildren("originInfo", NS_MODS).stream()
					.filter(publicationEvent).findFirst();
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

	private Element getPrimaryMods() {
		if (mets != null) {
//			String dmdId = getLinkFromLogicalRoot(mets.getLogicalStructMap());
			String dmdId = getDescriptiveMetadataIdentifier();
			DmdSec dmd = mets.getDmdSecById(dmdId);
			if (dmd != null) {
				return dmd.getMdWrap().getMetadata();
			} else {
				// hierarchical multivolume works from kitodo or semantics
				String firstDmdId = getLinkFromFirstVolume();
				if(firstDmdId == null) {
					return null;
				}
				dmd = mets.getDmdSecById(firstDmdId);
				if(dmd != null) {
					return dmd.getMdWrap().getMetadata();
				}
			}
		}
		return null;
//		throw new DigitalDerivansException("can't identify primary MODS section");
	}
	
	/**
	 * 
	 * Resolve identifier for descriptive section backwards
	 * start from Phys top container for MAX-Filegroup, and if 
	 * defaults to "physroot", over the struct map link for 
	 * "physroot" to the logical struct's DMDID-attribute 
	 * (VLS-like)
	 * OR
	 * count all map linkings, and pick the from link with 
	 * half of the links 
	 * (Kitodo-like)
	 * 
	 * @return
	 */
	private String getDescriptiveMetadataIdentifier() {
		LogicalStructMap logMap = (LogicalStructMap)mets.getStructMap("LOGICAL");
		LogicalDiv logDiv = logMap.getDivContainer();
		List<SmLink> smLinks = mets.getStructLink().getSmLinkByTo("physroot");
		if(! smLinks.isEmpty()) {
			String fromID = smLinks.get(0).getFrom();
			return inspectLogStruct(logDiv, fromID);
		} else {
			Map<String, Integer> mapToN = new HashMap<>();
			List<SmLink> links = mets.getStructLink().getSmLinks();
			int nLinks = links.size();
			for(SmLink link : links) {
				if (!mapToN.containsKey(link.getFrom())) {
					mapToN.put(link.getFrom(), 1);
				} else {
					mapToN.computeIfPresent(link.getFrom(), (k,v) -> v + 1);
				}
			}
			System.out.println(mapToN);
			for (Entry<String, Integer> entry : mapToN.entrySet()) {
				if(entry.getValue() * 2 == nLinks) {
					String fromID = entry.getKey();
					return inspectLogStruct(logDiv, fromID);
				}
			}
		}
		return null;
	}
	
	private static String inspectLogStruct(LogicalDiv logDiv, String fromID) {
		if (logDiv.getId().equals(fromID)) {
			return logDiv.getDmdId();
		}
		for (LogicalDiv logSubDiv : logDiv.getDescendants()) {
			if (logSubDiv != null && fromID.equals(logSubDiv.getId())) {
				return logSubDiv.getDmdId();
			}
		}
		return null;
	}
	
	/**
	 * 
	 * Only return from logical root element when we can be sure that
	 * it is an monograph
	 * 
	 * @param logMap
	 * @return
	 */
	private static String getLinkFromLogicalRoot(LogicalStructMap logMap) {
		LogicalDiv logRoot = logMap.getDivContainer(); 
		if (logRoot.getType().equals("monograph")) {
			return logRoot.getDmdId();
		}
		// hacky way to overcome semantics METS layout with MVW
		return null;
	}
	
	private String getLinkFromFirstVolume() {
		return this.handler.requestDMDSubDivIDs();
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
