package de.ulb.digital.derivans;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.config.DefaultConfiguration;
import de.ulb.digital.derivans.data.IMetadataStore;
import de.ulb.digital.derivans.data.MetadataStore;
import de.ulb.digital.derivans.derivate.BaseDerivateer;
import de.ulb.digital.derivans.derivate.IDerivateer;
import de.ulb.digital.derivans.derivate.ImageDerivateer;
import de.ulb.digital.derivans.derivate.ImageDerivateerJPGFooter;
import de.ulb.digital.derivans.derivate.ImageDerivateerJPGFooterGranular;
import de.ulb.digital.derivans.derivate.ImageDerivateerJPG;
import de.ulb.digital.derivans.derivate.PDFDerivateer;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DerivateStep;
import de.ulb.digital.derivans.model.DerivateType;
import de.ulb.digital.derivans.model.DescriptiveData;
import de.ulb.digital.derivans.model.DigitalFooter;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.DigitalStructureTree;
import de.ulb.digital.derivans.model.PDFMetaInformation;

/**
 * 
 * Derive digital entities like pages with footer information and PDF from given
 * dataset with images and Metadata in METS/MODS-format
 * 
 * @author hartwig
 */
public class Derivans {

	public static final Logger LOGGER = LogManager.getLogger(Derivans.class);

	public static final String LABEL = "DigitalDerivans";

	List<DerivateStep> steps;

	private Path processDir;

	private Path pathMetsFile;

	private final DerivansConfiguration config;

	private Optional<IMetadataStore> metadataStore = Optional.empty();

	private Optional<Path> optPDFPath = Optional.empty();

	private PDFMetaInformation pdfMeta;

	private DerivansPathResolver resolver;

	boolean footerDerivatesRendered;

	boolean footerDerivatesForPDFRendered;

	/**
	 * 
	 * Initialize Derivans Instance with according {@link DerivansConfiguration}.
	 * 
	 * @param conf
	 * @throws DigitalDerivansException
	 */
	public Derivans(DerivansConfiguration conf) throws DigitalDerivansException {
		this.config = conf;
		Path path = this.config.getPathDir();
		if (!Files.exists(path)) {
			String message = String.format("workdir not existing: '%s'!", path);
			LOGGER.error(message);
			throw new DigitalDerivansException(message);
		} else if (!(Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))) {
			String message = String.format("workdir must be dir: '%s'!", path);
			LOGGER.error(message);
			throw new DigitalDerivansException(message);
		}
		this.processDir = path;
		this.resolver = new DerivansPathResolver(this.processDir);
		this.resolver.setNamePrefixes(this.config.getPrefixes());

		// get common configuration
		this.pdfMeta = this.config.getPdfMetainformation();

		// handle Derivate Steps
		var confSteps = this.config.getDerivateSteps();
		if (confSteps == null || confSteps.isEmpty()) {
			String msg = "DerivateSteps missing!";
			LOGGER.error(msg);
			throw new DigitalDerivansException(msg);
		}
		this.steps = new ArrayList<>(this.config.getDerivateSteps());

