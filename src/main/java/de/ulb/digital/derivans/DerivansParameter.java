package de.ulb.digital.derivans;

import java.nio.file.Files;
import java.nio.file.Path;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

import de.ulb.digital.derivans.config.DefaultConfiguration;

/**
 * 
 * Application Parameters
 * 
 * @author hartwig
 *
 */
public class DerivansParameter {

	@Argument(index = 0, required = true, usage = "Path to METS/MODS file or Directory with images\nrequired")
	private Path pathInput;

	@Option(name = "-c", aliases = {
			"--config-path" }, required = false, usage = "Path to configuration file (default:'./config/derivans.ini')\nIf not present, guess configurations.")
	private Path pathConfig;

	@Option(name = "-q", aliases = { "--jpg-quality" }, required = false, usage = "JPG Derivates Compression (default: 80)")
	private Integer quality;

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

	public void setPathConfig(Path config) throws DigitalDerivansException {
		if (!Files.exists(config)) {
			throw new DigitalDerivansException("Invalid configuration path'" + config + "'");
		}
		this.pathConfig = config;
	}

	public void setPathInput(Path path) throws DigitalDerivansException {
		if (!Files.exists(path)) {
			throw new DigitalDerivansException("Invalid input path '" + path + "'");
		}
		this.pathInput = path;
	}

	public Path getPathConfig() throws DigitalDerivansException {
		if ((pathConfig != null) && (!Files.exists(pathConfig))) {
			throw new DigitalDerivansException("Invalid configuration file location '" + pathConfig + "'");
		}
		return this.pathConfig;
	}

	public Integer getQuality() throws DigitalDerivansException {
		if (quality == null) {
			quality = DefaultConfiguration.DEFAULT_QUALITY;
		}
		if (quality < 1 || quality > 99) {
			throw new DigitalDerivansException("Invalid quality '" + quality + "'");
		}
		return quality;
	}
}
