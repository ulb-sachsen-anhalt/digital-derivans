package de.ulb.digital.derivans.generate.image;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.config.DefaultConfiguration;
import de.ulb.digital.derivans.data.image.ImageMetadata;

/**
 * 
 * Low-Level Image Processing
 * 
 * @author hartwig
 *
 */
public class ImageProcessor {

	static final Logger LOGGER = LogManager.getLogger(ImageProcessor.class);

	/**
	 * Percentage image quality
	 */
	private float qualityRatio = 1.0f;

	/**
	 * Maximal image dimension in width or height
	 */
	private int maximal = DefaultConfiguration.DEFAULT_MAXIMAL;

	/**
	 * Default minimal image dimension in width or height
	 */
	public static final Integer DEFAULT_MINIMAL_DIMENSION = 500;

	/**
	 * Error marker, if a large number of subsequent down scales make the footer
	 * disappear after all or way to small images must be processed
	 */
	public static final Integer MIN_SCALED_FOOTER_HEIGHT = 20;

	public ImageProcessor() {
	}

	public ImageProcessor(int quality, int maximal) {
		this.setQuality(quality);
		this.setMaximal(maximal);
	}

	public void setMaximal(int maximal) {
		if (maximal > 0 && maximal <= DefaultConfiguration.DEFAULT_MAXIMAL) {
			this.maximal = maximal;
		}
	}

	public int getMaximal() {
		return this.maximal;
	}

	public void setQuality(int quality) {
		if (quality > 0 && quality <= 100) {
			this.qualityRatio = quality / 100.0f;
		}
	}

	public float getQuality() {
		return this.qualityRatio;
	}

	BufferedImage merge(BufferedImage bufferedImage, BufferedImage addedBuffer, boolean appendRightSide) {
		int bType = bufferedImage.getType();
		if (bType == 0) {
			bType = bufferedImage.getColorModel().getColorSpace().getType();
		}
		int newHeight = bufferedImage.getHeight() + addedBuffer.getHeight();
		int newWidth = bufferedImage.getWidth();
		if(appendRightSide) {
			newHeight = bufferedImage.getHeight();
			newWidth = bufferedImage.getWidth() + addedBuffer.getWidth();
		}
		BufferedImage newImage = new BufferedImage(newWidth, newHeight, bType);
		Graphics2D g2d = newImage.createGraphics();
		g2d.drawImage(bufferedImage, 0, 0, null);
		if(appendRightSide) {
			g2d.drawImage(addedBuffer, bufferedImage.getWidth(), 0, null);
		} else {
			g2d.drawImage(addedBuffer, 0, bufferedImage.getHeight(), null);
		}
		g2d.dispose();
		return newImage;
	}

