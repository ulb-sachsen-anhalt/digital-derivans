package de.ulb.digital.derivans.model;

import java.nio.file.Path;

import de.ulb.digital.derivans.config.DefaultConfiguration;

/**
 * 
 * Hold Information for single Derivate
 * 
 * @author hartwig
 *
 */
public class DerivateStep {

	private String outputType;

	private Path inputPath;

	private Path outputPath;

	private String outputPrefix;

	private Integer quality;

	private Integer maximal;

	private Integer poolsize;

	private Path pathTemplate;

	private String footerLabel;

	private DerivateType derivateType;
	
	public DerivateStep() {
		this.outputType = DefaultConfiguration.DEFAULT_OUTPUT_TYPE;
		this.quality = DefaultConfiguration.DEFAULT_QUALITY;
		this.poolsize = DefaultConfiguration.DEFAULT_POOLSIZE;
		this.derivateType = DerivateType.JPG;
	}

	public String getOutputType() {
		return outputType;
	}

	public void setOutputType(String outputType) {
		this.outputType = outputType;
	}

	public Path getInputPath() {
		return inputPath;
	}

	public void setInputPath(Path pathInput) {
		this.inputPath = pathInput;
	}

	public Path getOutputPath() {
		return outputPath;
	}

	public void setOutputPath(Path pathOutput) {
		this.outputPath = pathOutput;
	}

	public String getOutputPrefix() {
		return outputPrefix;
	}

	public void setOutputPrefix(String outputPrefix) {
		this.outputPrefix = outputPrefix;
	}

	public Integer getQuality() {
		return quality;
	}

	public void setQuality(Integer quality) {
		this.quality = quality;
	}

	public Integer getMaximal() {
		return maximal;
	}

	public void setMaximal(Integer maximal) {
		this.maximal = maximal;
	}

	public Integer getPoolsize() {
		return poolsize;
	}

	public void setPoolsize(Integer poolsize) {
		this.poolsize = poolsize;
	}

	public Path getPathTemplate() {
		return pathTemplate;
	}

	public void setPathTemplate(Path pathTemplate) {
		this.pathTemplate = pathTemplate;
	}

	public String getFooterLabel() {
		return footerLabel;
	}

	public void setFooterLabel(String footerLabel) {
		this.footerLabel = footerLabel;
	}

	public DerivateType getDerivateType() {
		return derivateType;
	}

	public void setDerivateType(DerivateType derivateType) {
		if (derivateType != null) {
			this.derivateType = derivateType;
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(super.toString());
		builder.append('{');
		if (inputPath != null)
			builder.append(inputPath).append(':');
		if (outputType != null)
			builder.append(outputType);
		builder.append('}');
		return builder.toString();
	}
}