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
import de.ulb.digital.derivans.model.DerivateFS;
import de.ulb.digital.derivans.model.DerivateMD;
import de.ulb.digital.derivans.model.IDerivate;
import de.ulb.digital.derivans.model.step.DerivateStep;
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

    /**
     * Transform given steps into sequence of {@link Generator generators}
     *
     * @param steps
     * @return
     * @throws DigitalDerivansException
     */
    public List<Generator> init(Path theInput) throws DigitalDerivansException {
        LOGGER.info("set derivate in %s", theInput);
        if (Files.isDirectory(theInput)) {
            this.derivate = new DerivateFS(theInput);
            String ocrDir = this.config.getParamOCR().orElse(IDerivans.FULLTEXT_DIR);
            Path ocrDirPath = Path.of(ocrDir);
            if (Files.exists(theInput.resolve(ocrDirPath))) {
                LOGGER.info("set fulltext dir {} for {}", ocrDir, this.derivate);
                ((DerivateFS) this.derivate).setFulltextdir(Path.of(ocrDir));
            } else {
                LOGGER.error("param fulltext dir {} not present in {}!", ocrDir, theInput);
            }
        } else if (Files.isRegularFile(theInput, LinkOption.NOFOLLOW_LINKS)) {
            this.derivate = new DerivateMD(theInput);
            var optOCRParam = this.config.getParamOCR();
            if (optOCRParam.isPresent()) {
                String ocrParam = optOCRParam.get();
                LOGGER.warn("param fulltext dir {} set but ignored because METS/MODS present!", ocrParam);
            }
        }
        this.generators = new ArrayList<>();
        for (DerivateStep step : this.steps) {
            if (!this.derivate.isInited()) {
                if (step.getInputType() == DerivateType.TIF) {
                    this.derivate.setStartFileExtension(".tif");
                }
                this.derivate.init(Path.of(step.getInputDir()));
            }
            DerivateType type = step.getOutputType();
            Generator theGenerator = Derivans.forType(type);
            theGenerator.setDerivate(derivate); // first set derivate ...
            if (type == DerivateType.PDF) { // some peculiar PDF parameters
                DerivateStepPDF pdfStep = (DerivateStepPDF) step;
                pdfStep.setConformanceLevel(DefaultConfiguration.PDFA_CONFORMANCE_LEVEL);
            }
            theGenerator.setStep(step); // .. then set step object
            this.generators.add(theGenerator);
        }
        return this.generators;
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

    public static Generator forType(DerivateType dType) throws DigitalDerivansException {
        if (dType == DerivateType.JPG || dType == DerivateType.IMAGE) {
            return new GeneratorImageJPG();
        } else if (dType == DerivateType.JPG_FOOTER) {
            return new GeneratorImageJPGFooter();
        } else if (dType == DerivateType.PDF) {
            return new GeneratorPDF();
        }
        throw new DigitalDerivansException("Unknown type " + dType);
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
    public List<Generator> getGenerators() {
        return this.generators;
    }
}
