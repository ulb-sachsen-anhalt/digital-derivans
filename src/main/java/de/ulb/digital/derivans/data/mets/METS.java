package de.ulb.digital.derivans.data.mets;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import org.jdom2.filter.Filter;
import org.jdom2.filter.Filters;

import de.ulb.digital.derivans.Derivans;
import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.data.io.JarResource;
import de.ulb.digital.derivans.data.mets.parse.METSElementAttributeFilter;
import de.ulb.digital.derivans.data.xml.XMLHandler;
import de.ulb.digital.derivans.IDerivans;

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
	static final String METS_STRUCTMAP_TYPE_PHYSICAL = "PHYSICAL";
	static final String DMD_ID = "DMDID";
	static final String METS_CONTAINER = "div";
	static final String METS_CONTAINER_ID = "ID";
	static final String METS_FILE_ID = "FILEID";
	static final String METS_STRUCTMAP_TYPE = "TYPE";

	private String imgFileGroup = IDerivans.IMAGE_DIR_DEFAULT;

	private String ocrFileGroup = IDerivans.FULLTEXT_DIR;

	public static final DateTimeFormatter MD_DT_FORMAT = new DateTimeFormatterBuilder()
			.appendPattern("YYYY-MM-dd")
			.appendLiteral('T')
			.appendPattern("HH:mm:SS")
			.toFormatter();

	static final String METS_PHYSROOT = "physroot";

	private static List<Namespace> namespaces = List.of(NS_METS, NS_MODS, NS_XLINK);

	private Path file;

	private Document document;

	private XMLHandler xmlHandler;

	private MODS primeMods;

	private Element primeLog;

	private boolean isInited;

	private HashMap<String, METSFile> metsFiles = new LinkedHashMap<>();

	private HashMap<String, METSContainer> physicalContainer = new LinkedHashMap<>();

	private HashMap<String, METSContainer> structuralContainer = new LinkedHashMap<>();

	private HashMap<String, List<String>> smLinks = new LinkedHashMap<>();

	private METSContainer logicalRoot;

	public METS(Path metsfile) throws DigitalDerivansException {
		this.file = metsfile;
		this.xmlHandler = new XMLHandler(file);
		this.document = this.xmlHandler.getDocument();
	}

	public METS(Path metsfile, String imageFileGroup) throws DigitalDerivansException {
		this.file = metsfile;
		this.imgFileGroup = imageFileGroup;
		this.xmlHandler = new XMLHandler(file);
		this.document = this.xmlHandler.getDocument();
	}

	public void setImgFileGroup(String imgFileGroup) {
		this.imgFileGroup = imgFileGroup;
	}

	/**
	 * Resolve identifier for descriptive section.
	 * <ol>
	 * <li>1st, look for simple cases like mongraphs or volumes</li>
	 * <li>2nd, inspect the structural Linkings</li>
	 * </ol>
	 */
	public void init() throws DigitalDerivansException {
		String primeId = this.oneRoot().orElse(this.calculatePrimeMODSId());
		if (this.primeLog == null) {
			throw new DigitalDerivansException("No logical root found at " + this.getPath());
		}
		String xpr = String.format("//mets:dmdSec[@ID='%s']//mods:mods", primeId);
		List<Element> modsSecs = this.evaluate(xpr);
		if (modsSecs.size() != 1) {
			throw new DigitalDerivansException("can't identify primary MODS section using " + primeId);
		}
		var primeElement = modsSecs.get(0);
		this.primeMods = new MODS(primeId, primeElement);
		this.handleFiles();
		this.handleStructLinks();
		this.handleContainers();
		this.logicalRoot = new METSContainer(this.primeLog);
		this.completeHierarchy(this.logicalRoot);
		// this.determineLinkedFiles(this.logicalRoot);
		this.isInited = true;
	}

	/**
	 * 
	 * Handle structural containers (logical and physical [aka pages])
	 * 
	 * @throws DigitalDerivansException
	 */
	private void handleContainers() throws DigitalDerivansException {
		Filter<Element> structMapFilter = Filters.element(NS_METS).refine(Filters.element("structMap", NS_METS));
		Iterator<Element> structMaps = this.document.getDescendants(structMapFilter);
		Element phyStruct = null;
		while (structMaps.hasNext()) {
			Element fgElement = structMaps.next();
			String structType = fgElement.getAttributeValue("TYPE");
			LOGGER.debug("Found structMap of TYPE={}", structType);
			if (structType.equalsIgnoreCase(METS_STRUCTMAP_TYPE_PHYSICAL)) {
				phyStruct = fgElement;
			}
		}
		// get containers below logical root
		Iterator<Element> logDivIt = this.primeLog
				.getDescendants(Filters.element(NS_METS).refine(Filters.element("div", NS_METS)));
		while (logDivIt.hasNext()) {
			Element logDiv = logDivIt.next();
			String theId = logDiv.getAttributeValue("ID");
			METSContainer div = new METSContainer(logDiv);
			for (var fptr : logDiv.getChildren("fptr", NS_METS)) {
				String fileId = fptr.getAttributeValue(METS_FILE_ID);
				if (this.metsFiles.containsKey(fileId)) {
					METSFile metsFile = this.metsFiles.get(fileId);
					div.addFile(metsFile);
				}
			}
			this.structuralContainer.put(theId, div);
		}
		// pages
		if (phyStruct != null) {
			var pageFilter = new METSElementAttributeFilter(METS_CONTAINER, "TYPE", "page");
			Iterator<Element> physDivIt = phyStruct.getDescendants(pageFilter);
			while (physDivIt.hasNext()) {
				Element physDiv = physDivIt.next();
				String theId = physDiv.getAttributeValue("ID");
				METSContainer div = new METSContainer(physDiv);
				for (var fptr : physDiv.getChildren("fptr", NS_METS)) {
					String fileId = fptr.getAttributeValue(METS_FILE_ID);
					if (this.metsFiles.containsKey(fileId)) {
						METSFile metsFile = this.metsFiles.get(fileId);
						div.addFile(metsFile);
					}
				}
				this.structuralContainer.put(theId, div);
			}
		}
	}

	private void completeHierarchy(METSContainer current) throws DigitalDerivansException {
		String containerId = current.getId();
		// get final page containers
		if (this.smLinks.containsKey(containerId)) {
			List<METSContainer> children = current.getChildren();
			List<String> linkedPages = this.smLinks.get(containerId);
			METSContainer previousSibling = null;
			for (String linkTo : linkedPages) {
				if (this.structuralContainer.containsKey(linkTo) &&
						!children.contains(this.structuralContainer.get(linkTo))) {
					METSContainer child = this.structuralContainer.get(linkTo);
					// order matters!
					child.setParent(current);
					current.addChild(child);
				}
			}
			// must be last step since it may move
			// files around from parent to child
			this.clearLinks(current);
		}
		// we only care for non-page children since they can be moved around
		List<METSContainer> realWildChilds = current.getChildren().stream()
				.filter(c -> c.getType() != METSContainerType.PAGE)
				.collect(Collectors.toList());
		for (int i = 0; i < realWildChilds.size(); i++) {
			METSContainer childDiv = realWildChilds.get(i);
			this.completeHierarchy(childDiv);
		}
	}

	private void handleFiles() throws DigitalDerivansException {
		Filter<Element> fileGrpFilter = Filters.element(NS_METS).refine(Filters.element("fileGrp", NS_METS));
		Iterator<Element> fileGrpIterator = this.document.getDescendants(fileGrpFilter);
		Element useImageGrp = null;
		Element groupUseFulltext = null;
		while (fileGrpIterator.hasNext()) {
			Element fgElement = fileGrpIterator.next();
			if (fgElement.getAttributeValue("USE").equals(this.imgFileGroup)) {
				useImageGrp = fgElement;
			}
			if (fgElement.getAttributeValue("USE").equals(this.ocrFileGroup)) {
				groupUseFulltext = fgElement;
			}
		}
		if (useImageGrp == null) {
			throw new DigitalDerivansException("Invalid input mets:fileGrp " + this.imgFileGroup + "!");
		}
		List<Element> files = useImageGrp.getChildren("file", NS_METS);
		for (Element fileElement : files) {
			String fileId = fileElement.getAttributeValue("ID");
			METSFile metsFile = new METSFile(fileElement, this.imgFileGroup);
			metsFile.setLocalRoot(this.getPath().getParent());
			this.metsFiles.put(fileId, metsFile);
		}
		if (groupUseFulltext != null) {
			List<Element> ocrFiles = groupUseFulltext.getChildren("file", NS_METS);
			for (Element ocrFile : ocrFiles) {
				String fileId = ocrFile.getAttributeValue("ID");
				METSFile metsFile = new METSFile(ocrFile, this.ocrFileGroup);
				metsFile.setLocalRoot(this.getPath().getParent());
				this.metsFiles.put(fileId, metsFile);
			}
		}
	}

	private void handleStructLinks() throws DigitalDerivansException {
		Filter<Element> structLinkFilter = Filters.element(NS_METS).refine(Filters.element("structLink", NS_METS));
		Iterator<Element> iter = this.document.getDescendants(structLinkFilter);
		if (!iter.hasNext()) {
			throw new DigitalDerivansException("No structLinks found");
		}
		Element structLink = iter.next();
		List<Element> mapLinks = structLink.getChildren("smLink", NS_METS);
		for (Element smLink : mapLinks) {
			String fromLogical = smLink.getAttributeValue("from", NS_XLINK);
			String toPhysical = smLink.getAttributeValue("to", NS_XLINK);
			if (toPhysical.equalsIgnoreCase(METS_PHYSROOT)) {
				LOGGER.warn("Ignore link from {} to {}", fromLogical, METS_PHYSROOT);
				continue;
			}
			List<String> prevTos = this.smLinks.getOrDefault(fromLogical, new ArrayList<>());
			prevTos.add(toPhysical);
			this.smLinks.put(fromLogical, prevTos);
		}

	}

	/**
	 * 
	 * Remove multiple linkings of page containers from
	 * parent containers further down the tree
	 * if and only if parent exists and has pages linked
	 * 
	 * @param current
	 * @throws DigitalDerivansException
	 */
	private void clearLinks(METSContainer current) {
		METSContainer currentParent = current.getParent();
		if (currentParent == null || currentParent.getChildren().isEmpty()) {
			return;
		}
		// if digitization systems would take care of linking pages and prevent
		// multiple linkings pushing files around like this wouldn't be necessary ...
		int movedFiles = 0;
		List<METSContainer> pageContainer = current.getChildren().stream()
				.filter(c -> c.getType() == METSContainerType.PAGE)
				.collect(Collectors.toList());
		for (METSContainer currentPage : pageContainer) {
			List<METSContainer> parentChildren = currentParent.getChildren();
			for (int i = 0; i < parentChildren.size(); i++) {
				METSContainer parentChild = parentChildren.get(i);
				if (parentChild.getId().equals(currentPage.getId())) {
					currentParent.getChildren().remove(parentChild);
					movedFiles++;
				}
			}
		}
		if (movedFiles > 0) {
			LOGGER.warn("Moved {} page containers from parent {} to child container {}",
					movedFiles, currentParent, current);
		}
	}

	/**
	 * Ensure some invariants of METS structure
	 * 
	 * => All logical sections _must_ link to at least one physical section (page)
	 * 
	 * @throws DigitalDerivansException
	 */
	public void validate() throws DigitalDerivansException {
		if (!this.isInited) {
			this.init();
		}
		var errors = new ArrayList<String>();

		// rather common scenario with empty logical sections
		List<METSContainer> nonPageContainers = this.structuralContainer.entrySet().stream()
			.filter(e -> e.getValue().getType() != METSContainerType.PAGE)
			.map(Map.Entry::getValue)
			.collect(Collectors.toList());
		for (var entry : nonPageContainers) {
			String fromLogicalId = entry.getId();
			if (!this.smLinks.containsKey(fromLogicalId)) {
				String err = String.format("No files link div %s (LABEL: %s)",
						fromLogicalId, entry.determineLabel());
				errors.add(err);
			}
		}
		// rather tricky check for referenced but missing pages
		for (var linkEntry : this.smLinks.entrySet()) {
			String fromLogicalId = linkEntry.getKey();
			List<String> toPhysicalIds = linkEntry.getValue();
			for (String toPhysicalId : toPhysicalIds) {
				if (!this.structuralContainer.containsKey(toPhysicalId)) {
					String err = String.format("Linked div %s from %s missing!",
							toPhysicalId, fromLogicalId);
					errors.add(err);
				}
			}
		}

		// finally
		if (errors.isEmpty()) {
			return;
		}
		throw new DigitalDerivansException(String.join(", ", errors));
	}

	public Map<String, METSContainer> getPages() {
		return this.structuralContainer.entrySet().stream()
				.filter(e -> e.getValue().getType() == METSContainerType.PAGE)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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
					var dmdId = primeLog.getAttributeValue(DMD_ID);
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
		List<Element> metsDivs = this.evaluate("//mets:structMap//mets:div[@DMDID and @TYPE]");
		if (metsDivs.size() == 1) {
			var primeDiv = metsDivs.get(0);
			if (primeDiv.hasAttributes()) {
				String dmdId = primeDiv.getAttributeValue(DMD_ID);
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
				return metsDiv.getAttributeValue(DMD_ID);
			}
		}
		throw new DigitalDerivansException("Can't determine mets:div with DMD identifier in " + this.getPath());
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

	/**
	 * @param useGroup
	 * @param identifier
	 * @param mimeType
	 * @throws DigitalDerivansException
	 */
	public String addDownloadFile(String useGroup, String identifier, String mimeType) throws DigitalDerivansException {
		String pdfFileID = "PDF_" + identifier;
		METSFile metsFile = new METSFile(pdfFileID, identifier + ".pdf", useGroup, mimeType);
		Element fileElement = metsFile.asElement();
		// attach or re-use existing group
		Element fileGrp = null;
		var existingGroups = this.evaluate(String.format("//mets:fileGrp[@USE='%s']", useGroup));
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
		String agentNoteText = "mets:file@ID=" + pdfFileID + " created at " + ts;
		this.enrichAgent(agentNoteText);
		// link as fptr to logical section
		var pdfFPtr = new Element("fptr", NS_METS);
		pdfFPtr.setAttribute("FILEID", pdfFileID);
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

	public METSContainer getLogicalRoot() {
		return this.logicalRoot;
	}

	/**
	 * Depending on Type of digital object returns
	 * 
	 * => nothing meaningfull for monographies
	 * => periodical/multivolume_work for volumes
	 * => newspaper for issues
	 * 
	 * @return
	 * @throws DigitalDerivansException
	 */
	public Optional<METSContainer> optParentStruct() throws DigitalDerivansException {
		String typeLabel = this.primeLog.getAttributeValue("TYPE");
		METSContainerType metsType = METSContainerType.forLabel(typeLabel);
		if (metsType == METSContainerType.MONOGRAPH || metsType == METSContainerType.MANUSCRIPT) {
			return Optional.empty();
		}
		var optParent = this.getParent(metsType);
		if (optParent.isPresent()) {
			return Optional.of(new METSContainer(optParent.get()));
		}
		return Optional.empty();
	}

	private Optional<Element> getParent(METSContainerType metsType) {
		if (metsType == METSContainerType.VOLUME) {
			return Optional.of(this.primeLog.getParentElement());
		} else if (metsType == METSContainerType.ISSUE || metsType == METSContainerType.ADDITIONAL) {
			Element parent = this.primeLog.getParentElement();
			while (!parent.getAttributeValue("TYPE").equals("newspaper")) {
				parent = parent.getParentElement();
			}
			return Optional.of(parent);
		}
		return Optional.empty();
	}

	public boolean write() {
		return this.xmlHandler.write(this.file);
	}

	public List<METSContainer> getPages(METSContainer div)
			throws DigitalDerivansException {
		String logID = div.getId();
		List<METSContainer> pageContainers = new ArrayList<>();
		List<String> linkedTo = this.smLinks.getOrDefault(logID, new ArrayList<>());
		for (String linkedId : linkedTo) {
			if (this.physicalContainer.containsKey(linkedId)) {
				METSContainer physCnt = this.physicalContainer.get(linkedId);
				if (physCnt.getType() != METSContainerType.PAGE) {
					// this happened as far as known in legacy inhouse newspaper
					// structures only when top most physSequence is linked
					// from logical structMap
					var cntLabel = div.determineLabel();
					var physTypeLabel = physCnt.getType().getLabel();
					LOGGER.warn("linked div {} / {} -> page {} '/ {}", logID, cntLabel, linkedId, physTypeLabel);
				} else {
					pageContainers.add(physCnt);
				}
			}
		}
		if (pageContainers.isEmpty()) {
			String alert = String.format("No files link div %s/%s in @USE=%s!",
					logID, div.determineLabel(), this.imgFileGroup);
			throw new DigitalDerivansException(alert);
		}
		return pageContainers;
	}

	// public METSFilePack getPageFiles(METSContainer container) throws DigitalDerivansException {
	// 	List<Element> allFiles = container.get().getChildren("fptr", METS.NS_METS);
	// 	List<String> fileIds = allFiles.stream().map(aFile -> aFile.getAttributeValue("FILEID"))
	// 			.collect(Collectors.toList());
	// 	METSFile imgFile = null;
	// 	for (String fileId : fileIds) {
	// 		if (this.metsFiles.containsKey(fileId)) {
	// 			METSFile tmpFile = this.metsFiles.get(fileId);
	// 			if (tmpFile.getFileGroup().equals(this.imgFileGroup)) {
	// 				imgFile = tmpFile;
	// 				break;
	// 			}
	// 		}
	// 	}
	// 	if (imgFile == null) {
	// 		var msg = "Can't find image file in @USE=" + this.imgFileGroup + " for container " + container.getId();
	// 		throw new DigitalDerivansException(msg);
	// 	}
	// 	imgFile.setLocalRoot(this.getPath().getParent());
	// 	METSFilePack pack = new METSFilePack();
	// 	pack.imageFile = imgFile;
	// 	METSFile ocrFile = null;
	// 	for (String fileId : fileIds) {
	// 		if (this.metsFiles.containsKey(fileId)) {
	// 			METSFile tmpFile = this.metsFiles.get(fileId);
	// 			if (tmpFile.getFileGroup().equals(this.ocrFileGroup)) {
	// 				ocrFile = this.metsFiles.get(fileId);
	// 				break;
	// 			}
	// 		}
	// 	}
	// 	if (ocrFile != null) {
	// 		ocrFile.setLocalRoot(this.getPath().getParent());
	// 		pack.ocrFile = Optional.of(ocrFile);
	// 	}
	// 	return pack;
	// }

	public boolean isInited() {
		return this.isInited;
	}

	public void setIdentifierExpression(String xpressiob) {
		throw new UnsupportedOperationException("Unimplemented method 'setIdentifierExpression'");
	}

	public String enrichPDF(String identifier) throws DigitalDerivansException {
		String mimeType = "application/pdf";
		String fileGroup = "DOWNLOAD";
		LOGGER.info("enrich pdf '{}' as '{}' in '{}'", identifier, mimeType, fileGroup);
		var resultText = this.addDownloadFile("DOWNLOAD", identifier, "application/pdf");
		LOGGER.info("integrated mets:agent {}", resultText);
		return resultText;
	}

	public static class METSFilePack {
		public METSFile imageFile;
		public Optional<METSFile> ocrFile = Optional.empty();

	}
}
