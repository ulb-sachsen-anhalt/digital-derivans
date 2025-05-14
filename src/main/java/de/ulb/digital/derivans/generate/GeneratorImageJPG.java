package de.ulb.digital.derivans.generate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.DigitalDerivansRuntimeException;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.step.DerivateStep;
import de.ulb.digital.derivans.model.step.DerivateStepImage;

/**
 * 
 * Create JPG-Derivates from given Path that can contain JPG or TIF images
 * 
 * @author hartwig
 *
 */
public class GeneratorImageJPG extends GeneratorImage {

	public GeneratorImageJPG() {
		super();
	}

	// /**
	//  * 
	//  * Constructor to set input- and output data and required JPG quality
	//  * 
	//  * @param input
	//  * @param output
	//  * @param quality
	//  */
	// public GeneratorImageJPG(DerivansData input, DerivansData output, Integer quality) {
	// 	super(input, output);
	// 	this.imageProcessor.setQuality(quality);
	// }

	// public GeneratorImageJPG(Generator base, Integer quality) {
	// 	super(base.getInput(), base.getOutput());
	// 	this.digitalPages = base.getDigitalPages();
	// 	this.imageProcessor.setQuality(quality);
	// }

	@Override
	public void setStep(DerivateStep step) {
		super.setStep(step);
		DerivateStepImage imgStep = (DerivateStepImage) step;
		this.setQuality(imgStep.getQuality());
		this.setPoolsize(imgStep.getPoolsize());
		this.setMaximal(imgStep.getMaximal());
		this.setOutputPrefix(imgStep.getOutputPrefix());
		this.setInputPrefix(imgStep.getInputPrefix()); // check for chained derivates !!!!
	}

	private String render(DigitalPage page) {
		Path pathIn = this.setInpath(page);
		if (!Files.exists(pathIn)) {
			throw new DigitalDerivansRuntimeException("input '" + pathIn + "' missing!");
		}
		Path pathOut = this.setOutpath(page);
		try {
			LOGGER.trace("start to write JPEG {} ({})", pathOut, imageProcessor.getQuality());
			imageProcessor.writeJPG(pathIn, pathOut);
		} catch (DigitalDerivansException e1) {
			String msg = String.format("%s:%s", pathIn, e1.getMessage());
			LOGGER.error("processing error {}", msg);
		} catch (IOException e2) {
			String msg = String.format("%s:%s", pathIn, e2.getMessage());
			LOGGER.error("I/O error {}", msg);
		}
		return pathOut.toString();
	}

	@Override
	public boolean forward() throws DigitalDerivansException {
		return this.runWithPool(() -> this.getDigitalPages().parallelStream().forEach(this::render));
	}

}
