package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.config.DerivansParameter;
import de.ulb.digital.derivans.model.step.DerivateStepImage;

/**
 * 
 * Used config: src/test/resources/config/derivans.ini
 * 
 * @author hartwig
 *
 */
public class TestDerivansFulltextODEM {

	@TempDir
	static Path tempDir;

	static Path workDir;

	static Path pdfPath;

	static int nImages = 17;

	@BeforeAll
	public static void setupBeforeClass() throws Exception {
		// arrange migration configuration with extended derivates
		Path configSourceDir = Path.of("src/test/resources/config");
		Path configTargetDir = tempDir.resolve("config");
		if (Files.exists(configTargetDir)) {
			Files.walk(configTargetDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(configTargetDir);
		}
		TestHelper.copyTree(configSourceDir, configTargetDir);
		workDir = tempDir.resolve("148811035");

		// use existing images
		Path pathImageMax = workDir.resolve("MAX");
		Files.createDirectories(pathImageMax);
		Path sourceImageDir = Path.of("src/test/resources/alto/148811035/MAX");
		TestHelper.copyTree(sourceImageDir, pathImageMax);

		// create artificial testimages
		// List<String> ids = IntStream.range(1, 17).mapToObj(i -> String.format("%08d",
		// i)).collect(Collectors.toList());
		// generateJpgsFromList(pathImageMax, 2164, 2448, ids);

		Path sourceMets = Path.of("src/test/resources/alto/148811035/mets.xml");
		Path targetMets = workDir.resolve(Path.of("mets.xml"));
		Files.copy(sourceMets, targetMets);
		Path sourceOcr = Path.of("src/test/resources/alto/148811035/FULLTEXT");
		Path targetOcr = workDir.resolve("FULLTEXT");
		TestHelper.copyTree(sourceOcr, targetOcr);

		DerivansParameter dp = new DerivansParameter();
		dp.setPathInput(targetMets);
		dp.setPathConfig(configTargetDir.resolve("derivans.ini"));
		DerivansConfiguration dc = new DerivansConfiguration(dp);

		// apply some scaling, too
		int maximal = 2339; // A4 200 DPI ok
		// int maximal = 1754; // A4 150 DPI tw, print vanishes over top up to "Sero
		// ..."
		// int maximal = 1170; // A4 100 DPI ok with smaller text
		((DerivateStepImage) dc.getDerivateSteps().get(1)).setMaximal(maximal);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.create();
		pdfPath = workDir.resolve("148811035.pdf");
	}

	@Test
	void testDerivateJPGsWithFooterWritten() throws Exception {
		Path footerDir = workDir.resolve("IMAGE_FOOTER");
		assertTrue(Files.exists(footerDir));
		for (int i = 1; i < nImages; i++) {
			var imageLabel = String.format("%08d.jpg", i);
			assertTrue(footerDir.resolve(imageLabel).toFile().exists());
		}
	}

	@Test
	void testDerivatesForPDFWritten() throws Exception {
		Path image80Dir = workDir.resolve("IMAGE_80");
		assertTrue(Files.exists(image80Dir));
		for (int i = 1; i < nImages; i++) {
			var imageLabel = String.format("%08d.jpg", i);
			assertTrue(image80Dir.resolve(imageLabel).toFile().exists());
		}
	}

	@Test
	void testPDFWritten() {
		assertTrue(Files.exists(pdfPath));
	}

	@Test
	void testPage01NoContents() throws Exception {
		assertEquals("", TestHelper.getTextAsSingleLine(pdfPath, 1));
	}

	@Test
	void testPage07ContainsText() throws Exception {
		var textPage07 = TestHelper.getTextAsSingleLine(pdfPath, 7);
		assertTrue(textPage07.contains("an den Grantzen des Hertzogthums Florentz"));
	}

	/**
	 * 
	 * Test total length of resultant text including whitespaces
	 * 
	 * @throws Exception
	 */
	@Test
	void testPage07HasCertainLength() throws Exception {
		var textPage07 = TestHelper.getTextAsSingleLine(pdfPath, 7);
		assertEquals(1328, textPage07.length());
	}
}
