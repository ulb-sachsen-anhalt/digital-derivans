package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.config.DerivansParameter;
import de.ulb.digital.derivans.model.step.DerivateStepImage;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;

/**
 * 
 * Used config: src/test/resources/config/derivans.ini
 * 
 * @author hartwig
 *
 */
public class TestDerivansFulltextRahbar {

	@TempDir
	static Path tempDir;

	static Path workDir;

	static Path pdfPath;

	static String rahbar88120_p10 = "1981185920_88120_00000010";

	/**
	 * Fixture with common configuration and just a start
	 * directory
	 * 
	 * @throws Exception
	 */
	@BeforeAll
	public static void setupBeforeClass() throws Exception {
		var rahbar88120 = "1981185920_88120";
		Path configSourceDir = Path.of("src/test/resources/config");
		Path configTargetDir = tempDir.resolve("config");
		if (Files.exists(configTargetDir)) {
			Files.walk(configTargetDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(configTargetDir);
		}
		TestHelper.copyTree(configSourceDir, configTargetDir);
		workDir = tempDir.resolve(rahbar88120);
		Path pathImageMax = workDir.resolve("MAX");
		Files.createDirectories(pathImageMax);
		TestHelper.generateJpgsFromList(pathImageMax, 2289, 3173, List.of(rahbar88120_p10));
		Path sourceOcr = TestResource.SHARE_IT_RAHBAR_88120.get();
		Path targetOcrDir = workDir.resolve("FULLTEXT");
		Files.createDirectories(targetOcrDir);
		Path targetOcr = targetOcrDir.resolve(sourceOcr.getFileName());
		Files.copy(sourceOcr, targetOcr);
		DerivansParameter dp = new DerivansParameter();
		dp.setPathInput(workDir);
		dp.setPathConfig(configTargetDir.resolve("derivans.ini"));
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		int maximal = 2339; // scale A4 with 200 DPI
		((DerivateStepImage) dc.getDerivateSteps().get(1)).setMaximal(maximal);
		((DerivateStepPDF) dc.getDerivateSteps().get(2)).setRenderModus("visible");
		Derivans derivans = new Derivans(dc);

		// act
		derivans.create();
		pdfPath = workDir.resolve("1981185920_88120.pdf");
	}

	@Test
	void testDerivatesForPDFWritten() throws Exception {
		Path image80Dir = workDir.resolve("IMAGE_80");
		assertTrue(Files.exists(image80Dir));
		assertTrue(image80Dir.resolve(rahbar88120_p10 + ".jpg").toFile().exists());
	}

	@Test
	void testPDFWritten() {
		assertTrue(Files.exists(pdfPath));
	}

	/**
	 * Please note:
	 * This test is *not really* valuable with it's current
	 * TextExtractionStrategy because this hides that
	 * the chars order inside the PDF does *not* comply
	 * 
	 * @throws Exception
	 */
	@Test
	void testPage01ContentsFirstLine() throws Exception {
		var textTokens = TestHelper.getText(pdfPath, 1).split("\n");
		assertEquals("دیبا", textTokens[0]);
		assertEquals("چه", textTokens[1]);
	}

	/**
	 * 
	 * Test total length of resultant text including whitespaces
	 * 
	 * @throws Exception
	 */
	@Test
	void testPage01TextLength() throws Exception {
		var textPage07 = TestHelper.getTextAsSingleLine(pdfPath, 1);
		assertEquals(1612, textPage07.length());
	}
}
