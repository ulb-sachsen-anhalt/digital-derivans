package de.ulb.digital.derivans.data.mets;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

import de.ulb.digital.derivans.Derivans;
import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.data.IMetadataStore;
import de.ulb.digital.derivans.data.io.JarResource;
import de.ulb.digital.derivans.data.xml.XMLHandler;
import de.ulb.digital.derivans.model.DerivateStruct;
import de.ulb.digital.derivans.model.DigitalStructureTree;

/**
 * 
 * Encapsulate METS format
 * 
 * @author u.hartwig
 * 
 */
public class METS {

	private static final Logger LOGGER = LogManager.getLogger(METS.class);

	public static final Namespace NS_METS = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");
	public static final Namespace NS_MODS = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");
	public static final Namespace NS_XLINK = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");

	// mark missing data
	public static final String UNSET = "n.a.";

	public static final String STRUCT_PHYSICAL_ROOT = "physroot";
	static final String METS_STRUCTMAP_TYPE_LOGICAL = "LOGICAL";
	static final String DMD_ID = "DMDID";
	static final String METS_CONTAINER = "div";
	static final String METS_CONTAINER_ID = "ID";
	static final String METS_STRUCTMAP_TYPE = "TYPE";

	public static final DateTimeFormatter MD_DT_FORMAT = new DateTimeFormatterBuilder()
			.appendPattern("YYYY-MM-dd")
			.appendLiteral('T')
			.appendPattern("HH:mm:SS")
			.toFormatter();

	private static List<Namespace> namespaces = List.of(NS_METS, NS_MODS, NS_XLINK);

	private Path file;

	private Document document;

	private XMLHandler xmlHandler;

	private String primeId;

	private MODS primeMods;

	// private METSContainer logicalRoot;

	private Element primeLog;

	private Map<String, List<METSFile>> files = new LinkedHashMap<>();

	// private List<METSContainer> logContainers = new ArrayList<>();

	// private List<METSContainer> phyContainers = new ArrayList<>();

	// public List<METSContainer> getPhyContainers() {
	// return phyContainers;
	// }

	// private Map<String, Integer> pageOrders = new LinkedHashMap<>();

	private METSContainer structure;

	// private AtomicInteger currOrder = new AtomicInteger(1);

	private Map<String, List<Integer>> logicalOrder = new LinkedHashMap<>();

	public METS(Path metsfile) throws DigitalDerivansException {
		this.file = metsfile;
		this.xmlHandler = new XMLHandler(file);
		this.document = this.xmlHandler.getDocument();
	}

	/**
	 * Resolve identifier for descriptive section.
	 * <ol>
	 * <li>1st, look for simple cases like mongraphs or volumes</li>
	 * <li>2nd, inspect the structural Linkings</li>
	 * </ol>
	 */
	public void determine() throws DigitalDerivansException {
		Optional<String> optRoot = this.oneRoot();
		if (optRoot.isPresent()) {
			this.primeId = optRoot.get();
		} else {
			this.primeId = this.calculatePrimeMODSId();
		}
		String xpr = String.format("//mets:dmdSec[@ID='%s']//mods:mods", primeId);
		List<Element> modsSecs = this.evaluate(xpr);
		if (modsSecs.size() != 1) {
			throw new DigitalDerivansException("can't identify primary MODS section using " + primeId);
		}
		var primeElement = modsSecs.get(0);
		this.primeMods = new MODS(this.primeId, primeElement);
	}

