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
import de.ulb.digital.derivans.data.DerivansPathResolver;
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
import de.ulb.digital.derivans.model.DescriptiveData;
import de.ulb.digital.derivans.model.DigitalFooter;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.DigitalStructureTree;
// import de.ulb.digital.derivans.model.PDFMetaInformation;
import de.ulb.digital.derivans.model.step.DerivateStep;
import de.ulb.digital.derivans.model.step.DerivateStepImage;
import de.ulb.digital.derivans.model.step.DerivateStepImageFooter;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;
import de.ulb.digital.derivans.model.step.DerivateType;

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

	private Optional<IMetadataStore> optMetadataStore = Optional.empty();

	// private Optional<Path> optPDFPath = Optional.empty();

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
		LOGGER.info("set derivates pathDir: '{}'", processDir);
		this.resolver = new DerivansPathResolver(this.processDir);
		// this.resolver.setNamePrefixes(this.config.getPrefixes());

		// get common configuration
		// this.pdfMeta = this.config.getPdfMetainformation();

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
			LOGGER.info("set derivans metadata from: '{}'", pathMetsFile);
			this.optMetadataStore = Optional.of(new MetadataStore(pathMetsFile));
		} else {
			LOGGER.info("set derivates pathDir: '{}' without Metadata", processDir);
		}
	}

	/**
	 * 
	 * Examine and tranform given steps into correspondig derivateer-instances
	 * 
	 * @param steps
	 * @return
	 * @throws DigitalDerivansException
	 */
	public List<IDerivateer> init(List<DerivateStep> steps) throws DigitalDerivansException {
		List<IDerivateer> derivateers = new ArrayList<>();
		List<DigitalPage> pages = setDigitalPages(steps);

		for (DerivateStep step : steps) {
			// create basic input-output pair for each Derivateer
			DerivansData input = new DerivansData(step.getInputPath(), DerivateType.IMAGE);
			DerivansData output = new DerivansData(step.getOutputPath(), DerivateType.JPG);

			// respect derivate step type
			DerivateType type = step.getDerivateType();
			if (type == DerivateType.JPG) {
				DerivateStepImage imgStep = (DerivateStepImage)step;
				var quality = imgStep.getQuality();
				var imgDerivateer = new ImageDerivateerJPG(input, output, quality);
				imgDerivateer.setPoolsize(imgStep.getPoolsize());
				imgDerivateer.setMaximal(imgStep.getMaximal());
				imgDerivateer.setOutputPrefix(imgStep.getOutputPrefix());
				imgDerivateer.setDigitalPages(pages);
				derivateers.add(imgDerivateer);

			} else if (type == DerivateType.JPG_FOOTER) {
				DerivateStepImageFooter stepFooter = (DerivateStepImageFooter)step;
				String footerLabel = stepFooter.getFooterLabel();
				Path pathTemplate = stepFooter.getPathTemplate();
				DigitalFooter footer = new DigitalFooter(footerLabel, getIdentifier(), pathTemplate);
				Integer quality = stepFooter.getQuality();
				ImageDerivateerJPGFooter d = new ImageDerivateerJPGFooter(input, output, footer, pages, quality);
				boolean containsGranularUrns = inspect(pages);
				if (containsGranularUrns) {
					d = new ImageDerivateerJPGFooterGranular(d, quality);
				}
				derivateers.add(d);

			} else if (type == DerivateType.PDF) {
				DerivateStepPDF pdfStep = (DerivateStepPDF) step;
				String pdfALevel = DefaultConfiguration.PDFA_CONFORMANCE_LEVEL;
				pdfStep.setConformanceLevel(pdfALevel);
				DigitalStructureTree structure = new DigitalStructureTree();
				DescriptiveData descriptiveData = new DescriptiveData();
				if (this.optMetadataStore.isPresent()) {
					var store = this.optMetadataStore.get();
					descriptiveData = store.getDescriptiveData();
				}
				// calculate identifier
				Path pdfPath = resolver.calculatePDFPath(descriptiveData, step);
				LOGGER.info("calculate local pdf derivate path '{}'", pdfPath);
				output.setPath(pdfPath);
				var pdfDerivateer = new PDFDerivateer(input, output, structure, pages, pdfStep);
				if (this.optMetadataStore.isPresent()) {
					pdfDerivateer.setMetadataStore(optMetadataStore);
				}
				derivateers.add(pdfDerivateer);
			}
		}
		return derivateers;
	}

	private List<DigitalPage> setDigitalPages(List<DerivateStep> steps) {
		DerivateStep step0 = steps.get(0);
		if (this.optMetadataStore.isPresent()) {
			var store = this.optMetadataStore.get();
			var storePath = store.usedStore();
			LOGGER.info("use page information from metadata {}", storePath);
			var pages = store.getDigitalPagesInOrder();
			// sanitize path against input path
			var inputPath = step0.getInputPath();
			for(DigitalPage p : pages) {
				if(!p.getImagePath().isAbsolute()) {
					p.setImagePath(inputPath.resolveSibling(p.getImagePath()));
				}
			}
			return pages;
		} else {
			LOGGER.info("no metadata, resolve page information from '{}'", this.processDir);
			return this.resolver.resolveFromStep(step0);
		}
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

			// // post processing: enrich PDF metadata if exist
			// if (derivateer instanceof PDFDerivateer && optPDFPath.isPresent() && optMetadataStore.isPresent()) {
			// 	Path pdfPath = optPDFPath.get();
			// 	if (Files.exists(pdfPath)) {
			// 		PDFDerivateer pdffer = (PDFDerivateer)derivateer;
			// 		if (!pdffer.getConfig().isEnrichMetadata()) {
			// 			LOGGER.warn("pdf '{}' not enriched in metadata", pdfPath);
			// 		} else {
			// 			LOGGER.info("enrich created pdf '{}' in '{}'", pdfPath, this.pathMetsFile);
			// 			String filename = pdfPath.getFileName().toString();
			// 			String identifier = filename.substring(0, filename.indexOf('.'));
			// 			var store = this.optMetadataStore.get();
			// 			store.enrichPDF(identifier);
			// 		}
			// 	} else {
			// 		String msg3 = "Missing pdf " + pdfPath.toString() + "!";
			// 		LOGGER.error(msg3);
			// 		throw new DigitalDerivansException(msg3);
			// 	}
			// }
		}

		LOGGER.info("finished generating '{}' derivates at '{}'", derivateers.size(), processDir);
	}

	private boolean inspect(List<DigitalPage> pages) {
		return pages.stream()
				.filter(page -> page.getIdentifier().isPresent())
				.map(page -> page.getIdentifier().get())
				.findAny().isPresent();
	}

	private String getIdentifier() throws DigitalDerivansException {
		if (this.optMetadataStore.isPresent()) {
			var store = this.optMetadataStore.get();
			return store.getDescriptiveData().getUrn();
		}
		return IMetadataStore.UNKNOWN;
	}

	public List<DerivateStep> getSteps() {
		return this.steps;
	}
}
