package de.ulb.digital.derivans.data.mets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.ElementFilter;
import org.jdom2.util.IteratorIterable;

import org.mycore.mets.model.Mets;

import de.ulb.digital.derivans.Derivans;
import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.data.xml.XMLHandler;

/**
 * 
 * Handle Metadata for derivates generation on Path-and DOM-Level
 * 
 * @author hartwig
 *
 */
public class METSHandler {

	private static final String METS_STRUCTMAP_TYPE_LOGICAL = "LOGICAL";
	static final String METS_STRUCTMAP_TYPE = "TYPE";
	public static final Namespace NS_METS = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");
	public static final Namespace NS_MODS = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");
	public static final Namespace NS_XLINK = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");

	private static List<Namespace> namespaces = List.of(NS_METS, NS_MODS, NS_XLINK);

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

	private XMLHandler handler;

	private Mets mets;

	private MODS primeMods;

	public METSHandler(Path pathFile) throws DigitalDerivansException {
		this.pathFile = pathFile;
		this.handler = new XMLHandler(pathFile);
		this.document = handler.getDocument();
		try {
			this.mets = new Mets(this.document);
		} catch (IllegalArgumentException e) {
			throw new DigitalDerivansException(e);
		}
		this.setPrimeMods();
	}

	public Mets getMets() {
		return this.mets;
	}

	/**
	 * Resolve identifier for descriptive section.
	 * <ol>
	 * <li>1st, look for simple cases like mongraphs or volumes</li>
	 * <li>2nd, inspect the structural Linkings</li>
	 * </ol>
	 */
	public void setPrimeMods() throws DigitalDerivansException {
		Optional<String> optRoot = this.oneRoot();
		String primeId = null;
		if (optRoot.isPresent()) {
			primeId = optRoot.get();
		} else {
			primeId = this.calculatePrimeMODSId();
		}
		String xpr = String.format("//mets:dmdSec[@ID='%s']//mods:mods", primeId);
		List<Element> modsSecs = this.evaluate(xpr);
		if (modsSecs.size() != 1) {
			throw new DigitalDerivansException("can't identify primary MODS section using " + primeId);
		}
		var primeElement = modsSecs.get(0);
		this.primeMods = new MODS(primeId, primeElement);
	}

	private Optional<String> oneRoot() throws DigitalDerivansException {
		List<Element> physRoot = this.evaluate("//mets:div[@ID='physroot']");
		if (physRoot.size() == 1) {
			var rootLinks = this.evaluate("//*[name()='mets:smLink' and @*='physroot']");
			if (rootLinks.size() == 1) {
				var backLink = rootLinks.get(0).getAttributeValue("from", METSHandler.NS_XLINK);
				var xpr = String.format("//mets:div[@ID='%s']", backLink);
				var logDivs = this.evaluate(xpr);
				if (logDivs.size() == 1) {
					var dmdId = logDivs.get(0).getAttributeValue("DMDID");
					return Optional.of(dmdId);
				}
			}
		}
		return Optional.empty();
	}

	/**
	 * 
	 * Select from containers having @DMDID an @TYPE
	 * first match that is of typical digital object @TYPE
	 * like "monograph", "volume" or "issue"
	 * 
	 * Stop further investigation if only one container
	 * with both attributes exists
	 * 
	 * Yield Exception if nothing found
	 * 
	 * @return
	 * @throws DigitalDerivansException
	 */
	private String calculatePrimeMODSId() throws DigitalDerivansException {
		var dmdSects = this.evaluate("//mets:structMap//mets:div[@DMDID and @TYPE]");
		if (dmdSects.size() == 1) {
			var dmdOne = dmdSects.get(0);
			if (dmdOne.hasAttributes()) {
				String dmdId = dmdOne.getAttributeValue("DMDID");
				if (dmdId != null) {
					return dmdId;
				}
			}
		}
		for (var sect : dmdSects) {
			var type = sect.getAttributeValue("TYPE");
			var containerType = METSContainerType.forLabel(type);
			if (METSContainerType.isObject(containerType)) {
				return sect.getAttributeValue("DMDID");
			}
		}
		throw new DigitalDerivansException("Can't determine DMD identifier in " + this.getPath());
	}