		// handle optional METS-file
		var optMetadata = this.config.getMetadataFile();
		if (optMetadata.isPresent()) {
			this.pathMetsFile = optMetadata.get();
			LOGGER.info("set derivates pathDir: '{}', metsFile: '{}'", processDir, pathMetsFile);
			this.metadataStore = Optional.of(new MetadataStore(pathMetsFile, this.config));
		} else {
			LOGGER.info("set derivates pathDir: '{}' without Metadata", processDir);
		}
	}

	public List<IDerivateer> init(List<DerivateStep> steps) throws DigitalDerivansException {
		// determine start path
		DerivateStep step0 = steps.get(0);
		List<DigitalPage> pages = resolver.resolveFromStep(step0);

		DescriptiveData descriptiveData = new DescriptiveData();
		DigitalStructureTree structure = new DigitalStructureTree();

		// set optional metadata information
		if (this.metadataStore.isPresent()) {
			LOGGER.info("use information from optional metadata");
			var store = this.metadataStore.get();
			descriptiveData = store.getDescriptiveData();
			pages = store.getDigitalPagesInOrder();
			resolver.enrichAbsoluteStartPath(pages, this.processDir);
			structure = store.getStructure();
		} else {
			LOGGER.debug("enrich ocr from file system");
			resolver.enrichOCRFromFilesystem(pages);
		}

		List<IDerivateer> derivateers = new ArrayList<>();

		// examine given steps
		for (DerivateStep step : steps) {

			// create default base derivateer
			DerivansData input = new DerivansData(step.getInputPath(), DerivateType.IMAGE);
			DerivansData output = new DerivansData(step.getOutputPath(), DerivateType.JPG);
			BaseDerivateer base = new BaseDerivateer(input, output);
			base.setResolver(resolver);
			base.setDigitalPages(pages);

			// respect type
			DerivateType type = step.getDerivateType();
			if (type == DerivateType.JPG) {
				derivateers.add(transformToJPG(base, step, pages));

			} else if (type == DerivateType.JPG_FOOTER) {
				ImageDerivateerJPGFooter d = transformToJPGFooter(base, step, pages);
				boolean containsGranularUrns = inspect(pages);
				if (containsGranularUrns) {
					d = new ImageDerivateerJPGFooterGranular(d, step.getQuality());
				}
				derivateers.add(d);

			} else if (type == DerivateType.PDF) {
				// calculate final PDF path for post processing of metadata
				Path pdfPath = resolver.calculatePDFPath(descriptiveData, step);
				base.getOutput().setPath(pdfPath);
				String pdfALevel = DefaultConfiguration.PDFA_CONFORMANCE_LEVEL;
				pdfMeta.setConformanceLevel(pdfALevel);
				// merge configuration and metadata
				pdfMeta.mergeDescriptiveData(descriptiveData);
				derivateers.add(new PDFDerivateer(base, structure, pages, pdfMeta));
				optPDFPath = Optional.of(pdfPath);
			}
		}
		return derivateers;
	}

	public void create() throws DigitalDerivansException {

		List<IDerivateer> derivateers = this.init(steps);

		// run derivateers
		for (IDerivateer derivateer : derivateers) {

			int pages = derivateer.getDigitalPages().size();
			String msg = String.format("process '%02d' digital pages", pages);
			LOGGER.info(msg);

			Instant start = Instant.now();

			// forward to actual image creation implementation
			// subject to each concrete subclass
			int results = derivateer.create();

			Instant finish = Instant.now();
			long secsElapsed = Duration.between(start, finish).toSecondsPart();
			long minsElapsed = Duration.between(start, finish).toMinutesPart();

			if (results > 0) {
				String msg2 = String.format("created '%02d' results in '%dm%02ds'",
						results, minsElapsed, secsElapsed);
				LOGGER.info(msg2);
			}

			LOGGER.info("finished derivate step '{}': '{}'", derivateer.getClass().getSimpleName(), true);
		}

		// post processing: enrich PDF metadata if exist
		if (optPDFPath.isPresent() && metadataStore.isPresent()) {
			Path pdfPath = optPDFPath.get();
			if (Files.exists(pdfPath)) {
				if (!this.pdfMeta.isEnrichMetadata()) {
					LOGGER.warn("pdf '{}' not enriched in metadata", pdfPath);
				} else {
					LOGGER.info("enrich created pdf '{}' in '{}'", pdfPath, this.pathMetsFile);
					String filename = pdfPath.getFileName().toString();
					String identifier = filename.substring(0, filename.indexOf('.'));
					var store = this.metadataStore.get();
					store.enrichPDF(identifier);
				}
			} else {
				String msg = "Missing pdf " + pdfPath.toString() + "!";
				LOGGER.error(msg);
				throw new DigitalDerivansException(msg);
			}
		}
		LOGGER.info("finished generating '{}' derivates at '{}'", derivateers.size(), processDir);
	}

	private IDerivateer transformToJPG(BaseDerivateer base, DerivateStep step, List<DigitalPage> pages) {
		Integer quality = step.getQuality();
		ImageDerivateer d = new ImageDerivateerJPG(base, quality);
		d.setPoolsize(step.getPoolsize());
		d.setMaximal(step.getMaximal());
		d.setOutputPrefix(step.getOutputPrefix());
		d.setDigitalPages(pages);
		return d;
	}

	private ImageDerivateerJPGFooter transformToJPGFooter(BaseDerivateer base, DerivateStep step,
			List<DigitalPage> pages)
			throws DigitalDerivansException {
		String recordIdentifier = getIdentifier();
		LOGGER.info("got identifier {}", recordIdentifier);
		String footerLabel = step.getFooterLabel();
		Path pathTemplate = step.getPathTemplate();
		DigitalFooter footer = new DigitalFooter(footerLabel, recordIdentifier, pathTemplate);
		Integer quality = step.getQuality();
		ImageDerivateerJPGFooter d = new ImageDerivateerJPGFooter(base, quality, footer);
		d.setPoolsize(step.getPoolsize());
		d.setMaximal(step.getMaximal());
		return d;
	}

	private boolean inspect(List<DigitalPage> pages) {
		return pages.stream()
				.filter(page -> page.getIdentifier().isPresent())
				.map(page -> page.getIdentifier().get())
				.findAny().isPresent();
	}

	private String getIdentifier() {
		if (this.metadataStore.isPresent()) {
			var store = this.metadataStore.get();
			return store.getDescriptiveData().getUrn();
		}
		return IMetadataStore.UNKNOWN;
	}

}
