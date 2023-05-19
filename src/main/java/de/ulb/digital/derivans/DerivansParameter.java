package de.ulb.digital.derivans;

import java.nio.file.Path;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

import static de.ulb.digital.derivans.config.DefaultConfiguration.*;

/**
 * 
 * Application Parameters
 * 
 * @author hartwig
 *
 */
public class DerivansParameter {

	@Argument(index = 0, required = true, 
		usage = "Path to input data (required).\n" +
			"Stands for a path to metadata file (i.e., METS-file), or\n" +
			"path to a locale directory with subdirectories (locale mode)\n" +
			"for images (required) and fulltext data (optional)." +
			"(no default: required)"
	)
	private Path pathInput;

	@Option(name = "-c", aliases = {"--config-path" }, 
		required = false, 
		usage = "Path to Derivans configuration file.\n" +
			"If not present, search configuration at default location.\n" +
			"(default:'./config/" + DEFAULT_CONFIG_FILE_LABEL +"')"
	)
	private Path pathConfig;

	@Option(name = "-i", aliases = { "--images-subdir" }, 
		required = false,
		usage = "Path to image directory.\n" +
			"Absolute path or path relative to input directory.\n" +
			"If metadata present, stands for required image fileGroup label,\n" + 
			"which defaults to '" + DEFAULT_INPUT_IMAGES_LABEL + "'.\n" + 
			"If no metadata present, defaults to '" + DEFAULT_INPUT_IMAGES_LABEL + "'.\n"
	)
	private Path pathDirImages;
	
	@Option(name = "-o", aliases = { "--ocr-subdir" }, 
		required = false,
		usage = "Path to ocr-data directory.\n" + 
			"Absolute path or path relative to input directory.\n" +
			"If metadata present, stands for required ocr fileGroup label,\n" + 
			"which defaults to '" + DEFAULT_INPUT_FULLTEXT_LABEL + "'.\n" + 
			"If no metadata present, defaults to '" + DEFAULT_INPUT_FULLTEXT_LABEL + "'.\n"
	)
	private Path pathDirOcr;
	
	@Option(name = "-d", aliases = {"--debug-render" }, 
		required = false, 
		usage = "Render PDF-Layers for debugging if OCR-Data present.\n" + 
			"(default: false)"
	)
	private Boolean debugRender;
	
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

	public void setPathDirImages(Path imgDir) {
		this.pathDirImages = imgDir;
	}

	public Path getPathDirImages() {
		return this.pathDirImages;
	}

	public void setPathOcr(Path ocrDir) {
		this.pathDirOcr = ocrDir;
	}

	public Path getPathDirOcr() {
		return this.pathDirOcr;
	}

	public Boolean getDebugRender() {
		if (debugRender == null) {
			debugRender = Boolean.FALSE;
		}
		return debugRender;
	}
}
