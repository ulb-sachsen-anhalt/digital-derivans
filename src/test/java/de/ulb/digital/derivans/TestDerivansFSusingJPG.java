package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.config.DerivansParameter;
import de.ulb.digital.derivans.generate.Generator;
import de.ulb.digital.derivans.model.pdf.PDFOutlineEntry;

/**
 * 
 * @author hartwig
 *
 */
class TestDerivansFS {

	/**
	 * 
	 * Start images set flat in start directory
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testDerivatesResolveFlat(@TempDir Path tempDir) throws Exception {

		// arrange
		Path pathTarget = tempDir.resolve("only_images");
		TestHelper.generateImages(pathTarget, 300, 420, 4, "%04d.jpg");

		// act
		DerivansParameter dp = new DerivansParameter();
		dp.setImages(".");
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);
		List<Generator> des = derivans.init(pathTarget);
		assertEquals(2, des.size());
		derivans.forward();

		// assert
		Path pdfWritten = pathTarget.resolve("only_images.pdf");
		assertTrue(Files.exists(pdfWritten));
	}

	/**
	 * 
	 * Start images one level below actual start directory
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testDerivatesResolveMAXPath(@TempDir Path tempDir) throws Exception {

		// arrange
		Path pathTarget = tempDir.resolve("only_images");
		Path pathImageMax = pathTarget.resolve("MAX");
		Files.createDirectories(pathImageMax);
		TestHelper.generateImages(pathImageMax, 300, 420, 4, "%04d.jpg");

		// act
		DerivansParameter dp = new DerivansParameter();
		dp.setPathInput(pathTarget);
		dp.setImages("MAX");
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);
		derivans.init(pathTarget);
		derivans.forward();

		// assert
		Path pdfWritten = pathTarget.resolve("only_images.pdf");
		assertTrue(Files.exists(pdfWritten));

		PDFOutlineEntry outline = new TestHelper.PDFInspector(pdfWritten).getOutline();
		assertNotNull(outline);
		assertEquals("only_images", outline.getLabel());
		assertEquals(0, outline.getOutlineEntries().size());
	}

	/**
	 * 
	 * Start images one level below actual start directory
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testDerivatesResolveDEFAULTPath(@TempDir Path tempDir) throws Exception {

		// arrange
		Path pathTarget = tempDir.resolve("only_images");
		Path pathImageMax = pathTarget.resolve("DEFAULT");
		Files.createDirectories(pathImageMax);
		TestHelper.generateImages(pathImageMax, 300, 420, 4, "%04d.jpg");

		// act
		DerivansParameter dp = new DerivansParameter();
		dp.setPathInput(pathTarget);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);
		derivans.init(pathTarget);
		derivans.forward();

		// assert
		Path pdfWritten = pathTarget.resolve("only_images.pdf");
		assertTrue(Files.exists(pdfWritten));
	}

}
