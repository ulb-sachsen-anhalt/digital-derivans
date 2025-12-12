package de.ulb.digital.derivans.model.step;

import de.ulb.digital.derivans.model.DigitalType;

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
	protected DigitalType inputType;

	protected String inputPrefix;

	protected String outputDir;

	/**
	 * Kind of Derivate this step will produce
	 */
	protected DigitalType outputType;

	protected String outputPrefix;

	DerivateStep(String inputDir, String outputDir) {
		this.outputType = DigitalType.JPG;
		this.inputType = DigitalType.IMAGE;
		this.inputDir = inputDir;
		this.outputDir = outputDir;
	}

	public DigitalType getInputType() {
		return inputType;
	}

	public void setInputType(DigitalType inputType) {
		this.inputType = inputType;
	}

	public void setInputTypeFromLabel(String inputTypeLabel) {
		this.inputType = DigitalType.forLabel(inputTypeLabel);
	}

	public DigitalType getOutputType() {
		return outputType;
	}

	public void setOutputType(DigitalType outputType) {
		this.outputType = outputType;
	}

	public void setOutputTypeFromLabel(String outputTypeLabel) {
		this.outputType = DigitalType.forLabel(outputTypeLabel);
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

	public String getInputPrefix() {
		return this.inputPrefix;
	}

	public void setInputPrefix(String inputPrefix) {
		this.inputPrefix = inputPrefix;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName() + '@' + Integer.toHexString(hashCode()));
		builder.append('{');
		if (inputDir != null)
			builder.append("inputDir:").append(inputDir).append(',');
		if (outputDir != null)
			builder.append("outputDir:").append(outputDir);
		builder.append('}');
		return builder.toString();
	}

}