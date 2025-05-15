package de.ulb.digital.derivans.generate.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.config.TypeConfiguration;
import de.ulb.digital.derivans.generate.GeneratorPDF;
import de.ulb.digital.derivans.model.DerivateFS;
import de.ulb.digital.derivans.model.DerivateStruct;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.pdf.PDFTextElement;
import de.ulb.digital.derivans.model.pdf.PDFResult;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;

/**
 * 
 * MWE to generate PDF with OCR Layer at word level without outline
 * 
 * @author hartwig
 *
 */
class TestPDFSyntheticWesteuropeWords {

	private static final int N_PAGES = 1;

	private static final int TEST_DPI = 144;

	static PDFResult pdfWords;

	static PDFTextElement pdfWordlvlFirstLine;

	static int orgwidth = 575;

	static int orgHeight = 800;

	static int textMarginLeft = 50;

	static int textMarginTop = 200;

	@BeforeAll
	static void initAll(@TempDir Path tempDir) throws Exception {
		Path pathImages = tempDir.resolve("MAX");
		Files.createDirectory(pathImages);
		String imageName = String.format("%04d.jpg", 1);
		Path jpgFile = pathImages.resolve(imageName);
		BufferedImage bi2 = new BufferedImage(orgwidth, orgHeight, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g2d = bi2.createGraphics();
		g2d.setColor(Color.ORANGE);
		g2d.fillRect(0, 0, orgwidth, orgHeight);
		ImageIO.write(bi2, "JPG", jpgFile.toFile());
		DigitalPage digiPage = new DigitalPage("MAX_0001", 1, jpgFile);
		digiPage.setOcrData(TestHelper.italianOCR());
		List<DigitalPage> pages = new ArrayList<>();
		pages.add(digiPage);
		DerivateStruct testStruct = new DerivateStruct(1, "0001");
		testStruct.getPages().add(digiPage);
		DerivateFS testDerivate = new DerivateFS(tempDir);
		testDerivate.setStructure(testStruct);

		// act
		DerivateStepPDF pdfStep1 = new DerivateStepPDF();
		pdfStep1.setImageDpi(TEST_DPI);
		pdfStep1.setDebugRender(true);
		var pdfWordName = String.format("pdf-word-%04d.pdf", N_PAGES);
		Path outputWordPath = tempDir.resolve(pdfWordName);
		pdfStep1.setRenderLevel(TypeConfiguration.RENDER_LEVEL_WORD);
		pdfStep1.setPathPDF(outputWordPath);
		var generatorWords = new GeneratorPDF();
		generatorWords.setDerivate(testDerivate);
		generatorWords.setStep(pdfStep1);
		generatorWords.create();
		pdfWords = generatorWords.getPDFResult();
		pdfWordlvlFirstLine = pdfWords.getPdfPages().get(0).getTextcontent().get().get(0);
	}

	@Test
	void pdfWordLevelExists() {
		assertTrue(Files.exists(pdfWords.getPath()));
	}

	/**
	 * 
	 * Baseline Word 01 (25.0 303.75) => (50.0 303.75)
	 * 
	 */
	@Test
	void wordLevelFirstlineBaseline() {
		var line01Baseline = pdfWordlvlFirstLine.getBaseline();
		assertEquals(175.0, line01Baseline.length());
	}

	/**
	 * 
	 * Baseline Word 01 (25.0 303.75) => (50.0 303.75)
	 * 
	 */
	@Test
	void wordLevelFirstlineBaselineWord01() {
		var firstLineKids = pdfWordlvlFirstLine.getChildren();
		assertEquals(4, firstLineKids.size());
		var baselineW1 = firstLineKids.get(0).getBaseline();
		var yBaseline = 303.75f - (float) firstLineKids.get(0).getBox().getHeight();
		var expectedBaselineW1 = new PDFTextElement.Baseline(25.0f, yBaseline, 60.0f, yBaseline);
		assertEquals(expectedBaselineW1, baselineW1);
		assertEquals(35.0, baselineW1.length());
	}

	/**
	 * 
	 * Last word line 01 length 50 (because 100 * dpiScale)
	 * 
	 */
	@Test
	void wordLevelFirstlineBaselineLastWord() {
		var firstLineKids = pdfWordlvlFirstLine.getChildren();
		var lastLine = firstLineKids.get(3).getBaseline();
		assertEquals(50.0, lastLine.length());
	}

}
