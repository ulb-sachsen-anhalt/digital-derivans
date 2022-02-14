package de.ulb.digital.derivans.derivate;

import java.awt.Graphics2D;
import java.awt.Image;
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
	public static final Integer EXPECTED_MINIMAL_HEIGHT = 25;

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

	BufferedImage scale(BufferedImage original, float ratio) {
		int newW = (int) (ratio * original.getWidth());
		int newH = (int) (ratio * original.getHeight());
		Image tmp = original.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
		BufferedImage dimg = new BufferedImage(newW, newH, original.getType());
		Graphics2D g2d = dimg.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
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
		jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpegParams.setCompressionQuality(this.getQuality());
		jpegParams.setDestinationType(imageType);
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
		IIOMetadata metadata = this.previousMetadata(pathIn);
		this.writeJPGWithQualityAndMetadata(buffer, pathOut, metadata);
		buffer.flush();
		return true;
	}

	public int writeJPGwithFooter(Path pathIn, Path pathOut, BufferedImage footerBuffer)
			throws IOException, DigitalDerivansException {

		// prepare footer buffer
		BufferedImage buffer = ImageIO.read(pathIn.toFile());
		float ratio = (float) buffer.getWidth() / (float) footerBuffer.getWidth();
		BufferedImage scaledFooter = this.scale(footerBuffer, ratio);
		if (scaledFooter.getHeight() < EXPECTED_MINIMAL_HEIGHT) {
			String msg2 = String.format("scale problem: heigth dropped beneath '%d'", footerBuffer.getHeight());
			throw new DigitalDerivansException(msg2);
		}
		int addHeight = scaledFooter.getHeight();

		// append footer buffer at bottom
		BufferedImage totalBuffer = this.append(buffer, scaledFooter);
		
		IIOMetadata metadata = previousMetadata(pathIn);
		this.writeJPGWithQualityAndMetadata(totalBuffer, pathOut, metadata);
		buffer.flush();
		scaledFooter.flush();
		totalBuffer.flush();

		// return new heigt (with footer added)
		return addHeight;
	}

	private IIOMetadata previousMetadata(Path pathIn) throws IOException, DigitalDerivansException {
		IIOMetadata metadata = null;
		ImageInputStream iis = ImageIO.createImageInputStream(pathIn.toFile());
		Iterator<ImageReader> readerator = ImageIO.getImageReaders(iis);
		if (readerator.hasNext()) {
			ImageReader readerOne = readerator.next();
			readerOne.setInput(iis);
			String previousFormat = readerOne.getFormatName();
			ImageWriter imageWriter = ImageIO.getImageWritersBySuffix("jpg").next();
			ImageWriteParam defaultParams = imageWriter.getDefaultWriteParam();
			ImageTypeSpecifier imgType = readerOne.getImageTypes(0).next();
			metadata = imageWriter.getDefaultImageMetadata(imgType, defaultParams);
			IIOMetadata oldMetadata = readerOne.getImageMetadata(0);
			if (previousFormat.equalsIgnoreCase("tif")) {
				this.enrichFromTIF(metadata, oldMetadata);
			} else {
				metadata = readerOne.getImageMetadata(0);
			}
		}
		return metadata;
	}

	/**
	 * 
	 * Enrich IIOMetadata from original image at derived image IIOMetadata
	 * 
	 * Please not, that it's assumed: DPI X = DPI Y
	 * 
	 * Important TIFF EXIF data take into account
	 * 
	 * <TIFFIFD ... 
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
	void enrichFromTIF(IIOMetadata target, IIOMetadata source) throws DigitalDerivansException {

		// prepare target
		Element tree = (Element) target.getAsTree(ImageProcessor.JAVAX_IMAGEIO_JPEG);
		Element jfif = (Element) tree.getElementsByTagName("app0JFIF").item(0);

		// inspect source
		Node originRoot = source.getAsTree("javax_imageio_tiff_image_1.0");
		Node tiffIFD = originRoot.getFirstChild(); // TIFFIFD root
		jfif.setAttribute("resUnits", "1"); // density dots per inch == 1
		NodeList originChilds = tiffIFD.getChildNodes();
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
			target.setFromTree(ImageProcessor.JAVAX_IMAGEIO_JPEG, tree);
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

	void displayMetadata(Node node, int level) {
		// print open tag of element
		indent(level);
		System.out.print("<" + node.getNodeName());
		NamedNodeMap map = node.getAttributes();
		if (map != null) {
			// print attribute values
			int length = map.getLength();
			for (int i = 0; i < length; i++) {
				Node attr = map.item(i);
				System.out.print(" " + attr.getNodeName() + "=\"" + attr.getNodeValue() + "\"");
			}
		}
		Node child = node.getFirstChild();
		if (child == null) {
			// no children, so close element and return
			System.out.println("/>");
			return;
		}
		// children, so close current tag
		System.out.println(">");
		while (child != null) {
			// print children recursively
			displayMetadata(child, level + 1);
			child = child.getNextSibling();
		}
		// print close tag of element
		indent(level);
		System.out.println("</" + node.getNodeName() + ">");
	}

	void indent(int level) {
		for (int i = 0; i < level; i++)
			System.out.print("\t");
	}
}
