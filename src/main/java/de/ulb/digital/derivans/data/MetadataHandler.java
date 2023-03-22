package de.ulb.digital.derivans.data;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingInt;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map.Entry;

import javax.xml.XMLConstants;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.util.IteratorIterable;
import org.mycore.mets.model.Mets;
import org.mycore.mets.model.sections.DmdSec;
import org.mycore.mets.model.struct.SmLink;

import de.ulb.digital.derivans.Derivans;
import de.ulb.digital.derivans.DigitalDerivansException;

/**
 * 
 * Handle Metadata for derivates generation on Path-and DOM-Level
 * 
 * @author hartwig
 *
 */
public class MetadataHandler {

	private static final String METS_STRUCTMAP_TYPE_LOGICAL = "LOGICAL";
	static final String METS_STRUCTMAP_TYPE = "TYPE";
	public static final Namespace NS_METS = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");
	public static final Namespace NS_MODS = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");

	public static final DateTimeFormatter MD_DT_FORMAT = new DateTimeFormatterBuilder()
			.appendPattern("YYYY-MM-dd")
			.appendLiteral('T')
			.appendPattern("HH:mm:SS")
			.toFormatter();

	static final String DMD_ID = "DMDID";
	static final String METS_CONTAINER = "div";
	static final String METS_CONTAINER_ID = "ID";
	static final String METS_TYPE = "TYPE";
	static final String METS_CONTAINER_PHYS_ROOT = "physroot";
	static final String KITDODO2_DEFAULT_FIRST_CHILD_ID = "LOG_0001";
	static final String KITDODO2_DEFAULT_PARENT_ID = "LOG_0000";

	private Path pathFile;

	private Document document;

	private Mets mets;

	private Element primaryMods;

	public MetadataHandler(Path pathFile) throws DigitalDerivansException {
		this.pathFile = pathFile;
		try {
			this.mets = read();
			this.primaryMods = setPrimaryMods();
		} catch (JDOMException | IOException | IllegalArgumentException | DigitalDerivansException e) {
			throw new DigitalDerivansException(e);
		}
	}

	public Mets getMets() {
		return this.mets;
	}

	private Mets read() throws JDOMException, IOException, IllegalArgumentException {
		File f = new File(this.pathFile.toString());
		SAXBuilder builder = new SAXBuilder();
		// please sonarqube "Disable XML external entity (XXE) processing"
		builder.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		document = builder.build(f);
		return new Mets(document);
	}

