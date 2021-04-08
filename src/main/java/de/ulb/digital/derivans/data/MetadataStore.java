package de.ulb.digital.derivans.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.mycore.mets.model.Mets;
import org.mycore.mets.model.files.FLocat;
import org.mycore.mets.model.files.File;
import org.mycore.mets.model.files.FileGrp;
import org.mycore.mets.model.struct.Fptr;
import org.mycore.mets.model.struct.LOCTYPE;
import org.mycore.mets.model.struct.PhysicalStructMap;
import org.mycore.mets.model.struct.PhysicalSubDiv;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.data.ocr.OCRReaderFactory;
import de.ulb.digital.derivans.model.DescriptiveData;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.DigitalStructureTree;
import de.ulb.digital.derivans.model.ocr.OCRData;

/**
 * 
 * Implementation of {@link IMetadataStore} Interface
 * 
 * @author hartwig
 *
 */
public class MetadataStore implements IMetadataStore {

	// METS file group for images with maximal resolution
	public static final String FILEGROUP_MAX = "MAX";

	// METS file group for OCR-data with, most likely, MIMETYPE="application/alto+xml"
	public static final String FILEGROUP_FULLTEXT = "FULLTEXT";

	// Mark unresolved information about author, title, ...
	public static final String UNKNOWN = "n.a.";

	public static final Namespace NS_MODS = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");

	public static final Namespace NS_METS = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");
	
	private static final Logger LOGGER = LogManager.getLogger(MetadataStore.class);

	private DescriptiveData descriptiveData;

	private MetadataHandler handler;

	private Mets mets;

	/**
	 * Constructor if no METS/MODS available
	 */
	public MetadataStore() {
	}

	/**
	 * 
	 * Default constructor requires Path to valid Metadata file
	 * 
	 * @param filePath
	 * @throws DigitalDerivansException
	 */
	public MetadataStore(Path filePath) throws DigitalDerivansException {
		this.handler = new MetadataHandler(filePath);
		this.load();
		LOGGER.info("created new metadatastore from '{}'", filePath);
	}

	private void load() throws DigitalDerivansException {
		try {
			mets = handler.read();
		} catch (IOException | JDOMException e) {
			LOGGER.error(e);
			throw new DigitalDerivansException(e);
		}
	}

	@Override
	public DigitalStructureTree getStructure() throws DigitalDerivansException {
		StructureMapper creator = new StructureMapper(mets, getDescriptiveData().getTitle());
		return creator.build();
	}

	@Override
	public List<DigitalPage> getDigitalPagesInOrder() {
		List<DigitalPage> pages = new ArrayList<>();
		int n = 1;
		if (this.mets != null) {
			PhysicalStructMap physStruct = mets.getPhysicalStructMap();
			if (physStruct != null) {
				for (PhysicalSubDiv physSubDiv : physStruct.getDivContainer().getChildren()) {
					List<FilePointerMatch> fptrs = getFilePointer(physSubDiv);
					DigitalPage page = new DigitalPage(n);

					// handle image file
					Optional<FilePointerMatch> optMaxImage = fptrs.stream()
							.filter(fptr -> FILEGROUP_MAX.equals(fptr.getFileGroup())).findFirst();
					if (optMaxImage.isPresent()) {
						FilePointerMatch match = optMaxImage.get();
						enrichImageData(physSubDiv, page, match);
					}
					
					// handle optional attached ocr file
					Optional<FilePointerMatch> optFulltext = fptrs.stream()
							.filter(fptr -> FILEGROUP_FULLTEXT.equals(fptr.getFileGroup())).findFirst();
					if (optFulltext.isPresent()) {
						FilePointerMatch match = optFulltext.get();
						enrichFulltextData(physSubDiv, page, match);
					}
					
					pages.add(page);
					n++;
				}
			}
		}
		return pages;
	}

	/**
	 * 
	 * Enrich Information about physical images that represent pages
	 * <ul>
	 * 	<li>sanitize file extension (likely missing when got over OAI)</li>
	 * 	<li>take care of optional granular URN as unique identifier</li>
	 * </ul>
	 * 
	 * @param physSubDiv
	 * @param page
	 * @param match
	 */
	private void enrichImageData(PhysicalSubDiv physSubDiv, DigitalPage page, FilePointerMatch match) {
		String fileRefSegment = match.reference;
		// sanitize file endings that are missing in METS links for valid local access
		if (!fileRefSegment.endsWith(".jpg")) {
			fileRefSegment += ".jpg";
		}
		page.setImagePath(Path.of(fileRefSegment));
		// handle granular urn (aka CONTENTIDS)
		String contentIds = physSubDiv.getContentIds();
		if (contentIds != null) {
			LOGGER.debug("[{}] contentids '{}'", physSubDiv.getId(), contentIds);
			page.setIdentifier(contentIds);
		}
	}
	
