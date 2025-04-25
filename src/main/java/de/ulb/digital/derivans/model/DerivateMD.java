package de.ulb.digital.derivans.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.data.mets.DescriptiveMetadataBuilder;
import de.ulb.digital.derivans.data.mets.METS;
import de.ulb.digital.derivans.data.mets.METSContainer;
import de.ulb.digital.derivans.data.mets.METSFile;
import de.ulb.digital.derivans.derivate.IDerivateer;
import de.ulb.digital.derivans.model.pdf.DescriptiveMetadata;

/**
 * 
 * Input, output and final destination of Derivans' efforts.
 * 
 * This kind of derivate describes a local file system
 * layout, with the start directory being set as label
 * for the final derivate.
 * 
 * All contained images files are assumed
 * to be JPG files in a sub directory named
 * alike the corresponding mets:fileGroup
 * like "MAX" or "DEFAULT".
 * 
 * Each image file must be linked to at least one
 * logical section in the logical mets:structMap.
 * As for DFG-METS flavor, this includes an
 * additional physical mets:structMap representing
 * TYPE=page
 * 
 * @author u.hartwig
 */
public class DerivateMD implements IDerivate {

	private String startFileExtension = ".jpg";

	private String imageGroup = IDerivateer.IMAGE_DIR_MAX;

	private String ocrLocalDir = "FULLTEXT";

	private Path pathInputDir;

	private boolean inited;

	private boolean testRessourceExists = true;

	private final AtomicInteger mdPageOrder = new AtomicInteger(1);

	private METS mets;

	private DerivateStruct struct;

	private final List<DigitalPage> allPages = new ArrayList<>();

	private DescriptiveMetadata descriptiveData;

	private String identifierExpression;

	public DerivateMD(Path pathInput) throws DigitalDerivansException {
		METS m = new METS(pathInput);
		m.determine(); // critical
		this.mets = m;
		this.pathInputDir = pathInput.getParent();
	}

	/**
	 * Include minimal resolving of image sub dirs: => MAX, DEFAULT, if exist => use
	 * same directory to search for images
	 */
	@Override
	public void init(Path startPath) throws DigitalDerivansException {
		var orderNr = this.mdPageOrder.get();
		METSContainer logicalRoot = this.mets.getLogicalRoot();
		String logicalLabel = logicalRoot.determineLabel();
		if (startPath == null) {
			startPath = Path.of(IDerivateer.IMAGE_DIR_DEFAULT);
		}
		if(startPath.isAbsolute()) {
			this.imageGroup = startPath.getFileName().toString();
		} else {
			this.imageGroup = startPath.toString();
		}
		this.struct = new DerivateStruct(orderNr, logicalLabel);
		this.populateStruct(logicalRoot, this.startFileExtension);
		this.inited = true;
	}

	private void populateStruct(METSContainer root, String fileExt) throws DigitalDerivansException {
		if (root.getChildren().isEmpty()) {
			List<METSFile> digiFiles = this.mets.getFiles(root, this.imageGroup, fileExt);
			for (var digiFile : digiFiles) {
				Path filePath = this.pathInputDir.resolve(digiFile.getLocalPath(this.testRessourceExists));
				int currOrder = this.mdPageOrder.getAndIncrement();
				DigitalPage page = new DigitalPage(currOrder, filePath);
				this.struct.getPages().add(page);
				this.allPages.add(page); // also store in derivate's list
			}
		} else {
			for (var subContainer : root.getChildren()) {
				this.traverseStruct(subContainer, this.struct, fileExt);
			}
		}
	}

	private void traverseStruct(METSContainer currentCnt, DerivateStruct parentStruct,
			String fileExt) throws DigitalDerivansException {
		String logicalLabel = currentCnt.determineLabel();
		Integer structOrder = -1;
		if (currentCnt.getAttribute("ORDER") != null) {
			structOrder = Integer.valueOf(currentCnt.getAttribute("ORDER"));
		}
		DerivateStruct currentStruct = new DerivateStruct(structOrder, logicalLabel);
		parentStruct.getChildren().add(currentStruct);
		if (!currentCnt.getChildren().isEmpty()) {
			for (var subContainer : currentCnt.getChildren()) {
				this.traverseStruct(subContainer, currentStruct, fileExt);
			}
		}
		List<METSFile> digiFiles = this.mets.getFiles(currentCnt, this.imageGroup, fileExt);
		for (var digiFile : digiFiles) {
			Path localFilePath = digiFile.getLocalPath(this.testRessourceExists);
			int currOrder = this.mdPageOrder.getAndIncrement();
			DigitalPage page = new DigitalPage(currOrder, localFilePath);
			if (digiFile.getPageLabel() != null) {
				page.setPageLabel(digiFile.getPageLabel());
			}
			if (digiFile.getContentIds() != null) {
				page.setIdentifier(digiFile.getContentIds());
			}
			currentStruct.getPages().add(page);
			this.allPages.add(page); // also store in derivate's list
		}
	}

