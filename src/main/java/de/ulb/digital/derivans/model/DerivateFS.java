package de.ulb.digital.derivans.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.IDerivans;

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

	private Path fulltextDir;

	private Path rootDir;

	private boolean inited;

	private DerivateStruct struct;

	public DerivateFS(Path pathInput) {
		this.pathInput = pathInput;
		this.rootDir = pathInput;
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
			startPath = this.pathInput.resolve(IDerivans.IMAGE_DIR_DEFAULT); // 1st try: look for "DEFAULT"
			if (Files.notExists(startPath)) {
				startPath = this.pathInput.resolve(IDerivans.IMAGE_DIR_ORIGINAL); // 2nd try: look for "ORIGINAL"
				if (Files.notExists(startPath)) {
					startPath = this.pathInput.resolve("."); // hard fallback
				}
			}
		} else {
			if (!startPath.isAbsolute()) {
				startPath = this.rootDir.resolve(startPath);
			}
		}
		if (Files.exists(this.rootDir.resolve(IDerivans.FULLTEXT_DIR))) {
			this.fulltextDir = this.rootDir.resolve(IDerivans.FULLTEXT_DIR);
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
		List<Path> fulltextFiles = new ArrayList<>();
		if (this.fulltextDir != null) {
			fulltextFiles = this.filePathsFrom(this.fulltextDir, ".xml");
		}
		for (int i = 0; i < allFiles.size(); i++) {
			var currentOrder = this.theOrder.getAndIncrement();
			Path currPath = allFiles.get(i);
			String currentId = String.format("FILE_%d04", i + 1);
			DigitalPage dp = new DigitalPage(currentId, currentOrder, currPath);
			dp.setPageLabel(String.format("[%d]", currentOrder));
			String fileName = currPath.getFileName().toString().split(fileExt)[0];
			Optional<Path> fulltextMatch = fulltextFiles.stream()
					.filter(file -> { String pathName = file.getFileName().toString();
									  return pathName.startsWith(fileName) || fileName.startsWith(pathName);
									})
					.findFirst();
			if(fulltextMatch.isPresent()){
				Path match = fulltextMatch.get();
				dp.setOcrFile(match);
			}
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

	/**
	 * 
	 * Intended for testing purposes *ONLY*
	 * 
	 * @param testRoot
	 */
	public void setStructure(DerivateStruct testRoot) {
		this.struct = testRoot;
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
	public Path getRootDir() {
		return this.rootDir;
	}

	@Override
	public boolean isMetadataPresent() {
		return false;
	}

	@Override
	public boolean isInited() {
		return this.inited;
	}

	public void setFulltextdir(Path ftDir) {
		this.fulltextDir = ftDir;
	}

	public Path getFulltextDir() {
		return this.getFulltextDir();
	}
}
