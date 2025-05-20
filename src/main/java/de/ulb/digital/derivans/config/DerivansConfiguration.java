package de.ulb.digital.derivans.config;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.model.step.DerivateStep;
import de.ulb.digital.derivans.model.step.DerivateStepImage;
import de.ulb.digital.derivans.model.step.DerivateStepImageFooter;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;
import de.ulb.digital.derivans.model.step.DerivateType;

/**
 * 
 * Derivans Application Configuration
 * 
 * Beside the main configuration file, for example "derivans.ini", it may include
 * further ressources referenced in this file, like logging configuration or
 * path to an optional footer logo image. If for the latter two relative paths
 * are provided, then it is assumed that they reside in the same directory
 * than the main configuration file.
 * 
 * @author u.hartwig
 *
 */
public class DerivansConfiguration {

	public static final Logger LOGGER = LogManager.getLogger(DerivansConfiguration.class);

	private DerivansParameter parameter;

	private Path pathConfigFile;

	private Integer quality = DefaultConfiguration.DEFAULT_QUALITY;

	private Integer defaultPoolsize = DefaultConfiguration.DEFAULT_POOLSIZE;

	private List<DerivateStep> derivateSteps = new ArrayList<>();

	/**
	 * 
	 * Default constructor from given parameters.
	 * 
	 * 1) Determine configuration provided and if so, parse it.
	 * If not file present, use fallback configuration
	 * contained in Derivans Jar "derivans.ini" or if
	 * this file is missing, use programmed fallback cfg.
	 * 
	 * 2) If optional CLI-parameters present, use
	 * these of overwrite settings from configuration file
	 * of set special information
	 * 
	 * @param params
	 * @throws DigitalDerivansException
	 */
	public DerivansConfiguration(DerivansParameter params) throws DigitalDerivansException {
		this.parameter = params;
		if (params.getPathConfig() != null) {
			LOGGER.debug("inspect cli config file {}", params.getPathConfig());
			this.pathConfigFile = params.getPathConfig();
			this.initConfigurationFromFile();
		} else {
			Path defaultConfigLocation = Path.of(".").resolve("config")
					.resolve(DefaultConfiguration.DEFAULT_CONFIG_FILE_LABEL);
			LOGGER.info("no config from cli, inspect default {}", defaultConfigLocation);
			if (!Files.exists(defaultConfigLocation)) {
				LOGGER.warn("no config file '{}'", defaultConfigLocation);
			} else {
				this.pathConfigFile = defaultConfigLocation;
				LOGGER.info("found config file at default location '{}'", defaultConfigLocation);
				this.initConfigurationFromFile();
			}
		}
		// take care if no configuration file present
		if (derivateSteps.isEmpty()) {
			List<DerivateStep> defaultSteps = DefaultConfiguration.provideDefaultSteps();
			if (this.parameter.getImages() != null) {
				defaultSteps.get(0).setInputDir(this.parameter.getImages());
			}
			this.derivateSteps = new ArrayList<>(defaultSteps);
			LOGGER.warn("no config read, use fallback with {} steps", this.derivateSteps.size());
		}
		LOGGER.info("use configuration with {} steps", this.derivateSteps.size());
		LOGGER.debug("first inputs from '{}'", this.derivateSteps.get(0).getInputDir());
		// inspect further (optional) parameters
		this.processOptParameters(params);
	}

	private void initConfigurationFromFile() throws DigitalDerivansException {
		if (!this.pathConfigFile.toString().endsWith(".ini")) {
			LOGGER.warn("consider to change '{}' file ext to '.ini'", this.pathConfigFile);
		}
		LOGGER.info("use configuration {}", this.pathConfigFile);
		INIConfiguration conf = new INIConfiguration();
		parse(conf);
		evaluate(conf);
	}