	@Override
	public DerivateStruct getStructure() {
		return this.struct;
	}

	@Override
	public List<DigitalPage> getAllPages() {
		return this.allPages;
	}

	public Path getPathRootDir() {
		return this.pathInputDir;
	}

	@Override
	public boolean isInited() {
		return this.inited;
	}

	// @Override
	// public String getImageLocalDir() {
	// 	return this.imageGroup;
	// }

	// @Override
	// public void setImageLocalDir(String localDir) {
	// 	this.imageGroup = localDir;
	// }

	@Override
	public void setOcr(Path ocrPath) throws DigitalDerivansException {
		// var ocrDir = this.pathInput.resolve(ocrPath);
		// List<Path> ocrFiles = new ArrayList<>();
		// try {
		// this.traverseDirectory(ocrDir, ocrFiles, ".xml");
		// } catch (IOException e) {
		// throw new DigitalDerivansException(e);
		// }
		// for (var p : this.getAllPages()) {
		// var currPageName =
		// p.getImagePath().getFileName().toString().replaceAll("(?<!^)[.][^.]*$", "");
		// for (int i = 0; i < ocrFiles.size(); i++) {
		// var ocrFile = ocrFiles.get(i).getFileName();
		// if (ocrFile.toString().startsWith(currPageName)) {
		// p.setOcrFile(ocrFiles.get(i));
		// ocrFiles.remove(i);
		// }
		// }
		// }
	}

	public void setIdentifierExpression(String xpressiob) {
		this.identifierExpression = xpressiob;
	}

	public DescriptiveMetadata getDescriptiveData() throws DigitalDerivansException {
		DescriptiveMetadataBuilder builder = new DescriptiveMetadataBuilder();
		builder.setMetadataStore(this.mets);
		if (this.identifierExpression != null) { // identifier might have changed for PDF labelling
			builder.setIdentifierExpression(this.identifierExpression);
		}
		this.descriptiveData = builder.person().access().identifier().title().urn().year().build();
		return this.descriptiveData;
	}

	@Override
	public boolean isMetadataPresent() {
		return true;
	}

	@Override
	public String getStartFileExtension() {
		return startFileExtension;
	}

	@Override
	public void setStartFileExtension(String startFileExtension) {
		this.startFileExtension = startFileExtension;
	}

	// public Path setPDFPath(Path pdfDir, String identifier, DerivateStepPDF step)
	// {
	// // String identifier = dd.getIdentifier();
	// // if metadata is *not* present, the identifier must be invalid ("n.a.")
	// if (identifier.equals(IMetadataStore.UNKNOWN)) {
	// identifier = pdfDir.getFileName().toString();
	// // LOGGER.warn("invalid descriptive data, use '{}' to name PDF-file",
	// identifier);
	// }
	// if(step.getNamePDF().isPresent()) {
	// var namePDF = step.getNamePDF().get();
	// identifier = namePDF;
	// }
	// // LOGGER.info("use '{}' to name PDF-file", identifier);
	// String fileName = identifier;
	// if (! identifier.endsWith(".pdf")) {
	// fileName = fileName + ".pdf";
	// }
	// String prefix = step.getOutputPrefix();
	// if (prefix != null && (!prefix.isBlank())) {
	// fileName = prefix.concat(fileName);
	// }
	// return pdfDir.resolve(fileName).normalize();
	// }

	public String getIdentifierURN() {
		return this.mets.getPrimeMODS().getIdentifierURN();
	}

	public METS getMets() {
		return this.mets;
	}

	public void setRessourceExists(boolean check) {
		this.testRessourceExists = check;
	}

	public boolean isGranularIdentifierPresent() {
		return this.getAllPages().stream()
				.filter(page -> page.optIdentifier().isPresent())
				.map(page -> page.optIdentifier().get())
				.findAny().isPresent();
	}

}
