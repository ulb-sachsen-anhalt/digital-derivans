package de.ulb.digital.derivans.derivate.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.config.TypeConfiguration;
import de.ulb.digital.derivans.data.io.DerivansPathResolver;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.pdf.PDFResult;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;
import de.ulb.digital.derivans.model.step.DerivateType;

/**
 * 
 * introspect rahbar farsi PDF result
 * 
 * @author hartwig
 *
 */
public class TestPDFRahbarOCRWords {

	static PDFResult wordLvlResult;

	@TempDir
	static Path tempDir;

	static Path pdfPathWord;

	static String rahbar88120Page10 = "1981185920_88120_00000010";

	@BeforeAll
	public static void setupBeforeClass() throws Exception {
		// arrange
		String word = "1981185920_88120_word";
		Path workDirWord = tempDir.resolve(word);
		pdfPathWord = workDirWord.resolve(word + ".pdf");
		Path pathImageMax = workDirWord.resolve("MAX");
		Files.createDirectories(pathImageMax);
		Path sourceOcr = TestResource.SHARE_IT_RAHBAR_88120.get();
		Path targetOcrDir = workDirWord.resolve("FULLTEXT");
		Files.createDirectories(targetOcrDir);
		Path targetOcr = targetOcrDir.resolve(sourceOcr.getFileName());
		Files.copy(sourceOcr, targetOcr);
		Path sourceImg = Path.of("src/test/resources/images/1981185920_88120_00000010.jpg");
		assertTrue(Files.exists(sourceImg));
		Files.copy(sourceImg, pathImageMax.resolve(sourceImg.getFileName()));

		// arrange base derivateer
		DerivansData input = new DerivansData(pathImageMax, DerivateType.JPG);
		DerivansData output = new DerivansData(workDirWord, DerivateType.PDF);

		// arrange pdf path and pages
		DerivansPathResolver resolver = new DerivansPathResolver(workDirWord);
		DerivateStepPDF step = new DerivateStepPDF();
		step.setOutputPath(workDirWord);
		step.setInputPath(pathImageMax);
		List<DigitalPage> pages = resolver.resolveFromStep(step);
		resolver.enrichOCRFromFilesystem(pages, targetOcrDir);
		step.setRenderLevel(TypeConfiguration.RENDER_LEVEL_WORD);
		step.setDebugRender(true);
		step.setImageDpi(300); // prevent re-scaling for testing
		PDFDerivateer wordLvlDerivateer = new PDFDerivateer(input, output, pages, step);

		// act
		wordLvlDerivateer.create();
		wordLvlResult = wordLvlDerivateer.getPDFResult();
	}

	@Test
	void testPDFWordLevelWritten() {
		assertTrue(Files.exists(pdfPathWord));
		assertTrue(Files.exists(wordLvlResult.getPath()));
	}

	/**
	 * 
	 * Original OCR
	 * line x:1017, y:342, w: 697, h:51
	 * word0001 x: 1061, y: 342, w: 60, h: 48, content: "دیبا"
	 * word0002 x: 1019, y: 350, w: 30, h: 40, content: "چه"
	 * image: 3173 x 2289 300 DPI =>
	 */
	@Test
	void testWordLevelPage01Line01ElementCoords() {
		var optLine01 = wordLvlResult.getPdfPages().get(0).getTextcontent();
		assertTrue(optLine01.isPresent());
		var line01 = optLine01.get().get(0);
		var baseline01 = line01.getBaseline();
		assertEquals(244.0, baseline01.getX1());
		assertEquals(410.0, baseline01.getX2());
		assertEquals(667.5, baseline01.getY1());
		assertEquals(678, line01.getBox().getMaxY());
		assertEquals(baseline01.getY1(), baseline01.getY2());
		assertEquals(166, baseline01.length());

		var word01 = line01.getChildren().get(0);
		var word02 = line01.getChildren().get(1);
		assertEquals(word01.getBox().getMaxY(), word02.getBox().getMaxY()); // same bottom line
	}

	/**
	 * 
	 * Test total length of resultant text including whitespaces
	 * 
	 * @throws Exception
	 */
	@Test
	void testWordLevelPage01TextLength() throws Exception {
		var textPage07 = TestHelper.getTextAsSingleLine(pdfPathWord, 1);
		assertEquals(1485, textPage07.length());
	}

}
