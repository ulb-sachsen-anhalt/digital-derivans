package de.ulb.digital.derivans.derivate;

import static de.ulb.digital.derivans.data.image.ImageMetadata.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.metadata.IIOMetadata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.data.image.ImageMetadata;
import de.ulb.digital.derivans.derivate.image.ImageProcessor;

/**
 * 
 * Test Specification for {@link ImageProcessor} 
 * using {@link ImageMetadata}
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
		var metadata = new ImageMetadata(targetPath);
		IIOMetadata newMetadata = metadata.getData();
		Node root = newMetadata.getAsTree(JAVAX_IMAGEIO_JPEG);
		Node firstChild = root.getFirstChild();
		assertEquals(JPEG_VARIETY, firstChild.getLocalName());
		Node firstGrandchild = firstChild.getFirstChild();
		assertEquals(JFIF_ROOT_NODE, firstGrandchild.getLocalName());
		assertEquals(DEFAULT_IMAGE_DPI,
				firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_XDENSITY).getNodeValue());
		assertEquals(DEFAULT_IMAGE_DPI,
				firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_YDENSITY).getNodeValue());
		assertEquals(DEFAULT_IMAGE_RES,
				firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_RESUNITS).getNodeValue());
	}

	@Test
	void testAccessWrongMetadataformatFails() throws DigitalDerivansException, IOException {
		// arrange
		Path sourcePath = TestResource.IMG_JPG_148811035_MAX_1.get();
		var metadata = new ImageMetadata(sourcePath);
		IIOMetadata newMetadata = metadata.getData();

		// act
		var result = assertThrows(IllegalArgumentException.class,
				() -> newMetadata.getAsTree(JAVAX_IMAGEIO_TIFF));

		// assert
		assertEquals("Unsupported format name: "+ JAVAX_IMAGEIO_TIFF, result.getMessage());
	}

	/**
	 * 
	 * Common grayscale TIFF ignores progressive mode
	 * in usual derivate step workflows
	 * 
	 * @throws Exception
	 */
	@Test
	void testReadMetadata_TIF_GreyscaleIgnoresProgressiveMode() throws Exception {
		Path sourcePath = TestResource.IMG_TIF_ZD1_GREY.get();

		// act
		var mdWrapper = new ImageMetadata();
		mdWrapper.enrichFrom(sourcePath);

		// assert
		assertFalse(mdWrapper.requiresProgressiveMode());
	}


	/**
	 * 
	 * Common grayscale TIFF ignores progressive mode
	 * _not_ if created from scratch
	 * 
	 * @throws Exception
	 */
	@Test
	void testReadMetadata_TIF_GreyscaleRequiresProgressiveMode() throws Exception {
		Path sourcePath = TestResource.IMG_TIF_ZD1_GREY.get();

		// act
		var mdWrapper = new ImageMetadata(sourcePath);

		// assert
		assertTrue(mdWrapper.requiresProgressiveMode());
	}

	/**
	 * 
	 * Ensure: original resolution and DPI enriched
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testImageMetadata_TIFGreyscale_to_JPG(@TempDir Path tempDir) throws Exception {
		Path sourcePath = TestResource.IMG_TIF_ZD1_GREY.get();
		Path targetDir = tempDir.resolve("IMAGE");
		Files.createDirectory(targetDir);
		Path targetPath = targetDir.resolve("1.jpg");

		// act
		boolean outcome = imageProcessor.writeJPG(sourcePath, targetPath);

		// assert
		assertTrue(outcome);
		var mdWrapper = new ImageMetadata(targetPath);
		var metadata = mdWrapper.getData();
		Node root = metadata.getAsTree(ImageMetadata.JAVAX_IMAGEIO_JPEG);
		assertNotNull(root);
		Node firstChild = root.getFirstChild();
		assertEquals(JPEG_VARIETY, firstChild.getLocalName());
		Node firstGrandchild = firstChild.getFirstChild();
		assertEquals(JFIF_ROOT_NODE, firstGrandchild.getLocalName());
		assertEquals("470", firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_XDENSITY).getNodeValue());
		assertEquals("470", firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_YDENSITY).getNodeValue());
		assertEquals(DEFAULT_IMAGE_RES,
				firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_RESUNITS).getNodeValue());
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
		var mdWrapper = new ImageMetadata(sourcePath);
		var metadata = mdWrapper.getData();

		// assert
		Node root = metadata.getAsTree(ImageMetadata.JAVAX_IMAGEIO_JPEG);
		assertNotNull(root);
		Node firstChild = root.getFirstChild();
		assertEquals(JPEG_VARIETY, firstChild.getLocalName());
		Node firstGrandchild = firstChild.getFirstChild();
		assertEquals(JFIF_ROOT_NODE, firstGrandchild.getLocalName());
		assertEquals("470", firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_XDENSITY).getNodeValue());
		assertEquals("470", firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_YDENSITY).getNodeValue());
		assertEquals(DEFAULT_IMAGE_RES,
				firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_RESUNITS).getNodeValue());
	}

	/**
	 * 
	 * Testdata: first page from "Bote für das Saalethal" 1864
	 * 
	 */
	@Test
	void testReadMetadata_TIF_RGB() throws Exception {
		Path sourcePath = TestResource.IMG_TIF_ZD1_RGB.get();

		// act
		var mdWrapper = new ImageMetadata(sourcePath);
		var metadata = mdWrapper.getData();

		// assert
		Node root = metadata.getAsTree(ImageMetadata.JAVAX_IMAGEIO_TIFF);
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
		var actualExc = assertThrows(DigitalDerivansException.class,
				() -> imageProcessor.writeJPG(sourcePath, targetPath));

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
		var mdWrapper = new ImageMetadata(targetPath);
		var metadata = mdWrapper.getData();

		// assert
		Node root = metadata.getAsTree(ImageMetadata.JAVAX_IMAGEIO_JPEG);
		assertNotNull(root);
		Node firstChild = root.getFirstChild();
		assertEquals(JPEG_VARIETY, firstChild.getLocalName());
		Node firstGrandchild = firstChild.getFirstChild();
		assertEquals(JFIF_ROOT_NODE, firstGrandchild.getLocalName());
		assertEquals(DEFAULT_IMAGE_DPI,
				firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_XDENSITY).getNodeValue());
		assertEquals(DEFAULT_IMAGE_DPI,
				firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_YDENSITY).getNodeValue());
		assertEquals(DEFAULT_IMAGE_RES,
				firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_RESUNITS).getNodeValue());
	}

	@Test
	void testReadMetadata_TIF_RGB_withInvalidAttributes() throws Exception {
		Path sourcePath = TestResource.IMG_TIF_MENA_1.get();

		// act
		var mdWrapper = new ImageMetadata(sourcePath);
		var metadata = mdWrapper.getData();

		// assert
		Node root = metadata.getAsTree(ImageMetadata.JAVAX_IMAGEIO_TIFF);
		NodeList grandChilds = root.getFirstChild().getChildNodes();
		assertEquals(22, grandChilds.getLength());
		assertEquals(TIFF_NODE, grandChilds.item(6).getNodeName());
		assertEquals(METADATA_TIFF_X_RESOLUTION, grandChilds.item(12).getAttributes().item(1).getNodeValue());
		assertEquals("300/1",
				grandChilds.item(12).getFirstChild().getFirstChild().getAttributes().item(0).getNodeValue());
		assertEquals(METADATA_TIFF_Y_RESOLUTION, grandChilds.item(13).getAttributes().item(1).getNodeValue());
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
		var mdWrapper = new ImageMetadata(imgSource);
		var metadata = mdWrapper.getData();

		// assert
		Node root = metadata.getAsTree(ImageMetadata.JAVAX_IMAGEIO_JPEG);
		NodeList grandChilds = root.getFirstChild().getChildNodes();
		Node firstChild = root.getFirstChild();
		assertEquals(JPEG_VARIETY, firstChild.getLocalName());
		Node firstGrandchild = firstChild.getFirstChild();
		assertEquals(1, grandChilds.getLength());
		assertEquals(JFIF_ROOT_NODE, firstGrandchild.getLocalName());
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
		var metadataNew = new ImageMetadata(targetPath);

		// assert
		Node root = metadataNew.getData().getAsTree(JAVAX_IMAGEIO_JPEG);
		Node firstChild = root.getFirstChild();
		assertEquals(JPEG_VARIETY, firstChild.getLocalName());
		Node firstGrandchild = firstChild.getFirstChild();
		assertEquals(JFIF_ROOT_NODE, firstGrandchild.getLocalName());
		assertEquals(DEFAULT_IMAGE_DPI,
				firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_XDENSITY).getNodeValue());
		assertEquals(DEFAULT_IMAGE_DPI,
				firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_YDENSITY).getNodeValue());
		assertEquals(DEFAULT_IMAGE_RES,
				firstGrandchild.getAttributes().getNamedItem(METADATA_JPEG_RESUNITS).getNodeValue());
		// Node root2 = newMetadata.getAsTree(ImageProcessor.JAVAX_IMAGEIO);
		// Node childThree = root2.getChildNodes().item(1);
		// assertEquals("Compression", childThree.getLocalName());
		// String actualCompr =
		// childThree.getChildNodes().item(2).getAttributes().getNamedItem("value").getNodeValue();
		// assertEquals("10", actualCompr);
		assertFalse(metadataNew.requiresProgressiveMode());
	}

	/**
	 * 
	 * Ensure: once image processed and stored,
	 * it won't need to be processed as progressive
	 * 
	 * @param tempDir
	 * @throws DigitalDerivansException
	 * @throws IOException
	 */
	@Test
	void testProgressionRequiredForSLUBImage(@TempDir Path tempDir) throws DigitalDerivansException, IOException {
		// arrange
		String img = "src/test/resources/images/FILE_0001_MAX.jpg";
		Path imgSource = Paths.get(img).toAbsolutePath();
		Path targetDir = tempDir.resolve("IMAGE-BASELINE");
		Files.createDirectory(targetDir);
		Path targetPath = targetDir.resolve("FILE_0001_MAX.jpg");
		var prevImageMetadata = new ImageMetadata(imgSource);
		assertTrue(prevImageMetadata.requiresProgressiveMode());
		
		// act
		boolean outcome = imageProcessor.writeJPG(imgSource, targetPath);

		// assert
		assertTrue(outcome);
		var metadata = new ImageMetadata(targetPath);
		assertFalse(metadata.requiresProgressiveMode());
	}

	/**
	 * 
	 * Ensure: once image has been stored as processed,
	 * it won't need to be processed as progressive
	 * 
	 * @param tempDir
	 * @throws DigitalDerivansException
	 * @throws IOException
	 */
	@Test
	void testProgressionNotRequiredForVD18(@TempDir Path tempDir) throws DigitalDerivansException, IOException {
		// arrange
		Path sourcePath = TestResource.IMG_JPG_148811035_MAX_1.get();
		Path targetDir = tempDir.resolve("148811035");
		Files.createDirectory(targetDir);
		Path targetPath = targetDir.resolve("148811035.jpg");
		var prevImageMetadata = new ImageMetadata(sourcePath);
		assertFalse(prevImageMetadata.requiresProgressiveMode());

		// act
		boolean outcome = imageProcessor.writeJPG(sourcePath, targetPath);
		assertTrue(outcome);

		// assert
		var metadata = new ImageMetadata(targetPath);
		assertFalse(metadata.requiresProgressiveMode());
	}

	@Test
	void testProcessImageFromEmptyInputFails(@TempDir Path tempDir) throws Exception {
		Path sourcePath = TestResource.IMG_JPG_ZERO.get();
		Path targetDir = tempDir.resolve("IMAGE");
		Files.createDirectory(targetDir);
		Path targetPath = targetDir.resolve("zero.jpg");

		// act
		var actual = assertThrows(DigitalDerivansException.class,
				() -> imageProcessor.writeJPG(sourcePath, targetPath));

		// assert
		assertEquals("Invalid fileSize 0 for src/test/resources/images/00000020.jpg!", actual.getMessage());
	}
}
