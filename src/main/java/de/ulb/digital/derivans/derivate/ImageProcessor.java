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

	public static final String JAVAX_IMAGEIO_JPEG = "javax_imageio_jpeg_image_1.0";

	public static final String JAVAX_IMAGEIO_TIFF = "javax_imageio_tiff_image_1.0";

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
	 * 	Don't use Image.getScaledInstance!
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

	boolean writeJPGWithQualityAndMetadata(BufferedImage buffer, Path pathOut, IIOMetadata metadata) throws IOException {
		buffer = handleMaximalDimension(buffer);

		// determine BufferedImage.type
		//  5 = 8-bit RGB color components, corresponding to Windows-style BGR color model
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
			throw new DigitalDerivansException("Invalid image data " + pathIn+"!");
		}
		IIOMetadata metadata = this.getPreviousMetadata(pathIn);
		this.writeJPGWithQualityAndMetadata(buffer, pathOut, metadata);
		buffer.flush();
		return true;
	}

	public int writeJPGwithFooter(Path pathIn, Path pathOut, BufferedImage footerBuffer)
			throws IOException, DigitalDerivansException {

		// prepare footer buffer
		BufferedImage buffer = ImageIO.read(pathIn.toFile());
		if (buffer == null) {
			throw new DigitalDerivansException("Invalid image data " + pathIn+"!");
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
		
		IIOMetadata metadata = getPreviousMetadata(pathIn);
		this.writeJPGWithQualityAndMetadata(totalBuffer, pathOut, metadata);
		buffer.flush();
		scaledFooter.flush();
		totalBuffer.flush();

		// return new heigt (with footer added)
		return addHeight;
	}

	private IIOMetadata getPreviousMetadata(Path pathIn) throws IOException, DigitalDerivansException {
		IIOMetadata targetMetadata = null;
		ImageInputStream iis = ImageIO.createImageInputStream(pathIn.toFile());
		Iterator<ImageReader> readerator = ImageIO.getImageReaders(iis);
		if (readerator.hasNext()) {
			ImageReader inputReader = readerator.next();
			inputReader.setInput(iis);
			ImageTypeSpecifier imgType = inputReader.getImageTypes(0).next();
			ImageWriter jpgWriter = ImageIO.getImageWritersBySuffix("jpg").next();
			ImageWriteParam defaultParams = jpgWriter.getDefaultWriteParam();
			targetMetadata = jpgWriter.getDefaultImageMetadata(imgType, defaultParams);
			String previousFormat = inputReader.getFormatName();
			if (previousFormat.equalsIgnoreCase("tif")) {
				IIOMetadata oldMetadata = inputReader.getImageMetadata(0);
				this.enrichFromSource(targetMetadata, oldMetadata, ImageProcessor.JAVAX_IMAGEIO_TIFF);
			} else {
				targetMetadata = inputReader.getImageMetadata(0);
			}
			if (targetMetadata == null) {
				throw new DigitalDerivansException("Unable to gather image Metadata for '"+pathIn+"'!");
			}
		}
		return targetMetadata;
	}

	/**
	 * 
	 * Enrich IIOMetadata from original image at derived image IIOMetadata
	 * 
	 * Please not, that it's assumed: DPI X = DPI Y
	 * 
	 * Important EXIF data take into account 
	 * <ul>
	 * 	<li>XResolution</li>
	 * 	<li>YResolution</li>
	 * 	<li>resUnits</li>
	 * </ul>
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
	 * @param origin
	 * @return
	 */
	void enrichFromSource(IIOMetadata target, IIOMetadata source, String formatName) throws DigitalDerivansException {

		// prepare target
		Element targetTree = (Element) target.getAsTree(ImageProcessor.JAVAX_IMAGEIO_JPEG);
		Element jfif = (Element) targetTree.getElementsByTagName("app0JFIF").item(0);

		// inspect source
		Node sourceTree = source.getAsTree(formatName);
		Node firstMetadataNode = sourceTree.getFirstChild(); // Metadata for first Image
		jfif.setAttribute("resUnits", "1"); // density dots per inch == 1
		NodeList originChilds = firstMetadataNode.getChildNodes();
		for (int i = 0; i < originChilds.getLength(); i++) {
			Node originChild = originChilds.item(i);
			NamedNodeMap originAttributes = originChild.getAttributes();
			Node originNodeName = originAttributes.getNamedItem("name");
			if (originNodeName == null) { // no attribute "name"
				continue;
			}
			if (originNodeName.getNodeValue().equals("XResolution")) {
				String xRes = originChild.getFirstChild().getFirstChild().getAttributes().getNamedItem("value")
						.getNodeValue();
				String dpiX = xRes.split("/")[0];
				jfif.setAttribute("Xdensity", dpiX);
				jfif.setAttribute("Ydensity", dpiX);
			}
		}
		try {
			target.setFromTree(ImageProcessor.JAVAX_IMAGEIO_JPEG, targetTree);
		} catch (IIOInvalidTreeException e) {
			throw new DigitalDerivansException(e);
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
