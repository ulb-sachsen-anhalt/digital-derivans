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
import de.ulb.digital.derivans.generate.Generator;
import de.ulb.digital.derivans.generate.GeneratorImageJPG;
import de.ulb.digital.derivans.generate.GeneratorImageJPGFooter;
import de.ulb.digital.derivans.generate.GeneratorPDF;
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

    List<Generator> generators;

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

    public void forward() throws DigitalDerivansException {
        for (Generator generator : this.generators) {
            Instant start = Instant.now();
            int results = generator.create();
            Instant finish = Instant.now();
            long secsElapsed = Duration.between(start, finish).toSecondsPart();
            long minsElapsed = Duration.between(start, finish).toMinutesPart();
            if (results > 0) {
                LOGGER.info("created %02d results in %02dm%02ds", results, minsElapsed,
                        secsElapsed);
            }
            LOGGER.info("finished derivate step %s: %s",
                    generator.getClass().getSimpleName(), true);
        }

        LOGGER.info("finished %02d steps at %s", this.steps.size(),
                this.derivate.getRootDir());
    }

    /**
     * Transform given steps into corresponding derivateers
     *
     * @param steps
     * @return
     * @throws DigitalDerivansException
     */
    public List<Generator> init(Path theInput) throws DigitalDerivansException {
        LOGGER.info("set derivate in %s", theInput);
        if (Files.isDirectory(theInput)) {
            this.derivate = new DerivateFS(theInput);
        } else if (Files.isRegularFile(theInput, LinkOption.NOFOLLOW_LINKS)) {
            this.derivate = new DerivateMD(theInput);
        }
        this.generators = new ArrayList<>();
        for (DerivateStep step : this.steps) {
            if (!this.derivate.isInited()) {
                if(step.getInputType() == DerivateType.TIF) {
                    this.derivate.setStartFileExtension(".tif");
                }
                this.derivate.init(Path.of(step.getInputDir()));
            }
            // String pathInp = step.getInputDir();
            // String pathOut = step.getOutputDir();
            // DerivansData inputDerivansData = new DerivansData(this.derivate.getRootDir(), pathInp, step.getInputType());
            // DerivansData outputDerivansData = new DerivansData(this.derivate.getRootDir(), pathOut, step.getOutputType());
            DerivateType type = step.getOutputType();
            Generator theGenerator = this.forType(type);
            theGenerator.setDerivate(derivate); // first set derivate ...
            if (type == DerivateType.PDF) {     // some peculiar PDF parameters
                DerivateStepPDF pdfStep = (DerivateStepPDF) step;
		        pdfStep.setConformanceLevel(DefaultConfiguration.PDFA_CONFORMANCE_LEVEL);
		        pdfStep.setDebugRender(config.isDebugPdfRender());
            }
            theGenerator.setStep(step);         // .. then set step object
            // theGenerator.setInput(inputDerivansData);
            // theGenerator.setOutput(outputDerivansData);
            // theGenerator.setDerivate(derivate);
            if (type == DerivateType.JPG) {
                // DerivateStepImage imgStep = (DerivateStepImage) step;
                // var genImage = (GeneratorImageJPG) theGenerator;
                // genImage.setQuality(imgStep.getQuality());
                // genImage.setPoolsize(imgStep.getPoolsize());
                // genImage.setMaximal(imgStep.getMaximal());
                // genImage.setOutputPrefix(imgStep.getOutputPrefix());
                // genImage.setInputPrefix(imgStep.getInputPrefix()); // check for chained derivates !!!!
            } else if (type == DerivateType.JPG_FOOTER) {
                // DerivateStepImageFooter stepFooter = (DerivateStepImageFooter) step;
                // var footerDerivateer = (GeneratorImageJPGFooter) theGenerator;
                // String footerLabel = stepFooter.getFooterLabel();
                // int quality = stepFooter.getQuality();
                // footerDerivateer.setQuality(quality);
                // Path pathTemplate = stepFooter.getPathTemplate();
                // DigitalFooter footerUnknown = new DigitalFooter(footerLabel, IDerivans.UNKNOWN, pathTemplate);
                // footerDerivateer.setFooter(footerUnknown);
                // if (this.derivate.isMetadataPresent()) {
                //     var derivateMD = (DerivateMD) this.derivate;
                //     String workIdentifier = derivateMD.getIdentifierURN();
                //     DigitalFooter footerWithIdent = new DigitalFooter(footerLabel, workIdentifier, pathTemplate);
                //     if (derivateMD.isGranularIdentifierPresent()) {
                //         footerDerivateer = new GeneratorImageJPGFooter(footerDerivateer);
                //     }
                //     footerDerivateer.setFooter(footerWithIdent);
                //    theGenerator = footerDerivateer;
                // }
                // footerDerivateer.setDerivate(derivate);
            } else if (type == DerivateType.PDF) {
                // DerivateStepPDF pdfStep = (DerivateStepPDF) step;
                // String pdfALevel = DefaultConfiguration.PDFA_CONFORMANCE_LEVEL;
		        // pdfStep.setConformanceLevel(pdfALevel);
		        // pdfStep.setDebugRender(config.isDebugPdfRender());
                // String pdfALevel = DefaultConfiguration.PDFA_CONFORMANCE_LEVEL;
                // pdfStep.setConformanceLevel(pdfALevel);
                // pdfStep.setDebugRender(config.isDebugPdfRender());
                // this.setPDFPath(pdfStep);
                // GeneratorPDF genPDF = (GeneratorPDF) theGenerator;
                // genPDF.setPDFStep(pdfStep);
                // if(this.derivate.isMetadataPresent()) {
                //     genPDF.setMETS(((DerivateMD)this.derivate).getMets());
                // }
                // genPDF.setStructure(this.derivate.getStructure());
            }
            this.generators.add(theGenerator);
        }
        return this.generators;
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
    public List<Generator> getDerivateers() {
        return this.generators;
    }

    public Generator forType(DerivateType dType) throws DigitalDerivansException {
        if (dType == DerivateType.JPG || dType == DerivateType.IMAGE) {
            return new GeneratorImageJPG();
        } else if( dType == DerivateType.JPG_FOOTER) {
            return new GeneratorImageJPGFooter();
        } else if (dType == DerivateType.PDF) {
            return new GeneratorPDF();
        }
        throw new DigitalDerivansException("Unknown type "+dType);
    }

    // private void setPDFPath(DerivateStepPDF pdfStep) throws DigitalDerivansException {
    //     Path   rootDir = this.derivate.getRootDir();
    //     String pdfName = rootDir.getFileName().toString() + ".pdf"; // default PDF name like workdir
    //     if (this.derivate instanceof DerivateMD) {
    //         var derivateMd = (DerivateMD) this.derivate;
    //         pdfStep.getModsIdentifierXPath().ifPresent(derivateMd::setIdentifierExpression);
    //         DescriptiveMetadata descriptiveData = derivateMd.getDescriptiveData();
    //         pdfName = descriptiveData.getIdentifier()+ ".pdf"; // if metadata present, use as PDF name
    //     }
    //     String finalPDFName = pdfStep.getNamePDF().orElse(pdfName); // if name arg passed, use as PDF name
    //     if(!finalPDFName.endsWith(".pdf")) {
    //         finalPDFName += ".pdf";
    //     }
    //     if(Path.of(finalPDFName).isAbsolute()) {
    //         pdfStep.setPathPDF(Path.of(finalPDFName));
    //     } else {
    //         pdfStep.setPathPDF(rootDir.resolve(finalPDFName));
    //     }
    // }
}
