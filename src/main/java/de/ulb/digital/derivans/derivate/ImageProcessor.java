package de.ulb.digital.derivans.derivate;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.config.DefaultConfiguration;

/**
 * 
 * Low-Level Image Processing
 * 
 * @author hartwig
 *
 */
class ImageProcessor {

	private static final String JFIF_ROOT_NODE = "app0JFIF";

	private static final String METADATA_TIFF_X_RESOLUTION = "XResolution";

	/*
	 * String because it gets written into image metadata section.
	 */
	public static final String DEFAULT_IMAGE_DPI = "300";

	public static final String DEFAULT_IMAGE_RES = "1";

	public static final String JAVAX_IMAGEIO_JPEG = "javax_imageio_jpeg_image_1.0";

	public static final String JAVAX_IMAGEIO_TIFF = "javax_imageio_tiff_image_1.0";

	public static final String FILE_EXT_JPEG = "jpeg";

	public static final String METADATA_JPEG_RESUNITS = "resUnits";

	public static final String METADATA_JPEG_XDENSITY = "Xdensity";

	public static final String METADATA_JPEG_YDENSITY = "Ydensity";

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
		BufferedImage dimg = new BufferedImage(newW, newH, original.getType());
		Graphics2D g2d = dimg.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2d.drawImage(original, 0, 0, newW, newH, null);
		g2d.dispose();
		return dimg;
	}

	boolean writeJPGWithQualityAndMetadata(BufferedImage buffer, Path pathOut, IIOMetadata metadata)
			throws IOException {
		buffer = handleMaximalDimension(buffer);

		// determine BufferedImage.type
		// 5 = 8-bit RGB color components, corresponding to Windows-style BGR color
		// model
		// 10 = unsigned byte grayscale image, non-indexed (CS_GRY)
		int bType = buffer.getType();
		if (bType == 0) {
			bType = buffer.getColorModel().getColorSpace().getType();
		}
		ImageTypeSpecifier imageType = ImageTypeSpecifier.createFromBufferedImageType(bType);

		// determine JPG write parameters
		JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
		jpegParams.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
		jpegParams.setOptimizeHuffmanTables(true);
		jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpegParams.setCompressionQuality(this.getQuality());
		jpegParams.setDestinationType(imageType);

		// write image buffer
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
		FileImageOutputStream fios = new FileImageOutputStream(pathOut.toFile());
		writer.setOutput(fios);
		writer.write(null, new IIOImage(buffer, null, metadata), jpegParams);
		fios.close();
		return true;
	}

	public boolean writeJPG(Path pathIn, Path pathOut) throws IOException, DigitalDerivansException {
		BufferedImage buffer = ImageIO.read(pathIn.toFile());
		if (buffer == null) {
			throw new DigitalDerivansException("Invalid image data " + pathIn + "!");
		}
		IIOMetadata metadata = calculateMetadata(pathIn);
		this.writeJPGWithQualityAndMetadata(buffer, pathOut, metadata);
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

		IIOMetadata metadata = calculateMetadata(pathIn);
		this.writeJPGWithQualityAndMetadata(totalBuffer, pathOut, metadata);
		buffer.flush();
		scaledFooter.flush();
		totalBuffer.flush();

		// return new heigt (with footer added)
		return addHeight;
	}

	/**
	 * 
	 * Calculate image metadata for new image from
	 * Defaults regarding target format and some
	 * attributes of preceeding image depending
	 * on it's actual image metadata format
	 * 
	 * @param pathIn
	 * @return
	 * @throws IOException
	 * @throws DigitalDerivansException
	 */
	static IIOMetadata calculateMetadata(Path pathIn) throws IOException, DigitalDerivansException {
		ImageInputStream iis = ImageIO.createImageInputStream(pathIn.toFile());
		Iterator<ImageReader> readerator = ImageIO.getImageReaders(iis);
		if (!readerator.hasNext()) {
			throw new DigitalDerivansException("Unable to recognize image '" + pathIn + "'!");
		}
		ImageReader inputReader = readerator.next();
		inputReader.setInput(iis);
		ImageTypeSpecifier imgType = inputReader.getImageTypes(0).next();
		ImageWriter jpgWriter = ImageIO.getImageWritersBySuffix(ImageProcessor.FILE_EXT_JPEG).next();
		ImageWriteParam defaultParams = jpgWriter.getDefaultWriteParam();
		// imgType considers only Colorspace and SampleModel, so it's
		// okay to re-use this, even for conversions from TIFF to JPEG
		IIOMetadata targetMetadata = jpgWriter.getDefaultImageMetadata(imgType, defaultParams);
		Element targetTree = (Element) targetMetadata.getAsTree(ImageProcessor.JAVAX_IMAGEIO_JPEG);
		Element jfif = (Element) targetTree.getElementsByTagName(JFIF_ROOT_NODE).item(0);
		// respect previous metadata
		// gather previous Metadata from pathIn
		String previousFormat = inputReader.getFormatName();
		IIOMetadata sourceMetadata = inputReader.getImageMetadata(0);
		Element sourceTree = null;
		if (isTiffInput(previousFormat)) {
			sourceTree = (Element) sourceMetadata.getAsTree(ImageProcessor.JAVAX_IMAGEIO_TIFF);
		} else if (isJpegInput(previousFormat)) {
			sourceTree = (Element) sourceMetadata.getAsTree(ImageProcessor.JAVAX_IMAGEIO_JPEG);
		}
		// enrich if source determined
		if (sourceTree != null) {
			enrichMetadata(jfif, sourceTree);
			// sanitize and update only if previous 
			// unknown meta data came into play
			sanitizeMetadata(jfif);
			// update default metadata
			try {
				targetMetadata.setFromTree(ImageProcessor.JAVAX_IMAGEIO_JPEG, targetTree);
			} catch (IIOInvalidTreeException e) {
				throw new DigitalDerivansException(e);
			}
		}
		return targetMetadata;
	}

	private static boolean isTiffInput(String label) {
		return "tif".equalsIgnoreCase(label) || "tiff".equalsIgnoreCase(label);
	}

	private static boolean isJpegInput(String label) {
		return "jpg".equalsIgnoreCase(label) || FILE_EXT_JPEG.equalsIgnoreCase(label);
	}

	/**
	 * 
	 * Enrich Metadata for Resoulution/Density
	 * from previous image, if available.
	 * 
	 * Example TIFF Image Metadata:
	 * 
	 * <TIFFField number="282" name="XResolution">
	 * <TIFFRationals><TIFFRational value="300/1"/></TIFFRationals>
	 * </TIFFField>
	 * <TIFFField number="283" name="YResolution">
	 * <TIFFRationals><TIFFRational value="300/1"/>
	 * </TIFFRationals>
	 * </TIFFField>
	 * 
	 * JFIF - JPEG Interchange File Format
	 * 
	 * @param jfif
	 * @param sourceTree
	 */
	static void enrichMetadata(Element jfif, Node sourceTree) {
		Node firstMetadataNode = sourceTree.getFirstChild(); // Metadata for first Image
		NodeList childs = firstMetadataNode.getChildNodes();
		if (childs.getLength() == 0) { // no metadata nodes, just stop
			return;
		}
		if (ImageProcessor.JAVAX_IMAGEIO_JPEG.equals(sourceTree.getNodeName())) {
			Node xDensitiyNode = childs.item(0).getAttributes().getNamedItem(METADATA_JPEG_XDENSITY);
			if (xDensitiyNode != null) {
				String xDensity = xDensitiyNode.getNodeValue();
				jfif.setAttribute(METADATA_JPEG_XDENSITY, xDensity);
				jfif.setAttribute(METADATA_JPEG_YDENSITY, xDensity);
			}
		} else if (ImageProcessor.JAVAX_IMAGEIO_TIFF.equals(sourceTree.getNodeName())) {
			for (int i = 0; i < childs.getLength(); i++) {
				Node originChild = childs.item(i);
				NamedNodeMap originAttributes = originChild.getAttributes();
				Node originNodeName = originAttributes.getNamedItem("name");
				if (originNodeName == null) { // no attribute "name"
					continue;
				}
				if (originNodeName.getNodeValue().equals(METADATA_TIFF_X_RESOLUTION)) {
					String xRes = originChild.getFirstChild().getFirstChild().getAttributes().getNamedItem("value")
							.getNodeValue();
					String dpiX = xRes.split("/")[0];
					jfif.setAttribute(METADATA_JPEG_XDENSITY, dpiX);
					jfif.setAttribute(METADATA_JPEG_YDENSITY, dpiX);
				}
			}
		}
	}

	/**
	 * 
	 * Ensure that Important metadata for subsequent
	 * steps holds valid information, i.e.:
	 * 
	 * <ul>
	 * <li>XResolution, default: 300</li>
	 * <li>YResolution, default: 300</li>
	 * <li>resUnits: density dots per inch == 1, "0" means unknown</li>
	 * </ul>
	 * 
	 * Please note, that DPI X = DPI Y is assumed
	 * 
	 * @param jfif
	 */
	static void sanitizeMetadata(Element jfif) {
		String resUnit = jfif.getAttribute(METADATA_JPEG_RESUNITS);
		if (resUnit.equals("0")) {
			jfif.setAttribute(METADATA_JPEG_RESUNITS, DEFAULT_IMAGE_RES);
		}
		String xDensity = jfif.getAttribute(METADATA_JPEG_XDENSITY);
		if (xDensity == null || xDensity.isEmpty() || xDensity.equals("1")) {
			jfif.setAttribute(METADATA_JPEG_XDENSITY, DEFAULT_IMAGE_DPI);
		}
		String yDensity = jfif.getAttribute(METADATA_JPEG_YDENSITY);
		if (yDensity == null || yDensity.isEmpty() || yDensity.equals("1")) {
			jfif.setAttribute(METADATA_JPEG_YDENSITY, DEFAULT_IMAGE_DPI);
		}
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

	public static Optional<IIOMetadata> getMetadataFromImagePath(Path sourcePath) {
		try {
			File file = sourcePath.toFile();
			ImageInputStream iis = ImageIO.createImageInputStream(file);
			Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
			if (readers.hasNext()) {
				ImageReader reader = readers.next();
				reader.setInput(iis, true);
				return Optional.of(reader.getImageMetadata(0));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Optional.empty();
	}

}