	/**
	 * 
	 * (Optional) CLI Parameters which *might* override
	 * previously configured settings
	 * 
	 * @param params
	 */
	private void processOptParameters(DerivansParameter params) {
		// determine if debug render for PDF desired
		var pdfStep = this.firstStepByClazz("DerivateStepPDF");
		if (Boolean.TRUE.equals(params.isDebugPdfRender())) {
			pdfStep.ifPresent(step -> ((DerivateStepPDF) step).setDebugRender(Boolean.TRUE));
		}
		// take care of different initial image directory/group
		var firstStep = this.derivateSteps.get(0);
		if (params.getImages() != null && firstStep instanceof DerivateStepImage) {
			DerivateStepImage imgStep = (DerivateStepImage) firstStep;
			String prevImg = imgStep.getInputDir();
			LOGGER.info("set image input from {} to {}",
					prevImg, params.getImages());
			imgStep.setInputDir(params.getImages());
		}
		if (params.getNamePDF() != null) {
			var pdfName = params.getNamePDF();
			if (pdfStep.isPresent()) {
				((DerivateStepPDF) pdfStep.get()).setNamePDF(pdfName);
				LOGGER.info("force PDF name {}", pdfName);
			} else {
				LOGGER.warn("refuse to set {} - no PDF step present", pdfName);
			}
		}
		var theStep = this.firstStepByClazz("DerivateStepImageFooter");
		if (params.getPathFooter() != null) {
			var newTemplate = params.getPathFooter();
			if (theStep.isPresent()) {
				if (!newTemplate.isAbsolute()) {
					newTemplate = this.pathConfigFile.getParent().resolve(newTemplate);
				}
				DerivateStepImageFooter footerStep = (DerivateStepImageFooter) theStep.get();
				var footerPrev = footerStep.getPathTemplate();
				LOGGER.info("set footer from {} to {}",
						footerPrev, newTemplate);
				footerStep.setPathTemplate(newTemplate);
			} else {
				LOGGER.warn("refuse to set {} - no footer step present", newTemplate);
			}
		}
	}

	public Integer getQuality() {
		return this.quality;
	}

	public Integer getDefaultPoolsize() {
		return defaultPoolsize;
	}

	public void setDefaultPoolsize(Integer poolsize) {
		this.defaultPoolsize = poolsize;
	}

	public List<DerivateStep> getDerivateSteps() {
		return derivateSteps;
	}

	/**
	 * 
	 * Configuration is expect to follow common *.ini-Style
	 * with starting global section for logging and defaults
	 * and following sections prefixed with "derivate_"
	 * combined with an 2-digit numerical mark like
	 * "derivate_01", "derivate_02" and so on.
	 * 
	 * If a step requires the output of an previous step, then
	 * this previous step section is expected to occour *forehand*.
	 * 
	 * @param conf
	 * @throws DigitalDerivansException
	 */
	private void evaluate(INIConfiguration conf) throws DigitalDerivansException {
		// read global configuration
		if (conf.containsKey("default_quality")) {
			this.quality = conf.getInt("default_quality");
		}
		if (conf.containsKey("default_poolsize")) {
			this.defaultPoolsize = conf.getInt("default_poolsize");
		}
		if (conf.containsKey("logger_configuration_file")) {
			String logFile = conf.getString("logger_configuration_file");
			Path pathLogFile = Path.of(logFile);
			if (!pathLogFile.isAbsolute()) {
				pathLogFile = this.pathConfigFile.getParent().resolve(pathLogFile);
			}
			if (Files.exists(pathLogFile)) {
				LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
				context.setConfigLocation(pathLogFile.toUri());
				LOGGER.info("reconfigured application logging using '{}'", pathLogFile);
				LOGGER.info("configuration file '{}'", pathConfigFile);
			} else {
				LOGGER.warn("invalid location of logger configuration: '{}'. use default configuration.", pathLogFile);
			}
		}

		// read specific derivate sections
		List<String> sectionLabels = conf.getSections().stream().filter(Objects::nonNull).collect(Collectors.toList());
		LOGGER.info("process {} dedicated derivate step sections", sectionLabels.size());
		Collections.sort(sectionLabels, Comparator.comparing(String::toLowerCase));
		for(var sectionLabel : sectionLabels) {
			DerivateStep step = getDerivateStepIO(conf, sectionLabel);
			if (step instanceof DerivateStepImage) {
				this.enrichImageDerivateInformation((DerivateStepImage) step, conf, sectionLabel);
				// propably additional information required
				if (step instanceof DerivateStepImageFooter) {
					this.enrichImageFooterInformation((DerivateStepImageFooter) step, conf, sectionLabel);
				}
			} else if (step instanceof DerivateStepPDF) {
				enrichPDFDerivateInformation((DerivateStepPDF) step, conf, sectionLabel);
			}
			this.derivateSteps.add(step);
		}
	}

	private static <T> Optional<T> extractValue(INIConfiguration conf, String key, Class<T> clazz) {
		if (conf.containsKey(key)) {
			return Optional.of(conf.get(clazz, key));
		}
		return Optional.empty();
	}

	private void parse(INIConfiguration conf) throws DigitalDerivansException {
		try {
			conf.read(new FileReader(this.pathConfigFile.toFile()));
			LOGGER.info("read configuration from '{}'", this.pathConfigFile);
		} catch (ConfigurationException | IOException e) {
			LOGGER.error(e);
			throw new DigitalDerivansException(e);
		}
	}

