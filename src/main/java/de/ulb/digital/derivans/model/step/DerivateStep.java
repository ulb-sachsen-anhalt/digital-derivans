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

	protected String inputSubDir;

	protected String outputSubDir;

	protected String outputPrefix;

	protected DerivateType derivateType;

	DerivateStep() {
		this.outputType = DefaultConfiguration.DEFAULT_OUTPUT_TYPE;
		this.derivateType = DerivateType.JPG;
	}

	public String getOutputType() {
		return outputType;
	}

	public void setOutputType(String outputType) {
		this.outputType = outputType;
	}

	public String getInputSubDir() {
		return inputSubDir;
	}

	public void setInputSubDir(String pathInput) {
		this.inputSubDir = pathInput;
	}

	public String getOutputSubDir() {
		return this.outputSubDir;
	}

	public void setOutputSubDir(String outputSubDir) {
		this.outputSubDir = outputSubDir;
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
		if (inputSubDir != null)
			builder.append(inputSubDir).append(':');
		builder.append('}');
		return builder.toString();
	}

}