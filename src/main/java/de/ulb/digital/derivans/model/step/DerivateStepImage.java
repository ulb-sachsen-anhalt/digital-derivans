package de.ulb.digital.derivans.model.step;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.config.DefaultConfiguration;

/**
 * Specific Image Derivate Generation Step
 * 
 * @author u.hartwig
 * 
 */
public class DerivateStepImage extends DerivateStep {

	protected Integer quality = DefaultConfiguration.DEFAULT_QUALITY;
	protected Integer maximal  = DefaultConfiguration.DEFAULT_POOLSIZE;
	protected Integer poolsize = DefaultConfiguration.DEFAULT_MAXIMAL;
	protected int imageDpi;

	public DerivateStepImage() {}

	public DerivateStepImage(DerivateStepImage anotherImg) {
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
