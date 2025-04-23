package de.ulb.digital.derivans.config;

import java.nio.file.Path;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

import static de.ulb.digital.derivans.config.DefaultConfiguration.*;
import de.ulb.digital.derivans.derivate.IDerivateer;

/**
 * 
 * Application Parameters on CLI-level.
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
			"(default: '" + IDerivateer.DEFAULT_INPUT_IMAGES + "').\n")
	private String images;

	@Option(name = "-o", aliases = { "--ocr" }, required = false, usage = "Identify OCR-Data, depending on context.\n" +
			"If in local mode, stands for directory containing ocr files.\n" +
			"If metadata present, stands for required ocr fileGroup label.\n" +
			"(default: '" + IDerivateer.DEFAULT_INPUT_FULLTEXT + "').\n")
	private String ocr;

	@Option(name = "-d", aliases = {
			"--debug-pdf-render" }, required = false, usage = "Render PDF-Layers for debugging if OCR-Data present.\n" +
					"(default: false)")
	private Boolean debugPdfRender;

	@Option(name = "-n", aliases = { "--name-pdf" }, required = false, usage = "Name resulting PDF.\n" +
			"Determine name of resulting PDF-file, if created by current workflow.\n" +
			"Overwrites default naming logics from METS (if present) and directory.\n" +
			"(No default).\n")
	private String namePDF;


	@Option(name = "-f", aliases = { "--footer" }, required = false, usage = "Path to footer template file.\n" +
			"Determine absolute path to used footer image file.\n" +
			"Overwrites default footer setting if exists.\n" +
			"(No default).\n")
	private Path pathFooter;

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

	public String getNamePDF() {
		return namePDF;
	}

	public void setNamePDF(String namePDF) {
		this.namePDF = namePDF;
	}

	public Boolean isDebugPdfRender() {
		return debugPdfRender != null;
	}

	public void setDebugPdfRender(boolean isRequired) {
		this.debugPdfRender = isRequired;
	}

	public Path getPathFooter(){
		return this.pathFooter;
	}

	public void setPathFooter(Path pathFooter) {
		this.pathFooter = pathFooter;
	}
}
