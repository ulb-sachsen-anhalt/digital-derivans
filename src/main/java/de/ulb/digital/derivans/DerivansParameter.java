package de.ulb.digital.derivans;

import java.nio.file.Files;
import java.nio.file.Path;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

/**
 * 
 * Application Parameters
 * 
 * @author hartwig
 *
 */
public class DerivansParameter {

	@Argument(index = 0, required = true, 
		usage = "Path to METS/MODS file or Directory with images\nrequired")
	private Path pathInput;

	@Option(name = "-c", aliases = {"--config-path" }, 
		required = false, 
		usage = "Path to configuration file (default:'./config/derivans.ini')\nIf not present, guess configurations.")
	private Path pathConfig;

	@Option(name = "-d", aliases = {"--debug-render" }, 
		required = false, 
		usage = "Render PDF-Layers; Usefull only if OCR-Data present (default: false)")
	private Boolean debugRender;

	@Option(name = "-i", aliases = { "--image-subdir" }, 
		required = false,
		usage = "Path to image directory, absolute or relative to input")
	private Path pathDirImages;

	@Option(name = "-o", aliases = { "--ocr-subdir" }, 
		required = false,
		usage = "Path to ocr-data directory, absolute or relative to input")
	private Path pathDirOcr;

	/**
	 * 
	 * Set specific Parser Information
	 * 
	 * @return
	 */
	public ParserProperties getProperties() {
		ParserProperties pp = ParserProperties.defaults();
		pp.withUsageWidth(80);
		return pp;
	}

	public Path getPathInput() {
		return this.pathInput;
	}

	public void setPathInput(Path path) throws DigitalDerivansException {
		if (!Files.exists(path)) {
			throw new DigitalDerivansException("Invalid input path '" + path + "'");
		}
		this.pathInput = path;
	}

	public void setPathConfig(Path config) throws DigitalDerivansException {
		if (!Files.exists(config)) {
			throw new DigitalDerivansException("Invalid configuration path'" + config + "'");
		}
		this.pathConfig = config;
	}

	public Path getPathConfig() throws DigitalDerivansException {
		if ((pathConfig != null) && (!Files.exists(pathConfig))) {
			throw new DigitalDerivansException("Invalid configuration file location '" + pathConfig + "'");
		}
		return this.pathConfig;
	}

	/**
	 * 
	 * Path to find image directory.
	 * If Path doesn't exist, try to
	 * resolve directory relative to
	 * {@link #pathInput} 
	 * 
	 * @param imgDir
	 * @throws DigitalDerivansException
	 */
	public void setPathDirImages(Path imgDir) throws DigitalDerivansException {
		if (!Files.exists(imgDir)) {
			imgDir = this.pathInput.resolve(imgDir);
			if (!Files.exists(imgDir)) {
				throw new DigitalDerivansException("Invalid image subdir '" + imgDir + "'");
			}
		}
		this.pathDirImages = imgDir;
	}

	public Path getPathDirImages() throws DigitalDerivansException {
		if ((this.pathDirImages != null) && (!Files.exists(this.pathDirImages))) {
				throw new DigitalDerivansException("Invalid image subdir '" + pathDirImages + "'");
			}
		return this.pathDirImages;
	}

	/**
	 *  
	 * Path to find fulltext directory.
	 * If Path doesn't exist, try to
	 * resolve directory relative to
	 * {@link #pathInput}
	 * 
	 * @param ocrDir
	 * @throws DigitalDerivansException
	 */
	public void setPathOcr(Path ocrDir) throws DigitalDerivansException {
		if (!Files.exists(ocrDir)) {
			ocrDir = this.pathInput.resolve(ocrDir);
			if(!Files.exists(ocrDir)) {
				throw new DigitalDerivansException("Invalid image subdir '" + ocrDir + "'");
			}
		}
		this.pathDirOcr = ocrDir;
	}

	public Path getPathDirOcr() throws DigitalDerivansException {
		if ((this.pathDirOcr != null) && (!Files.exists(this.pathDirOcr))) {
			throw new DigitalDerivansException("Invalid image subdir '" + pathDirOcr + "'");
		}
		return this.pathDirOcr;
	}

	public Boolean getDebugRender() {
		if (debugRender == null) {
			debugRender = Boolean.FALSE;
		}
		return debugRender;
	}
}
