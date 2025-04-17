package de.ulb.digital.derivans.data.mets;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
// import org.mycore.mets.model.Mets;
// import org.mycore.mets.model.files.File;
// import org.mycore.mets.model.files.FileGrp;
// import org.mycore.mets.model.struct.Fptr;
// import org.mycore.mets.model.struct.PhysicalStructMap;
// import org.mycore.mets.model.struct.PhysicalSubDiv;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.data.IMetadataStore;
import de.ulb.digital.derivans.data.io.DerivansPathResolver;
import de.ulb.digital.derivans.data.IDescriptiveMetadataBuilder;
import de.ulb.digital.derivans.data.ocr.OCRReaderFactory;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.DigitalStructureTree;
import de.ulb.digital.derivans.model.ocr.OCRData;
import de.ulb.digital.derivans.model.pdf.DescriptiveMetadata;

/**
 * 
 * Implementation of {@link IMetadataStore} Interface
 * 
 * @author hartwig
 *
 */
public class MetadataStore implements IMetadataStore {

	private static final Logger LOGGER = LogManager.getLogger(MetadataStore.class);

	private DescriptiveMetadata descriptiveData;

	private METS mets;

	private final Optional<String> storePath;

	private String fileGroupImages = IMetadataStore.DEFAULT_INPUT_IMAGES;

	private String fileGroupOcr = IMetadataStore.DEFAULT_INPUT_FULLTEXT;

	private Optional<String> xPathIdentifier = Optional.empty();

	/**
	 * Constructor if no METS/MODS available
	 */
	public MetadataStore() {
		storePath = Optional.empty();
	}

	/**
	 * 
	 * Default constructor
	 * 
	 * @param filePath
	 * @throws DigitalDerivansException
	 */
	public MetadataStore(Path filePath) throws DigitalDerivansException {
		this.storePath = Optional.of(filePath.toString());
		this.mets = new METS(filePath);
		LOGGER.info("set metadatastore from '{}'", filePath);
		this.mets.determine();
	}

	
	@Override
	public void setMetadata()  throws DigitalDerivansException {
		this.mets.determine();
	}

	@Override
	public void setStructure() throws DigitalDerivansException {
		this.mets.setFiles(this.fileGroupImages);
	}

	@Override
	public void setFileGroupImages(String fileGrp) {
		this.fileGroupImages = fileGrp;
	}

	@Override
	public void setFileGroupOCR(String fileGrp) {
		this.fileGroupOcr = fileGrp;
	}

	@Override
	public void setIdentifierExpression(String xPath) {
		this.xPathIdentifier = Optional.of(xPath);
	}

	@Override
	public Optional<String> optionalIdentifierExpression() {
		return this.xPathIdentifier;
	}

	@Override
	public DigitalStructureTree getStructure() throws DigitalDerivansException {
		// METSStructLogical creator = new METSStructLogical(this.mets,
		// getDescriptiveData().getTitle());
		// return creator.build();
		// return this.mets.getContainer(getDescriptiveData().getTitle());
		return mets.getLogicalStructure();
	}

	@Override
	public List<DigitalPage> getDigitalPagesInOrder() {
		List<DigitalPage> pages = new ArrayList<>();
		int n = 1;
		// List<METSContainer> metsPages = mets.getPhyContainers();
		// for(var metsPage : metsPages) {
			
		// }
		// PhysicalStructMap physStruct = mets.getPhysicalStructMap();
		// if (physStruct != null) {
		// 	for (PhysicalSubDiv physSubDiv : physStruct.getDivContainer().getChildren()) {
		// 		List<FilePointerRef> fptrs = getFilePointer(physSubDiv);
		// 		DigitalPage page = new DigitalPage(n);

		// 		// handle image file
		// 		Optional<FilePointerRef> optImage = fptrs.stream()
		// 				.filter(fptr -> this.fileGroupImages.equals(fptr.getFileGroup())).findFirst();
		// 		LOGGER.debug("enrich digital page from {}", optImage);
		// 		if (optImage.isPresent()) {
		// 			FilePointerRef match = optImage.get();
		// 			enrichImageData(physSubDiv, page, match);
		// 			// probably encountered something empty
		// 			// since no filtering forehand, might contain
		// 			// top-level mets:fptr to PDFs or alike
		// 		} else {
		// 			continue;
		// 		}

		// 		// handle optional attached ocr file
		// 		Optional<FilePointerRef> optFulltext = fptrs.stream()
		// 				.filter(fptr -> this.fileGroupOcr.equals(fptr.getFileGroup())).findFirst();
		// 		if (optFulltext.isPresent()) {
		// 			LOGGER.trace("enrich ocr from {}", optFulltext);
		// 			FilePointerRef match = optFulltext.get();
		// 			enrichFulltextData(physSubDiv, page, match);
		// 		}

		// 		pages.add(page);
		// 		n++;
		// 	}
		// }

		// List<METSContainer> cnts = this.mets.getPhyContainers();
		// if (cnts.isEmpty()) {
		// 	return pages; // no DFG met:div page container, go home
		// }
		List<METSFile> imageFiles = this.mets.getFiles(this.fileGroupImages);
		List<METSFile> optOcrs = this.mets.getFiles(this.fileGroupOcr);
		for (var img : imageFiles) {
			DigitalPage page = new DigitalPage(n);
			LOGGER.debug("enrich image {}", img.getLocation());
			enrichImageData(img, page);
			pages.add(page);
			n++;
		}

		return pages;
	}