	public boolean write() {
		try (OutputStream metsOut = Files.newOutputStream(this.pathFile)) {
			XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
			xout.output(document, metsOut);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public String enrichAgent(String fileId) {
		Element agent = createAgentSection(fileId);
		Element hdrSection = getMetsHdr();
		hdrSection.addContent(agent);
		return agent.getChildText("note", NS_METS);
	}

	private Element createAgentSection(String fileId) {
		Element agent = new Element("agent", NS_METS);
		agent.setAttribute(METS_STRUCTMAP_TYPE, "OTHER");
		agent.setAttribute("ROLE", "OTHER");
		agent.setAttribute("OTHERTYPE", "SOFTWARE");
		Element agentName = new Element("name", NS_METS);
		agentName.setText(this.getLabel());
		Element agentNote = new Element("note", NS_METS);
		String ts = LocalDateTime.now().format(MD_DT_FORMAT);
		String agentNoteText = "PDF FileGroup for " + fileId + " created at " + ts;
		agentNote.setText(agentNoteText);
		agent.addContent(List.of(agentName, agentNote));
		return agent;
	}

	/**
	 * 
	 * Read version label from properties-file if exists, otherwise use hard-coded
	 * default value
	 * 
	 * @return
	 */
	public String getLabel() {
		return readRevisionProperties().orElse(Derivans.LABEL);
	}

	
	public Element getPrimaryMods() {
		return this.primaryMods;
	}
	
	private Optional<String> readRevisionProperties() {
		String fileName = "derivans-git.properties";
		ClassLoader classLoader = getClass().getClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream(fileName);
		StringBuilder resultStringBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.strip().length() > 3) {
					resultStringBuilder.append(line);
				}
			}
			String contents = resultStringBuilder.toString();
			String[] parts = contents.split(":");
			if (parts.length > 1) {
				String version = parts[1].replace('"', ' ').strip();
				return Optional.of(Derivans.LABEL + " V" + version);
			}
		} catch (IOException e) {
			Derivans.LOGGER.warn("cannot read {}", fileName);
		}
		return Optional.empty();
	}

	public Path getPath() {
		return pathFile;
	}

	/**
	 * 
	 * Get metsHdr for given Metadata Document or create new one, if not existing.
	 * 
	 * Since mets-model library wipes existing metsHdr-Information, this information
	 * must be kept otherwise
	 * 
	 * @return
	 */
	private Element getMetsHdr() {
		List<Content> cs = document.getContent();
		Element r = (Element) cs.get(0);
		List<Element> es = r.getChildren();
		for (Element e : es) {
			if (e.getName().equalsIgnoreCase("metsHdr")) {
				return e;
			}
		}

		Element hdrSection = new Element("metsHdr", NS_METS);
		hdrSection.setAttribute("CREATEDATE", Instant.now().toString());
		this.document.addContent(hdrSection);
		return hdrSection;
	}

	public void addFileGroup(Element asElement) {
		var cs = document.getContent();
		Element r = (Element) cs.get(0);
		r.getChildren("fileSec", NS_METS).get(0).addContent(asElement);
	}

	/**
	 * 
	 * Add single element to topLevel METS-element identified by given valueType
	 * 
	 * @param asElement
	 * @param typeValue
	 * @param first
	 */
	public void addTo(Element asElement, boolean reorder) {
		IteratorIterable<Element> elements = document.getDescendants(new MetsDivFilter());
		String id = primaryMods.getParentElement().getParentElement().getParentElement().getAttributeValue(METS_CONTAINER_ID);
		Element logDiv = null;
		for (Element e : elements) {
			if (id.equals(e.getAttributeValue(DMD_ID)))
				logDiv = e;
		}
		if (logDiv != null) {
			logDiv.addContent(asElement);
			// ATTENTION: the specific goal to re-order is inversion
			// to ensure mets:fptr is *before* any subsequent mets:divs
			if (reorder) {
				logDiv.sortChildren((el1, el2) -> Math.negateExact(el1.getName().compareToIgnoreCase(el2.getName())));
			}
		}
	}

	/**
	 * 
	 * Catch logical sub-containers.
	 * 
	 * Containers with attribute DMDID must have 
	 * a descriptive MODS section.
	 * 
	 * @param includeAllSubcontainers additionally 
	 * 		include ancestors with only ID attribute
	 * 		(i.e., without custom descriptive metadata) 
	 * @return
	 */
	public List<Element> requestSubcontainers(boolean includeAllSubcontainers) {
		Element logRoot = extractLogicalRoot();
		IteratorIterable<Element> iter = logRoot.getDescendants(new LogSubContainers(includeAllSubcontainers));
		List<Element> subContainers = new ArrayList<>();
		for (Element el : iter) {
			subContainers.add(el);
		}
		return subContainers;
	}

	private Element extractLogicalRoot() {
		var elements = document.getContent(new ElementFilter());
		return elements.stream()
				.map(Element::getChildren)
				.flatMap(List::stream)
				.filter(el -> METS_STRUCTMAP_TYPE_LOGICAL.equals(el.getAttributeValue(METS_STRUCTMAP_TYPE)))
				.findFirst().orElseThrow();
	}

	/**
	* Resolve identifier for descriptive section. 
	* 
	* <ol>
	* 	<li>1st, look for simple cases like mongraphs or volumes</li>
	* 	<li>2nd, inspect the structural Linkings</li>
	* </ol>
	*/
	private Element setPrimaryMods() throws DigitalDerivansException {
		String dmdId = getSimple().orElse(calculatePrimeModsIdentifier());
		DmdSec primaryDMD = mets.getDmdSecById(dmdId);
		if (primaryDMD == null) {
			throw new DigitalDerivansException("can't identify primary MODS section");
		}
		Iterable<Element> iter = primaryDMD.asElement().getDescendants(new ModsFilter());
		return iter.iterator().next();
	}

	/**
	 * 
	 * Starting from Physical Top-Level container
	 * pick the most reasonable link and use
	 * this to identify the proper logical type.
	 * 
	 * CAUTION
	 * Review of mets-model parsing necessary since it doesn't read
	 * properly all Element's Attributes like "DMDID"
	 * 
	 * @return
	 * @throws DigitalDerivansException 
	 */
	private String calculatePrimeModsIdentifier() throws DigitalDerivansException {
		String logId = getContainerIDWithMostLinkings();
		List<Element> logSubcontainers = this.requestSubcontainers(false);
		for (Element e : logSubcontainers) {
			if (e.getAttributeValue(METS_CONTAINER_ID).equals(logId)) {
				return e.getAttributeValue(DMD_ID);
			}
		}
		throw new DigitalDerivansException("Can't determine DMD identifier for "+ this.getPath());
	}

	private Optional<String> getSimple() {
		var optElement = this.mets.getLogicalStructMap().asElement().getContent(new TypeFilter()).stream().findFirst();
		if (optElement.isPresent()) {
			var el = optElement.get();
			if (el.getAttribute(DMD_ID) != null) {
				return Optional.of(el.getAttributeValue(DMD_ID));
			}
		}
		return Optional.empty();
	}

	/**
	 * 
	 * Identify the logical root element of any METS/MODS print work.
	 * 
	 * Comes into different flavors:
	 * 
	 * <ul>
	 * 	<li>If the links in the structMapping refer straight to a 
	 * 		"physroot" element, it's considered to be this print's 
	 * 		logical root element, i.e. a monography/volume/issue. 
	 * 		This is like VLS/semantics are doing.</li>
	 * 	<li>Kitodo2 contains an additional linking from each physical 
	 * 		container to the logical root element. To find it, count all 
	 * 		links from logical to physical and get the one with most links.
	 * 		_PLEASE NOTE_:
	 * 		Doesn't hold for K2 MENADOC digis!</li>
	 * 	<li>There's a reasonable exception for Kitodo2, when there's only
	 * 		one logical child in the print, i.e. a print with only
	 * 		a single section/chapter (a chart, a map, ...). 
	 * 		In this case, the logical root and it's only child will 
	 * 		have the same number of references to the physical pages. 
	 * 		Therefore, it's required to determine, which of the two 
	 * 		elements is actually the parent of the other.
	 * 		This element will be the logical root element.</li>
	 * 	</ul>
	 * 
	 * In both cases, follow the link's origin to the logical map.
	 * 
	 * @return
	 * @throws DigitalDerivansException 
	 */
	private String getContainerIDWithMostLinkings() throws DigitalDerivansException {
		List<SmLink> smLinksToPhysRoot = mets.getStructLink().getSmLinkByTo(METS_CONTAINER_PHYS_ROOT);
		if (!smLinksToPhysRoot.isEmpty()) {
			return smLinksToPhysRoot.get(0).getFrom();
		} else {
			List<SmLink> links = mets.getStructLink().getSmLinks();
			// map SmLinks to their @from-count
			Map<String, Integer> mapToN = links.stream()
				.collect(groupingBy(SmLink::getFrom, summingInt(n -> 1)));
			// get identifier of SmLink with MAX counts
			var optMaxLink = mapToN.entrySet().stream().max(Comparator.comparingInt(Entry::getValue));
			if (optMaxLink.isPresent()) {
				var maxMapping = optMaxLink.get();
				// when dealing with small logical structs of 
				// only 1 child container this value needs to be 
				// inspected further because it can't be trusted
				if (mapToN.size() == 2) {
					return this.determine_parent_id(new IDPair(mapToN.keySet()));
				} else {
					return maxMapping.getKey();
				}
			}
		}
		return null;
	}
	
	/**
	 * 
	 * Determine which of the elements referenced by the
	 * provided IDs is a direct child of the other one.
	 * 
	 * @param pair Pair consisting of two ID attribute values
	 * @return parent ID
	 * @throws DigitalDerivansException
	 */
	private String determine_parent_id(IDPair pair) throws DigitalDerivansException {
		var currentId = pair.id1;
		List<Element> logSubcontainers = this.requestSubcontainers(true);
		var currentElement = logSubcontainers.stream()
			.filter(e -> currentId.equals(e.getAttributeValue(METS_CONTAINER_ID)))
			.findFirst().orElseThrow();
		var currentChildIds = currentElement.getChildren(METS_CONTAINER, NS_METS)
			.stream().map(c -> c.getAttributeValue(METS_CONTAINER_ID)).collect(Collectors.toList());
		if (currentChildIds.isEmpty()) {
			return pair.id2;
		} else if (currentChildIds.contains(pair.id2)) {
			return pair.id1;
		}
		throw new DigitalDerivansException("Can't determine parent element for sure from "+ pair);
	}
}

