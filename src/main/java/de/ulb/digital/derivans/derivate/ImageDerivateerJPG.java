package de.ulb.digital.derivans.derivate;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import javax.imageio.ImageIO;

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
		// now next output must be set
		String target = page.getImagePath().toString();
		

		try {
			BufferedImage buffer = ImageIO.read(pathIn.toFile());
			int type = buffer.getType();
			LOGGER.trace("read {} ({})", pathIn, type);
			if (this.maximal != null) {
				buffer = handleMaximalDimension(buffer);
			}
			float qualityRatio = ((float) quality) / 100.0f;
			LOGGER.trace("write {} ({})", target, qualityRatio);
			imageProcessor.writeJPGWithQuality(buffer, target, qualityRatio);
			buffer.flush();
		} catch (IOException e) {
			LOGGER.error(e);
		}

		return target;
	}

	protected BufferedImage handleMaximalDimension(BufferedImage buffer) {
		int width = buffer.getWidth();
		int height = buffer.getHeight();
		if (width > this.maximal || height > this.maximal) {
			float ratio = calculateRatio(buffer);
			return imageProcessor.scale(buffer, ratio);
		}
		return buffer;

	}

	protected float calculateRatio(BufferedImage orig) {
		int maxDim = orig.getHeight() > orig.getWidth() ? orig.getHeight() : orig.getWidth();
		return (float) this.maximal / (float) maxDim;
	}

	@Override
	public boolean forward() throws DigitalDerivansException {
		return this.runWithPool(() -> this.getDigitalPages().parallelStream().forEach(this::render));
	}

}
