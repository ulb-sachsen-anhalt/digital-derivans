package de.ulb.digital.derivans.model;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.data.mets.METS;
import de.ulb.digital.derivans.data.mets.METSContainer;
import de.ulb.digital.derivans.data.mets.METSFile;

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

	private Path pathInput;

	private String startFileExtension = ".jpg";

	private String imageGroup = "MAX";

	private String ocrLocalDir = "FULLTEXT";

	private Path pathInputDir;

	private boolean inited;

	private static final AtomicInteger MD_PAGE_ORDER = new AtomicInteger(1);

	private Optional<METS> optMetadata = Optional.empty();

	private DerivateStruct struct;

	private final List<DigitalPage> allPages = new ArrayList<>();

	public DerivateMD(Path pathInput) throws DigitalDerivansException {
		this.pathInput = pathInput;
		METS m = new METS(pathInput);
		m.determine(); // critical
		this.optMetadata = Optional.of(m);
		this.pathInputDir = pathInput.getParent();
	}

	/**
	 * Include minimal resolving of image sub dirs: => MAX, DEFAULT, if exist => use
	 * same directory to search for images
	 */
	@Override
	public void init(Path localStartDir) throws DigitalDerivansException {
		// Path populateFrom = this.pathInputDir;
		// if (startSubDir == null) {
		// // first try
		// startSubDir = pathInput.resolve(this.imageLocalDir);
		// if (Files.notExists(startSubDir)) {
		// startSubDir = pathInput.resolve("DEFAULT");
		// if (Files.notExists(startSubDir)) {
		// startSubDir = pathInput.resolve(".");
		// }
		// }
		// }
		// if (startSubDir != null) {
		// populateFrom = this.pathInputDir.resolve(startSubDir);
		// }
		var orderNr = DerivateMD.MD_PAGE_ORDER.get();
		METSContainer logicalRoot = this.optMetadata.get().getLogicalRoot();
		String logicalLabel = logicalRoot.determineLabel();
		this.struct = new DerivateStruct(orderNr, logicalLabel);
		this.populateStruct(logicalRoot, this.startFileExtension);
		this.inited = true;
	}

	private void populateStruct(METSContainer root, String fileExt) throws DigitalDerivansException {
		METS metsData = this.optMetadata.get();
		if (root.getChildren().isEmpty()) {
			List<METSFile> digiFiles = metsData.getFiles(root, this.imageGroup, fileExt);
			for (var digiFile : digiFiles) {
				Path filePath = this.pathInputDir.resolve(digiFile.getLocalPath());
				int currOrder = DerivateMD.MD_PAGE_ORDER.getAndIncrement();
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
		Integer structOrder = Integer.valueOf(currentCnt.getAttribute("ORDER"));
		DerivateStruct currentStruct = new DerivateStruct(structOrder, logicalLabel);
		parentStruct.getChildren().add(currentStruct);
		if (!currentCnt.getChildren().isEmpty()) {
			for (var subContainer : currentCnt.getChildren()) {
				this.traverseStruct(subContainer, currentStruct, fileExt);
			}
		}
		METS metsData = this.optMetadata.get();
		List<METSFile> digiFiles = metsData.getFiles(currentCnt, this.imageGroup, fileExt);
		for (var digiFile : digiFiles) {
			Path localFilePath = digiFile.getLocalPath();
			int currOrder = DerivateMD.MD_PAGE_ORDER.getAndIncrement();
			DigitalPage page = new DigitalPage(currOrder, localFilePath);
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

	// public static List<DigitalPage> getPages(DerivateStruct struct) {
	// List<DigitalPage> currPages = struct.getPages();
	// if (!struct.getChildren().isEmpty()) {
	// for (var kid : struct.getChildren()) {
	// currPages.addAll(DerivateFS.getPages(kid));
	// }
	// }
	// return currPages;
	// }

	public Path getPathInputDir() {
		return this.pathInputDir;
	}

	@Override
	public Optional<METS> optMetadata() {
		return this.optMetadata;
	}

	@Override
	public boolean isInited() {
		return this.inited;
	}

	@Override
	public String getImageLocalDir() {
		return this.imageGroup;
	}

	@Override
	public void setImageLocalDir(String localDir) {
		this.imageGroup = localDir;
	}

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
}
