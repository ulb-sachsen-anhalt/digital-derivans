package de.ulb.digital.derivans;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.ulb.digital.derivans.config.DefaultConfiguration;
import de.ulb.digital.derivans.data.IMetadataStore;
import de.ulb.digital.derivans.data.ocr.OCRReaderFactory;
import de.ulb.digital.derivans.derivate.BaseDerivateer;
import de.ulb.digital.derivans.model.DescriptiveData;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.ocr.OCRData;
import de.ulb.digital.derivans.model.step.DerivateStep;
import de.ulb.digital.derivans.model.step.DerivateType;

/**
 * 
 * Unified Path resolving
 * 
 * <p>
 * Check if source paths exist, create target paths (i.e. directories) if
 * necessary.
 * </p>
 * <p>
 * Search for images in given input directories and set output file extensions.
 * </p>
 * <p>
 * Take care of optional inputs like OCR-Data.
 * </p>
 * 
 * @author u.hartwig
 *
 */
public class DerivansPathResolver {

	public static final Logger LOGGER = LogManager.getLogger(DerivansPathResolver.class);

	private Predicate<Path> imageFilter;
	
	private List<String> namePrefixes;
	
	private Path rootDir;

	/**
	 * 
	 * Default constructor: one root directory to rule them all
	 * 
	 * @param processDir
	 */
	public DerivansPathResolver(Path rootDir) {
		this.rootDir = rootDir;
		this.imageFilter = new PredicateFileJPGorTIF();
		this.namePrefixes = new ArrayList<>();
	}

	public DerivansPathResolver() {
		this(null);
	}
	
	/**
	 * 
	 * Set optional {@link DerivateStep} information about used prefixes for images
	 * 
	 * @param prefixes
	 */
	public void setNamePrefixes(List<String> prefixes) {
		this.namePrefixes = new ArrayList<>(prefixes);
	}

	public List<DigitalPage> resolveFromStep(DerivateStep step) {
		List<DigitalPage> pages = new ArrayList<>();
		Path inputPath = step.getInputPath();
		switch (step.getDerivateType()) {
		case JPG:
		case JPG_FOOTER:
		case IMAGE:
		case PDF:
			try {
				if (!Files.exists(inputPath, LinkOption.NOFOLLOW_LINKS)) {
					LOGGER.info("create path '{}'", inputPath);
					Files.createDirectory(inputPath);
				}
				List<Path> paths = getFilePaths(inputPath, imageFilter);
				for (Path path : paths) {
					pages.add(new DigitalPage(path));
				}
			} catch (IOException e) {
				LOGGER.error(e);
			}
			break;

		case UNKNOWN:
			LOGGER.warn("encountered unknown derivate type at step '{}'", step);
		}
		return pages;
	}
	
	
	/**
	 * 
	 * Try to match OCR Files from {@link DefaultConfiguration#DEFAULT_INPUT_FULLTEXT}
	 * _by name_ in file system only.
	 * 
	 * @param pages
	 * @return
	 */
	public List<DigitalPage> enrichOCRFromFilesystem(List<DigitalPage> pages, Path ocrPath) {
		if(this.rootDir == null) {
			LOGGER.warn("cant enrich ocr: root-dir unset!");
			return pages;
		}
		// Path ocrPath = rootDir.resolve(DefaultConfiguration.DEFAULT_INPUT_FULLTEXT);
		if(!Files.exists(ocrPath, LinkOption.NOFOLLOW_LINKS)) {
			LOGGER.warn("cant enrich ocr: invalid path '{}'!", ocrPath);
			return pages;
		}
		List<Path> ocrFiles = new ArrayList<>();
		try (Stream<Path> files = Files.list(ocrPath)) {
			ocrFiles = files.collect(Collectors.toList());
			LOGGER.info("found {} local ocr files in sub dir '{}'", ocrFiles.size(), ocrPath);
		} catch (IOException e) {
			LOGGER.error("fail read {}:{}", ocrPath, e.getMessage());
			return pages;
		}
		
		if(ocrFiles.isEmpty()) {
			LOGGER.warn("cant enrich ocr: no files in '{}'!", ocrPath);
			return pages;
		}
		
		for(DigitalPage page : pages) {
			Path imagePath = page.getImagePath().getFileName();
			String imageName = imagePath.toString().split("\\.")[0];
			for(Path ocrFile : ocrFiles) {
				Path ocrFilePath = ocrFile.getFileName();
				if(ocrFilePath.toString().startsWith(imageName)) {
					try {
						OCRData data = OCRReaderFactory.from(ocrFile).get(ocrFile);
						page.setOcrData(data);
					} catch (DigitalDerivansException e) {
						LOGGER.error("fail read {}:{}", ocrFile, e);
					}
				}
			}
		}
		
		return pages;
	}