/**
 * 
 * Get all Descendants with attribute "DMDID"
 * 
 * @author u.hartwig
 *
 */
@SuppressWarnings("serial")
class LogSubContainers extends ElementFilter {

	boolean includeIds;

	/**
	 * Default constructor
	 */
	public LogSubContainers() {
	}
	
	/**
	 * 
	 * Construct a different filter which includes also
	 * _all_ elements that have an {@link MetadataHandler.ID} 
	 * attribute set.
	 * 
	 * Be aware: 
	 * This will return literally _any_ logical container element.
	 * 
	 * @param includeAllIds
	 */
	public LogSubContainers(boolean includeAllIds) {
		this.includeIds = includeAllIds;
	}
	
	@Override
	public Element filter(Object content) {
		if (content instanceof Element) {
			Element el = (Element) content;
			if (!MetadataHandler.METS_CONTAINER.equals(el.getName())) {
				return null;
			}
			if (!MetadataHandler.NS_METS.equals(el.getNamespace())) {
				return null;
			}
			boolean hasDMDID = el.getAttribute(MetadataHandler.DMD_ID) != null;
			boolean hasID = el.getAttribute(MetadataHandler.METS_CONTAINER_ID) != null;
			if (hasDMDID || hasID) {
				return el;
			}
		}
		return null;
	}
}

