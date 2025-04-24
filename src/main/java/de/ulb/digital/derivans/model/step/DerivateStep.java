package de.ulb.digital.derivans.model.step;

import de.ulb.digital.derivans.config.DefaultConfiguration;

/**
 * 
 * Basic Information for creating derivates
 * 
 * @author hartwig
 *
 */
public abstract class DerivateStep {

	protected String inputDir;

	/**
	 * Kind of Data this step expects as input
	 */
	protected DerivateType inputType;

	protected String outputDir;

	/**
	 * Kind od Derivate this step will produce
	 */
	protected DerivateType outputType;

	protected String outputPrefix;

	// protected DerivateType derivateType;

	DerivateStep() {
		this.outputType = DerivateType.JPG;
		this.inputType = DerivateType.IMAGE;
	}

	public DerivateType getInputType() {
		return inputType;
	}

	public void setInputType(DerivateType inputType) {
		this.inputType = inputType;
	}

	public DerivateType getOutputType() {
		return outputType;
	}

	public void setOutputType(DerivateType outputType) {
		this.outputType = outputType;
	}

	public String getInputDir() {
		return inputDir;
	}

	public void setInputDir(String pathInput) {
		this.inputDir = pathInput;
	}

	public String getOutputDir() {
		return this.outputDir;
	}

	public void setOutputDir(String outputSubDir) {
		this.outputDir = outputSubDir;
	}

	public String getOutputPrefix() {
		return outputPrefix;
	}

	public void setOutputPrefix(String outputPrefix) {
		this.outputPrefix = outputPrefix;
	}

	// public DerivateType getDerivateType() {
	// return derivateType;
	// }

	// public void setDerivateType(DerivateType derivateType) {
	// if (derivateType != null) {
	// this.derivateType = derivateType;
	// }
	// }

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(super.toString());
		builder.append('{');
		if (inputDir != null)
			builder.append(inputDir).append(':');
		builder.append('}');
		return builder.toString();
	}

}