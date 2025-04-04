package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.config.DerivansParameter;
import de.ulb.digital.derivans.model.IPDFProcessor;

/**
 * 
 * Please note:
 * In real life we usually scale large pages to 3308px.
 * This dropped to 1170px to have faster test execution
 * but still renders most text chars.
 * Original TIFF-scan was like 7544x10536 px, this
 * was also dropped due faster execution.
 * 
 * Used config: src/test/resources/config3/derivans-tif.ini
 * 
 * 
 * @author hartwig
 *
 */
public class TestDerivansNewspaper {

	@TempDir
	static Path tempDir;

	static Path workDir;

	static String fileLabel = "1667524704_J_0150_0512";

	@BeforeAll
	public static void setupBeforeClass() throws Exception {
		workDir = tempDir.resolve("zd1");

		// push ocr data
		Path sourceOcr = Path.of("src/test/resources/alto/1667524704_J_0150/" + fileLabel + ".xml");
		assertTrue(Files.exists(sourceOcr, LinkOption.NOFOLLOW_LINKS));
		Path sourceFile = sourceOcr.getFileName();
		Path targetDir = workDir.resolve("FULLTEXT");
		Files.createDirectories(targetDir);
		Path targetOcr = targetDir.resolve(sourceFile);
		Files.copy(sourceOcr, targetOcr);

		// create image
		Path pathImageMax = workDir.resolve("TIF");
		Files.createDirectory(pathImageMax);
		Path imagePath = pathImageMax.resolve(fileLabel + ".tif");
		TestHelper.writeImage(imagePath, 1855, 2634, BufferedImage.TYPE_BYTE_GRAY, "TIFF");

		// trigger derivans
		DerivansParameter dp = new DerivansParameter();
		dp.setPathInput(workDir);
		Path configPath = Path.of("src/test/resources/config3/derivans-tif.ini");
		dp.setPathConfig(configPath);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);
		// derivans.create();
	}

	@Test
	void testDerivateJPGFooterWritten() throws Exception {
		Path footerImage = workDir.resolve("IMAGE_FOOTER").resolve(fileLabel + ".jpg");
		assertTrue(Files.exists(footerImage));
	}

	@Test
	void testDerivateQuality80Written() throws Exception {
		Path footerImage = workDir.resolve("IMAGE_80").resolve(fileLabel + ".jpg");
		assertTrue(Files.exists(footerImage));
	}

	@Test
	void testDerivateQuality80Scaled() throws Exception {
		Path footerImage = workDir.resolve("IMAGE_80").resolve(fileLabel + ".jpg");
		BufferedImage bi = ImageIO.read(footerImage.toFile());
		assertEquals(1170, bi.getHeight());
	}

	@Test
	void testPDFWritten() throws Exception {
		Path pdfWritten = workDir.resolve("zd1.pdf");
		assertTrue(Files.exists(pdfWritten));
	}

	/**
	 * 
	 * Ensure there are lots of characters in the textlayer
	 * Total would be 25.630, but due downscaling some
	 * chars dropped below render threshold {@link IPDFProcessor#MIN_CHAR_SIZE})
	 * 
	 * iText5    25.525 
	 * iText8    25.476
	 * PDFBox 3x  5.964
	 * 
	 * @throws Exception
	 */
	@Test
	void testPDFContainsText() throws Exception {
		Path pdfWritten = workDir.resolve("zd1.pdf");
		assertEquals(25476, TestHelper.getTextAsSingleLine(pdfWritten, 1).length());
	}

}