	/**
	 * 
	 * Determine Derivate Step Type by generated Output
	 * 
	 * @param conf
	 * @param stepSection
	 * @return derivateStep
	 * @throws DigitalDerivansException
	 */
	public DerivateStep getStepFor(INIConfiguration conf, String stepSection) throws DigitalDerivansException {
		String keyOutType = stepSection + ".output_type";
		DerivateType outPutType = DerivateType.JPG;
		Optional<String> optOutType = extractValue(conf, keyOutType, String.class);
		if (optOutType.isPresent()) {
			outPutType = DerivateType.forLabel(optOutType.get());
		}
		DerivateStep step = null;
		if (outPutType == DerivateType.PDF) {
			step = new DerivateStepPDF();
		} else if (outPutType == DerivateType.JPG) {
			step = new DerivateStepImage();
			if (isFooterDerivateSection(conf, stepSection)) {
				step = new DerivateStepImageFooter((DerivateStepImage) step);
			}
		} else {
			throw new DigitalDerivansException("No DerivateType for " + stepSection + "!");
		}
		step.setOutputType(outPutType);
		return step;
	}

	/**
	 * 
	 * Create {@link DerivateStep derivate step instance}
	 * along with some basic information
	 * 
	 * @param conf
	 * @param stepSection
	 * @return
	 * @throws DigitalDerivansException
	 */
	public DerivateStep getDerivateStepIO(INIConfiguration conf, String stepSection)
			throws DigitalDerivansException {
		DerivateStep step = getStepFor(conf, stepSection);
		// input
		String keyInputDir = stepSection + ".input_dir";
		extractValue(conf, keyInputDir, String.class).ifPresent(step::setInputDir);
		extractValue(conf, stepSection + ".input_type", String.class).ifPresent(step::setInputTypeFromLabel);
		// output
		String keyOutputDir = stepSection + ".output_dir";
		extractValue(conf, keyOutputDir, String.class).ifPresent(step::setOutputDir);
		extractValue(conf, stepSection + ".output_type", String.class).ifPresent(step::setOutputTypeFromLabel);
		// optional prefixes for additional derivates
		extractValue(conf, stepSection + ".input_prefix", String.class).ifPresent(step::setInputPrefix);
		extractValue(conf, stepSection + ".output_prefix", String.class).ifPresent(step::setOutputPrefix);
		return step;
	}

	protected void enrichImageDerivateInformation(DerivateStepImage step, INIConfiguration conf, String stepSection)
			throws DigitalDerivansException {
		// poolsize
		step.setPoolsize(this.getDefaultPoolsize());
		String keyPoolsize = stepSection + ".poolsize";
		extractValue(conf, keyPoolsize, Integer.class).ifPresent(step::setPoolsize);
		// quality
		step.setQuality(this.getQuality());
		String keyQuality = stepSection + ".quality";
		extractValue(conf, keyQuality, Integer.class).ifPresent(step::setQuality);
		// maximal dimension, width or height
		String keyMaximal = stepSection + ".maximal";
		Optional<Integer> optMaximal = extractValue(conf, keyMaximal, Integer.class);
		if (optMaximal.isPresent()) {
			step.setMaximal(optMaximal.get()); // might raise Exception
		}
	}

	protected void enrichImageFooterInformation(DerivateStepImageFooter step, INIConfiguration conf,
			String stepSection) {
		String keyTemplate = stepSection + ".footer_template";
		Optional<String> optTemplate = extractValue(conf, keyTemplate, String.class);
		if (optTemplate.isPresent()) {
			Path pathTemplate = Path.of(optTemplate.get());
			if (!pathTemplate.isAbsolute()) {
				pathTemplate = this.pathConfigFile.getParent().resolve(pathTemplate);
			}
			if (Files.exists(pathTemplate)) {
				LOGGER.info("set footer template '{}'", pathTemplate);
				step.setPathTemplate(pathTemplate);
			} else {
				LOGGER.warn("invalid footer template '{}'", pathTemplate);
			}
			// image footer label copyright
			String keyFooterCopy = stepSection + ".footer_label_copyright";
			Optional<String> optFooterCopy = extractValue(conf, keyFooterCopy, String.class);
			if (optFooterCopy.isPresent()) {
				step.setFooterLabel(optFooterCopy.get());
			}
		}
	}

