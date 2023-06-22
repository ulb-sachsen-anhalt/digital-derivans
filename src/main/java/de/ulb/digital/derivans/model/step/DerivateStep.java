package de.ulb.digital.derivans.model.step;

import java.nio.file.Path;

import de.ulb.digital.derivans.config.DefaultConfiguration;

/**
 * 
 * Basic Information for creating derivates
 * 
 * @author hartwig
 *
 */
public abstract class DerivateStep {

	protected String outputType;

	protected Path inputPath;

	protected Path outputPath;

	protected String outputPrefix;

	protected DerivateType derivateType;

	public DerivateStep() {
		this.outputType = DefaultConfiguration.DEFAULT_OUTPUT_TYPE;
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
		if (derivateType != null)
			builder.append(derivateType).append(':');
		if (inputPath != null)
			builder.append(inputPath).append(':');
		if (outputType != null)
			builder.append(outputType);
		builder.append('}');
		return builder.toString();
	}

}