package de.ulb.digital.derivans.data.mets;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.AbstractFilter;
import org.jdom2.filter.Filter;
import org.jdom2.filter.Filters;

import de.ulb.digital.derivans.Derivans;
import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.data.io.JarResource;
import de.ulb.digital.derivans.data.xml.XMLHandler;

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
	
	static final String STRUCT_PHYSICAL_ROOT = "physroot";
	static final String METS_STRUCTMAP_TYPE_PHYSICAL = "PHYSICAL";
	static final String METS_STRUCTMAP_TYPE_LOGICAL = "LOGICAL";
	static final String DMD_ID = "DMDID";
	static final String METS_CONTAINER = "div";
	static final String METS_CONTAINER_ID = "ID";
	static final String METS_FILE_ID = "FILEID";
	static final String METS_STRUCTMAP_TYPE = "TYPE";

	private Set<String> fileGroupLabels = new HashSet<>();

	public static final DateTimeFormatter MD_DT_FORMAT = new DateTimeFormatterBuilder()
			.appendPattern("YYYY-MM-dd")
			.appendLiteral('T')
			.appendPattern("HH:mm:SS")
			.toFormatter();

	private static List<Namespace> namespaces = List.of(NS_METS, NS_MODS, NS_XLINK);

	private Path file;

	private Document document;

	private XMLHandler xmlHandler;

	private MODS primeMods;

	private Element primeLog;

	private boolean isInited;

	private HashMap<String, METSFile> metsFiles = new LinkedHashMap<>();

	private HashMap<String, METSContainer> pageMap = new LinkedHashMap<>();

	private METSContainer logicalRoot;

	private HashMap<String, List<String>> smLinks = new LinkedHashMap<>();

	public METS(Path metsfile) throws DigitalDerivansException {
		this.file = metsfile;
		this.xmlHandler = new XMLHandler(file);
		this.document = this.xmlHandler.getDocument();
	}

	public METS(Path metsfile, String imgFileGroup) throws DigitalDerivansException {
		this.file = metsfile;
		this.addFileGroup(imgFileGroup);
		this.xmlHandler = new XMLHandler(file);
		this.document = this.xmlHandler.getDocument();
	}
	
	public void addFileGroup(String imgFileGroup) {
		if (!this.fileGroupLabels.contains(imgFileGroup)) {
			this.fileGroupLabels.add(imgFileGroup);
		}
	}

	public void removeFileGroup(String imgFileGroup) {
		if (this.fileGroupLabels.contains(imgFileGroup)) {
			this.fileGroupLabels.remove(imgFileGroup);
		}
	}
	/**
	 * Resolve identifier for descriptive section.
	 * <ol>
	 * <li>1st, look for simple cases like mongraphs or volumes</li>
	 * <li>2nd, inspect the structural Linkings</li>
	 * </ol>
	 */
	public void init() throws DigitalDerivansException {
		this.buildInternalRepresentation();
		String primeId = this.oneRoot().orElse(this.calculatePrimeMODSId());
		String xpr = String.format("//mets:dmdSec[@ID='%s']//mods:mods", primeId);
		List<Element> modsSecs = this.evaluate(xpr);
		if (modsSecs.size() != 1) {
			throw new DigitalDerivansException("can't identify primary MODS section using " + primeId);
		}
		var primeElement = modsSecs.get(0);
		this.primeMods = new MODS(primeId, primeElement);
		this.isInited = true;
	}

	private void buildInternalRepresentation() throws DigitalDerivansException {
		Filter<Element> structMapFilter = Filters.element(NS_METS).refine(Filters.element("structMap", NS_METS));
		Iterator<Element> structMaps = this.document.getDescendants(structMapFilter);
		Filter<Element> structLinkFilter = Filters.element(NS_METS).refine(Filters.element("structLink", NS_METS));
		Filter<Element> fileGrpFilter = Filters.element(NS_METS).refine(Filters.element("fileGrp", NS_METS));
		Iterator<Element> fileGrpIterator = this.document.getDescendants(fileGrpFilter);

		// any present files related to certain file group labels
		var presentFileGroups = new ArrayList<Element>();
		var presentFileGroupLabels = new HashSet<String>();
		while (fileGrpIterator.hasNext()) {
		 	Element fgElement = fileGrpIterator.next();
			 presentFileGroups.add(fgElement);
			presentFileGroupLabels.add(fgElement.getAttributeValue("USE"));
		}
		LOGGER.debug("having {} mets:fileGrp, considering labels: {}", presentFileGroupLabels, this.fileGroupLabels);
		
		for (Element e : presentFileGroups) {
			String currentGroupLabel = e.getAttributeValue("USE");
			if (this.fileGroupLabels.contains(currentGroupLabel)) {
				List<Element> files = e.getChildren("file", NS_METS);
				for (Element fileElement : files) {
					String fileId = fileElement.getAttributeValue("ID");
					METSFile metsFile = new METSFile(fileElement, currentGroupLabel);
					this.metsFiles.put(fileId, metsFile);
				}
			}
		}
		// physical containers: means mets:div@TYPE="page"
		Element logStruct = null;
		Element phyStruct = null;
		while(structMaps.hasNext()) {
			Element fgElement = structMaps.next();
			String structType = fgElement.getAttributeValue("TYPE");
			LOGGER.debug("Found structMap of TYPE={}", structType);
			if (structType.equalsIgnoreCase(METS_STRUCTMAP_TYPE_LOGICAL)) {
				logStruct = fgElement;
			}
			if (structType.equalsIgnoreCase(METS_STRUCTMAP_TYPE_PHYSICAL)) {
				phyStruct = fgElement;
			}
		}
		// logical struct
		if (logStruct == null) {
			throw new DigitalDerivansException("No logical structMap found");
		}
		List<Element> logicalDivs = logStruct.getChildren("div", NS_METS);
		if(logicalDivs.size() != 1) {
			LOGGER.warn("Multiple top-level logical divs found in mets:structMap");
		}
		var rootElement = logicalDivs.get(0);
		METSContainer root = new METSContainer(rootElement);
		this.logicalRoot = root;
		this.buildLogicalMap(this.logicalRoot);

		if (phyStruct != null) {
			var pageFilter = new EnhancedMETSAttributeFilter(METS_CONTAINER, "TYPE", "page");
			Iterator<Element> metsPages = phyStruct.getDescendants(pageFilter);
			while (metsPages.hasNext()) {
				Element pageElement = metsPages.next();
				String pageId = pageElement.getAttributeValue("ID");
				METSContainer pageContainer = new METSContainer(pageElement);
				for (var pageFile : pageElement.getChildren("fptr", NS_METS)) {
					String fileId = pageFile.getAttributeValue(METS_FILE_ID);
					if (this.metsFiles.containsKey(fileId)) {
						METSFile metsFile = this.metsFiles.get(fileId);
						pageContainer.addFile(metsFile);
					}
				}
				this.pageMap.put(pageId, pageContainer);
			}
		}
		// links
		Iterator<Element> iter = this.document.getDescendants(structLinkFilter);
		if (!iter.hasNext()) {
			throw new DigitalDerivansException("No structLinks found");
		}
		Element structLink = iter.next();
		List<Element> mapLinks = structLink.getChildren("smLink", NS_METS);
		for (Element smLink : mapLinks) {
			String fromSection = smLink.getAttributeValue("from", NS_XLINK);
			String toPage = smLink.getAttributeValue("to", NS_XLINK);
			if(toPage.equalsIgnoreCase(STRUCT_PHYSICAL_ROOT)) {
				LOGGER.warn("Ignoring link to physroot from {}", fromSection);
				continue;
			}
			List<String> linkedPages = this.smLinks.getOrDefault(fromSection, new ArrayList<>());
			linkedPages.add(toPage);
			this.smLinks.put(fromSection, linkedPages);
		}
	}

	/**
	 * 
	 * Recursively build logical map by following smLinks with
	 * respect to DFG-METS flavour with separate physical and logical
	 * structMaps.
	 * 
	 * @param current
	 * @throws DigitalDerivansException
	 */
	private void buildLogicalMap(METSContainer current) throws DigitalDerivansException {
		String parentId = current.getId();
		// if digitization systems would take care of linking pages and prevent
		// multiple linkings pushing files around like this wouldn't be necessary ...
		if (this.smLinks.containsKey(parentId)) { 
			List<String> linkedPages = this.smLinks.get(parentId);
			int movedFiles = 0;
			METSContainer currentParent = current.getParent();
			for (String pageId : linkedPages) {
				if (this.pageMap.containsKey(pageId)) {
					METSContainer pageContainer = this.pageMap.get(pageId);
					List<METSFile> cfiles = pageContainer.getFiles();
					for(METSFile cfile : cfiles) {
						current.addFile(cfile);
						if(currentParent != null) {
							List<METSFile> parentFiles = currentParent.getFiles();
							for(int i=0; i< parentFiles.size(); i++) {
								METSFile pFile = parentFiles.get(i);
								if(pFile.getFileId().equals(cfile.getFileId())) {
									currentParent.removeFile(pFile);
									movedFiles++;
								}
							}
						}
					}
				} else {
					// we must know all page containers at this time
					throw new DigitalDerivansException("No page container for ID " + pageId);
				}
			}
			if (movedFiles > 0) {
				LOGGER.warn("Moved {} files from parent {} to child container {}",
					movedFiles, currentParent, current);
			}
		}
		// respect direct linked files, too
		Element parentElement = current.get();
		List<Element> filePointers = parentElement.getChildren("fptr", NS_METS);
		for (Element fptr : filePointers) {
			String fileId = fptr.getAttributeValue(METS_FILE_ID);
			if (this.pageMap.containsKey(fileId)) {
				METSContainer pageContainer = this.pageMap.get(fileId);
				current.addChild(pageContainer);
			} else {
				
				// ignore alerts from linked PDF files or teaser images
				LOGGER.warn("No page container links ID {}", fileId);
			}
		}
		List<Element> childDivs = parentElement.getChildren("div", NS_METS);
		for (Element childDiv : childDivs) {
			METSContainer childContainer = new METSContainer(childDiv);
			childContainer.setParent(current);
			this.buildLogicalMap(childContainer);
		}
	}

	public Map<String, METSContainer> getPages() {
		return this.pageMap;
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
		pdfFPtr.setAttribute(METS_FILE_ID, pdfFileID);
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

	public METSContainer getLogicalRoot() throws DigitalDerivansException {
		return new METSContainer(this.primeLog);
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

	public METSFilePack getPageFiles(METSContainer container, List<String> fileGroups) {
		List<Element> allFiles = container.get().getChildren("fptr", METS.NS_METS);
		List<String> fileIds = allFiles.stream().map(aFile -> aFile.getAttributeValue(METS_FILE_ID))
				.collect(Collectors.toList());
		METSFilePack pack = new METSFilePack();
		for (String fileId : fileIds) {
			for(var fg : fileGroups) {
				if (this.metsFiles.containsKey(fileId)) {
					METSFile tmpFile = this.metsFiles.get(fileId);
					if(tmpFile.getFileGroup().equals(fg)) {
						tmpFile.setLocalRoot(this.getPath().getParent());
						pack.groupFiles.put(fg, tmpFile);
					}
				}
			}
		}
		return pack;
	}

	public boolean isInited() {
		return this.isInited;
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
		public Map<String, METSFile> groupFiles = new HashMap<>();
	}

	/**
	 * Custom JDOM2 filter that filters elements by name and checks for attribute presence
	 */
	public static class EnhancedMETSAttributeFilter extends AbstractFilter<Element> {
		
		private String elementName;
		private String attributeName;
		private String attributeValue;
	
		/**
		 * Create a filter for elements with specific name and attribute in a namespace
		 * 
		 * @param elementName The name of the element to filter
		 * @param attributeName The name of the attribute that must be present
		 * @param namespace The namespace of the attribute (can be null for no namespace)
		 */
		public EnhancedMETSAttributeFilter(String elementName, String attributeName, String value) {
			this.elementName = elementName;
			this.attributeName = attributeName;
			this.attributeValue = value;
		}
		
		@Override
		public Element filter(Object content) {
			if (content instanceof Element) {
				Element element = (Element) content;
				// First check element name
				if (element.getName().equals(elementName)) {
					// Then check for attribute and value
					Attribute attr = element.getAttribute(attributeName);
					if (attr != null && attr.getValue().equals(attributeValue)) {
						return element;
					}
				}
			}
			return null;
		}
	}
}