	private void enrichFulltextData(PhysicalSubDiv physSubDiv, DigitalPage page, FilePointerMatch match) {
		String fileRefSegment = match.reference;
		Path ocrFilePath = sanitizePath(Path.of(fileRefSegment)); 
		try {
			OCRData data = OCRReaderFactory.from(ocrFilePath).get(ocrFilePath);
			page.setOcrData(data);
			LOGGER.debug("[{}] enriched ocr data with '{}' lines", physSubDiv.getId(), data.getTextlines().size());
		} catch (DigitalDerivansException e) {
			LOGGER.error(e);
		}
	}

	/**
	 * 
	 * Guess where OCR-Data physically resides
	 * 
	 * @param path
	 * @return
	 */
	private Path sanitizePath(Path path) {
		Path metsDir = this.handler.getPath().getParent();
		Path p = metsDir.resolve(Path.of(FILEGROUP_FULLTEXT)).resolve(path);
		if(Files.exists(p, LinkOption.NOFOLLOW_LINKS)) {
			LOGGER.debug("found ocr data file '{}'", p);
			return p;
		}
		return path;
	}

	private List<FilePointerMatch> getFilePointer(PhysicalSubDiv physSubDiv) {
		var filePointers = physSubDiv.getChildren();
		return filePointers.stream().map(Fptr::getFileId).map(this::matchFileGroup).collect(Collectors.toList());
	}

	/**
	 * 
	 * Inspect ALL METS FileGrps, not only "MAX", to match the provided physical
	 * identifier
	 * 
	 * @param fptrId
	 * @return
	 */
	private FilePointerMatch matchFileGroup(String fptrId) {
		for (FileGrp fileGrp : mets.getFileSec().getFileGroups()) {
			for (File fileGrpfile : fileGrp.getFileList()) {
				if (fileGrpfile.getId().equals(fptrId)) {
					String link = fileGrpfile.getFLocat().getHref();
					if (!link.isBlank()) {
						String[] linkTokens = link.split("/");
						String reference = linkTokens[linkTokens.length - 1];
						return new FilePointerMatch(fileGrp.getUse(), reference);
					}
				}
			}
		}
		// dummy return
		return new FilePointerMatch();
	}

	@Override
	public boolean enrichPDF(String identifier) {
		if (MetadataStore.UNKNOWN.equals(identifier)) {
			LOGGER.warn("no mets available to enrich created PDF");
			return false;
		}
		if (this.handler != null) {
			String mimeType = "application/pdf";
			String fileGroup = "DOWNLOAD";
			LOGGER.info("enrich derivate with href '{}' as '{}' in '{}'", identifier, mimeType, fileGroup);
			String fileId = integrateFileGroup(fileGroup, identifier, mimeType);
			String note = this.handler.enrichAgent(fileId);
			integrateFprt("PDF_" + identifier);
			this.handler.write();
			LOGGER.info("integrated pdf fileId '{}' in '{}'", fileId, this.handler.getPath());
			LOGGER.info("integrated mets:agent {}", note);
			return true;
		}
		return false;
	}

	private String integrateFileGroup(String fileGroupUse, String identifier, String mimeType) {
		FileGrp fileGrp = new FileGrp(fileGroupUse);
		String fileId = "PDF_" + identifier;
		File f = new File(fileId, mimeType);
		FLocat fLocat = new FLocat(LOCTYPE.URL, identifier + ".pdf");
		f.setFLocat(fLocat);
		fileGrp.addFile(f);
		this.handler.addFileGroup(fileGrp.asElement());
		return fileId;
	}

	private void integrateFprt(String fileHref) {
		Fptr fpts = new Fptr(fileHref);
		this.handler.addTo(fpts.asElement(), "LOGICAL", true);
	}

	@Override
	public DescriptiveData getDescriptiveData() {
		if (descriptiveData == null) {
			DescriptiveDataBuilder builder = new DescriptiveDataBuilder(this.mets);
			builder.setHandler(this.handler);
			try {
				descriptiveData = builder.person().access().identifier().title().urn().year().build();
			} catch (DigitalDerivansException e) {
				LOGGER.error(e);
			}
		}
		return descriptiveData;
	}

	/**
	 * 
	 * Carry the knowledge, which res ID belongs to which group, through execution
	 * time.
	 * 
	 * @author u.hartwig
	 *
	 */
	static class FilePointerMatch {

		private String fileGroup = UNKNOWN;
		private String reference = UNKNOWN;

		public FilePointerMatch() {
		}

		public FilePointerMatch(String fileGroup, String reference) {
			this.fileGroup = fileGroup;
			this.reference = reference;
		}

		public String getFileGroup() {
			return fileGroup;
		}

		public String getReference() {
			return reference;
		}
	}
}
