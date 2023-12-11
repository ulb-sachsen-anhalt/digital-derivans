package de.ulb.digital.derivans;

import de.ulb.digital.derivans.config.DefaultConfiguration;
import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.data.DerivansPathResolver;
import de.ulb.digital.derivans.data.IMetadataStore;
import de.ulb.digital.derivans.data.MetadataStore;
import de.ulb.digital.derivans.derivate.*;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DescriptiveData;
import de.ulb.digital.derivans.model.DigitalFooter;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.step.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Derive digital entities like pages with footer information and PDF from given
 * dataset with images and optional OCR-and metadata (METS/MODS)
 *
 * @author hartwig
 */
public class Derivans {

    public static final Logger LOGGER = LogManager.getLogger(Derivans.class);

    public static final String LABEL = "DigitalDerivans";

    List<DerivateStep> steps;

	List<IDerivateer> derivateers;

    private Path processDir;

    private Path pathMetsFile;

    private final DerivansConfiguration config;

    private Optional<IMetadataStore> optMetadataStore = Optional.empty();

    private DerivansPathResolver resolver;

    boolean footerDerivatesRendered;

    boolean footerDerivatesForPDFRendered;

    /**
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
        this.resolver.setNamePrefixes(this.config.getDerivatePrefixes());
        var confSteps = this.config.getDerivateSteps();
        if (confSteps == null || confSteps.isEmpty()) {
            String msg = "DerivateSteps missing!";
            LOGGER.error(msg);
            throw new DigitalDerivansException(msg);
        }
        this.steps = new ArrayList<>(this.config.getDerivateSteps());
        // optional METS-file
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
     * Transform given steps into corresponding derivateers
     *
     * @param steps
     * @return
     * @throws DigitalDerivansException
     */
    public List<IDerivateer> init(List<DerivateStep> steps) throws DigitalDerivansException {
        this.derivateers = new ArrayList<>();
        List<DigitalPage> pages = initDigitalPages(steps);

        for (DerivateStep step : steps) {
            // create basic input-output pair for each Derivateer
            DerivansData input = new DerivansData(step.getInputPath(), DerivateType.IMAGE);
            DerivansData output = new DerivansData(step.getOutputPath(), DerivateType.JPG);

            // respect derivate step type
            DerivateType type = step.getDerivateType();
            if (type == DerivateType.JPG) {
                DerivateStepImage imgStep = (DerivateStepImage) step;
                var quality = imgStep.getQuality();
                var imgDerivateer = new ImageDerivateerJPG(input, output, quality);
                imgDerivateer.setPoolsize(imgStep.getPoolsize());
                imgDerivateer.setMaximal(imgStep.getMaximal());
                imgDerivateer.setOutputPrefix(imgStep.getOutputPrefix());
                imgDerivateer.setDigitalPages(pages);
                imgDerivateer.setResolver(this.resolver);
                derivateers.add(imgDerivateer);

            } else if (type == DerivateType.JPG_FOOTER) {
                DerivateStepImageFooter stepFooter = (DerivateStepImageFooter) step;
                String footerLabel = stepFooter.getFooterLabel();
                Path pathTemplate = stepFooter.getPathTemplate();
                DigitalFooter footer = new DigitalFooter(footerLabel, getIdentifier(), pathTemplate);
                Integer quality = stepFooter.getQuality();
                ImageDerivateerJPGFooter d = new ImageDerivateerJPGFooter(input, output, footer, pages, quality);
				if (this.optMetadataStore.isPresent()) {
					var store = this.optMetadataStore.get();
					var mdsPages = store.getDigitalPagesInOrder();
					var storeLabel = store.usedStore();
					if (isGranularIdentifierPresent(mdsPages)) {
						LOGGER.debug("detected granular URN at {}", storeLabel);
						var enrichedPages = enrichGranularURN(pages, mdsPages);
						d = new ImageDerivateerJPGFooterGranular(d, quality);
						d.setDigitalPages(enrichedPages);
					}
				}
                d.setResolver(this.resolver);
                derivateers.add(d);

            } else if (type == DerivateType.PDF) {
                DerivateStepPDF pdfStep = (DerivateStepPDF) step;
                String pdfALevel = DefaultConfiguration.PDFA_CONFORMANCE_LEVEL;
                pdfStep.setConformanceLevel(pdfALevel);
                DescriptiveData descriptiveData = new DescriptiveData();
                if (this.optMetadataStore.isPresent()) {
                    var store = this.optMetadataStore.get();
                    String storePath = store.usedStore();
                    LOGGER.info("use metadata from {}", storePath);
                    // if store present, set additional information
                    pdfStep.getModsIdentifierXPath().ifPresent(store::setIdentifierExpression);
                    store.setFileGroupOCR(pdfStep.getParamOCR());
                    store.setFileGroupImages(pdfStep.getParamImages());
                    descriptiveData = store.getDescriptiveData();
                } else {
                    LOGGER.info("enrich ocr from file system via {}", pdfStep.getParamOCR());
                    Path ocrPath = this.processDir.resolve(pdfStep.getParamOCR());
                    resolver.enrichOCRFromFilesystem(pages, ocrPath);
                }
                // calculate identifier
                Path pdfPath = resolver.calculatePDFPath(descriptiveData, step);
                LOGGER.info("calculate local pdf derivate path '{}'", pdfPath);
                output.setPath(pdfPath);
				// required for proper resolving with kitodo2
				var pdfInput = new DerivansData(input.getPath(), DerivateType.JPG);
                var pdfDerivateer = new PDFDerivateer(pdfInput, output, pages, pdfStep);
                if (this.optMetadataStore.isPresent()) {
                    pdfDerivateer.setMetadataStore(optMetadataStore);
                }
                pdfDerivateer.setResolver(this.resolver);
                derivateers.add(pdfDerivateer);
            }
        }
        return derivateers;
    }

