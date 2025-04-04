package de.ulb.digital.derivans.model;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.data.mets.METS;

/**
 * 
 * Input, output and final destination of Derivans' efforts.
 * 
 * @author u.hartwig
 */
public class DerivateFS implements IDerivate {

	private Path pathInput;

	private String startFileExtension = ".jpg";

	private Path pathInputDir;

	private boolean inited;

	private Optional<METS> optMetadata = Optional.empty();

	private DerivateStruct struct = new DerivateStruct();

	public DerivateFS(Path pathInput) {
		this.pathInput = pathInput;
		if (Files.isDirectory(pathInput)) {
			this.pathInputDir = pathInput;
			this.struct.label = this.pathInput.getFileName().toString();
			// } else if (Files.isRegularFile(pathInput, LinkOption.NOFOLLOW_LINKS)) {
			// METS m = new METS(pathInput);
			// this.optMetadata = Optional.of(m);
			// this.pathInputDir = pathInput.getParent();
		}
	}

	/**
	 * Include minimal resolving of image sub dirs: => MAX, DEFAULT, if exist => use
	 * same directory to search for images
	 */
	@Override
	public void init(Path startSubDir) throws DigitalDerivansException {
		Path populateFrom = this.pathInputDir;
		if (startSubDir == null) {
			// first try
			startSubDir = pathInput.resolve("MAX");
			if (Files.notExists(startSubDir)) {
				startSubDir = pathInput.resolve("DEFAULT");
				if (Files.notExists(startSubDir)) {
					startSubDir = pathInput.resolve(".");
				}
			}
		}
		if (startSubDir != null) {
			populateFrom = this.pathInputDir.resolve(startSubDir);
		}
		this.populateStruct(populateFrom, this.startFileExtension);
		this.inited = true;
	}

	private void populateStruct(Path rootDir, String fileExt) throws DigitalDerivansException {
		List<Path> allFiles = new ArrayList<>();
		try {
			listAllFiles(rootDir, allFiles, fileExt);
		} catch (IOException e) {
			throw new DigitalDerivansException(e);
		}
		for (var file : allFiles) {
			this.struct.pages.add(new DigitalPage(file));
		}
	}

	private static void listAllFiles(Path currentPath, List<Path> allFiles, String fileExt) throws IOException {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentPath)) {
			for (Path entry : stream) {
				if (Files.isDirectory(entry)) {
					listAllFiles(entry, allFiles, fileExt);
				} else if (entry.toString().toLowerCase().endsWith(fileExt)) {
					allFiles.add(entry);
				}
			}
		}
	}

	@Override
	public List<DigitalPage> getAllPages() {
		List<DigitalPage> pages = this.struct.pages;
		for (var kids : this.struct.children) {
			pages.addAll(DerivateFS.getPages(kids));
		}
		return pages;
	}

	public static List<DigitalPage> getPages(DerivateStruct struct) {
		List<DigitalPage> currPages = struct.pages;
		if (!struct.children.isEmpty()) {
			for (var kid : struct.children) {
				currPages.addAll(DerivateFS.getPages(kid));
			}
		}
		return currPages;
	}

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
	public void setOcr(Path ocrPath) throws DigitalDerivansException {
		var ocrDir = this.pathInput.resolve(ocrPath);
		List<Path> ocrFiles = new ArrayList<>() ;
		try {
			DerivateFS.listAllFiles(ocrDir, ocrFiles, ".xml");
		} catch (IOException e) {
			throw new DigitalDerivansException(e);
		}
		for(var p : this.getAllPages()) {
			var currPageName = p.getImagePath().getFileName().toString().replaceAll("(?<!^)[.][^.]*$", ""); 
			for (int i=0; i < ocrFiles.size(); i++) {
				var ocrFile = ocrFiles.get(i).getFileName();
				if (ocrFile.toString().startsWith(currPageName)) {
					p.setOcrFile(ocrFiles.get(i));
					ocrFiles.remove(i);
				}
			}
		}
	}
}
