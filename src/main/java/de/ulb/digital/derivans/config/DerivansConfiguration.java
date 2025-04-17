package de.ulb.digital.derivans.config;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.model.step.DerivateStep;
import de.ulb.digital.derivans.model.step.DerivateStepImage;
import de.ulb.digital.derivans.model.step.DerivateStepImageFooter;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;
import de.ulb.digital.derivans.model.step.DerivateType;

import static de.ulb.digital.derivans.data.IMetadataStore.DEFAULT_INPUT_FULLTEXT;
import static de.ulb.digital.derivans.data.IMetadataStore.DEFAULT_INPUT_IMAGES;

/**
 * 
 * Application Configuration
 * 
 * Start directory for image data
 * 
 * Defaults to {@link DefaultConfiguration#DEFAULT_INPUT_IMAGES}
 * 
 * @author hartwig
 *
 */
public class DerivansConfiguration {

	public static final Logger LOGGER = LogManager.getLogger(DerivansConfiguration.class);

	private Optional<Path> metadataFile;

	// private Path pathDir;

	private Path pathConfigFile;

	private boolean debugPdfRender;

	private Optional<String> paramImages = Optional.empty();

	private Optional<String> paramOCR = Optional.empty();

	private Integer quality = DefaultConfiguration.DEFAULT_QUALITY;

	private Integer poolsize = DefaultConfiguration.DEFAULT_POOLSIZE;

	private List<DerivateStep> derivateSteps = new ArrayList<>();

	private List<String> derivatePrefixes = new ArrayList<>();