	protected void enrichPDFDerivateInformation(DerivateStepPDF step, INIConfiguration conf, String section)
			throws DigitalDerivansException {
		String keyMetadataCreator = section + ".metadata_creator";
		Optional<String> optCreator = extractValue(conf, keyMetadataCreator, String.class);
		if (optCreator.isPresent()) {
			step.setCreator(optCreator);
		}
		// metadata keyword pdf
		String keyMetadataKeyword = section + ".metadata_keywords";
		Optional<String> optKeywords = extractValue(conf, keyMetadataKeyword, String.class);
		if (optKeywords.isPresent()) {
			step.setKeywords(optKeywords);
		}
		// metadata licence
		String keyMetadataLicense = section + ".metadata_license";
		Optional<String> optLicense = extractValue(conf, keyMetadataLicense, String.class);
		if (optLicense.isPresent()) {
			step.setLicense(optLicense);
		}
		// pdf image dpi for scaling image data
		String keyPdfImageDPI = section + ".image_dpi";
		Optional<String> optImageDpi = extractValue(conf, keyPdfImageDPI, String.class);
		if (optImageDpi.isPresent()) {
			step.setImageDpi(Integer.valueOf(optImageDpi.get()));
		}
		// on which level optional text to render: per word, per line ... ?
		String keyPdfRenderLvl = section + ".render_text_level";
		Optional<String> optRenderLvl = extractValue(conf, keyPdfRenderLvl, String.class);
		if (optRenderLvl.isPresent()) {
			LOGGER.debug("set render text level '{}'", optRenderLvl.get());
			step.setRenderLevel(TypeConfiguration.get(optRenderLvl.get()));
		}
		// on which level optional text to render: per word, per line ... ?
		String keyPdfRenderVis = section + ".render_text_visibility";
		Optional<String> optRenderVis = extractValue(conf, keyPdfRenderVis, String.class);
		if (optRenderVis.isPresent()) {
			LOGGER.debug("set render text visibility '{}'", optRenderVis.get());
			step.setRenderModus(TypeConfiguration.get(optRenderVis.get()));
		}
		// disable automated enrichment of created PDF file into metadata file
		String keyPdfEnrichMeta = section + "." + DefaultConfiguration.Key.PDF_ENRICH_METADATA;
		Optional<String> optEnrichMeta = extractValue(conf, keyPdfEnrichMeta, String.class);
		if (optEnrichMeta.isPresent()) {
			String enrichMetaStr = optEnrichMeta.get();
			boolean mustEnrich = Boolean.parseBoolean(enrichMetaStr);
			LOGGER.debug("try to set enrich metadata to '{}'", mustEnrich);
			step.setEnrichMetadata(mustEnrich);
			step.setEnrichMetadata(mustEnrich);
		}
		// search optional xpath to get pdf label
		String optionPdfIdentifier = section + "." + DefaultConfiguration.Key.PDF_MODS_IDENTIFIER_XPATH;
		Optional<String> optPdfIdentifier = extractValue(conf, optionPdfIdentifier, String.class);
		if (optPdfIdentifier.isPresent()) {
			String pdfIdentXPath = optPdfIdentifier.get();
			LOGGER.debug("set pdf identifier xpath '{}'", pdfIdentXPath);
			step.setModsIdentifierXPath(pdfIdentXPath);
		}
		// optional try to set certain pdf archive conformance degree
		String optionConformace = section + "." + DefaultConfiguration.Key.PDF_CONFORMANCE;
		Optional<String> optConformance = extractValue(conf, optionConformace, String.class);
		if (optConformance.isPresent()) {
			String pdfConf = optConformance.get();
			LOGGER.debug("set pdf/a conformance '{}'", pdfConf);
			step.setConformanceLevel(pdfConf);
		}
		// images and filegroup param
		String optionPdfImageGroup = section + "." + DefaultConfiguration.Key.PDF_METS_FILEGROUP_IMAGES;
		Optional<String> optPdfImageGroup = extractValue(conf, optionPdfImageGroup, String.class);
		// only set image dir/group when PDF is the only step to go
		if (optPdfImageGroup.isPresent() && this.derivateSteps.size() == 1) {
			String pdfImageGroup = optPdfImageGroup.get();
			LOGGER.debug("set fulltext input for pdf '{}'", pdfImageGroup);
			step.setInputDir(pdfImageGroup);
		}
	}

	private static boolean isFooterDerivateSection(INIConfiguration conf, String section) {
		Iterator<String> itKeys = conf.configurationAt(section).getKeys();
		while (itKeys.hasNext()) {
			if (itKeys.next().contains("footer")) {
				return true;
			}
		}
		return false;
	}

	public Optional<String> getParamOCR() {
		if (this.parameter != null && this.parameter.getOcr() != null) {
			return Optional.of(this.parameter.getOcr());
		}
		return Optional.empty();
	}

	/**
	 * Pick *very first* step matching {@link DerivateType}
	 * 
	 * @param type
	 * @return
	 */
	private Optional<DerivateStep> firstStepByClazz(String clazzLabel) {
		return this.derivateSteps.stream()
				.filter(s -> s.getClass().getSimpleName().equals(clazzLabel)).findFirst();
	}
}
