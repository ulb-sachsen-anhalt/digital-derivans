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
import de.ulb.digital.derivans.config.TypeConfiguration;
import de.ulb.digital.derivans.model.step.DerivateStepImage;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;

/**
 * 
 * Used config: src/test/resources/config/derivans.ini
 * 
 * @author hartwig
 *
 */
class TestDerivansFulltextRahbar {

	@TempDir
	static Path tempDir;

	static Path workDirWord;

	static Path pdfPathWord;

	static Path pdfLines;

	static String rahbar88120Page10 = "1981185920_88120_00000010";

	/**
	 * Fixture with common configuration and just a start
	 * directory
	 * 
	 * @throws Exception
	 */
	@BeforeAll
	static void setupBeforeClass() throws Exception {
		Path configTargetDir = tempDir.resolve("config");
		if (Files.exists(configTargetDir)) {
			Files.walk(configTargetDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(configTargetDir);
		}
		TestHelper.copyTree(TestResource.CONFIG_RES_DIR.get(), configTargetDir);

		// arange 01
		String line = "1981185920_88120_line";
		var workDirLine = setWorkdir(tempDir, line);
		var dConfLine = configure(configTargetDir, workDirLine, TypeConfiguration.RENDER_LEVEL_LINE);
		Derivans derivansLines = new Derivans(dConfLine);
		derivansLines.init(workDirLine);
		pdfLines = workDirLine.resolve(line + ".pdf");
		derivansLines.forward();

		// arrange 02
		String word = "1981185920_88120_word";
		workDirWord = setWorkdir(tempDir, word);
		var dConf = configure(configTargetDir, workDirWord, TypeConfiguration.RENDER_LEVEL_WORD);
		Derivans derivansWord = new Derivans(dConf);
		pdfPathWord = workDirWord.resolve(word + ".pdf");
		derivansWord.init(workDirWord);
		derivansWord.forward();
	}

	static Path setWorkdir(Path root, String subDir) throws Exception {
		Path workDir = root.resolve(subDir);
		Path pathImageMax = workDir.resolve("MAX");
		Files.createDirectories(pathImageMax);
		Path sourceOcr = TestResource.SHARE_IT_RAHBAR_88120_LEGACY.get();
		assertTrue(Files.exists(sourceOcr));
		Path targetOcrDir = workDir.resolve("FULLTEXT");
		Files.createDirectories(targetOcrDir);
		Path targetOcr = targetOcrDir.resolve(sourceOcr.getFileName());
		Files.copy(sourceOcr, targetOcr);
		Path sourceImg = Path.of("src/test/resources/images/1981185920_88120_00000010.jpg");
		assertTrue(Files.exists(sourceImg));
		Files.copy(sourceImg, pathImageMax.resolve(sourceImg.getFileName()));
		return workDir;
	}

	static DerivansConfiguration configure(Path configDir, Path workDir, TypeConfiguration renderLevel) throws Exception {
		DerivansParameter dParams = new DerivansParameter();
		dParams.setPathInput(workDir);
		dParams.setPathConfig(configDir.resolve("derivans_ulb_odem.ini"));
		dParams.setDebugPdfRender(true);
		DerivansConfiguration dConf = new DerivansConfiguration(dParams);
		int maximal = 2339; // scale A4 with 200 DPI
		((DerivateStepImage) dConf.getDerivateSteps().get(1)).setMaximal(maximal);
		((DerivateStepPDF) dConf.getDerivateSteps().get(2)).setRenderModus(TypeConfiguration.RENDER_MODUS_DBUG);
		((DerivateStepPDF) dConf.getDerivateSteps().get(2)).setRenderLevel(renderLevel);
		((DerivateStepPDF) dConf.getDerivateSteps().get(2)).setDebugRender(true);
		return dConf;
	}

	@Test
	void testDerivatesForPDFWritten() {
		Path image80Dir = workDirWord.resolve(IDerivans.IMAGE_Q80);
		assertTrue(Files.exists(image80Dir));
		assertTrue(image80Dir.resolve(rahbar88120Page10 + ".jpg").toFile().exists());
	}

	@Test
	void testPDFWordLevelWritten() {
		assertTrue(Files.exists(pdfPathWord));
	}

	/**
	 * 
	 * Test total length of resultant text including whitespaces
	 * 
	 * already 1318, 1336
	 * 
	 * @throws Exception
	 */
	@Test
	void testWordLevelPage01TextLength() throws Exception {
		var textPage07 = TestHelper.getTextAsSingleLine(pdfPathWord, 1);
		assertEquals(1336, textPage07.length());
	}

	@Test
	void testPDFLineLevelWritten() {
		assertTrue(Files.exists(pdfLines));
	}

	/**
	 * 
	 * Test total length of resultant text including whitespaces
	 * 
	 * Please note:
	 * test result differs from word level and time: 1614, 1636
	 * 
	 * @throws Exception
	 */
	@Test
	void testLineLevelPage01TextLength() throws Exception {
		var textPage07 = TestHelper.getTextAsSingleLine(pdfLines, 1);
		assertEquals(1636, textPage07.length());
	}
}
