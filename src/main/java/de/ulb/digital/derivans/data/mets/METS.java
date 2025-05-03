package de.ulb.digital.derivans.data.mets;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

import de.ulb.digital.derivans.Derivans;
import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.data.io.JarResource;
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
	static final String DMD_ID = "DMDID";
	static final String METS_CONTAINER = "div";
	static final String METS_CONTAINER_ID = "ID";
	static final String METS_STRUCTMAP_TYPE = "TYPE";

	private String imgFileGroup = IDerivans.IMAGE_DIR_DEFAULT;

	private String ocrFileGroup = IDerivans.FULLTEXT_DIR;

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
	public void determine() throws DigitalDerivansException {
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
		Element fileElement = new METSFile(identifier, identifier + ".pdf", useGroup, mimeType).asElement();
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

	public METSContainer getLogicalRoot() {
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
			for (Element page : pages) {
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
					if (!hRef.endsWith(fileExt)) {
						hRef += fileExt;
					}
					var thaFile = new METSFile(fileId, hRef, fileGroup);
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
			String alert = String.format("No files link div %s/%s in @USE=%s!",
					logID, div.determineLabel(), fileGroup);
			throw new DigitalDerivansException(alert);
		}
		return metsFiles;
	}

	public List<METSContainer> getPages(METSContainer div)
			throws DigitalDerivansException {
		String logID = div.getId();
		String q01 = String.format("//mets:structLink/mets:smLink[@xlink:from='%s']", logID);
		List<Element> smLinks = this.evaluate(q01);
		List<METSContainer> cntnrs = new ArrayList<>();
		for (Element smLink : smLinks) {
			var pageId = smLink.getAttributeValue("to", METS.NS_XLINK);
			List<Element> pages = this.evaluate(String.format("//mets:div[@ID='%s']", pageId));
			for (Element page : pages) {
				METSContainer pageContainer = new METSContainer(page);
				cntnrs.add(pageContainer);
			}
		}
		if (cntnrs.isEmpty()) {
			String alert = String.format("No files link div %s/%s in @USE=%s!",
					logID, div.determineLabel(), this.imgFileGroup);
			throw new DigitalDerivansException(alert);
		}
		return cntnrs;
	}

	public METSFilePack getPageFiles(METSContainer container)
			throws DigitalDerivansException {
		List<Element> allFiles = container.get().getChildren("fptr", METS.NS_METS);
		List<String> fileIds = allFiles.stream().map(aFile -> aFile.getAttributeValue("FILEID"))
				.collect(Collectors.toList());
		METSFile imgFile = this.pickFromGroup(fileIds, this.imgFileGroup, true);
		imgFile.setLocalRoot(this.getPath().getParent());
		METSFile ocrFile = this.pickFromGroup(fileIds, this.ocrFileGroup, false);
		METSFilePack pack = new METSFilePack();
		pack.imageFile = imgFile;
		if(ocrFile != null) {
			ocrFile.setLocalRoot(this.getPath().getParent());
			pack.ocrFile = Optional.of(ocrFile);
		}
		return pack;
	}

	private METSFile pickFromGroup(List<String> fileIds, String fileGroupUse, boolean check) throws DigitalDerivansException {
		Element theElement = null;
		for (String theId : fileIds) {
			String queryFile = String.format("//mets:fileGrp[@USE='%s']/mets:file[@ID='%s']", fileGroupUse, theId);
			List<Element> filesFromGroup = this.evaluate(queryFile);
			if (filesFromGroup.isEmpty()) {
				continue;
			}
			theElement = filesFromGroup.get(0);
			break;
		}
		if (theElement == null) {
			if (check) {
				throw new DigitalDerivansException("fileGroupUse");
			} else {
				return null;
			}
		}
		String mimeType = theElement.getAttributeValue("MIMETYPE");
		String fileId = theElement.getAttributeValue("ID");
		var fstLocat = theElement.getChildren("FLocat", METS.NS_METS).get(0);
		String hRef = fstLocat.getAttributeValue("href", METS.NS_XLINK);
		if (isTypeJPG(mimeType) && !hRef.endsWith(".jpg")) {
			hRef += ".jpg";
		}
		if (isTypeXML(mimeType) && !hRef.endsWith(".xml")) {
			hRef += ".xml";
		}
		METSFile metsFile = new METSFile(fileId, hRef, fileGroupUse);
		return metsFile;
	}

	private boolean isTypeJPG(String mimeType) {
		if (mimeType != null && mimeType.contains("image")) {
			return mimeType.contains("jpeg") || mimeType.contains("jpg");
		}
		return false;
	}

	private boolean isTypeXML(String mimeType) {
		return mimeType != null && mimeType.contains("xml");
	}

	public void setIdentifierExpression(String xpressiob) {
		throw new UnsupportedOperationException("Unimplemented method 'setIdentifierExpression'");
	}

	public String enrichPDF(String identifier) throws DigitalDerivansException {
		String mimeType = "application/pdf";
		String fileGroup = "DOWNLOAD";
		LOGGER.info("enrich pdf '{}' as '{}' in '{}'", identifier, mimeType, fileGroup);
		String fileId = "PDF_" + identifier;
		var resultText = this.addDownloadFile("DOWNLOAD", fileId, "application/pdf");
		LOGGER.info("integrated pdf fileId '{}' in '{}'", fileId, this.file);
		LOGGER.info("integrated mets:agent {}", resultText);
		return resultText;
	}

	public static class METSFilePack {
		public METSFile imageFile;
		public Optional<METSFile> ocrFile = Optional.empty();
	}
}
