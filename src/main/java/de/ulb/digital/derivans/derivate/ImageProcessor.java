package de.ulb.digital.derivans.derivate;

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

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.config.DefaultConfiguration;
import de.ulb.digital.derivans.data.ImageMetadata;

/**
 * 
 * Low-Level Image Processing
 * 
 * @author hartwig
 *
 */
class ImageProcessor {

	/**
	 * Percentage image quality
	 */
	private float qualityRatio = 1.0f;

	/**
	 * Maximal image dimension in width or height
	 */
	private int maximal = DefaultConfiguration.DEFAULT_MAXIMAL;

	/**
	 * Error marker, if a large number of subsequent down scales make the footer
	 * disappear after all
	 */
	public static final Integer EXPECTED_MINIMAL_HEIGHT = 10;

	public ImageProcessor() {
	}

	public ImageProcessor(int quality, int maximal) {
		this.setQuality(quality);
		this.setMaximal(maximal);
	}

	public void setMaximal(int maximal) {
		if (maximal > 0) {
			this.maximal = maximal;
		}
	}

	public void setQuality(int quality) {
		if (quality > 0 && quality <= 100) {
			this.qualityRatio = quality / 100.0f;
		}
	}

	public float getQuality() {
		return this.qualityRatio;
	}

	BufferedImage append(BufferedImage bufferedImage, BufferedImage footer) {
		int bType = bufferedImage.getType();
		if (bType == 0) {
			bType = bufferedImage.getColorModel().getColorSpace().getType();
		}

		int newHeight = bufferedImage.getHeight() + footer.getHeight();
		BufferedImage newImage = new BufferedImage(bufferedImage.getWidth(), newHeight, bType);
		Graphics2D g2d = newImage.createGraphics();
		g2d.drawImage(bufferedImage, 0, 0, null);
		g2d.drawImage(footer, 0, bufferedImage.getHeight(), null);
		g2d.dispose();
		return newImage;
	}

	BufferedImage clone(BufferedImage original) {
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

		// prepare footer buffer
		BufferedImage buffer = ImageIO.read(pathIn.toFile());
		if (buffer == null) {
			throw new DigitalDerivansException("Invalid image data " + pathIn + "!");
		}
		float ratio = (float) buffer.getWidth() / (float) footerBuffer.getWidth();
		BufferedImage scaledFooter = this.scale(footerBuffer, ratio);
		int scaledHeigth = scaledFooter.getHeight();
		if (scaledHeigth < EXPECTED_MINIMAL_HEIGHT) {
			String msg2 = String.format("problem: footer h '%d' dropped beneath '%d' (scale: '%.2f')",
					scaledHeigth, EXPECTED_MINIMAL_HEIGHT, ratio);
			throw new DigitalDerivansException(msg2);
		}
		int addHeight = scaledFooter.getHeight();

		// append footer buffer at bottom
		BufferedImage totalBuffer = this.append(buffer, scaledFooter);
		ImageMetadata imageMetada = new ImageMetadata();
		imageMetada.enrichFrom(pathIn);
		this.writeJPGWithQualityAndMetadata(totalBuffer, pathOut, imageMetada);
		buffer.flush();
		scaledFooter.flush();
		totalBuffer.flush();

		// return new heigt (with footer added)
		return addHeight;
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
}
