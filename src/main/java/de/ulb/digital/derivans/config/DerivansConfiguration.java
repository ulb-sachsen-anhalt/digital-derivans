package de.ulb.digital.derivans.config;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import de.ulb.digital.derivans.DerivansParameter;
import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.model.DerivateStep;
import de.ulb.digital.derivans.model.DerivateType;
import de.ulb.digital.derivans.model.PDFMetaInformation;

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

	private Path pathDir;

	private Path pathConfigFile;

	private Path initialImageDir = null;

	private Path optionalOcrDir = null;

	private Integer quality = DefaultConfiguration.DEFAULT_QUALITY;

	private Integer poolsize = DefaultConfiguration.DEFAULT_POOLSIZE;

	private List<DerivateStep> derivateSteps;
	
	private PDFMetaInformation pdfMeta;
	
	private List<String> prefixes;

	/**
	 * 
	 * Default Constructor
	 * 
	 * @param params
	 * @throws DigitalDerivansException
	 */
	public DerivansConfiguration(DerivansParameter params) throws DigitalDerivansException {
		this.quality = DefaultConfiguration.DEFAULT_QUALITY;
		this.prefixes = new ArrayList<>();
		this.pdfMeta = new PDFMetaInformation();
		this.pdfMeta.setDebugRender(params.getDebugRender());
		if (params.getPathInput() == null) {
			throw new DigitalDerivansException("invalid data path 'null'");
		}
		if (! Files.exists(params.getPathInput())) {
			throw new DigitalDerivansException("invalid data path '"+ params.getPathInput()+"'");
		}

		// sanitize: path must be absolute for PDF generation afterwards
		Path input = params.getPathInput();
		if (!input.isAbsolute()) {
			input = input.toAbsolutePath();
		}

		// determine if inputPath points to a METS-file or directory
		if (Files.isDirectory(input)) {
			this.metadataFile = Optional.empty();
			this.pathDir = input;
			LOGGER.warn("no metadata available for input '{}'", input);
		} else if (Files.isRegularFile(input, LinkOption.NOFOLLOW_LINKS)) {
			this.metadataFile = Optional.of(input);
			this.pathDir = input.getParent();
		}

		// set configuration file for later examination
		this.derivateSteps = new ArrayList<>();
		if (params.getPathConfig() != null) {
			LOGGER.debug("inspect cli config file {}", params.getPathConfig());
			this.pathConfigFile = params.getPathConfig();
			this.initConfigurationFromFile();
		} else {
			Path defaultConfigLocation = Path.of("").resolve("config").resolve(DefaultConfiguration.DEFAULT_CONFIG_FILE_LABEL);
			LOGGER.info("no config from cli, inspect default {}", defaultConfigLocation);
			if (!Files.exists(defaultConfigLocation)) {
				LOGGER.warn("no config file '{}'", defaultConfigLocation);
			} else {
				this.pathConfigFile = defaultConfigLocation;
				LOGGER.info("found config file at default location '{}'", defaultConfigLocation);
				this.initConfigurationFromFile();
			}
		}
		// take care of args for input images/ocr
		if (params.getPathDirImages() != null) {
			this.setInitialImageDir(params.getPathDirImages());
		}
		if(params.getPathDirOcr() != null) {
			this.setOptionalOcrDir(params.getPathDirOcr());
		}
		// take care if no config provided
		if (derivateSteps.isEmpty()) {
			provideDefaultSteps();
			LOGGER.warn("no config read, use fallback with {} steps", this.derivateSteps.size());
		}
		LOGGER.info("use config with {} steps", this.derivateSteps.size());
		LOGGER.debug("first step inputs from '{}'", this.derivateSteps.get(0).getInputPath());
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

	public Path getPathDir() {
		return this.pathDir;
	}

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

	public void setInitialImageDir(Path imgSubdir) throws DigitalDerivansException {
		if (imgSubdir != null) {
			if (!imgSubdir.isAbsolute()) {
				imgSubdir = this.pathDir.resolve(imgSubdir);
			}
			if(!Files.exists(imgSubdir)) {
				throw new DigitalDerivansException("Invalid image dir "+imgSubdir+" provided!");
			} else {
				this.initialImageDir = imgSubdir;
				LOGGER.debug("use images from {}", this.initialImageDir);
				this.reviewDerivates();
			}
		} 
	}
	
	private void reviewDerivates() throws DigitalDerivansException {
		if(!this.derivateSteps.isEmpty()) {
			var firstStep = this.derivateSteps.get(0);
			var prevImgDir = firstStep.getInputPath();
			if (!prevImgDir.equals(this.getInitialImageDir())) {
				LOGGER.warn("replace step0 previous image path {} with {},", prevImgDir, this.getInitialImageDir());
				firstStep.setInputPath(this.getInitialImageDir());
			}
		}
	}

	public Path getInitialImageDir() throws DigitalDerivansException {
		if(this.initialImageDir == null) {
			this.setInitialImageDir(DefaultConfiguration.DEFAULT_INPUT_IMAGES);
		}
		return this.initialImageDir;
	}

	public void setOptionalOcrDir(Path inputOcr) throws DigitalDerivansException {
		if (inputOcr != null) {
			if (!inputOcr.isAbsolute()) {
				inputOcr = this.pathDir.resolve(inputOcr);
			}
			if(!Files.exists(inputOcr)) {
				throw new DigitalDerivansException("Invalid dir "+inputOcr+" provided!");
			} else {
				this.optionalOcrDir = inputOcr;
				LOGGER.debug("use ocr from {}", this.initialImageDir);
			}
		} 
	}

	public Path getOptionalOcrDir() {
		return this.optionalOcrDir;
	}

	public List<DerivateStep> getDerivateSteps() {
		return derivateSteps;
	}
	
	public PDFMetaInformation getPdfMetainformation() {
		 return this.pdfMeta;
	}
	
	/**
	 * 
	 * Derivates that also carry a prefix in their name,
	 * keep track of this information which is later required for
	 * proper name and path resolving. 
	 * 
	 * @return
	 */
	public List<String> getPrefixes() {
		return this.prefixes;
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
			DerivateStep step = new DerivateStep();
			String outPutType = null;

			// output type
			String keyOutType = derivateSection + ".output_type";
			Optional<String> optOutType = extractValue(conf, keyOutType, String.class);
			if (optOutType.isPresent()) {
				outPutType = optOutType.get();
			} else {
				outPutType = DefaultConfiguration.DEFAULT_OUTPUT_TYPE;
			}
			step.setOutputType(outPutType);
			// depending on this set PDF type
			if ("pdf".equalsIgnoreCase(step.getOutputType())) {
				step.setDerivateType(DerivateType.PDF);
			}

			// output_prefix (used for additional derivates)
			String keyOutPrefix = derivateSection + ".output_prefix";
			Optional<String> optOutPrefix = extractValue(conf, keyOutPrefix, String.class);
			if (optOutPrefix.isPresent()) {
				String prefix = optOutPrefix.get();
				step.setOutputPrefix(prefix);
				prefixes.add(prefix);
			}

			// poolsize
			String keyPoolsize = derivateSection + ".poolsize";
			Optional<Integer> optPoolsize = extractValue(conf, keyPoolsize, Integer.class);
			if (optPoolsize.isPresent()) {
				step.setPoolsize(optPoolsize.get());
			} else {
				if (this.getPoolsize() != null) {
					step.setPoolsize(this.getPoolsize());
				}
			}

			// input dir
			String keyInputDir = derivateSection + ".input_dir";
			Optional<String> optInputDir = extractValue(conf, keyInputDir, String.class);
			if (optInputDir.isPresent()) {
				var inputPath = this.pathDir.resolve(optInputDir.get());
				step.setInputPath(inputPath);
				// set first section's input as global input
				if(nSection == 1 && this.initialImageDir == null) {
					this.initialImageDir = inputPath;
				}
			}

			// output dir
			String keyOutputDir = derivateSection + ".output_dir";
			Optional<String> optOutputDir = extractValue(conf, keyOutputDir, String.class);
			if (optOutputDir.isPresent()) {
				String outputDir = optOutputDir.get();
				Path output = this.pathDir.resolve(optOutputDir.get());
				if (".".equals(outputDir) || outputDir.isBlank()) {
					output = this.pathDir;
				}
				step.setOutputPath(output);
			}

			// quality
			String keyQuality = derivateSection + ".quality";
			Optional<Integer> valQuality = extractValue(conf, keyQuality, Integer.class);
			if (valQuality.isPresent()) {
				step.setQuality(valQuality.get());
			}

			// maximal dimension, width or height
			String keyMaximal = derivateSection + ".maximal";
			Optional<Integer> valMaximal = extractValue(conf, keyMaximal, Integer.class);
			if (valMaximal.isPresent()) {
				step.setMaximal(valMaximal.get());
			}

			// image footer template
			String keyTemplate = derivateSection + ".footer_template";
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
			}

			// image footer label copyright
			String keyFooterCopy = derivateSection + ".footer_label_copyright";
			Optional<String> optFooterCopy = extractValue(conf, keyFooterCopy, String.class);
			if (optFooterCopy.isPresent()) {
				step.setFooterLabel(optFooterCopy.get());
			}

			// metadata creator pdf
			String keyMetadataCreator = derivateSection + ".metadata_creator";
			Optional<String> optCreator = extractValue(conf, keyMetadataCreator, String.class);
			if(optCreator.isPresent()) {
				this.pdfMeta.setCreator(optCreator);
			}
			
			// metadata keyword pdf
			String keyMetadataKeyword = derivateSection + ".metadata_keywords";
			Optional<String> optKeywords = extractValue(conf, keyMetadataKeyword, String.class);
			if(optKeywords.isPresent()) {
				this.pdfMeta.setKeywords(optKeywords);
			}
						
			// metadata licence 
			String keyMetadataLicense = derivateSection + ".metadata_license";
			Optional<String> optLicense = extractValue(conf, keyMetadataLicense, String.class);
			if(optLicense.isPresent()) {
				this.pdfMeta.setLicense(optLicense);
			}
			
			// pdf image dpi for scaling image data 
			String keyPdfImageDPI = derivateSection + ".image_dpi";
			Optional<String> optImageDpi = extractValue(conf, keyPdfImageDPI, String.class);
			if(optImageDpi.isPresent()) {
				this.pdfMeta.setImageDpi(Integer.valueOf(optImageDpi.get()));
			}

			// on which level optional text to render: per word, per line ... ?
			String keyPdfRenderLvl = derivateSection + ".render_text_level";
			Optional<String> optRenderLvl = extractValue(conf, keyPdfRenderLvl, String.class);
			if(optRenderLvl.isPresent()) {
				LOGGER.debug("set render text level '{}'", optRenderLvl.get());
				this.pdfMeta.setRenderLevel(optRenderLvl.get());
			}

			// on which level optional text to render: per word, per line ... ?
			String keyPdfRenderVis = derivateSection + ".render_text_visibility";
			Optional<String> optRenderVis = extractValue(conf, keyPdfRenderVis, String.class);
			if(optRenderVis.isPresent()) {
				LOGGER.debug("set render text visibility '{}'", optRenderVis.get());
				this.pdfMeta.setRenderModus(optRenderVis.get());
			}

			// probably disable to enrich created PDF file into metadata file
			String keyPdfEnrichMeta = derivateSection + ".enrich_pdf_metadata";
			Optional<String> optEnrichMeta = extractValue(conf, keyPdfEnrichMeta, String.class);
			if(optEnrichMeta.isPresent()) {
				String enrichMetaStr = optEnrichMeta.get();
				boolean mustEnrich = Boolean.parseBoolean(enrichMetaStr);
				LOGGER.debug("try to set enrich metadata to '{}'", mustEnrich);
				this.pdfMeta.setEnrichMetadata(mustEnrich);
				step.setEnrichMetadata(mustEnrich);
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
	 * Default Steps supposed to work out-of-the-box, even without any METS-data at hand.
	 * @throws DigitalDerivansException
	 */
	private void provideDefaultSteps() throws DigitalDerivansException {
		DerivateStep createMins = new DerivateStep();
		createMins.setInputPath(this.getInitialImageDir());
		createMins.setOutputType(DefaultConfiguration.DEFAULT_OUTPUT_TYPE);
		createMins.setOutputPath(this.pathDir.resolve(DefaultConfiguration.DEFAULT_MIN_OUTPUT_LABEL));
		createMins.setQuality(DefaultConfiguration.DEFAULT_QUALITY);
		createMins.setPoolsize(DefaultConfiguration.DEFAULT_POOLSIZE);
		createMins.setDerivateType(DerivateType.JPG);
		this.derivateSteps.add(createMins);
		DerivateStep createPdf = new DerivateStep();
		createPdf.setInputPath(this.pathDir.resolve(DefaultConfiguration.DEFAULT_MIN_OUTPUT_LABEL));
		createPdf.setDerivateType(DerivateType.PDF);
		createPdf.setOutputType("pdf");
		createPdf.setOutputPath(this.pathDir);
		this.derivateSteps.add(createPdf);
	}
}