	public Element evaluateFirst(String xpathStr) throws DigitalDerivansException {
		return this.handler.evaluateFirst(xpathStr, List.of(NS_METS, NS_MODS));
	}

	public List<Element> evaluate(String xpathXpr) throws DigitalDerivansException {
		return this.handler.evaluate(xpathXpr, namespaces);
	}

	public static List<Element> evaluate(String xpath, Document document) throws DigitalDerivansException {
		return XMLHandler.evaluate(xpath, namespaces, document);
	}

	public MODS getPrimeMODS() {
		return this.primeMods;
	}

	public boolean write() {
		return this.handler.write(this.pathFile);
	}

	/**
	 * 
	 * 2023-11-29
	 * 
	 * Due conflicts with Kitodo3 Export XML ensure
	 * that agent is inserted as very first element
	 * if no other agent tags present or as last
	 * agent entry
	 * 
	 * @param fileId
	 * @return
	 */
	public String enrichAgent(String fileId) {
		Element agent = createAgentSection(fileId);
		Element hdrSection = getMetsHdr();
		var agents = hdrSection.getChildren("agent", NS_METS);
		if (agents.isEmpty()) {
			hdrSection.addContent(0, agent);
		} else {
			hdrSection.addContent(agents.size() - 1, agent);
		}
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

	/**
	 * @param useGroup
	 * @param identifier
	 * @param mimeType
	 * @throws DigitalDerivansException
	 */
	public String addDownloadFile(String useGroup, String identifier, String mimeType) throws DigitalDerivansException {
		Element fileElement = new METSFile(useGroup, identifier, mimeType, identifier + ".pdf").asElement();
		// attach or re-use existing group
		var existingGroups = this.evaluate(String.format("//mets:fileGrp[@USE='%s']", useGroup));
		Element fileGrp = null;
		if (existingGroups.size() == 1) {
			fileGrp = existingGroups.get(0);
		} else {
			fileGrp = new Element("fileGrp", NS_METS);
			fileGrp.setAttribute("USE", useGroup);
			this.addFileSection(fileGrp);
		}
		fileGrp.addContent(fileElement);
		// add agent mark
		String ts = LocalDateTime.now().format(MD_DT_FORMAT);
		String agentNoteText = "PDF FileGroup for " + identifier + " created at " + ts;
		this.enrichAgent(agentNoteText);
		// link as fptr to logical section
		var pdfFPtr = new Element("fptr", NS_METS);
		pdfFPtr.setAttribute("FILEID", identifier);
		var parent = this.evaluate(String.format("//mets:div[@DMDID='%s']", this.primeMods.getId())).get(0);
		parent.addContent(0, pdfFPtr);
		// store changes
		this.handler.write(this.pathFile);
		return agentNoteText;
	}

	private void addFileSection(Element asElement) {
		var cs = document.getContent();
		Element r = (Element) cs.get(0);
		r.getChildren("fileSec", NS_METS).get(0).addContent(asElement);
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

	/**
	 * 
	 * Catch logical sub-containers.
	 * 
	 * Containers with attribute DMDID must have
	 * a descriptive MODS section.
	 * 
	 * @param includeAllSubcontainers additionally
	 *                                include ancestors with only ID attribute
	 *                                (i.e., without custom descriptive metadata)
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
	 * _all_ elements that have an {@link de.ulb.digital.derivans.data.mets.ID}
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
			if (!METSHandler.METS_CONTAINER.equals(el.getName())) {
				return null;
			}
			if (!METSHandler.NS_METS.equals(el.getNamespace())) {
				return null;
			}
			boolean hasDMDID = el.getAttribute(METSHandler.DMD_ID) != null;
			boolean hasID = el.getAttribute(METSHandler.METS_CONTAINER_ID) != null;
			if (hasDMDID || hasID) {
				return el;
			}
		}
		return null;
	}
}