/**
 * 
 * Get the real mods:mods elements
 * 
 * @author u.hartwig
 *
 */
@SuppressWarnings("serial")
class ModsFilter extends ElementFilter {

	@Override
	public Element filter(Object content) {
		if (content instanceof Element) {
			Element el = (Element) content;
			if ("mods".equals(el.getName())) {
				return el;
			}
		}
		return null;
	}

}


/**
 * 
 * Get mets:div Containers with types
 * like 'monography', 'volume', ... , etc
 * 
 * @author u.hartwig
 *
 */
@SuppressWarnings("serial")
class TypeFilter extends ElementFilter {

	static final String[] TYPES = {
		StructureDFGViewer.MONOGRAPH.getLabel(),
		StructureDFGViewer.VOLUME.getLabel(),
		StructureDFGViewer.MANUSCRIPT.getLabel(),
		StructureDFGViewer.ISSUE.getLabel(),
		StructureDFGViewer.ADDITIONAL.getLabel(),
		StructureDFGViewer.ATLAS.getLabel(),
		StructureDFGViewer.MAP.getLabel(),
	};

	@Override
	public Element filter(Object content) {
		if (content instanceof Element) {
			Element el = (Element) content;
			String elType = el.getAttributeValue(MetadataHandler.METS_TYPE);
			for (String t : TYPES) {
				if (t.equals(elType)) {
					return el;
				}
			}
		}
		return null;
	}

}

@SuppressWarnings("serial")
class MetsDivFilter extends ElementFilter {

	@Override
	public Element filter(Object content) {
		if (content instanceof Element) {
			Element el = (Element) content;
			if (MetadataHandler.METS_CONTAINER.equals(el.getName())) {
				return el;
			}
		}
		return null;
	}

}

/**
 * 
 * Little Helper to express the relation parent-child
 * 
 * @author hartwig
 *
 */
class IDPair {
	String id1;
	String id2;
	IDPair(Set<String> ids){
		var arr = ids.toArray(new String[2]);
		this.id1 = arr[0];
		this.id2 = arr[1];
	}
	
	@Override
	public String toString() {
		return id1+","+id2;
	}
}

