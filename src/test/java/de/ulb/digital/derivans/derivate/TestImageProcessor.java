package de.ulb.digital.derivans.derivate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestResource;

/**
 * 
 * Test Specification for {@link ImageProcessor}
 * 
 * @author hartwig
 *
 */
class TestImageProcessor {

	@Test
	void testRendererJPGWithParameters(@TempDir Path tempDir) throws DigitalDerivansException, IOException {
		// arrange
		Path sourcePath = TestResource.IMG_JPG_148811035_MAX_1.get();
		Path targetDir = tempDir.resolve("IMAGE");
		Files.createDirectory(targetDir);
		Path targetPath = targetDir.resolve("1.jpg");
		BufferedImage buffer = ImageIO.read(sourcePath.toFile());
		ImageInputStream iis = ImageIO.createImageInputStream(sourcePath.toFile());
		Iterator<ImageReader> readerator = ImageIO.getImageReaders(iis);
		IIOMetadata metadata = null;
		if (readerator.hasNext()) {
			ImageReader readerOne = readerator.next();
			readerOne.setInput(iis);
			metadata = readerOne.getImageMetadata(0);
		}
//		IIOMetadata mOriginal = getMetadataFromImagePath(sourcePath).get();
//		displayMetadata(mOriginal);

		// act
		boolean outcome = new ImageProcessor().writeJPGWithQualityAndMetadata(buffer, targetPath.toString(), 0.8f,
				metadata);

		// assert
		assertTrue(outcome);
		Optional<IIOMetadata> optNewMetadata = getMetadataFromImagePath(targetPath);
		assertTrue(optNewMetadata.isPresent());
		IIOMetadata newMetadata = optNewMetadata.get();
		Node root = newMetadata.getAsTree("javax_imageio_jpeg_image_1.0");
		assertNotNull(root);
		Node firstChild = root.getFirstChild();
		assertEquals("JPEGvariety", firstChild.getLocalName());
		Node firstGrandchild = firstChild.getFirstChild();
		assertEquals("app0JFIF", firstGrandchild.getLocalName());
		assertEquals("300", firstGrandchild.getAttributes().getNamedItem("Xdensity").getNodeValue());
		assertEquals("300", firstGrandchild.getAttributes().getNamedItem("Ydensity").getNodeValue());
		assertEquals("1", firstGrandchild.getAttributes().getNamedItem("resUnits").getNodeValue());
	}

	private Optional<IIOMetadata> getMetadataFromImagePath(Path sourcePath) {
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

	private void displayMetadata(IIOMetadata metadata) {
		try {
			String[] names = metadata.getMetadataFormatNames();
			int length = names.length;
			for (int i = 0; i < length; i++) {
				System.out.println("Format name: " + names[i]);
				displayMetadata(metadata.getAsTree(names[i]));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void displayMetadata(Node root) {
		displayMetadata(root, 0);
	}

	void indent(int level) {
		for (int i = 0; i < level; i++)
			System.out.print("    ");
	}

	/**
	 * see: http://johnbokma.com/java/obtaining-image-metadata.html
	 * 
	 * @param node
	 * @param level
	 */
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
}
