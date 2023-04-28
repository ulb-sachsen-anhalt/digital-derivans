package de.ulb.digital.derivans.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.metadata.IIOInvalidTreeException;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.ulb.digital.derivans.DigitalDerivansException;

/**
 * 
 * Wrapper for processing Image ImageMetadata details
 * 
 */
public class ImageMetadata {

	public static final String JAVAX_IMAGEIO = "javax_imageio_1.0";

	public static final String JAVAX_IMAGEIO_JPEG = "javax_imageio_jpeg_image_1.0";

	public static final String JAVAX_IMAGEIO_TIFF = "javax_imageio_tiff_image_1.0";

	public static final String JFIF_ROOT_NODE = "app0JFIF";

	public static final String TIFF_NODE = "TIFFField";

	public static final String JPEG_VARIETY = "JPEGvariety";

	public static final String METADATA_TIFF_X_RESOLUTION = "XResolution";

	public static final String METADATA_TIFF_Y_RESOLUTION = "YResolution";

	/*
	 * String because it gets written into image metadata section.
	 */
	public static final String DEFAULT_IMAGE_DPI = "300";

	public static final String DEFAULT_IMAGE_RES = "1";

	public static final String FILE_EXT_JPEG = "jpeg";

	public static final String METADATA_JPEG_RESUNITS = "resUnits";

	public static final String METADATA_JPEG_XDENSITY = "Xdensity";

	public static final String METADATA_JPEG_YDENSITY = "Ydensity";

	public static final String COMPRESSION = "Compression";

	public static final String ATTRIBUTE_NUMPROGS = "NumProgressiveScans";

	// considered to mean: "Baseline"
	private static final String VALUE_PROGESSION_LOW = "1";

	private IIOMetadata iioMetadata;

	private boolean ignoreProgressiveMode;

	/**
	 * Default constructor
	 */
	public ImageMetadata() {
	}

	public ImageMetadata(IIOMetadata iioMetadata) {
		this.iioMetadata = iioMetadata;
	}

	public ImageMetadata(Path input) throws DigitalDerivansException {
		try {
			File file = input.toFile();
			ImageInputStream iis = ImageIO.createImageInputStream(file);
			Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
			if (readers.hasNext()) {
				ImageReader reader = readers.next();
				reader.setInput(iis, true);
				this.iioMetadata = reader.getImageMetadata(0);
			} else {
				throw new DigitalDerivansException("no imageReader matching " + input + "!");
			}
		} catch (Exception e) {
			throw new DigitalDerivansException(e);
		}
	}

	public IIOMetadata getData() {
		return iioMetadata;
	}

	public void ignoreProgressiveMode() {
		this.ignoreProgressiveMode = true;
	}

	public boolean getIgnoreProgressiveMode() {
		return this.ignoreProgressiveMode;
	}

	public boolean requiresProgressiveMode() {
		if (ignoreProgressiveMode) {
			return false;
		}
		var meta = iioMetadata.getAsTree(JAVAX_IMAGEIO);
		var kids = meta.getChildNodes();
		for (var i = 0; i < kids.getLength(); i++) {
			Node n = kids.item(i);
			if (COMPRESSION.equals(n.getNodeName())) {
				var compressions = n.getChildNodes();
				for (var j = 0; j < compressions.getLength(); j++) {
					Node m = compressions.item(j);
					if (ATTRIBUTE_NUMPROGS.equals(m.getNodeName())) {
						var value = m.getAttributes().getNamedItem("value").getNodeValue();
						return VALUE_PROGESSION_LOW.equals(value);
					}
				}
			}
		}
		return false;
	}