	/**
	 * 
	 * Default Constructor
	 * 
	 * @param params
	 * @throws DigitalDerivansException
	 */
	public DerivansConfiguration(DerivansParameter params) throws DigitalDerivansException {
		// if (params.getPathInput() == null) {
		// 	throw new DigitalDerivansException("invalid data path 'null'");
		// }
		// if (!Files.exists(params.getPathInput())) {
		// 	throw new DigitalDerivansException("invalid data path '" + params.getPathInput() + "'");
		// }

		// // sanitize: path must be absolute for PDF generation afterwards
		// Path input = params.getPathInput();
		// if (!input.isAbsolute()) {
		// 	input = input.toAbsolutePath();
		// }

		// determine if debug render for PDF required
		if (Boolean.TRUE.equals(params.isDebugPdfRender())) {
			this.debugPdfRender = Boolean.TRUE;
		}

		// take care of args for input images/ocr
		if (params.getImages() != null) {
			this.paramImages = Optional.of(params.getImages());
		}
		if (params.getOcr() != null) {
			this.paramOCR = Optional.of(params.getOcr());
		}

		// determine if inputPath points to a METS-file or directory
		// if (Files.isDirectory(input)) {
		// 	this.metadataFile = Optional.empty();
		// 	this.pathDir = input;
		// 	LOGGER.warn("no metadata available for input '{}'", input);
		// } else if (Files.isRegularFile(input, LinkOption.NOFOLLOW_LINKS)) {
		// 	this.metadataFile = Optional.of(input);
		// 	this.pathDir = input.getParent();
		// }

		// set configuration file
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
		
		// take care if no configuration file provided
		if (derivateSteps.isEmpty()) {
			provideDefaultSteps();
			LOGGER.warn("no config read, use fallback with {} steps", this.derivateSteps.size());
		}
		LOGGER.info("use config with {} steps", this.derivateSteps.size());
		LOGGER.debug("first step inputs from '{}'", this.derivateSteps.get(0).getInputPath());
		
		// inspect parameters which *might* override
		// any previously configured settings
		if(params.getNamePDF() != null) {
			var pdfStep = this.pick(DerivateType.PDF);
			var pdfName = params.getNamePDF();
			if(pdfStep.isPresent()) {
				((DerivateStepPDF) pdfStep.get()).setNamePDF(pdfName);
				LOGGER.info("force PDF name {}", pdfName);
			} else {
				LOGGER.warn("refuse to set {} - no PDF step present", pdfName);
			}
		}
		if(params.getPathFooter() != null) {
			var newTemplate = params.getPathFooter();
			var theStep = this.pick(DerivateType.JPG_FOOTER);
			if (theStep.isPresent()) {
				if (!newTemplate.isAbsolute()) {
					newTemplate = this.pathConfigFile.getParent().resolve(newTemplate);
				}
				DerivateStepImageFooter footerStep = (DerivateStepImageFooter) theStep.get();
				var footerPrev = footerStep.getPathTemplate();
				LOGGER.info("force footer template {} with {}", 
					footerPrev, newTemplate);
				footerStep.setPathTemplate(newTemplate);
			} else {
				LOGGER.warn("refuse to set {} - no footer step present", newTemplate);
			}
		}
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

	// public Path getPathDir() {
	// 	return this.pathDir;
	// }

	public Optional<Path> getMetadataFile() {
		return this.metadataFile;
	}

	/**
	 * 
	 * Only set Path for METS/MODS-File if it is a regular File
	 * 
	 * @param metsFile
	 * @throws DigitalDerivansException
	 */
	public void setPathFile(Path metsFile) throws DigitalDerivansException {
		if (!Files.exists(metsFile) && !(Files.isRegularFile(metsFile, LinkOption.NOFOLLOW_LINKS))) {
			String msg = String.format("Invalid METS/MODS: '{%s}'", metsFile);
			LOGGER.error(msg);
			throw new DigitalDerivansException(msg);
		}
		this.metadataFile = Optional.of(metsFile);
	}

	public Integer getQuality() {
		return this.quality;
	}

	public Integer getPoolsize() {
		return poolsize;
	}

	public void setPoolsize(Integer poolsize) {
		this.poolsize = poolsize;
	}

	public List<DerivateStep> getDerivateSteps() {
		return derivateSteps;
	}

	private void evaluate(INIConfiguration conf) throws DigitalDerivansException {

		// read global configuration
		if (conf.containsKey("default_quality")) {
			this.quality = conf.getInt("default_quality");
		}
		if (conf.containsKey("default_poolsize")) {
			this.poolsize = conf.getInt("default_poolsize");
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

		// read derivate sections
		int nSection = 1;
		String derivateSection = String.format("derivate_%02d", nSection);
		List<HierarchicalConfiguration<ImmutableNode>> section = conf.childConfigurationsAt(derivateSection);
		while (!section.isEmpty()) {
			DerivateStep step = getDerivateStepCommons(conf, derivateSection);
			if (isImageDerivate(step)) {
				enrichImageDerivateInformation((DerivateStepImage) step, conf, derivateSection);
				// propably some more information required
				if (step.getDerivateType() == DerivateType.JPG_FOOTER) {
					enrichImageFooterInformation((DerivateStepImageFooter) step, conf, derivateSection);
				}
				// if very first step and param image set, exchange
				if (nSection == 1 && this.paramImages.isPresent()) {
					var images = paramImages.get();
					var parent = step.getInputPath().getParent();
					step.setInputPath(parent.resolve(images));
				}
			} else if (step.getDerivateType() == DerivateType.PDF) {
				enrichPDFDerivateInformation((DerivateStepPDF) step, conf, derivateSection);
			}
			this.derivateSteps.add(step);
			nSection++;
			derivateSection = String.format("derivate_%02d", nSection);
			section = conf.childConfigurationsAt(derivateSection);
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
	 * Default Steps supposed to work out-of-the-box,
	 * without any METS-data to be respected.
	 * 
	 * Take care of optional provided CLI params.
	 * 
	 */
	private void provideDefaultSteps() {
		DerivateStepImage create80sJpgs = new DerivateStepImage();
		create80sJpgs.setOutputType(DefaultConfiguration.DEFAULT_OUTPUT_TYPE);
		var output = DefaultConfiguration.DEFAULT_MIN_OUTPUT_LABEL;
		create80sJpgs.setOutputPath(Path.of(output));
		create80sJpgs.setQuality(DefaultConfiguration.DEFAULT_QUALITY);
		create80sJpgs.setPoolsize(DefaultConfiguration.DEFAULT_POOLSIZE);
		create80sJpgs.setDerivateType(DerivateType.JPG);
		this.derivateSteps.add(create80sJpgs);
		DerivateStepPDF createPdf = new DerivateStepPDF();
		createPdf.setInputPath(Path.of(output));
		createPdf.setDerivateType(DerivateType.PDF);
		createPdf.setOutputType("pdf");
		createPdf.setOutputPath(Path.of("."));
		// handle optional image group too
		this.paramImages.ifPresent(createPdf::setParamImages);
		// handle optional OCR data
		var ocr = this.paramOCR.orElse(DEFAULT_INPUT_FULLTEXT);
		createPdf.setParamOCR(ocr);
		this.derivateSteps.add(createPdf);
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
		String outPutType = null;
		Optional<String> optOutType = extractValue(conf, keyOutType, String.class);
		if (optOutType.isPresent()) {
			outPutType = optOutType.get();
		} else {
			outPutType = DefaultConfiguration.DEFAULT_OUTPUT_TYPE;
		}
		DerivateStep step = null;
		if (outPutType.equalsIgnoreCase(DerivateType.PDF.toString())) {
			step = new DerivateStepPDF();
			step.setDerivateType(DerivateType.PDF);
		} else if (outPutType.startsWith(DerivateType.JPG.toString().toLowerCase())) {
			step = new DerivateStepImage();
			step.setDerivateType(DerivateType.JPG);
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
	public DerivateStep getDerivateStepCommons(INIConfiguration conf, String stepSection)
			throws DigitalDerivansException {

		DerivateStep step = getStepFor(conf, stepSection);
		// input dir
		String keyInputDir = stepSection + ".input_dir";
		Optional<String> optInputDir = extractValue(conf, keyInputDir, String.class);
		if (optInputDir.isPresent()) {
			var inputDir = Path.of(optInputDir.get());
			// if (!inputDir.isAbsolute()) {
			// 	inputDir = this.pathDir.resolve(inputDir);
			// }
			step.setInputPath(inputDir);
		}
		// output dir
		String keyOutputDir = stepSection + ".output_dir";
		Optional<String> optOutputDir = extractValue(conf, keyOutputDir, String.class);
		if (optOutputDir.isPresent()) {
			Path output = Path.of(optOutputDir.get());
			// if (".".equals(output.toString()) || output.toString().isBlank()) {
			// 	output = this.pathDir;
			// } else if (!output.isAbsolute()) {
			// 	output = this.pathDir.resolve(optOutputDir.get());
			// }
			step.setOutputPath(output);
		}
		// optional output_prefix (used for additional derivates)
		String keyOutPrefix = stepSection + ".output_prefix";
		Optional<String> optOutPrefix = extractValue(conf, keyOutPrefix, String.class);
		if (optOutPrefix.isPresent()) {
			String prefix = optOutPrefix.get();
			step.setOutputPrefix(prefix);
			this.derivatePrefixes.add(prefix);
		}
		return step;
	}

	protected void enrichImageDerivateInformation(DerivateStepImage step, INIConfiguration conf, String stepSection)
			throws DigitalDerivansException {
		// poolsize
		String keyPoolsize = stepSection + ".poolsize";
		Optional<Integer> optPoolsize = extractValue(conf, keyPoolsize, Integer.class);
		if (optPoolsize.isPresent()) {
			step.setPoolsize(optPoolsize.get());
		} else {
			if (this.getPoolsize() != null) {
				step.setPoolsize(this.getPoolsize());
			}
		}
		// quality
		String keyQuality = stepSection + ".quality";
		Optional<Integer> valQuality = extractValue(conf, keyQuality, Integer.class);
		if (valQuality.isPresent()) {
			step.setQuality(valQuality.get());
		}

		// maximal dimension, width or height
		String keyMaximal = stepSection + ".maximal";
		Optional<Integer> valMaximal = extractValue(conf, keyMaximal, Integer.class);
		if (valMaximal.isPresent()) {
			step.setMaximal(valMaximal.get());
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
			step.setDerivateType(DerivateType.JPG_FOOTER);
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

		// change pdf fulltext input filegroup/path
		String optionPdfFulltext = section + "." + DefaultConfiguration.Key.PDF_METS_FILEGROUP_FULLTEXT;
		Optional<String> optPdfFulltext = extractValue(conf, optionPdfFulltext, String.class);
		if (optPdfFulltext.isPresent()) {
			String pdfFulltext = optPdfFulltext.get();
			LOGGER.debug("set fulltext input for pdf '{}'", pdfFulltext);
			step.setParamOCR(pdfFulltext);
		}
		// probably set via CLI flag, even if nothing configured
		this.paramOCR.ifPresent(step::setParamOCR);

		// images and filegroup param
		String optionPdfImageGroup = section + "." + DefaultConfiguration.Key.PDF_METS_FILEGROUP_IMAGES;
		Optional<String> optPdfImageGroup = extractValue(conf, optionPdfImageGroup, String.class);
		if (optPdfImageGroup.isPresent()) {
			String pdfImageGroup = optPdfImageGroup.get();
			LOGGER.debug("set fulltext input for pdf '{}'", pdfImageGroup);
			step.setParamImages(pdfImageGroup);
		}
		// probably set via CLI flag, even if nothing configured
		this.paramImages.ifPresent(step::setParamImages);
	}

	private static boolean isImageDerivate(DerivateStep step) {
		var aType = step.getDerivateType();
		return aType == DerivateType.IMAGE || aType == DerivateType.JPG || aType == DerivateType.JPG_FOOTER;
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

	/**
	 * For testing purposes only
	 * 
	 * @return
	 */
	public String getParamImages() {
		return paramImages.get();
	}

	public void setParamImages(String paramImage) {
		this.paramImages = Optional.of(paramImage);
	}

	/**
	 * For testing purposes only
	 * 
	 * @return
	 */
	public String getParamOCR() {
		return paramOCR.get();
	}

	public void setParamOCR(String paramOCR) {
		this.paramOCR = Optional.of(paramOCR);
	}

	/**
	 * 
	 * Remember all encountered prefixes for
	 * derivates. Used in later stages for
	 * name resolving to clear each name from
	 * probably before enriched prefixes.
	 * 
	 * @return optional Prefixes
	 */
	public List<String> getDerivatePrefixes() {
		return this.derivatePrefixes;
	}

	/**
	 * 
	 * Render more information concerning
	 * PDF Textlayer
	 * 
	 * @return
	 */
	public boolean isDebugPdfRender() {
		return this.debugPdfRender;
	}

	/**
	 * 
	 * Pick first step with matching {@link DerivateType}
	 * 
	 * @param type
	 * @return
	 */
	private Optional<DerivateStep> pick(DerivateType type) {
		return this.derivateSteps.stream()
			.filter(s -> s.getDerivateType() == type).findFirst();
	}
}
