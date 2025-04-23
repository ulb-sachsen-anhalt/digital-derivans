package de.ulb.digital.derivans;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.ulb.digital.derivans.config.DefaultConfiguration;
import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.derivate.IDerivateer;
import de.ulb.digital.derivans.derivate.image.ImageDerivateerJPG;
import de.ulb.digital.derivans.derivate.image.ImageDerivateerJPGFooter;
import de.ulb.digital.derivans.derivate.image.ImageDerivateerJPGFooterGranular;
import de.ulb.digital.derivans.derivate.pdf.PDFDerivateer;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DerivateFS;
import de.ulb.digital.derivans.model.DerivateMD;
import de.ulb.digital.derivans.model.DigitalFooter;
import de.ulb.digital.derivans.model.IDerivate;
import de.ulb.digital.derivans.model.pdf.DescriptiveMetadata;
import de.ulb.digital.derivans.model.step.DerivateStep;
import de.ulb.digital.derivans.model.step.DerivateStepImage;
import de.ulb.digital.derivans.model.step.DerivateStepImageFooter;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;
import de.ulb.digital.derivans.model.step.DerivateType;

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

    private final DerivansConfiguration config;

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
        var confSteps = this.config.getDerivateSteps();
        if (confSteps == null || confSteps.isEmpty()) {
            String msg = "DerivateSteps missing!";
            LOGGER.error(msg);
            throw new DigitalDerivansException(msg);
        }
        this.steps = new ArrayList<>(this.config.getDerivateSteps());
    }

    public void create() throws DigitalDerivansException {
        for (IDerivateer derivateer : this.derivateers) {
            Instant start = Instant.now();
            int results = derivateer.create();
            Instant finish = Instant.now();
            long secsElapsed = Duration.between(start, finish).toSecondsPart();
            long minsElapsed = Duration.between(start, finish).toMinutesPart();
            if (results > 0) {
                LOGGER.info("created %02d results in %02dm%02ds", results, minsElapsed,
                        secsElapsed);
            }
            LOGGER.info("finished derivate step %s: %s",
                    derivateer.getClass().getSimpleName(), true);
        }

        LOGGER.info("finished %02d steps at %s", this.steps.size(),
                this.derivate.getPathRootDir());
    }

    /**
     * Transform given steps into corresponding derivateers
     *
     * @param steps
     * @return
     * @throws DigitalDerivansException
     */
    public List<IDerivateer> init(Path theInput) throws DigitalDerivansException {
        LOGGER.info("set derivate in %s", theInput);
        if (Files.isDirectory(theInput)) {
            this.derivate = new DerivateFS(theInput);
        } else if (Files.isRegularFile(theInput, LinkOption.NOFOLLOW_LINKS)) {
            this.derivate = new DerivateMD(theInput);
        }
        this.derivateers = new ArrayList<>();
        for (DerivateStep step : this.steps) {
            String pathInp = step.getInputSubDir();
            String pathOut = step.getOutputSubDir();
            DerivansData inputDerivansData = new DerivansData(this.derivate.getPathRootDir(), pathInp, DerivateType.IMAGE);
            DerivansData outputDerivansData = new DerivansData(this.derivate.getPathRootDir(), pathOut, step.getDerivateType());
            if(inputDerivansData.getType() == DerivateType.TIF) {
                this.derivate.setStartFileExtension(".tif");
            }
            if (!this.derivate.isInited()) {
                this.derivate.init(step.getInputSubDir());
            }
            DerivateType type = step.getDerivateType();
            IDerivateer derivateer = this.getForType(type);
            derivateer.setInput(inputDerivansData);
            derivateer.setOutput(outputDerivansData);
            derivateer.setDerivate(derivate);
            if (type == DerivateType.JPG) {
                DerivateStepImage imgStep = (DerivateStepImage) step;
                var imgDerivateer = (ImageDerivateerJPG) derivateer;
                imgDerivateer.setQuality(imgStep.getQuality());
                imgDerivateer.setPoolsize(imgStep.getPoolsize());
                imgDerivateer.setMaximal(imgStep.getMaximal());
                imgDerivateer.setOutputPrefix(imgStep.getOutputPrefix());
            } else if (type == DerivateType.JPG_FOOTER) {
                DerivateStepImageFooter stepFooter = (DerivateStepImageFooter) step;
                var footerDerivateer = (ImageDerivateerJPGFooter) derivateer;
                String footerLabel = stepFooter.getFooterLabel();
                int quality = stepFooter.getQuality();
                footerDerivateer.setQuality(quality);
                Path pathTemplate = stepFooter.getPathTemplate();
                DigitalFooter footerUnknown = new DigitalFooter(footerLabel, IDerivateer.UNKNOWN, pathTemplate);
                footerDerivateer.setFooter(footerUnknown);
                if (this.derivate.isMetadataPresent()) {
                    var derivateMD = (DerivateMD) this.derivate;
                    String workIdentifier = derivateMD.getIdentifierURN();
                    DigitalFooter footerWithIdent = new DigitalFooter(footerLabel, workIdentifier, pathTemplate);
                    if (derivateMD.isGranularIdentifierPresent()) {
                        footerDerivateer = new ImageDerivateerJPGFooterGranular(footerDerivateer);
                    }
                    footerDerivateer.setFooter(footerWithIdent);
                    derivateer = footerDerivateer;
                }
                footerDerivateer.setDerivate(derivate);
            } else if (type == DerivateType.PDF) {
                DerivateStepPDF pdfStep = (DerivateStepPDF) step;
                String pdfALevel = DefaultConfiguration.PDFA_CONFORMANCE_LEVEL;
                pdfStep.setConformanceLevel(pdfALevel);
                pdfStep.setDebugRender(config.isDebugPdfRender());
                this.setPDFPath(pdfStep);
                if (pdfStep.getParamOCR() != null) {
                    Path ocrPath = this.derivate.getPathRootDir().resolve(pdfStep.getParamOCR());
                    if (Files.isDirectory(ocrPath)) {
                        LOGGER.info("enrich optional ocr from directory %s", pdfStep.getParamOCR());
                        this.derivate.setOcr(ocrPath);
                    }
                }
                ((PDFDerivateer) derivateer).setPDFStep(pdfStep);
            }
            this.derivateers.add(derivateer);
        }
        return this.derivateers;
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

    public IDerivateer getForType(DerivateType dType) throws DigitalDerivansException {
        if (dType == DerivateType.JPG || dType == DerivateType.IMAGE) {
            return new ImageDerivateerJPG();
        } else if( dType == DerivateType.JPG_FOOTER) {
            return new ImageDerivateerJPGFooter();
        } else if (dType == DerivateType.PDF) {
            return new PDFDerivateer();
        }
        throw new DigitalDerivansException("Unknown type "+dType);
    }

    private void setPDFPath(DerivateStepPDF pdfStep) throws DigitalDerivansException {
        Path   rootDir = this.derivate.getPathRootDir();
        String pdfName = rootDir.getFileName().toString() + ".pdf"; // default PDF name like workdir
        if (this.derivate instanceof DerivateMD) {
            var derivateMd = (DerivateMD) this.derivate;
            pdfStep.getModsIdentifierXPath().ifPresent(derivateMd::setIdentifierExpression);
            DescriptiveMetadata descriptiveData = derivateMd.getDescriptiveData();
            pdfName = descriptiveData.getIdentifier()+ ".pdf"; // if metadata present, use as PDF name
        }
        String finalPDFName = pdfStep.getNamePDF().orElse(pdfName); // id arg passed, use as PDF name
        if(!finalPDFName.endsWith(".pdf")) {
            finalPDFName += ".pdf";
        }
        if(Path.of(finalPDFName).isAbsolute()) {
            pdfStep.setPathPDF(Path.of(finalPDFName));
        } else {
            pdfStep.setPathPDF(rootDir.resolve(finalPDFName));
        }
    }
}
