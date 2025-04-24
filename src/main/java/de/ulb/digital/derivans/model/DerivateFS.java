package de.ulb.digital.derivans.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.derivate.IDerivateer;

/**
 * 
 * Input, output and final destination of Derivans' efforts.
 * 
 * This kind of derivate describes a flat file system
 * layout, with the entry directory being set as label
 * for the final derivate.
 * 
 * All contained images files are assumed
 * to be JPG files in a sub directory named
 * "MAX" or "DEFAULT"; if nothing found,
 * searches root directory for JPG files.
 * 
 * Images are to be pages of the derivate in
 * flat outline layout in terms of final PDF file.
 * 
 * @author u.hartwig
 */
public class DerivateFS implements IDerivate {

	private Path pathInput;

	private AtomicInteger theOrder = new AtomicInteger(1);

	private String startFileExtension = ".jpg";

	private Path localStartDir;

	private Path pathRootDir;

	private boolean inited;

	private DerivateStruct struct;

	public DerivateFS(Path pathInput) {
		this.pathInput = pathInput;
		this.pathRootDir = pathInput;
	}

	/**
	 * Include minimal sub dir resolving:
	 * 1) ./MAX => default set at object creation
	 * 2) ./DEFAULT
	 * 3) ./<same_dir> which must exist
	 * to search images
	 */
	@Override
	public void init(Path startPath) throws DigitalDerivansException {
		if (startPath == null) {
			startPath = this.pathInput.resolve(IDerivateer.IMAGE_DIR_DEFAULT); // 1st try: look for "DEFAULT"
			if (Files.notExists(startPath)) {
				startPath = this.pathInput.resolve(IDerivateer.IMAGE_DIR_ORIGINAL); // 2nd try: look for "ORIGINAL"
				if (Files.notExists(startPath)) {
					startPath = this.pathInput.resolve("."); // hard fallback
				}
			}
		} else {
			if(!startPath.isAbsolute()) {
				startPath = this.pathRootDir.resolve(startPath);
			}
		}
		this.localStartDir = startPath;
		var label = this.pathInput.getFileName().toString();
		var orderNr = this.theOrder.get();
		this.struct = new DerivateStruct(orderNr, label);
		this.populateStruct(this.startFileExtension);
		this.inited = true;
	}

	private void populateStruct(String fileExt) throws DigitalDerivansException {
		List<Path> allFiles = this.filePathsFrom(this.localStartDir, fileExt);
		for (var file : allFiles) {
			var currentOrder = this.theOrder.getAndIncrement();
			DigitalPage dp = new DigitalPage(currentOrder, file);
			dp.setPageLabel(String.format("[%d]", currentOrder));
			this.struct.getPages().add(dp);
		}
	}

	private List<Path> filePathsFrom(Path theDir, String fileExt) throws DigitalDerivansException {
		List<Path> allFiles = new ArrayList<>();
		try (Stream<Path> stream = Files.list(theDir)) {
			allFiles = stream
					.filter(Files::isRegularFile)
					.filter(f -> f.toString().endsWith(fileExt))
					.collect(Collectors.toList());
		} catch (IOException e) {
			throw new DigitalDerivansException(e);
		}
		Collections.sort(allFiles);
		return allFiles;
	}

	@Override
	public DerivateStruct getStructure() {
		return this.struct;
	}

	@Override
	public List<DigitalPage> getAllPages() {
		List<DigitalPage> pages = new ArrayList<>(this.struct.getPages());
		for (var kids : this.struct.getChildren()) {
			pages.addAll(DerivateFS.getPages(kids));
		}
		return pages;
	}

	public static List<DigitalPage> getPages(DerivateStruct struct) {
		List<DigitalPage> currPages = struct.getPages();
		if (!struct.getChildren().isEmpty()) {
			for (var kid : struct.getChildren()) {
				currPages.addAll(DerivateFS.getPages(kid));
			}
		}
		return currPages;
	}

	@Override
	public String getStartFileExtension() {
		return startFileExtension;
	}

	@Override
	public void setStartFileExtension(String startFileExtension) {
		this.startFileExtension = startFileExtension;
	}

	@Override
	public Path getPathRootDir() {
		return this.pathRootDir;
	}

	@Override
	public boolean isMetadataPresent() {
		return false;
	}

	@Override
	public boolean isInited() {
		return this.inited;
	}

	// @Override
	// public String getImageLocalDir() {
	// 	return this.localStartDir;
	// }

	// @Override
	// public void setImageLocalDir(String localDir) {
	// 	this.localStartDir = localDir;
	// }

	@Override
	public void setOcr(Path ocrPath) throws DigitalDerivansException {
		var ocrDir = this.pathInput.resolve(ocrPath);
		List<Path> ocrFiles = this.filePathsFrom(ocrDir, ".xml");
		for (var p : this.getAllPages()) {
			// var currPageName =
			// p.getImagePath().getFileName().toString().replaceAll("(?<!^)[.][^.]*$", "");
			// for (int i = 0; i < ocrFiles.size(); i++) {
			// String ocrFile = ocrFiles.get(i).getFileName();
			// if (ocrFile.toString().startsWith(currPageName)) {
			// p.setOcrFile(ocrFiles.get(i));
			// break;
			// }
			// }
		}
	}

}