	public List<DigitalPage> resolveFromPath(Path inputPath) {
		try {
			List<Path> paths = getFilePaths(inputPath, imageFilter);
			return paths.stream().map(DigitalPage::new).collect(Collectors.toList());
		} catch (IOException e) {
			LOGGER.error("fail resolve {}:{}", inputPath, e.getMessage());
			return new ArrayList<>();
		}
	}

	/**
	 * 
	 * Enrich absolute local paths, which are missing on metadata entries
	 * which are in turn taken from xlink:href attribute.
	 * 
	 * Guess "*.jpg" as local file extension if it's missing.
	 * 
	 * @param pages
	 * @param nextDir
	 * @return
	 */
	public List<DigitalPage> enrichWithPath(List<DigitalPage> pages, Path nextDir) {
		for (DigitalPage page : pages) {
			Path prevPath = page.getImagePath();
			if (!prevPath.isAbsolute()) {
				Path filePath = prevPath.getFileName();
				// inspect file extension
				String fileName = filePath.toString();
				// if no jpg ext and also no tif .. guess jpg
				if (!fileName.endsWith(".jpg") && !fileName.endsWith(".tif")) {
					fileName = fileName + ".jpg";
					filePath = Path.of(fileName);
				}
				Path nextPath = nextDir.resolve(filePath);
				page.setImagePath(nextPath);
			}
		}
		return pages;
	}

	/**
	 * 
	 * Gather Image Path Information regarding a specific image type specified by
	 * fileFilter
	 * 
	 * @param path
	 * @param fileFilter
	 * @return
	 * @throws IOException
	 */
	public static List<Path> getFilePaths(Path path, Predicate<Path> fileFilter) throws IOException {
		try (Stream<Path> filesList = Files.list(path)) {
			return filesList.filter(Files::isRegularFile).filter(fileFilter).sorted().collect(Collectors.toList());
		}
	}

	public DigitalPage setImagePath(DigitalPage page, BaseDerivateer derivateer) {
		String previousFile = page.getImagePath().getFileName().toString();
		String[] parts = previousFile.split("\\.");
		String fileName = parts[0];

		// collect information on derivateer
		DerivateType type = derivateer.getType();
		Optional<String> optPrefix = derivateer.getOutputPrefix();
		Path nextDir = derivateer.getOutput().getPath();

		// respect type
		if (type == DerivateType.JPG || type == DerivateType.JPG_FOOTER) {
			fileName += ".jpg";
		}

		// prefix in name present?
		if(! namePrefixes.isEmpty()) {
			for(String prefix : namePrefixes) {
				if(fileName.contains(prefix)) {
					LOGGER.trace("replace prefix '{}' in '{}'", prefix, fileName);
					fileName = fileName.replace(prefix, "");
				}
			}
		}
		
		// prefix required?
		if (optPrefix.isPresent()) {
			String prefix = optPrefix.get();
			fileName = prefix + fileName;
		}

		// resolve with new parent dir
		Path nextPath = nextDir.resolve(Path.of(fileName));
		page.setImagePath(nextPath);
		
		return page;
	}

	public Path calculatePDFPath(DescriptiveData dd, DerivateStep step) throws DigitalDerivansException {
		Path pdfPath = step.getOutputPath();
		if (!Files.isDirectory(pdfPath, LinkOption.NOFOLLOW_LINKS)) {
			pdfPath = step.getOutputPath().getParent().resolve(pdfPath);
			try {
				LOGGER.warn("create non-existing PDF target path {}", pdfPath);
				Files.createDirectory(pdfPath);
			} catch (IOException e) {
				throw new DigitalDerivansException(e);
			}
		}
		if (Files.isDirectory(pdfPath)) {
			String identifier = dd.getIdentifier();
			// if metadata is *not* present, the identifier must be invalid ("n.a.")
			if (identifier.equals(IMetadataStore.UNKNOWN)) {
				identifier = pdfPath.getFileName().toString();
				LOGGER.warn("invalid descriptive data, use filename '{}' to name PDF-file", identifier);
			}
			LOGGER.info("use '{}' to name PDF-file", identifier);
			String fileName = identifier + ".pdf";
			String prefix = step.getOutputPrefix();
			if (prefix != null && (!prefix.isBlank())) {
				fileName = prefix.concat(fileName);
			}
			return pdfPath.resolve(fileName).normalize();
		}

		// if output path is set as file
		throw new DigitalDerivansException("Can't create PDF: '" + pdfPath + "' invalid!");
	}

	public void enrichAbsoluteStartPath(List<DigitalPage> pages, Path inputPath) {
		for (DigitalPage page : pages) {
			page.setImagePath(inputPath.resolve(page.getImagePath()));
		}
	}
}

class PredicateFileJPGorTIF implements Predicate<Path> {

	@Override
	public boolean test(Path p) {
		String pathString = p.toString();
		return pathString.endsWith(".jpg") || pathString.endsWith(".tif");
	}
}
