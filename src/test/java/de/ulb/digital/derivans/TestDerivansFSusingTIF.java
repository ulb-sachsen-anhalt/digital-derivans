package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.config.DerivansParameter;
import de.ulb.digital.derivans.generate.Generator;
import de.ulb.digital.derivans.model.pdf.PDFOutlineEntry;
import de.ulb.digital.derivans.model.step.DerivateStep;

/**
 * 
 * @author hartwig
 *
 */
class TestDerivansFSusingTIF {

	@TempDir
	static Path tempDir;

	static List<Generator> generators;

	static List<DerivateStep> steps;

	static Path workDir;

	static Path pdfWritten;

	@BeforeAll
	static void setupBeforeClass() throws Exception {
		Path configTargetDir = tempDir.resolve("config");
		TestHelper.copyTree(TestResource.CONFIG_RES_DIR.get(), configTargetDir);
		Path trgConfig = configTargetDir.resolve("derivans_ulb_export.ini");
		DerivansParameter dp = new DerivansParameter();
		dp.setPathConfig(trgConfig);
		Path pathTarget = tempDir.resolve("only_images");
		Path pathImageMax = pathTarget.resolve("MAX");
		Files.createDirectories(pathImageMax);
		TestHelper.generateImages(pathImageMax, 150, 210, 2, "%04d.tif");
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);
		derivans.init(pathTarget);
		generators = derivans.getGenerators();
		derivans.forward();
		steps = derivans.getSteps();
		pdfWritten = pathTarget.resolve("only_images.pdf");
	}

	@Test
	void testNumberOfParsedDerivansSteps() {
		assertEquals(5, steps.size());
	}

	@Test
	void checkGeneratorClazzes() {
		assertEquals(5, generators.size());
		assertEquals("GeneratorImageJPGFooter", generators.get(0).getClass().getSimpleName());
		assertEquals("GeneratorImageJPG", generators.get(1).getClass().getSimpleName());
		assertEquals("GeneratorPDF", generators.get(2).getClass().getSimpleName());
		assertEquals("GeneratorImageJPG", generators.get(3).getClass().getSimpleName());
		assertEquals("GeneratorImageJPG", generators.get(4).getClass().getSimpleName());
	}

	@Test
	void ensurePDFWritten() {
		assertTrue(Files.exists(pdfWritten));
	}

	@Test
	void checkPDFOutline() throws Exception {
		PDFOutlineEntry outline = new TestHelper.PDFInspector(pdfWritten).getOutline();
		assertNotNull(outline);
		assertEquals("only_images", outline.getLabel());
		assertEquals(0, outline.getOutlineEntries().size());
	}

	/**
	 *
	 * Fail currently due inconsistent File handling
	 *
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testDerivatesFailsUsingTIFF(@TempDir Path tempDir) throws Exception {

		// arrange
		Path pathTarget = tempDir.resolve("only_images");
		Path pathImageMax = pathTarget.resolve("DEFAULT");
		Files.createDirectories(pathImageMax);
		TestHelper.generateImages(pathImageMax, 300, 420, 4, "%04d.tif");

		// act
		DerivansParameter dp = new DerivansParameter();
		dp.setPathInput(pathTarget);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);
		derivans.init(pathTarget);
		assertThrows(DigitalDerivansException.class, derivans::forward);
	}

}