	public BufferedImage clone(BufferedImage original) {
		BufferedImage b = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());
		Graphics2D g = b.createGraphics();
		g.drawImage(original, 0, 0, null);
		g.dispose();
		return b;
	}

	/**
	 * 
	 * Please note:
	 * Don't use Image.getScaledInstance!
	 * 
	 * {@link https://stackoverflow.com/questions/20083554/bufferedimage-getscaledinstance-changes-brightness-of-picture}
	 * {@link https://community.oracle.com/docs/DOC-983611}
	 * 
	 * @param original
	 * @param ratio
	 * @return scaled ImageBuffer
	 */
	BufferedImage scale(BufferedImage original, float ratio) {
		int newW = (int) (ratio * original.getWidth());
		int newH = (int) (ratio * original.getHeight());
		int bType = original.getType();
		if (bType == 0) {
			bType = original.getColorModel().getColorSpace().getType();
		}
		BufferedImage dimg = new BufferedImage(newW, newH, bType);
		Graphics2D g2d = dimg.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2d.drawImage(original, 0, 0, newW, newH, null);
		g2d.dispose();
		return dimg;
	}

	boolean writeJPGWithQualityAndMetadata(BufferedImage buffer, Path pathOut, ImageMetadata metadata)
			throws DigitalDerivansException, IOException {
		buffer = handleMaximalDimension(buffer);

		// determine BufferedImage.type
		// 5 = 8-bit RGB color components, corresponding to
		// Windows-style BGR color model
		// 10 = unsigned byte grayscale image, non-indexed (CS_GRY)
		int bType = buffer.getType();
		if (bType == 0) {
			bType = buffer.getColorModel().getColorSpace().getType();
		}
		ImageTypeSpecifier imageType = ImageTypeSpecifier.createFromBufferedImageType(bType);

		// determine JPG write parameter
		JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
		if (metadata.requiresProgressiveMode()) {
			jpegParams.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
		}
		jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpegParams.setCompressionQuality(this.getQuality());
		jpegParams.setDestinationType(imageType);

		// write image buffer
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
		try (FileImageOutputStream fios = new FileImageOutputStream(pathOut.toFile());) {
			writer.setOutput(fios);
			writer.write(null, new IIOImage(buffer, null, metadata.getData()), jpegParams);
		} catch (IIOException e) {
			throw new DigitalDerivansException(e.getMessage() + ":" + pathOut);
		}
		return true;
	}

	public boolean writeJPG(Path pathIn, Path pathOut) throws IOException, DigitalDerivansException {
		var fileSize = Files.size(pathIn);
		if (fileSize < 1L) {
			throw new DigitalDerivansException("Invalid fileSize " + fileSize + " for " + pathIn + "!");
		}
		BufferedImage buffer = ImageIO.read(pathIn.toFile());
		if (buffer == null) {
			throw new DigitalDerivansException("Invalid image data " + pathIn + "!");
		}
		ImageMetadata imageMetada = new ImageMetadata();
		imageMetada.enrichFrom(pathIn);
		this.writeJPGWithQualityAndMetadata(buffer, pathOut, imageMetada);
		buffer.flush();
		return true;
	}

	public int writeJPGwithFooter(Path pathIn, Path pathOut, BufferedImage footerBuffer)
			throws IOException, DigitalDerivansException {
		int newHeight = 0;
		boolean isRotated = false;
		BufferedImage readBuffer = ImageIO.read(pathIn.toFile());
		if (readBuffer == null) {
			throw new DigitalDerivansException("Invalid image data " + pathIn + "!");
		}
		float origWidth = readBuffer.getWidth();
		float origHeight = readBuffer.getHeight();
		BufferedImage processedFooter = null;
		if (origWidth >= DEFAULT_MINIMAL_DIMENSION) {
			float ratio = origWidth / footerBuffer.getWidth();
			processedFooter = this.scale(footerBuffer, ratio);
			int scaledHeight = processedFooter.getHeight();
			if (scaledHeight < MIN_SCALED_FOOTER_HEIGHT) {
				var sRatio = String.format("%.2f", ratio);
				LOGGER.warn("Refuse to render footer to {} height {} < min '{}' (scale: {})",
						pathIn, scaledHeight, MIN_SCALED_FOOTER_HEIGHT, sRatio);
				this.writeJPG(pathIn, pathOut);
				return 0;
			}
		} else if (origHeight >= DEFAULT_MINIMAL_DIMENSION) {
			float ratio = origHeight / footerBuffer.getWidth();
			processedFooter = this.scale(footerBuffer, ratio);
			int scaledHeight = processedFooter.getHeight();
			if (scaledHeight < MIN_SCALED_FOOTER_HEIGHT) {
				var sRatio = String.format("%.2f", ratio);
				LOGGER.warn("Refuse to render footer to {} height {} < min '{}' (scale: {})",
						pathIn, scaledHeight, MIN_SCALED_FOOTER_HEIGHT, sRatio);
				this.writeJPG(pathIn, pathOut);
				return 0;
			}
			processedFooter = this.rotate(processedFooter, -90);
			isRotated = true;
		} else {
			// no footer added for too small images
			LOGGER.warn("Refuse to add footer to '{}' because of dimension {}x{}",
						pathIn, origWidth, origHeight);
			this.writeJPG(pathIn, pathOut);
			return 0;
		}

		// append footer buffer at bottom
		BufferedImage mergedlBuffers = this.merge(readBuffer, processedFooter, isRotated);
		ImageMetadata imageMetada = new ImageMetadata();
		imageMetada.enrichFrom(pathIn);
		this.writeJPGWithQualityAndMetadata(mergedlBuffers, pathOut, imageMetada);
		readBuffer.flush();
		processedFooter.flush();
		mergedlBuffers.flush();

		// return changed height/width with added footer
		return newHeight;
	}

	protected BufferedImage handleMaximalDimension(BufferedImage buffer) {
		int width = buffer.getWidth();
		int height = buffer.getHeight();
		if (width > this.maximal || height > this.maximal) {
			float ratio = calculateRatio(buffer);
			return this.scale(buffer, ratio);
		}
		return buffer;
	}

	protected float calculateRatio(BufferedImage orig) {
		int maxDim = orig.getHeight() > orig.getWidth() ? orig.getHeight() : orig.getWidth();
		return (float) this.maximal / (float) maxDim;
	}

	/**
	 * Rotates an image. Note that an angle of -90 is equal to 270.
	 * 
	 * @param img   The image to be rotated
	 * @param angle The angle in degrees (must be a multiple of 90°).
	 * @return The rotated image, or the original image, if the effective angle is 0°.
	 * @throws DigitalDerivansException 
	 */
	public BufferedImage rotate(BufferedImage img, int angle) throws DigitalDerivansException {
		if (angle < 0) {
			angle = 360 + (angle % 360);
		}
		if ((angle % 360) == 0) {
			return img;
		}
		final boolean r180 = angle == 180;
		if (angle != 90 && !r180 && angle != 270)
			throw new DigitalDerivansException("Invalid angle.");
		final int w = r180 ? img.getWidth() : img.getHeight();
		final int h = r180 ? img.getHeight() : img.getWidth();
		final int type = img.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB : img.getType();
		final BufferedImage rotated = new BufferedImage(w, h, type);
		final Graphics2D graphic = rotated.createGraphics();
		graphic.rotate(Math.toRadians(angle), w / 2d, h / 2d);
		final int offset = r180 ? 0 : (w - h) / 2;
		graphic.drawImage(img, null, offset, -offset);
		graphic.dispose();
		return rotated;
	}
}
