package de.ulb.digital.derivans.derivate;

import java.io.IOException;
import java.nio.file.Path;

import de.ulb.digital.derivans.DigitalDerivansException;
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
		this.quality = quality;
	}

	public ImageDerivateerJPG(BaseDerivateer base, Integer quality) {
		super(base.getInput(), base.getOutput());
		this.digitalPages = base.getDigitalPages();
		this.quality = quality;
		this.resolver = base.getResolver();
	}

	private String render(DigitalPage page) {
		Path pathIn = page.getImagePath();
		this.resolver.setImagePath(page, this);
		Path pathOut = page.getImagePath();
		try {
			LOGGER.trace("write {} ({})", pathOut, imageProcessor.getQuality());
			imageProcessor.writeJPG(pathIn, pathOut);
		} catch (IOException|DigitalDerivansException e) {
			LOGGER.error(e);
		}
		return pathOut.toString();
	}

	@Override
	public boolean forward() throws DigitalDerivansException {
		return this.runWithPool(() -> this.getDigitalPages().parallelStream().forEach(this::render));
	}

}