    /**
     * Initialize digital pages from first step's input
     *
     * @param steps
     * @return
     */
    private List<DigitalPage> initDigitalPages(List<DerivateStep> steps) {
        DerivateStep step0 = steps.get(0);
        LOGGER.info("resolve page information from local root dir '{}'", this.processDir);
        return this.resolver.resolveFromStep(step0);
    }

	/**
	 * 
	 * Enrich page-wise URN from first matching filename
	 * 
	 * @param targets
	 * @param sources
	 * @return
	 */
	private List<DigitalPage> enrichGranularURN(List<DigitalPage> targets, List<DigitalPage> sources) {
		List<DigitalPage> enriched = new ArrayList<>();
		for(DigitalPage target: targets) {
			for(DigitalPage src: sources) {
				var targetFile = target.getImagePath().getFileName();
				var sourceFile = src.getImagePath().getFileName();
				if (targetFile.equals(sourceFile)) {
					Optional<String> optIdent = src.optIdentifier();
					if(optIdent.isPresent()) {
						target.setIdentifier(optIdent.get());
						enriched.add(0, target);
						break;
					}
				}
			}
		}
		return enriched;
	}

    public void create() throws DigitalDerivansException {
        this.init(steps);

        // run derivateers
        for (IDerivateer derivateer : derivateers) {
            int pages = derivateer.getDigitalPages().size();
            String msg = String.format("process '%02d' digital pages", pages);
            LOGGER.info(msg);

            Instant start = Instant.now();
            // forward to actual implementation
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

        LOGGER.info("finished generating '{}' derivates at '{}'", derivateers.size(), processDir);
    }

    private boolean isGranularIdentifierPresent(List<DigitalPage> pages) {
        return pages.stream()
                .filter(page -> page.optIdentifier().isPresent())
                .map(page -> page.optIdentifier().get())
                .findAny().isPresent();
    }

    private String getIdentifier() throws DigitalDerivansException {
        if (this.optMetadataStore.isPresent()) {
            var store = this.optMetadataStore.get();
            return store.getDescriptiveData().getUrn();
        }
        return IMetadataStore.UNKNOWN;
    }

	/**
	 * 
	 * Access member just for testing purposes
	 * 
	 * @return
	 */
    public List<DerivateStep> getSteps() {
        return this.steps;
    }

	/**
	 * 
	 * Access member just for testing purposes
	 * 
	 * @return
	 */
	public List<IDerivateer> getDerivateers() {
		return this.derivateers;
	}
}