	private Optional<String> oneRoot() throws DigitalDerivansException {
		List<Element> physRoot = this.evaluate("//mets:div[@ID='physroot']");
		if (physRoot.size() == 1) {
			var rootLinks = this.evaluate("//*[name()='mets:smLink' and @*='physroot']");
			if (rootLinks.size() == 1) {
				var backLink = rootLinks.get(0).getAttributeValue("from", METS.NS_XLINK);
				var xpr = String.format("//mets:div[@ID='%s']", backLink);
				var logDivs = this.evaluate(xpr);
				if (logDivs.size() == 1) {
					this.primeLog = logDivs.get(0);
					var dmdId = primeLog.getAttributeValue("DMDID");
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
		var metsDivs = this.evaluate("//mets:structMap//mets:div[@DMDID and @TYPE]");
		if (metsDivs.size() == 1) {
			var primeDiv = metsDivs.get(0);
			if (primeDiv.hasAttributes()) {
				String dmdId = primeDiv.getAttributeValue("DMDID");
				if (dmdId != null) {
					this.primeLog = primeDiv;
					return dmdId;
				}
			}
		}
		for (var metsDiv : metsDivs) {
			var type = metsDiv.getAttributeValue("TYPE");
			var containerType = METSContainerType.forLabel(type);
			if (METSContainerType.isObject(containerType)) {
				this.primeLog = metsDiv;
				return metsDiv.getAttributeValue("DMDID");
			}
		}
		throw new DigitalDerivansException("Can't determine mets:div with DMD identifier in " + this.getPath());
	}

	public void setStructure() throws DigitalDerivansException {
		this.setContainer();
		this.setPages();
	}

	public void setFiles(String useGroup) throws DigitalDerivansException {
		var query = "//mets:fileGrp[@USE]";
		if (useGroup != null) {
			query = String.format("//mets:fileGrp[@USE='%s']", useGroup);
		}
		List<Element> fileGroups = this.evaluate(query);
		for (var group : fileGroups) {
			var fileGroup = group.getAttributeValue("USE");
			var theFiles = group.getChildren("file", NS_METS);
			for (var aFile : theFiles) {
				var pMimeType = aFile.getAttributeValue("MIMETYPE");
				var fId = aFile.getAttributeValue("ID");
				for (var fLoc : aFile.getChildren("FLocat", METS.NS_METS)) {
					var fRef = fLoc.getAttributeValue("href", METS.NS_XLINK);
					var f = new METSFile(fileGroup, fId, pMimeType, fRef);
					this.files.computeIfAbsent(fileGroup, k -> new ArrayList<METSFile>());
					this.files.computeIfPresent(fileGroup, (k, v) -> {
						v.add(f);
						return v;
					});
				}
			}
		}
	}

	/**
	 * With respect to DFG METS smLink mechanics and two different
	 * structural container types (physical + logical)
	 * 
	 * @param metsFile
	 * @throws DigitalDerivansException
	 */
	private void linkFile(METSFile metsFile) throws DigitalDerivansException {
		String fileId = metsFile.getFileId();
		List<Element> firstElements = this.evaluate(String.format("//mets:fptr[@FILEID='%s']", fileId));
		if (firstElements.size() != 1) {
			throw new DigitalDerivansException("File " + fileId + " invalid linked to " + firstElements);
		}
		var firstElementId = firstElements.get(0).getParentElement().getAttributeValue("ID");
		var firstContainer = this.getContainerForId(firstElementId);
		metsFile.addLinkedContainers(firstContainer);
		List<Element> linkElements = this.evaluate(String.format("//mets:smLink[@xlink:to='%s']", firstElementId));
		if (linkElements.isEmpty()) {
			throw new DigitalDerivansException("Page " + firstElementId + " not linked to any logical section!");
		}
		for (var e : linkElements) {
			String elementId = e.getAttributeValue("from", METS.NS_XLINK);
			var cnt = this.getContainerForId(elementId);
			metsFile.addLinkedContainers(cnt);
		}
	}

	public Path getPath() {
		return this.file;
	}

	public List<Element> evaluate(String xpathXpr) throws DigitalDerivansException {
		return this.xmlHandler.evaluate(xpathXpr, namespaces);
	}

	public static List<Element> evaluate(String xpath, Document document) throws DigitalDerivansException {
		return XMLHandler.evaluate(xpath, namespaces, document);
	}

	public MODS getPrimeMODS() {
		return this.primeMods;
	}

	// /**
	// *
	// * 2023-11-29
	// *
	// * Due conflicts with Kitodo3 Export XML ensure
	// * that agent is inserted as very first element
	// * if no other agent tags present or as last
	// * agent entry
	// *
	// * @param fileId
	// * @return
	// */
	// public String addAgent(String agentLabel, String agentNoteText) {
	// Element agent = createAgentSection(agentLabel, agentNoteText);
	// Element hdrSection = getMetsHdr();
	// var agents = hdrSection.getChildren("agent", NS_METS);
	// if (agents.isEmpty()) {
	// hdrSection.addContent(0, agent);
	// } else {
	// hdrSection.addContent(agents.size() - 1, agent);
	// }
	// return agent.getChildText("note", NS_METS);
	// }

	// private Element createAgentSection(String agentLabel, String agentNoteText) {
	// Element agent = new Element("agent", NS_METS);
	// agent.setAttribute(METS_STRUCTMAP_TYPE, "OTHER");
	// agent.setAttribute("ROLE", "OTHER");
	// agent.setAttribute("OTHERTYPE", "SOFTWARE");
	// Element agentName = new Element("name", NS_METS);
	// agentName.setText(agentLabel);
	// Element agentNote = new Element("note", NS_METS);
	// agentNote.setText(agentNoteText);
	// agent.addContent(List.of(agentName, agentNote));
	// return agent;
	// }

	public List<METSFile> getFiles(String groupLabel) {
		if (this.files.containsKey(groupLabel)) {
			return this.files.get(groupLabel);
		}
		return new ArrayList<>();
	}

	public Map<String, List<METSFile>> getFiles() {
		return this.files;
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
		this.xmlHandler.write(this.file);
		return agentNoteText;
	}

	private void addFileSection(Element asElement) {
		var cs = document.getContent();
		Element r = (Element) cs.get(0);
		r.getChildren("fileSec", NS_METS).get(0).addContent(asElement);
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
	 * @param noteText
	 * @return
	 */
	public String enrichAgent(String noteText) {
		Element agent = this.createAgentSection(noteText);
		Element hdrSection = this.getMetsHdr();
		var agents = hdrSection.getChildren("agent", NS_METS);
		if (agents.isEmpty()) {
			hdrSection.addContent(0, agent);
		} else {
			hdrSection.addContent(agents.size() - 1, agent);
		}
		return agent.getChildText("note", NS_METS);
	}

	/**
	 * 
	 * Get metsHdr for given Metadata Document or create new one, if not existing.
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

	private Element createAgentSection(String note) {
		Element agent = new Element("agent", NS_METS);
		agent.setAttribute(METS_STRUCTMAP_TYPE, "OTHER");
		agent.setAttribute("ROLE", "OTHER");
		agent.setAttribute("OTHERTYPE", "SOFTWARE");
		Element agentName = new Element("name", NS_METS);
		agentName.setText(this.readRevisionProperties().orElse(Derivans.LABEL));
		Element agentNote = new Element("note", NS_METS);
		agentNote.setText(note);
		agent.addContent(List.of(agentName, agentNote));
		return agent;
	}

	private Optional<String> readRevisionProperties() {
		String fileName = "derivans-git.properties";
		var optVersion = new JarResource().derivansVersion(fileName);
		if (optVersion.isPresent()) {
			return Optional.of(Derivans.LABEL + " V" + optVersion.get());
		}
		return Optional.empty();
	}

	/**
	 * 
	 * If valid METS/MODS source metadata present,
	 * use it's logical information
	 * to build structural representation
	 * 
	 * @return
	 * @throws DigitalDerivansException
	 */
	private void setContainer() throws DigitalDerivansException {
		if (this.file != null) {
			List<Element> firstChilds = this.evaluate("//mets:structMap[@TYPE='LOGICAL']/mets:div");
			if (firstChilds.size() != 1) {
				throw new DigitalDerivansException("Invalid structMap@TYPE=LOGICAL!");
			}
			Element rootElement = firstChilds.get(0);
			String rootId = rootElement.getAttributeValue("ID");
			METSContainer rootCnt = new METSContainer(rootId, rootElement);
			this.structure = rootCnt;
			// this.logContainers.add(rootCnt);
			List<Element> childElements = METS.evaluate(String.format("//mets:div[@ID='%s']/mets:div", rootId),
					rootElement.getDocument());
			for (var child : childElements) {
				this.processContainer(rootCnt, child);
			}
			// ///////////////////////////////
			// // VALIDATION SECTION ???????
			// // review pageNr and pageOrder
			// // var suspects = checkPageOrder(theRoot);
			// // if (!suspects.isEmpty()) {
			// // var excMessage = String.format("%s", suspects);
			// // throw new DigitalDerivansException(excMessage);
			// // }
			// // // review invalid page links
			// // clearInvalidPageLinks(theRoot);
			// // // review redundant page links
			// // clearRedundantPageLinks(theRoot);
			// return theRoot;
		}

	}

	public METSContainer getLogicalRoot() {
		return new METSContainer(this.primeLog);
	}

	private void processContainer(METSContainer currentParent, Element currentElement) throws DigitalDerivansException {
		String currentId = currentElement.getAttributeValue("ID");
		METSContainer currentCnt = new METSContainer(currentId, currentElement);
		currentCnt.setParent(currentParent);
		currentParent.addChild(currentCnt);
		// this.logContainers.add(currentCnt);
		List<Element> granElements = METS.evaluate(String.format("//mets:div[@ID='%s']/mets:div", currentId),
				currentElement.getDocument());
		for (var granElement : granElements) {
			this.processContainer(currentCnt, granElement);
		}
	}

	// public List<METSContainer> getLogContainers() {
	// return this.logContainers;
	// }

	/**
	 * Cruical part to resolve internal links between logical structs and
	 * physical file assets in DFG-flavour via mets:smLink relations
	 * with intermedia mets:div@TYPE="page" elements
	 * 
	 * Assumes correct ordering using mets:smLink => use first linked
	 * file asset as "landing image" for given logical section
	 * 
	 * do *not* trust mets:div@TYPE="page" attribute @ORDER => might be corrupt
	 * 
	 * @throws Exception
	 * 
	 */
	private void setPages() throws DigitalDerivansException {
		// for (var cnt : this.logContainers) {
		// var queryLinks =
		// String.format("//mets:structLink/mets:smLink[@xlink:from='%s']",
		// cnt.getId());
		// List<Element> smLinks = this.evaluate(queryLinks);
		// for (var smLink : smLinks) {
		// var pageId = smLink.getAttributeValue("to", METS.NS_XLINK);
		// if (STRUCT_PHYSICAL_ROOT.equalsIgnoreCase(pageId)) {
		// continue;
		// }
		// List<Element> pages = this.evaluate(String.format("//mets:div[@ID='%s']",
		// pageId));
		// for (var page : pages) {
		// var pageOrder = page.getAttributeValue("ORDER");
		// var theOrder = Integer.valueOf(pageOrder);
		// METSContainer phyCnt = new METSContainer(pageId, page);
		// // var expOrder = this.currOrder.getAndIncrement();
		// // if (theOrder != expOrder) {
		// // LOGGER.error("overwrite read @ORDER={} with expected:{}", theOrder,
		// // expOrder);
		// // theOrder = expOrder;
		// // phyCnt.addAttribute(METSContainerAttributeType.ORDER,
		// // String.valueOf(expOrder));
		// // }
		// // ensure each page is only added once even if
		// // linked several times
		// if (!this.phyContainers.contains(phyCnt)) {
		// this.phyContainers.add(phyCnt);
		// }
		// this.logicalOrder.computeIfAbsent(cnt.getId(), k -> new
		// ArrayList<>()).add(theOrder);
		// var orders = this.logicalOrder.get(cnt.getId());
		// orders.add(theOrder); // prevent "theOrder must be effectively final in
		// scope"
		// }
		// }
		// }
	}

	private METSContainer getContainerForId(String containerId) throws DigitalDerivansException {
		List<METSContainer> allContainer = new ArrayList<>();
		// allContainer.addAll(this.phyContainers);
		List<METSContainer> matches = allContainer.stream()
				.filter(c -> containerId.equals(c.getId()))
				.collect(Collectors.toList());
		if (matches.size() != 1) {
			throw new DigitalDerivansException("Invalid matches " + matches + " for " + containerId);
		}
		return matches.get(0);
	}

	public boolean write() {
		return this.xmlHandler.write(this.file);
	}

	// public DigitalStructureTree getLogicalStructure() throws
	// DigitalDerivansException {
	// var q = String.format("//mets:div[@DMDID='%s']", this.primeId);
	// List<Element> logRoot = this.evaluate(q);
	// String typeLabel = this.primeLog.getAttributeValue("TYPE");
	// var t = METSContainerType.forLabel(typeLabel);

	// return null;
	// }

	/**
	 * 
	 * Get any files, which are linked to given logical container.
	 * 
	 * Uses METS 1.12 DFG interlinking flavor:
	 * div => smLink => div@TYPE=page => file
	 * 
	 * If no files linked to given container, yield exception:
	 * There must not be an empty logical section!
	 * 
	 * @param div
	 * @param fileGroup
	 * @return
	 * @throws DigitalDerivansException
	 */
	public List<METSFile> getFiles(METSContainer div, String fileGroup, String fileExt)
			throws DigitalDerivansException {
		String logID = div.getId();
		String q01 = String.format("//mets:structLink/mets:smLink[@xlink:from='%s']", logID);
		List<Element> smLinks = this.evaluate(q01);
		List<METSFile> metsFiles = new ArrayList<>();
		for (Element smLink : smLinks) {
			var pageId = smLink.getAttributeValue("to", METS.NS_XLINK);
			List<Element> pages = this.evaluate(String.format("//mets:div[@ID='%s']", pageId));
			for (var page : pages) {
				METSContainer pageContainer = new METSContainer(page);
				String cid = pageContainer.getAttribute("CONTENTIDS");
				String pageLabel = pageContainer.determineLabel();
				var allFiles = page.getChildren("fptr", METS.NS_METS);
				for (var aFile : allFiles) {
					String ptrId = aFile.getAttributeValue("FILEID");
					String q02 = String.format("//mets:fileGrp[@USE='%s']/mets:file[@ID='%s']", fileGroup, ptrId);
					List<Element> filesFromGroup = this.evaluate(q02);
					if (filesFromGroup.isEmpty()) {
						continue;
					}
					Element fileFromGroup = filesFromGroup.get(0);
					String fileId = fileFromGroup.getAttributeValue("ID");
					var fstLocat = fileFromGroup.getChildren("FLocat", METS.NS_METS).get(0);
					String hRef = fstLocat.getAttributeValue("href", METS.NS_XLINK);
					var thaFile = new METSFile(fileId, hRef);
					thaFile.setPageLabel(pageLabel);
					thaFile.setLocalRoot(this.getPath().getParent());
					if (cid != null) {
						thaFile.setContentIds(cid);
					}
					metsFiles.add(thaFile);
				}
			}
		}
		if (metsFiles.isEmpty()) {
			String alert = String.format("No files link div %s/%s!",
					logID, div.determineLabel());
			throw new DigitalDerivansException(alert);
		}
		return metsFiles;
	}

	public void setIdentifierExpression(String xpressiob) {
		throw new UnsupportedOperationException("Unimplemented method 'setIdentifierExpression'");
	}

	public void enrichPDF(String identifier) throws DigitalDerivansException {
		String mimeType = "application/pdf";
		String fileGroup = "DOWNLOAD";
		LOGGER.info("enrich pdf '{}' as '{}' in '{}'", identifier, mimeType, fileGroup);
		String fileId = "PDF_" + identifier;
		var resultText = this.addDownloadFile("DOWNLOAD", fileId, "application/pdf");
		LOGGER.info("integrated pdf fileId '{}' in '{}'", fileId, this.file);
		LOGGER.info("integrated mets:agent {}", resultText);
	}

}