	/**
	 * 
	 * Enrich Information about physical images that represent pages
	 * <ul>
	 * <li>sanitize file extension (likely missing when got over OAI)</li>
	 * <li>take care of optional granular URN as unique identifier</li>
	 * </ul>
	 * 
	 * @param imgFile
	 * @param page
	 * @param match
	 * @throws DigitalDerivansException
	 */
	private void enrichImageData(METSFile imgFile, DigitalPage page) {
		String fileRefSegment = imgFile.getLocation();
		// sanitize file endings that are missing in METS links for valid local access
		if (!fileRefSegment.endsWith(".jpg") && !DerivansPathResolver.isTIFF(fileRefSegment)) {
			fileRefSegment += ".jpg";
		}
		if (fileRefSegment.startsWith("http")) {
			var preLocTokens = fileRefSegment.split("/");
			var newLoc = preLocTokens[preLocTokens.length-1];
			LOGGER.debug("file location {} will be replaced/shortened to {}/{}",
				fileRefSegment, imgFile.getFileGroup(), newLoc);
			fileRefSegment = newLoc;
		}
		page.setImagePath(Path.of(imgFile.getFileGroup(), fileRefSegment));
		// handle granular urn (aka CONTENTIDS)
		String cntIds = imgFile.getContentIds();
		// if(optCntIds.isPresent()) {
			// var cntIds = optCntIds.get();
			LOGGER.debug("[{}] contentids '{}'", imgFile.getFileId(), cntIds);
			page.setIdentifier(cntIds);
		// }
	}

	private void enrichFulltextData(METSContainer pageCnt, DigitalPage page, FilePointerRef match) {
		String fileRefSegment = match.reference;
		Optional<Path> optOCRPath = calculateOCRPath(Path.of(fileRefSegment));
		if (optOCRPath.isPresent()) {
			Path ocrFilePath = optOCRPath.get();
			try {
				OCRData data = OCRReaderFactory.from(ocrFilePath).get(ocrFilePath);
				page.setOcrData(data);
				LOGGER.debug("[{}] enriched ocr data with '{}' lines", pageCnt.getId(), data.getTextlines().size());
			} catch (DigitalDerivansException e) {
				LOGGER.error(e);
			}
		}
	}

	/**
	 * 
	 * Guess where OCR-Data physically resides
	 * by first sanitizing the extension and
	 * afterwards resolve local path from
	 * root and OCR-data sub dir
	 * 
	 * @param path
	 * @return
	 */
	private Optional<Path> calculateOCRPath(Path path) {
		Path metsDir = this.mets.getPath();
		if (!path.toString().endsWith(".xml")) {
			path = Path.of(path.toString() + ".xml");
		}
		Path p = metsDir.resolve(Path.of(this.fileGroupOcr)).resolve(path);
		if (Files.exists(p, LinkOption.NOFOLLOW_LINKS)) {
			LOGGER.debug("found ocr data file '{}'", p);
			return Optional.of(p);
		} else {
			LOGGER.warn("missing ocr data '{}'!", p);
		}
		return Optional.empty();
	}


	@Override
	public boolean enrichPDF(String identifier) throws DigitalDerivansException {
		if (IMetadataStore.UNKNOWN.equals(identifier)) {
			LOGGER.warn("no mets available to enrich created PDF");
			return false;
		}
		if (this.mets != null) {
			String mimeType = "application/pdf";
			String fileGroup = "DOWNLOAD";
			LOGGER.info("enrich pdf '{}' as '{}' in '{}'", identifier, mimeType, fileGroup);
			String fileId = "PDF_" + identifier;
			var resultText = this.mets.addDownloadFile("DOWNLOAD", fileId, "application/pdf");
			LOGGER.info("integrated pdf fileId '{}' in '{}'", fileId, this.storePath);
			LOGGER.info("integrated mets:agent {}", resultText);
			return true;

		}
		return false;
	}

	@Override
	public DescriptiveMetadata getDescriptiveData() throws DigitalDerivansException {
		if (descriptiveData == null) {
			DescriptiveMetadataBuilder builder = new DescriptiveMetadataBuilder();
			builder.setMetadataStore(this);
			descriptiveData = builder.person().access().identifier().title().urn().year().build();
		} else if (this.xPathIdentifier.isPresent()) {
			// identifier might have changed just for PDF labelling
			DescriptiveMetadataBuilder builder = new DescriptiveMetadataBuilder();
			builder.setMetadataStore(this);
			builder.identifier();
			descriptiveData.setIdentifier(builder.build().getIdentifier());
		}
		return descriptiveData;
	}

	@Override
	public String usedStore() {
		return this.storePath.orElse(IMetadataStore.UNKNOWN);
	}

	public METS getMets() {
		return this.mets;
	}

	/**
	 * 
	 * Store knowledge which physical struct file pointer
	 * references/matches corresponding physical file.
	 * 
	 * @author u.hartwig
	 *
	 */
	static class FilePointerRef {

		private String fileGroup = IMetadataStore.UNKNOWN;
		private String reference = IMetadataStore.UNKNOWN;

		public FilePointerRef() {
		}

		public FilePointerRef(String fileGroup, String reference) {
			this.fileGroup = fileGroup;
			this.reference = reference;
		}

		public String getFileGroup() {
			return fileGroup;
		}

		public String getReference() {
			return reference;
		}

		@Override
		public String toString() {
			return fileGroup + "=>" + reference;
		}
	}
}
