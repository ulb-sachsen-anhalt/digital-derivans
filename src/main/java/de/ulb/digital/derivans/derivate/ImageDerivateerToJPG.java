package de.ulb.digital.derivans.derivate;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Predicate;

import javax.imageio.ImageIO;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.model.DerivansData;

/**
 * 
 * Create JPG-Derivates from given Path that can contain JPG or TIF images
 * 
 * @author hartwig
 *
 */
public class ImageDerivateerToJPG extends ImageDerivateer {

	/**
	 * 
	 * Constructor to set input- and output data and required JPG quality
	 * 
	 * @param input
	 * @param output
	 * @param quality
	 */
	public ImageDerivateerToJPG(DerivansData input, DerivansData output, Integer quality) {
		super(input, output);
		this.imageFilter = new PredicateFileJPGorTIF();
		this.quality = quality;
	}

	public ImageDerivateerToJPG(BaseDerivateer base, Integer quality) {
		super(base.getInput(), base.getOutput());
		this.imageFilter = new PredicateFileJPGorTIF();
		this.quality = quality;
	}

	private String render(Path pathIn) {
		String pathStr = pathIn.toString();
		String fileNameOut = new File(pathStr).getName();
		if (getOutputPrefix() != null) {
			fileNameOut = getOutputPrefix() + fileNameOut;
		}
		String target = this.outputDir.resolve(fileNameOut).toString();

		// enforce jpg output
		if (target.endsWith(".tif")) {
			target = target.replace(".tif", ".jpg");
		}

		try {
			BufferedImage buffer = ImageIO.read(pathIn.toFile());
			int type = buffer.getType();
			LOGGER.debug("read {} ({})", pathIn, type);
			if (this.maximal != null) {
				buffer = handleMaximalDimension(buffer);
			}
			float qualityRatio = ((float) quality) / 100.0f;
			LOGGER.debug("write {} ({})", target, qualityRatio);
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
		return this.runWithPool(() -> this.inputPaths.parallelStream().forEach(this::render));
	}

	public static class PredicateFileJPGorTIF implements Predicate<Path> {

		@Override
		public boolean test(Path p) {
			String pathString = p.toString();
			return pathString.endsWith(".jpg") || pathString.endsWith(".tif");
		}
	}
}
