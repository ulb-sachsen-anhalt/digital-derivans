package de.ulb.digital.derivans.generate.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.config.DefaultConfiguration;
import de.ulb.digital.derivans.config.TypeConfiguration;
import de.ulb.digital.derivans.generate.GeneratorPDF;
import de.ulb.digital.derivans.IDerivans;
import de.ulb.digital.derivans.model.DerivateFS;
import de.ulb.digital.derivans.model.DerivateStruct;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.ocr.OCRData;
import de.ulb.digital.derivans.model.pdf.PDFTextElement;
import de.ulb.digital.derivans.model.pdf.PDFResult;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;
import de.ulb.digital.derivans.model.text.Textline;
import de.ulb.digital.derivans.model.text.Word;

/**
 * 
 * MWE PDF with textlayer at word level, no metadata
 * 
 * @author hartwig
 *
 */
class TestPDFSyntheticArabicWords {

	private static final int N_PAGES = 1;

	private static final int TEST_DPI = 144;

	static PDFResult pdfWithWords;

	static PDFTextElement pdfWordlvlFirstLine;

	static int orgwidth = 575;

	static int orgHeight = 800;

	static int textMarginLeft = 50;

	static int textMarginTop = 200;

	@BeforeAll
	static void initAll(@TempDir Path tempDir) throws Exception {
		Path pathImages = tempDir.resolve(IDerivans.IMAGE_DIR_MAX);
		Files.createDirectory(pathImages);
		String imageName = String.format("%04d.jpg", 1);
		Path jpgFile = pathImages.resolve(imageName);
		BufferedImage bi2 = new BufferedImage(orgwidth, orgHeight, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g2d = bi2.createGraphics();
		g2d.setColor(Color.ORANGE);
		g2d.fillRect(0, 0, orgwidth, orgHeight);
		ImageIO.write(bi2, "JPG", jpgFile.toFile());

		DigitalPage dp = new DigitalPage("MAX_0001", 1, jpgFile);
		dp.setOcrData(arabicOCR());
		List<DigitalPage> pages = new ArrayList<>();
		pages.add(dp);
		DerivateStruct struct = new DerivateStruct(1, "00001");
		struct.getPages().add(dp);
		DerivateFS dummyDerivate = new DerivateFS(tempDir);
		dummyDerivate.setStructure(struct);

		// act
		String pdfLineName = String.format("pdf-linelevel-%04d.pdf", N_PAGES);
		Path outputLinePath = tempDir.resolve(pdfLineName);
		DerivateStepPDF pdfStep = new DerivateStepPDF(IDerivans.IMAGE_DIR_MAX, ".");
		pdfStep.setImageDpi(TEST_DPI);
		pdfStep.setRenderLevel(DefaultConfiguration.DEFAULT_RENDER_LEVEL);
		pdfStep.setDebugRender(true);
		pdfStep.setPathPDF(outputLinePath);
		pdfStep.setRenderLevel(TypeConfiguration.RENDER_LEVEL_WORD);
		var genPDF = new GeneratorPDF();
		genPDF.setDerivate(dummyDerivate);
		genPDF.setStep(pdfStep);
		genPDF.create();
		pdfWithWords = genPDF.getPDFResult();
		pdfWordlvlFirstLine = pdfWithWords.getPdfPages().get(0).getTextcontent().get().get(0);
	}

	@Test
	void pdfWordLevelExists() {
		assertTrue(Files.exists(pdfWithWords.getPath()));
	}

	/**
	 * 
	 * Baseline Word 01 (200.0 288.75) => (208.0 288.75) "٨"
	 * 
	 */
	@Test
	void wordLevelFirstlineBaselineWord01() {
		var firstLineKids = pdfWordlvlFirstLine.getChildren();
		assertEquals(3, firstLineKids.size());
		var baselineW1 = firstLineKids.get(0).getBaseline();
		var yBaseline = 303.75f - (float) firstLineKids.get(0).getBox().getHeight();
		var expectedBaselineW1 = new PDFTextElement.Baseline(200.0f, yBaseline, 208.0f, yBaseline);
		assertEquals(expectedBaselineW1, baselineW1);
		assertEquals(8.0, baselineW1.length());
	}

	/**
	 * 
	 * Assuming char width = 1/2 height
	 * 
	 * @return
	 */
	static OCRData arabicOCR() {
		var w1 = new Word("٨", new Rectangle(400, textMarginTop, 15, 30)); // ٨
		assertEquals(1, w1.getText().length());
		var w2 = new Word("ديبا", new Rectangle(200, textMarginTop, 60, 30));
		assertEquals(4, w2.getText().length());
		var w3 = new Word("جه", new Rectangle(160, textMarginTop, 30, 30));
		assertEquals(2, w3.getText().length());
		//
		var w4 = new Word("\u0627\u0644\u0633\u0639\u0631",
				new Rectangle(400, textMarginTop + 40, 75, 30)); // السعر
		assertEquals(5, w4.getText().length());
		var w5 = new Word("\u0627\u0644\u0627\u062c\u0645\u0627\u0644\u064a",
				new Rectangle(250, textMarginTop + 40, 120, 30)); // الاجمالي
		assertEquals(8, w5.getText().length());
		List<Textline> lines = List.of(
				new Textline(List.of(w1, w2, w3)),
				new Textline(List.of(w4, w5)));
		return new OCRData(lines, new Dimension(600, 800));
	}

}
