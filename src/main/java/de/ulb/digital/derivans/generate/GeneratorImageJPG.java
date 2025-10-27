package de.ulb.digital.derivans.generate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.DigitalDerivansRuntimeException;
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

	@Override
	public void setStep(DerivateStep step) throws DigitalDerivansException {
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
			String msg = String.format("input '%s' missing!", pathIn);
			LOGGER.error(msg);
			throw new DigitalDerivansRuntimeException(msg);
		}
		Path pathOut = this.setOutpath(page);
		try {
			LOGGER.trace("start to write JPEG {} ({})", pathOut, this.imageProcessor.getQuality());
			this.imageProcessor.writeJPG(pathIn, pathOut);
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
