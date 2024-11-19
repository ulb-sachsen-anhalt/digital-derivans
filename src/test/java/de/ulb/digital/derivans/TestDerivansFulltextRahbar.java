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
public class TestDerivansFulltextRahbar {

	@TempDir
	static Path tempDir;

	static Path workDirWord;

	static Path pdfPathWord;

	static Path pdfPathLine;

	static String rahbar88120Page10 = "1981185920_88120_00000010";

	/**
	 * Fixture with common configuration and just a start
	 * directory
	 * 
	 * @throws Exception
	 */
	@BeforeAll
	public static void setupBeforeClass() throws Exception {
		Path configSourceDir = Path.of("src/test/resources/config");
		Path configTargetDir = tempDir.resolve("config");
		if (Files.exists(configTargetDir)) {
			Files.walk(configTargetDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(configTargetDir);
		}
		TestHelper.copyTree(configSourceDir, configTargetDir);

		// arange 01
		String line = "1981185920_88120_line";
		var workDirLine = setWorkdir(tempDir, line);
		var dConfLine = configure(configTargetDir, workDirLine, TypeConfiguration.RENDER_LEVEL_LINE);
		Derivans derivansLineLevel = new Derivans(dConfLine);
		pdfPathLine = workDirLine.resolve(line + ".pdf");
		derivansLineLevel.create();

		// arrange 02
		String word = "1981185920_88120_word";
		workDirWord = setWorkdir(tempDir, word);
		var dConf = configure(configTargetDir, workDirWord, TypeConfiguration.RENDER_LEVEL_WORD);
		Derivans derivansWordLevel = new Derivans(dConf);
		pdfPathWord = workDirWord.resolve(word + ".pdf");
		derivansWordLevel.create();
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
		dParams.setPathConfig(configDir.resolve("derivans.ini"));
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
		Path image80Dir = workDirWord.resolve("IMAGE_80");
		assertTrue(Files.exists(image80Dir));
		assertTrue(image80Dir.resolve(rahbar88120Page10 + ".jpg").toFile().exists());
	}

	@Test
	void testPDFWordLevelWritten() {
		assertTrue(Files.exists(pdfPathWord));
	}

	/**
	 * Please note:
	 * This test is *not really* valuable with current
	 * TextExtractionStrategy because it shadows the
	 * fact that the character order is okay
	 * therefore "ﻪﭼ" is actually "چه"
	 * 
	 * 2024/11
	 * Due PDFBox to read now starts with "‮‭چه"
	 * 
	 * @throws Exception
	 */
	@Test
	void testWordLevelPage01Contents() throws Exception {
		var textTokens = TestHelper.getText(pdfPathWord, 1);
		assertTrue(textTokens.startsWith("‮‭چه"));
	}

	/**
	 * 
	 * Test total length of resultant text including whitespaces
	 * 
	 * Please note:
	 * test ref value changed from 1578 to 1616 due
	 * refactoring of rendering for right-to-left fonts
	 * 
	 * changed 24/11 iText8
	 * 1616 => 1315
	 * 
	 * @throws Exception
	 */
	@Test
	void testWordLevelPage01TextLength() throws Exception {
		var textPage07 = TestHelper.getTextAsSingleLine(pdfPathWord, 1);
		assertEquals(1315, textPage07.length());
	}

	@Test
	void testPDFLineLevelWritten() {
		assertTrue(Files.exists(pdfPathLine));
	}

	/**
	 * 
	 * Test total length of resultant text including whitespaces
	 * 
	 * Please note:
	 * test result differs slightly from word level
	 * 
	 * Changed 24/11 itext8
	 * 1610 => 1419
	 * 
	 * @throws Exception
	 */
	@Test
	void testLineLevelPage01TextLength() throws Exception {
		var textPage07 = TestHelper.getTextAsSingleLine(pdfPathLine, 1);
		assertEquals(1419, textPage07.length());
	}
}
