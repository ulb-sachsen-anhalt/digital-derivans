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
import de.ulb.digital.derivans.data.DerivansPathResolver;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.pdf.PDFDocument;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;
import de.ulb.digital.derivans.model.step.DerivateType;

/**
 * 
 * introspect rahbar print
 * 
 * @author hartwig
 *
 */
public class TestPDFRahbarOCRWords {

	static PDFDocument resultDoc;

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
		step.setRenderLevel("word");
		PDFDerivateer handler = new PDFDerivateer(input, output, pages, step);
	
		// act
		handler.create();
		resultDoc = handler.getPDFDocument();
	}

	@Test
	void testPDFWordLevelWritten() {
		assertTrue(Files.exists(pdfPathWord));
	}

	/**
	 * Please note:
	 * This test is *not really* valuable with current
	 * TextExtractionStrategy because it shadows the
	 * fact that the characters order is okay
	 * therefore "ﻪﭼ" is actually "چه"
	 * ﻪﭼ
	 * 
	 */
	@Test
	void testWordLevelPage01Contents() {
		var lineword01 = resultDoc.getPdfPages().get(0).getlines().get(0).getText();
		assertEquals("دیبا", lineword01);
		var lineword02 = resultDoc.getPdfPages().get(0).getlines().get(1).getText();
		assertEquals("چه", lineword02);
	}

	/**
	 * 
	 * Test total length of resultant text including whitespaces
	 * 
	 * Please note:
	 * test ref value changed from 1578 to 1616 due
	 * refactoring of rendering for right-to-left fonts
	 * 
	 * @throws Exception
	 */
	@Test
	void testWordLevelPage01TextLength() throws Exception {
		var textPage07 = TestHelper.getTextAsSingleLine(pdfPathWord, 1);
		assertEquals(1616, textPage07.length());
	}

}
