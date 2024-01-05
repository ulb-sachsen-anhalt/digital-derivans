package de.ulb.digital.derivans.config;

import java.nio.file.Path;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

import static de.ulb.digital.derivans.config.DefaultConfiguration.*;
import static de.ulb.digital.derivans.data.IMetadataStore.*;

/**
 * 
 * Application Parameters.
 * 
 * Can be set global on CLI-level.
 * 
 * @author hartwig
 *
 */
public class DerivansParameter {

	@Argument(index = 0, required = true, usage = "Path to input data (required).\n" +
			"Stands for a path to metadata file (i.e., METS-file), or\n" +
			"path to a locale directory with subdirectories (locale mode)\n" +
			"for images (required) and fulltext data (optional)." +
			"(no default: required)")
	private Path pathInput;

	@Option(name = "-c", aliases = {
			"--config-path" }, required = false, usage = "Path to Derivans configuration file.\n" +
					"If not present, search configuration at default location.\n" +
					"(default:'./config/" + DEFAULT_CONFIG_FILE_LABEL + "')")
	private Path pathConfig;

	@Option(name = "-i", aliases = { "--images" }, required = false, usage = "Identify images, depending on context.\n"
			+
			"If in local mode, stands for directory containing images.\n" +
			"If metadata present, stands for required image fileGroup label.\n" +
			"which defaults to '" + DEFAULT_INPUT_IMAGES + "'.\n" +
			"If no metadata present, defaults to '" + DEFAULT_INPUT_IMAGES + "'.\n")
	private String images;

	@Option(name = "-o", aliases = { "--ocr" }, required = false, usage = "Identify OCR-Data, depending on context.\n" +
			"If in local mode, stands for directory containing ocr files.\n" +
			"If metadata present, stands for required ocr fileGroup label,\n" +
			"which defaults otherwise to '" + DEFAULT_INPUT_FULLTEXT + "'.\n" +
			"If no metadata present, defaults to '" + DEFAULT_INPUT_FULLTEXT + "'.\n")
	private String ocr;

	@Option(name = "-d", aliases = {
			"--debug-pdf-render" }, required = false, usage = "Render PDF-Layers for debugging if OCR-Data present.\n" +
					"(default: false)")
	private Boolean debugPdfRender;

	/**
	 * 
	 * Set specific Parser Information
	 * 
	 * @return
	 */
	public ParserProperties getProperties() {
		ParserProperties pp = ParserProperties.defaults();
		pp.withUsageWidth(120);
		return pp;
	}

	public Path getPathInput() {
		return this.pathInput;
	}

	public void setPathInput(Path path) {
		this.pathInput = path;
	}

	public void setPathConfig(Path config) {
		this.pathConfig = config;
	}

	public Path getPathConfig() {
		return this.pathConfig;
	}

	public void setImages(String images) {
		this.images = images;
	}

	public String getImages() {
		return this.images;
	}

	public void setOcr(String ocr) {
		this.ocr = ocr;
	}

	public String getOcr() {
		return this.ocr;
	}

	public Boolean getDebugPdfRender() {
		if (debugPdfRender == null) {
			debugPdfRender = Boolean.FALSE;
		}
		return debugPdfRender;
	}
}
