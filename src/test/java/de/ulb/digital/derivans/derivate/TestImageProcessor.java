package de.ulb.digital.derivans.derivate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javax.imageio.metadata.IIOMetadata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.ulb.digital.derivans.DigitalDerivansException;
import static de.ulb.digital.derivans.derivate.ImageProcessor.*;
import de.ulb.digital.derivans.TestResource;

/**
 * 
 * Test Specification for {@link ImageProcessor}
 * 
 * @author hartwig
 *
 */
class TestImageProcessor {

	static ImageProcessor imageProcessor = new ImageProcessor(80, 3320);

	@Test
	void testWrite_JPGRGB_to_JPGRGB(@TempDir Path tempDir) throws DigitalDerivansException, IOException {
		// arrange
		Path sourcePath = TestResource.IMG_JPG_148811035_MAX_1.get();
		Path targetDir = tempDir.resolve("IMAGE");
		Files.createDirectory(targetDir);
		Path targetPath = targetDir.resolve("1.jpg");

		// act
		boolean outcome = imageProcessor.writeJPG(sourcePath, targetPath);

		// assert
		assertTrue(outcome);
		Optional<IIOMetadata> optNewMetadata = ImageProcessor.getMetadataFromImagePath(targetPath);
		assertTrue(optNewMetadata.isPresent());
		IIOMetadata newMetadata = optNewMetadata.get();
		Node root = newMetadata.getAsTree(ImageProcessor.JAVAX_IMAGEIO_JPEG);
		Node firstChild = root.getFirstChild();
		assertEquals("JPEGvariety", firstChild.getLocalName());
		Node firstGrandchild = firstChild.getFirstChild();
		assertEquals("app0JFIF", firstGrandchild.getLocalName());
		assertEquals(DEFAULT_IMAGE_DPI, firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_XDENSITY).getNodeValue());
		assertEquals(DEFAULT_IMAGE_DPI, firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_YDENSITY).getNodeValue());
		assertEquals(DEFAULT_IMAGE_RES, firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_RESUNITS).getNodeValue());
	}

	@Test
	void testReadMetadata_JPG_RGB() {
		Path sourcePath = TestResource.IMG_JPG_148811035_MAX_1.get();

		// act
		var metadata = ImageProcessor.getMetadataFromImagePath(sourcePath);

		// assert
		assertTrue(metadata.isPresent());
		Node root = metadata.get().getAsTree(ImageProcessor.JAVAX_IMAGEIO_JPEG);
		var grandChilds = root.getFirstChild().getChildNodes();
		assertEquals(1, grandChilds.getLength());
	}

	@Test
	void testReadMetadata_TIF_Greyscale() {
		Path sourcePath = TestResource.IMG_TIF_ZD1_GREY.get();

		// act
		var metadata = ImageProcessor.getMetadataFromImagePath(sourcePath);

		// assert
		assertTrue(metadata.isPresent());
		Node root = metadata.get().getAsTree(ImageProcessor.JAVAX_IMAGEIO_TIFF);
		var grandChilds = root.getFirstChild().getChildNodes();
		assertEquals(25, grandChilds.getLength());
		assertEquals("TIFFField", grandChilds.item(6).getNodeName());
		assertEquals("XResolution", grandChilds.item(13).getAttributes().item(1).getNodeValue());
		assertEquals("YResolution", grandChilds.item(14).getAttributes().item(1).getNodeValue());
	}

	@Test
	void testProcessImage_TIFGreyscale_to_JPG(@TempDir Path tempDir) throws Exception {
		Path sourcePath = TestResource.IMG_TIF_ZD1_GREY.get();
		Path targetDir = tempDir.resolve("IMAGE");
		Files.createDirectory(targetDir);
		Path targetPath = targetDir.resolve("1.jpg");

		// act
		boolean outcome = imageProcessor.writeJPG(sourcePath, targetPath);

		// assert
		assertTrue(outcome);
		Optional<IIOMetadata> optNewMetadata = ImageProcessor.getMetadataFromImagePath(targetPath);
		assertTrue(optNewMetadata.isPresent());
		IIOMetadata newMetadata = optNewMetadata.get();
		Node root = newMetadata.getAsTree(ImageProcessor.JAVAX_IMAGEIO_JPEG);
		assertNotNull(root);
		Node firstChild = root.getFirstChild();
		assertEquals("JPEGvariety", firstChild.getLocalName());
		Node firstGrandchild = firstChild.getFirstChild();
		assertEquals("app0JFIF", firstGrandchild.getLocalName());
		assertEquals("470", firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_XDENSITY).getNodeValue());
		assertEquals("470", firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_YDENSITY).getNodeValue());
		assertEquals(DEFAULT_IMAGE_RES, firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_RESUNITS).getNodeValue());
	}

	@Test
	void testProcessImage_JPGGreyscale_to_JPG2(@TempDir Path tempDir) throws Exception {
		Path sourcePath = TestResource.IMG_JPG_ZD2_GREY.get();
		Path targetDir = tempDir.resolve("IMAGE");
		Files.createDirectory(targetDir);
		Path targetPath = targetDir.resolve("1.jpg");

		// act
		boolean outcome = imageProcessor.writeJPG(sourcePath, targetPath);

		// assert
		assertTrue(outcome);
		Optional<IIOMetadata> optNewMetadata = ImageProcessor.getMetadataFromImagePath(targetPath);
		assertTrue(optNewMetadata.isPresent());
		IIOMetadata newMetadata = optNewMetadata.get();
		Node root = newMetadata.getAsTree(ImageProcessor.JAVAX_IMAGEIO_JPEG);
		assertNotNull(root);
		Node firstChild = root.getFirstChild();
		assertEquals("JPEGvariety", firstChild.getLocalName());
		Node firstGrandchild = firstChild.getFirstChild();
		assertEquals("app0JFIF", firstGrandchild.getLocalName());
		assertEquals("470", firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_XDENSITY).getNodeValue());
		assertEquals("470", firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_YDENSITY).getNodeValue());
		assertEquals(DEFAULT_IMAGE_RES, firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_RESUNITS).getNodeValue());
	}

	/**
	 * 
	 * Testdata: first page from "Bote für das Saalethal" 1864
	 * 
	 */
	@Test
	void testReadMetadata_TIF_RGB() {
		Path sourcePath = TestResource.IMG_TIF_ZD1_RGB.get();

		// act
		var metadata = ImageProcessor.getMetadataFromImagePath(sourcePath);

		// assert
		assertTrue(metadata.isPresent());
		Node root = metadata.get().getAsTree(JAVAX_IMAGEIO_TIFF);
		NodeList children = root.getChildNodes();
		assertEquals(1, children.getLength());

	}

	@Test
	void testReadMetadata_TIF_Postscan_RGB(@TempDir Path tempDir) throws Exception {
		Path sourcePath = Path.of("src/test/resources/images/00000020-small.tif");
		Path targetDir = tempDir.resolve("MAX");
		Files.createDirectory(targetDir);
		Path targetPath = targetDir.resolve("20.jpg");

		// act
		var actualExc = assertThrows(DigitalDerivansException.class, () -> imageProcessor.writeJPG(sourcePath, targetPath));

		// assert
		var excMsg = actualExc.getMessage();
		assertTrue(excMsg.startsWith("Illegal band size: should be 0 < size <= 8:"));
		assertTrue(excMsg.endsWith("MAX/20.jpg"));
	}

	@Test
	void testProcessImage_TIFRGB_to_JPG(@TempDir Path tempDir) throws Exception {
		Path sourcePath = TestResource.IMG_TIF_ZD1_RGB.get();
		Path targetDir = tempDir.resolve("IMAGE");
		Files.createDirectory(targetDir);
		Path targetPath = targetDir.resolve("1.jpg");

		// act
		boolean outcome = imageProcessor.writeJPG(sourcePath, targetPath);

		// assert
		assertTrue(outcome);
		Optional<IIOMetadata> optNewMetadata = ImageProcessor.getMetadataFromImagePath(targetPath);
		assertTrue(optNewMetadata.isPresent());
		IIOMetadata newMetadata = optNewMetadata.get();
		Node root = newMetadata.getAsTree("javax_imageio_jpeg_image_1.0");
		assertNotNull(root);
		Node firstChild = root.getFirstChild();
		assertEquals("JPEGvariety", firstChild.getLocalName());
		Node firstGrandchild = firstChild.getFirstChild();
		assertEquals("app0JFIF", firstGrandchild.getLocalName());
		assertEquals(DEFAULT_IMAGE_DPI, firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_XDENSITY).getNodeValue());
		assertEquals(DEFAULT_IMAGE_DPI, firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_YDENSITY).getNodeValue());
		assertEquals(DEFAULT_IMAGE_RES, firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_RESUNITS).getNodeValue());
	}

	@Test
	void testReadMetadata_TIF_RGB_withInvalidAttributes() {
		Path sourcePath = TestResource.IMG_TIF_MENA_1.get();

		// act
		var metadata = ImageProcessor.getMetadataFromImagePath(sourcePath);

		// assert
		assertTrue(Files.exists(sourcePath, LinkOption.NOFOLLOW_LINKS));
		Node root = metadata.get().getAsTree("javax_imageio_tiff_image_1.0");
		NodeList grandChilds = root.getFirstChild().getChildNodes();
		assertEquals(22, grandChilds.getLength());
		assertEquals("TIFFField", grandChilds.item(6).getNodeName());
		assertEquals("XResolution", grandChilds.item(12).getAttributes().item(1).getNodeValue());
		assertEquals("300/1",
				grandChilds.item(12).getFirstChild().getFirstChild().getAttributes().item(0).getNodeValue());
		assertEquals("YResolution", grandChilds.item(13).getAttributes().item(1).getNodeValue());
		assertEquals("300/1",
				grandChilds.item(13).getFirstChild().getFirstChild().getAttributes().item(0).getNodeValue());
	}

	/**
	 * 
	 * Ensure we determine the *real* data from given image
	 * 
	 * @param tempDir
	 * @throws DigitalDerivansException
	 * @throws IOException
	 */
	@Test
	void testProcessSLUBImageBaselineMetadata() throws DigitalDerivansException, IOException {
		// arrange
		String img = "src/test/resources/images/FILE_0001_MAX.jpg";
		Path imgSource = Paths.get(img).toAbsolutePath();

		// act
		var metadata = ImageProcessor.getMetadataFromImagePath(imgSource);

		// assert
		assertTrue(Files.exists(imgSource, LinkOption.NOFOLLOW_LINKS));
		Node root = metadata.get().getAsTree(JAVAX_IMAGEIO_JPEG);
		NodeList grandChilds = root.getFirstChild().getChildNodes();
		Node firstChild = root.getFirstChild();
		assertEquals("JPEGvariety", firstChild.getLocalName());
		Node firstGrandchild = firstChild.getFirstChild();
		assertEquals(1, grandChilds.getLength());
		assertEquals("app0JFIF", firstGrandchild.getLocalName());
		assertEquals("1", firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_XDENSITY).getNodeValue());
		assertEquals("1", firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_YDENSITY).getNodeValue());
		assertEquals("0", firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_RESUNITS).getNodeValue());
	}

	/**
	 * 
	 * Ensure if we process this image, we have valid values afterwards
	 * 
	 * @param tempDir
	 * @throws DigitalDerivansException
	 * @throws IOException
	 */
	@Test
	void testProcessSLUBImageBaseline(@TempDir Path tempDir) throws DigitalDerivansException, IOException {
		// arrange
		String img = "src/test/resources/images/FILE_0001_MAX.jpg";
		Path imgSource = Paths.get(img).toAbsolutePath();
		Path targetDir = tempDir.resolve("IMAGE-BASELINE");
		Files.createDirectory(targetDir);
		Path targetPath = targetDir.resolve("FILE_0001_MAX.jpg");

		// act
		boolean outcome = imageProcessor.writeJPG(imgSource, targetPath);
		assertTrue(outcome);
		Optional<IIOMetadata> optNewMetadata = ImageProcessor.getMetadataFromImagePath(targetPath);

		// assert
		assertTrue(optNewMetadata.isPresent());
		IIOMetadata newMetadata = optNewMetadata.get();
		Node root = newMetadata.getAsTree("javax_imageio_jpeg_image_1.0");
		Node firstChild = root.getFirstChild();
		assertEquals("JPEGvariety", firstChild.getLocalName());
		Node firstGrandchild = firstChild.getFirstChild();
		assertEquals("app0JFIF", firstGrandchild.getLocalName());
		assertEquals(DEFAULT_IMAGE_DPI, firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_XDENSITY).getNodeValue());
		assertEquals(DEFAULT_IMAGE_DPI, firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_YDENSITY).getNodeValue());
		assertEquals(DEFAULT_IMAGE_RES, firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_RESUNITS).getNodeValue());
	}

	@Test
	void testProcessImageFromEmptyInput(@TempDir Path tempDir) throws Exception {
		Path sourcePath = TestResource.IMG_JPG_ZERO.get();
		Path targetDir = tempDir.resolve("IMAGE");
		Files.createDirectory(targetDir);
		Path targetPath = targetDir.resolve("zero.jpg");

		// act
		var actual = assertThrows(DigitalDerivansException.class, () -> imageProcessor.writeJPG(sourcePath, targetPath));

		// assert
		assertEquals("Invalid fileSize 0 for src/test/resources/images/00000020.jpg!", actual.getMessage());
	}
}