	/**
	 * 
	 * Gather image metadata for new image from
	 * Defaults regarding target format and some
	 * attributes of preceeding image depending
	 * on it's actual image metadata format
	 * 
	 * @param pathIn
	 * @return
	 * @throws IOException
	 * @throws DigitalDerivansException
	 */
	public void enrichFrom(Path pathIn) throws IOException, DigitalDerivansException {
		ImageInputStream iis = ImageIO.createImageInputStream(pathIn.toFile());
		Iterator<ImageReader> readerator = ImageIO.getImageReaders(iis);
		if (!readerator.hasNext()) {
			throw new DigitalDerivansException("Unable to recognize image '" + pathIn + "'!");
		}
		ImageReader inputReader = readerator.next();
		inputReader.setInput(iis);
		ImageTypeSpecifier imageType = inputReader.getImageTypes(0).next();
		ImageWriter jpgWriter = ImageIO.getImageWritersBySuffix(FILE_EXT_JPEG).next();
		ImageWriteParam defaultParams = jpgWriter.getDefaultWriteParam();
		// imgType considers only Colorspace and SampleModel, so it's
		// okay to re-use this, even for conversions from TIFF to JPEG
		IIOMetadata targetMetadata = jpgWriter.getDefaultImageMetadata(imageType, defaultParams);
		Element targetTree = (Element) targetMetadata.getAsTree(JAVAX_IMAGEIO_JPEG);
		// respect previous metadata
		// gather previous ImageMetadata from pathIn
		String previousFormat = inputReader.getFormatName();
		IIOMetadata sourceMetadata = inputReader.getImageMetadata(0);
		Node sourceTree = null;
		try {
			Element jfif = (Element) targetTree.getElementsByTagName(JFIF_ROOT_NODE).item(0);
			if (isTiffInput(previousFormat)) {
				sourceTree = sourceMetadata.getAsTree(JAVAX_IMAGEIO_TIFF);
				this.ignoreProgressiveMode = true;
			} else if (isJpegInput(previousFormat)) {
				sourceTree = sourceMetadata.getAsTree(JAVAX_IMAGEIO_JPEG);
			}
			// enrich and sanitize if source determined
			if (sourceTree == null) {
				throw new DigitalDerivansException("no valid metadata source for " + pathIn + "!");
			}
			enrichMetadata(jfif, sourceTree);
			sanitizeMetadata(jfif);
			targetMetadata.setFromTree(JAVAX_IMAGEIO_JPEG, targetTree);
		} catch (IIOInvalidTreeException e) {
			throw new DigitalDerivansException(e);
		}
		this.iioMetadata = targetMetadata;
	}

	private static boolean isTiffInput(String label) {
		return "tif".equalsIgnoreCase(label) || "tiff".equalsIgnoreCase(label);
	}

	private static boolean isJpegInput(String label) {
		return "jpg".equalsIgnoreCase(label) || FILE_EXT_JPEG.equalsIgnoreCase(label);
	}

	/**
	 * 
	 * Enrich ImageMetadata for Resoulution/Density
	 * from previous image, if available.
	 * 
	 * Example TIFF Image ImageMetadata:
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
	 * JFIF - JPEG Interchange File Format
	 * 
	 * @param jfif
	 * @param sourceTree
	 */
	static void enrichMetadata(Element jfif, Node sourceTree) {
		Node firstMetadataNode = sourceTree.getFirstChild(); // ImageMetadata for first Image
		NodeList childs = firstMetadataNode.getChildNodes();
		if (childs.getLength() == 0) { // no metadata nodes, just stop
			return;
		}
		if (JAVAX_IMAGEIO_JPEG.equals(sourceTree.getNodeName())) {
			Node xDensitiyNode = childs.item(0).getAttributes().getNamedItem(METADATA_JPEG_XDENSITY);
			if (xDensitiyNode != null) {
				String xDensity = xDensitiyNode.getNodeValue();
				jfif.setAttribute(METADATA_JPEG_XDENSITY, xDensity);
				jfif.setAttribute(METADATA_JPEG_YDENSITY, xDensity);
			}
		} else if (JAVAX_IMAGEIO_TIFF.equals(sourceTree.getNodeName())) {
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

}