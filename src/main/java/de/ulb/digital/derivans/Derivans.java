package de.ulb.digital.derivans;

import de.ulb.digital.derivans.config.DefaultConfiguration;
import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.data.io.DerivansPathResolver;
import de.ulb.digital.derivans.data.IMetadataStore;
import de.ulb.digital.derivans.data.mets.METS;
import de.ulb.digital.derivans.data.mets.MetadataStore;
import de.ulb.digital.derivans.derivate.*;
import de.ulb.digital.derivans.derivate.image.ImageDerivateerJPG;
import de.ulb.digital.derivans.derivate.image.ImageDerivateerJPGFooter;
import de.ulb.digital.derivans.derivate.image.ImageDerivateerJPGFooterGranular;
import de.ulb.digital.derivans.derivate.pdf.PDFDerivateer;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DerivateFS;
import de.ulb.digital.derivans.model.DigitalFooter;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.IDerivate;
import de.ulb.digital.derivans.model.pdf.DescriptiveMetadata;
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

    public static final Logger LOGGER = LogManager.getFormatterLogger(Derivans.class);

    public static final String LABEL = "DigitalDerivans";

    List<DerivateStep> steps;

    List<IDerivateer> derivateers;

    private IDerivate derivate;

    private Path derivateDir;

    // private Path processMDFile;

    private final DerivansConfiguration config;

    // private Optional<IMetadataStore> optMetadataStore = Optional.empty();

    // private Optional<METS> optMetadata = Optional.empty();

    // private DerivansPathResolver resolver;

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
        // this.processDir = path;
        // LOGGER.info("set derivates pathDir: '{}'", processDir);
        // this.resolver = new DerivansPathResolver(this.processDir);
        // this.resolver.setNamePrefixes(this.config.getDerivatePrefixes());
        var confSteps = this.config.getDerivateSteps();
        if (confSteps == null || confSteps.isEmpty()) {
            String msg = "DerivateSteps missing!";
            LOGGER.error(msg);
            throw new DigitalDerivansException(msg);
        }
        this.steps = new ArrayList<>(this.config.getDerivateSteps());
    }

    public void create(Path pathInput) throws DigitalDerivansException {
        this.init(pathInput);
        var theInput = this.derivate.getPathInputDir();
        List<DigitalPage> pages = new ArrayList<>();
        for (DerivateStep step : this.steps) {
        	if (!this.derivate.isInited()) {
        		this.derivate.init(step.getInputPath());
        		pages = this.derivate.getAllPages();
        	}
        	var pathOut = theInput.resolve(step.getOutputPath());
            DerivansData input = new DerivansData(this.derivate.getPathInputDir(), DerivateType.IMAGE);
            DerivansData output = new DerivansData(pathOut, DerivateType.JPG);
            DerivateType type = step.getDerivateType();
            IDerivateer derivateer = null;
            if (type == DerivateType.JPG) {
                DerivateStepImage imgStep = (DerivateStepImage) step;
                var quality = imgStep.getQuality();
                derivateer= new ImageDerivateerJPG(input, output, quality);
                ((ImageDerivateerJPG)derivateer).setPoolsize(imgStep.getPoolsize());
                ((ImageDerivateerJPG)derivateer).setMaximal(imgStep.getMaximal());
                ((ImageDerivateerJPG)derivateer).setOutputPrefix(imgStep.getOutputPrefix());
                derivateer.setDigitalPages(pages);
            } else if (type == DerivateType.JPG_FOOTER) {
                DerivateStepImageFooter stepFooter = (DerivateStepImageFooter) step;
                String footerLabel = stepFooter.getFooterLabel();
                Path pathTemplate = stepFooter.getPathTemplate();
                String ident = IMetadataStore.UNKNOWN;
                ImageDerivateerJPGFooter d = null;
                if (this.derivate.optMetadata().isPresent()) {
                    ident = this.derivate.optMetadata().get().getPrimeMODS().getIdentifierURN();
                    DigitalFooter footer = new DigitalFooter(footerLabel, ident, pathTemplate);
                    Integer quality = stepFooter.getQuality();
                    derivateer = new ImageDerivateerJPGFooter(input, output, footer, pages, quality);
                    var store = this.derivate.optMetadata().get();
                    // var mdsPages = store.getDigitalPagesInOrder();
                    // // var storeLabel = store.usedStore();
                    // if (this.isGranularIdentifierPresent(mdsPages)) {
                    //     LOGGER.debug("detected granular URN at {}", storeLabel);
                    //     var enrichedPages = this.enrichGranularURN(pages, mdsPages);
                    //     d = new ImageDerivateerJPGFooterGranular(d, quality);
                    //     d.setDigitalPages(enrichedPages);
                    // }
                }
            } else if (type == DerivateType.PDF) {
                DerivateStepPDF pdfStep = (DerivateStepPDF) step;
                String pdfALevel = DefaultConfiguration.PDFA_CONFORMANCE_LEVEL;
                pdfStep.setConformanceLevel(pdfALevel);
                pdfStep.setDebugRender(config.isDebugPdfRender());
                DescriptiveMetadata descriptiveData = new DescriptiveMetadata();
                // if store present, set additional information
                if (this.derivate.optMetadata().isPresent()) {
                    var store = this.derivate.optMetadata().get();
                    // String storePath = store.usedStore();
                    // LOGGER.info("use metadata from {}", storePath);
                    // pdfStep.getModsIdentifierXPath().ifPresent(store::setIdentifierExpression);
                    // store.setFileGroupOCR(pdfStep.getParamOCR());
                    // store.setFileGroupImages(pdfStep.getParamImages());
                    // descriptiveData = store.getDescriptiveData();
                } else {
                    Path ocrPath = this.derivateDir.resolve(pdfStep.getParamOCR());
                    if (Files.isDirectory(ocrPath)) {
                    	LOGGER.info("enrich optional ocr from directory %s", pdfStep.getParamOCR());
                    	this.derivate.setOcr(ocrPath);
                    }
                    // resolver.enrichOCRFromFilesystem(pages, ocrPath);
                }
                // calculate identifier
                // Path pdfPath = resolver.calculatePDFPath(descriptiveData, pdfStep);
                // LOGGER.info("calculate local pdf derivate path '{}'", pdfPath);
                // output.setPath(pdfPath);
                // required for proper resolving with kitodo2
                var pdfInput = new DerivansData(input.getPath(), DerivateType.JPG);
                derivateer = new PDFDerivateer(pdfInput, output, pages, pdfStep);
                if (this.derivate.optMetadata().isPresent()) {
                    // ToDo
                    //pdfDerivateer.setMetadataStore(optMetadataStore);
                }
            }
            Instant start = Instant.now();
            int results = derivateer.create();
            Instant finish = Instant.now();
            long secsElapsed = Duration.between(start, finish).toSecondsPart();
            long minsElapsed = Duration.between(start, finish).toMinutesPart();
            if (results > 0) {
            	LOGGER.info("created %02d results in %02dm%02ds", results, minsElapsed, secsElapsed);
            }
            LOGGER.info("finished derivate step %s: %s", derivateer.getClass().getSimpleName(), true);
        }

        
        // run derivateers
//        for (IDerivateer derivateer : derivateers) {
//            int pages = derivateer.getDigitalPages().size();
//            String msg = String.format("process '%02d' digital pages", pages);
//            LOGGER.info(msg);
//
//            Instant start = Instant.now();
//            int results = derivateer.create();
//            Instant finish = Instant.now();
//            long secsElapsed = Duration.between(start, finish).toSecondsPart();
//            long minsElapsed = Duration.between(start, finish).toMinutesPart();
//
//            if (results > 0) {
//                String msg2 = String.format("created '%02d' results in '%dm%02ds'",
//                        results, minsElapsed, secsElapsed);
//                LOGGER.info(msg2);
//            }
//            LOGGER.info("finished derivate step '{}': '{}'", derivateer.getClass().getSimpleName(), true);
//        }

        LOGGER.info("finished %02d steps at %s", this.steps.size(), derivateDir);
    }

    /**
     * Transform given steps into corresponding derivateers
     *
     * @param steps
     * @return
     * @throws DigitalDerivansException
     */
    private List<IDerivateer> init(Path theInput) throws DigitalDerivansException {
    	LOGGER.info("set derivate in %s", theInput);
        if (Files.isDirectory(theInput)) {
            this.derivate = new DerivateFS(theInput);
            // this.pathInputDir = pathInput;
            // this.struct.label = this.pathInput.getFileName().toString();
        // } else if (Files.isRegularFile(pathInput, LinkOption.NOFOLLOW_LINKS)) {
        //     METS m = new METS(pathInput);
        //     this.optMetadata = Optional.of(m);
        //     this.pathInputDir = pathInput.getParent();
        }
        this.derivateDir = theInput;
        this.derivateers = new ArrayList<>();
//        this.derivate.init(theInput);
//        var pages = this.derivate.getAllPages();
//        for (DerivateStep step : this.steps) {
//            // create basic input-output pair for each Derivateer
//        	var pathIn = theInput.resolve(step.getInputPath());
//        	var pathOut = theInput.resolve(step.getOutputPath());
//            DerivansData input = new DerivansData(pathIn, DerivateType.IMAGE);
//            DerivansData output = new DerivansData(pathOut, DerivateType.JPG);
//            // respect derivate step type
//            DerivateType type = step.getDerivateType();
//            if (type == DerivateType.JPG) {
//                DerivateStepImage imgStep = (DerivateStepImage) step;
//                var quality = imgStep.getQuality();
//                var imgDerivateer = new ImageDerivateerJPG(input, output, quality);
//                imgDerivateer.setPoolsize(imgStep.getPoolsize());
//                imgDerivateer.setMaximal(imgStep.getMaximal());
//                imgDerivateer.setOutputPrefix(imgStep.getOutputPrefix());
//                imgDerivateer.setDigitalPages(pages);
//                // imgDerivateer.setResolver(this.resolver);
//                derivateers.add(imgDerivateer);
//            } else if (type == DerivateType.JPG_FOOTER) {
//                DerivateStepImageFooter stepFooter = (DerivateStepImageFooter) step;
//                String footerLabel = stepFooter.getFooterLabel();
//                Path pathTemplate = stepFooter.getPathTemplate();
//                String ident = IMetadataStore.UNKNOWN;
//                ImageDerivateerJPGFooter d = null;
//                if (this.derivate.optMetadata().isPresent()) {
//                    ident = this.derivate.optMetadata().get().getPrimeMODS().getIdentifierURN();
//                    DigitalFooter footer = new DigitalFooter(footerLabel, ident, pathTemplate);
//                    Integer quality = stepFooter.getQuality();
//                    d = new ImageDerivateerJPGFooter(input, output, footer, pages, quality);
//                    var store = this.derivate.optMetadata().get();
//                    // var mdsPages = store.getDigitalPagesInOrder();
//                    // // var storeLabel = store.usedStore();
//                    // if (this.isGranularIdentifierPresent(mdsPages)) {
//                    //     LOGGER.debug("detected granular URN at {}", storeLabel);
//                    //     var enrichedPages = this.enrichGranularURN(pages, mdsPages);
//                    //     d = new ImageDerivateerJPGFooterGranular(d, quality);
//                    //     d.setDigitalPages(enrichedPages);
//                    // }
//                }
//                // d.setResolver(this.resolver);
//                derivateers.add(d);
//            } else if (type == DerivateType.PDF) {
//                DerivateStepPDF pdfStep = (DerivateStepPDF) step;
//                String pdfALevel = DefaultConfiguration.PDFA_CONFORMANCE_LEVEL;
//                pdfStep.setConformanceLevel(pdfALevel);
//                pdfStep.setDebugRender(config.isDebugPdfRender());
//                DescriptiveMetadata descriptiveData = new DescriptiveMetadata();
//                if (this.derivate.optMetadata().isPresent()) {
//                    var store = this.derivate.optMetadata().get();
//                    // String storePath = store.usedStore();
//                    // LOGGER.info("use metadata from {}", storePath);
//                    // // if store present, set additional information
//                    // pdfStep.getModsIdentifierXPath().ifPresent(store::setIdentifierExpression);
//                    // store.setFileGroupOCR(pdfStep.getParamOCR());
//                    // store.setFileGroupImages(pdfStep.getParamImages());
//                    // descriptiveData = store.getDescriptiveData();
//                } else {
//                    LOGGER.info("enrich ocr from file system via {}", pdfStep.getParamOCR());
//                    Path ocrPath = this.derivateDir.resolve(pdfStep.getParamOCR());
//                    // resolver.enrichOCRFromFilesystem(pages, ocrPath);
//                }
//                // calculate identifier
//                // Path pdfPath = resolver.calculatePDFPath(descriptiveData, pdfStep);
//                // LOGGER.info("calculate local pdf derivate path '{}'", pdfPath);
//                // output.setPath(pdfPath);
//                // required for proper resolving with kitodo2
//                var pdfInput = new DerivansData(input.getPath(), DerivateType.JPG);
//                var pdfDerivateer = new PDFDerivateer(pdfInput, output, pages, pdfStep);
//                if (this.derivate.optMetadata().isPresent()) {
//                    // ToDo
//                    //pdfDerivateer.setMetadataStore(optMetadataStore);
//                }
//                // pdfDerivateer.setResolver(this.resolver);
//                derivateers.add(pdfDerivateer);
//            }
//        }
        return derivateers;
    }

    /**
     * Initialize digital pages from first step's input
     *
     * @param steps
     * @return
     */
    // private List<DigitalPage> initDigitalPages(List<DerivateStep> steps) {
    // DerivateStep step0 = steps.get(0);
    // LOGGER.info("resolve page information from local root dir '{}'",
    // this.derivansDir);
    // return this.resolver.resolveFromStep(step0);
    // }

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
        for (DigitalPage target : targets) {
            for (DigitalPage src : sources) {
                var targetFile = target.getImagePath().getFileName();
                var sourceFile = src.getImagePath().getFileName();
                if (targetFile.equals(sourceFile)) {
                    Optional<String> optIdent = src.optIdentifier();
                    if (optIdent.isPresent()) {
                        target.setIdentifier(optIdent.get());
                        enriched.add(0, target);
                        break;
                    }
                }
            }
        }
        return enriched;
    }



    private boolean isGranularIdentifierPresent(List<DigitalPage> pages) {
        return pages.stream()
                .filter(page -> page.optIdentifier().isPresent())
                .map(page -> page.optIdentifier().get())
                .findAny().isPresent();
    }

    // private String getIdentifier() throws DigitalDerivansException {
    // if (this.optMetadataStore.isPresent()) {
    // var store = this.optMetadataStore.get();
    // return store.getDescriptiveData().getUrn();
    // }
    // return IMetadataStore.UNKNOWN;
    // }

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
