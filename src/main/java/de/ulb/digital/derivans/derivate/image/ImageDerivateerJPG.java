package de.ulb.digital.derivans.derivate.image;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.DigitalDerivansRuntimeException;
import de.ulb.digital.derivans.derivate.BaseDerivateer;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DigitalPage;

/**
 * 
 * Create JPG-Derivates from given Path that can contain JPG or TIF images
 * 
 * @author hartwig
 *
 */
public class ImageDerivateerJPG extends ImageDerivateer {


	public ImageDerivateerJPG() {
		super();
	}

	/**
	 * 
	 * Constructor to set input- and output data and required JPG quality
	 * 
	 * @param input
	 * @param output
	 * @param quality
	 */
	public ImageDerivateerJPG(DerivansData input, DerivansData output, Integer quality) {
		super(input, output);
		this.imageProcessor.setQuality(quality);
	}

	public ImageDerivateerJPG(BaseDerivateer base, Integer quality) {
		super(base.getInput(), base.getOutput());
		this.digitalPages = base.getDigitalPages();
		this.imageProcessor.setQuality(quality);
	}

	private String render(DigitalPage page) {
		// Path pathIn = page.getFile().withDirname(this.input.getSubDir());
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
