package de.ulb.digital.derivans.model.step;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.config.DefaultConfiguration;
import de.ulb.digital.derivans.model.DigitalType;

/**
 * Specific Image Derivate Generation Step
 * 
 * @author u.hartwig
 * 
 */
public class DerivateStepImage extends DerivateStep {

	protected Integer quality = DefaultConfiguration.DEFAULT_QUALITY;
	protected Integer maximal = DefaultConfiguration.DEFAULT_MAXIMAL;
	protected Integer poolsize = DefaultConfiguration.DEFAULT_POOLSIZE;
	protected int imageDpi;

	public DerivateStepImage(String inputDir, String outputDir) {
		super(inputDir, outputDir);
		this.setOutputType(DigitalType.JPG);
		this.imageDpi = DefaultConfiguration.DEFAULT_IMAGE_DPI;
	}

	public DerivateStepImage(DerivateStepImage anotherImg) {
		super(anotherImg.inputDir, anotherImg.outputDir);
		this.inputType = anotherImg.inputType;
		this.outputType = anotherImg.outputType;
		this.imageDpi = anotherImg.imageDpi;
		this.quality = anotherImg.quality;
		this.maximal = anotherImg.maximal;
		this.poolsize = anotherImg.poolsize;
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

	public void setMaximal(Integer maximal) throws DigitalDerivansException {
		int max = DefaultConfiguration.DEFAULT_MAXIMAL;
		if (maximal > max) {
			throw new DigitalDerivansException("maximal too large: " + max);
		}
		this.maximal = maximal;
	}

	public Integer getPoolsize() {
		return poolsize;
	}

	public void setPoolsize(Integer poolsize) {
		this.poolsize = poolsize;
	}

	public void setImageDpi(int dpi) {
		this.imageDpi = dpi;
	}

	public int getImageDpi() {
		return this.imageDpi;
	}



}
