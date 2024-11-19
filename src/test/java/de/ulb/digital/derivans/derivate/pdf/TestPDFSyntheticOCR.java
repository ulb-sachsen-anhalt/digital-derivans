package de.ulb.digital.derivans.derivate.pdf;

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
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.ITextElement;
import de.ulb.digital.derivans.model.ocr.OCRData;
import de.ulb.digital.derivans.model.pdf.PDFTextElement;
import de.ulb.digital.derivans.model.pdf.PDFResult;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;
import de.ulb.digital.derivans.model.step.DerivateType;
import de.ulb.digital.derivans.model.text.Textline;
import de.ulb.digital.derivans.model.text.Word;

/**
 * 
 * MWE
 *  PDF with 1 page, 1 synthetic Textlayer, no METS/MODS-metadata,
 *  no outline
 * 
 *  ONCE for line level
 *  THEN for word level
 * 
 * @author hartwig
 *
 */
class TestPDFSyntheticOCR {

	private static final int N_PAGES = 1;

	private static final int TEST_DPI = 144;

	static PDFResult pdfWithLines;

	static PDFResult pdfWithWords;

	static PDFTextElement pdfLinelvlFirstLine;

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
		
		DigitalPage e = new DigitalPage(1, imageName);
		e.setOcrData(createOCR());
		List<DigitalPage> pages = new ArrayList<>();
		pages.add(e);

		// act
		String pdfLineName = String.format("pdf-linelevel-%04d.pdf", N_PAGES);
		Path outputLinePath = tempDir.resolve(pdfLineName);
		DerivansData input = new DerivansData(pathImages, DerivateType.JPG);
		DerivansData outputLine = new DerivansData(outputLinePath, DerivateType.PDF);
		DerivateStepPDF pdfStep = new DerivateStepPDF();
		pdfStep.setImageDpi(TEST_DPI);
		pdfStep.setRenderLevel(DefaultConfiguration.DEFAULT_RENDER_LEVEL);
		pdfStep.setDebugRender(true);
		
		// act once
		var handlerOne = new PDFDerivateer(input, outputLine, pages, pdfStep);
		handlerOne.create();
		pdfWithLines = handlerOne.getPDFResult();
		pdfLinelvlFirstLine = pdfWithLines.getPdfPages().get(0).getTextcontent().get().get(0);

		// act twice
		var pdfWordName = String.format("pdf-wordlevel-%04d.pdf", N_PAGES);
		var outputWord = new DerivansData(tempDir.resolve(pdfWordName), DerivateType.PDF);
		pdfStep.setRenderLevel(TypeConfiguration.RENDER_LEVEL_WORD);
		var handlerTwo = new PDFDerivateer(input, outputWord, pages, pdfStep);
		handlerTwo.create();
		pdfWithWords = handlerTwo.getPDFResult();
		pdfWordlvlFirstLine = pdfWithWords.getPdfPages().get(0).getTextcontent().get().get(0);
	}
	
	@Test
	void pdfLinelevelExists() {
		assertTrue(Files.exists(pdfWithLines.getPath()));
	}

	@Test
	void numberOfAddedPages() {
		assertEquals(1, pdfWithLines.getPdfPages().size());
	}

	/**
	 * Since there was no structural metadata present,
	 * there can't be a valid outline with links
	 */
	@Test
	void ensureNoOutlinePresent() {
		assertEquals(0, pdfWithLines.getOutline().size());
	}

	@Test
	void inspectPageOneNumber() {
		var pageOne = pdfWithLines.getPdfPages().get(0);
		assertEquals(1, pageOne.getNumber());
	}

	/**
	 * Image dimension depends on actual
	 * PDF rendering backend
	 * 
	 * iText5 assumed input 72 DPI data, even for
	 * 300 DPI scanner data
	 * 
	 * 287 vs. 575
	 * 399 vs. 799
	 */
	@Test
	void inspectPageOneDimension() {
		var pageOne = pdfWithLines.getPdfPages().get(0);
		assertEquals(orgwidth / 2, pageOne.getDimension().getWidth());
		assertEquals(orgHeight / 2, pageOne.getDimension().getHeight());
	}


	/**
	 * iText assumes 72 DPI resolution, we enforce 144 DPI
	 * => dimensions 1/2:
	 *    height 400 instead of 800
	 *    y 100 instead of 200
	 * => PDF Rendering starts relative to bottom
	 *    y 300 instead of 100: scaled page hight - org_size * 0.5
	 * 
	 * width  is 130 px instead of 260 (in OCR)
	 * height is  15 px instead of  30 (in OCR)
	 * 
	 * Changed with iText Update (11/2024)
	 * 
	 */
	@Test
	void inspectFirstLineBox() {
		assertEquals(textMarginLeft / 2, pdfLinelvlFirstLine.getBox().getX());
		assertEquals(400 - textMarginTop / 2 - 15.0, pdfLinelvlFirstLine.getBox().getY()); // substract box height too
		assertEquals(350 / 2, pdfLinelvlFirstLine.getBox().getWidth());
		assertEquals(30 / 2, pdfLinelvlFirstLine.getBox().getHeight());
	}
	
	/**
	 * 
	 * Check fontsize in ... pixels ?
	 * 
	 */
	@Test
	void inspectFirstLineFont() {
		assertEquals(11.25, ((PDFTextElement)pdfLinelvlFirstLine).getFontSize());
	}

	@Test
	void inspectTextualContent() {
		assertEquals("BELLA CHIAO (DELLE MODINE)", ((ITextElement)pdfLinelvlFirstLine).getText());
	}

	/**
	 * 
	 * Pageheight - (row descent)
	 * 
	 * 279.375 ?
	 * 
	 */
	@Test
	void inspectFirstLineBaseline() {
		assertEquals(288.75, pdfLinelvlFirstLine.getBaseline().getY1());
	}

	/**
	 * 
	 * Box bottom being the largest Y-value of this line
	 * 
	 * 315.0
	 * 
	 */
	@Test
	void inspectFirstLineBottom() {
		assertEquals(300.0, pdfLinelvlFirstLine.getBox().getMaxY());
	}

	/**
	 * Top Y-coord is smaller than bottom and baseline
	 * and actually 300.0
	 */
	@Test
	void firstLineTop() {
		var topY = pdfLinelvlFirstLine.getBox().getMinY();
		var bottomY = pdfLinelvlFirstLine.getBox().getMaxY();
		assertTrue(topY < bottomY);
		assertTrue(topY < pdfLinelvlFirstLine.getBaseline().getY1());
		assertEquals(285, topY);
	}

	@Test
	void linelevelBaseline01() {
		assertEquals(175, pdfLinelvlFirstLine.getBaseline().length());
	}

	/**
	 * First line contains 4 words
	 */
	@Test
	void lineLevelFirstLineKids() {
		assertEquals(4, pdfLinelvlFirstLine.getChildren().size());
	}


	@Test
	void pdfWordLevelExists() {
		assertTrue(Files.exists(pdfWithWords.getPath()));
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
		var yBaseline = 303.75f - (float)firstLineKids.get(0).getBox().getHeight();
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

	/**
	 * 
	 * Create artificial OCR data
	 * (Between word/token on-a-line +10 pixel gap)
	 * 
	 * @return
	 */
	static OCRData createOCR() {
		List<Word> texts1 = new ArrayList<>();
		texts1.add(new Word("BELLA", new Rectangle(textMarginLeft, textMarginTop, 70, 30)));
		texts1.add(new Word("CHIAO", new Rectangle(130, textMarginTop, 70, 30)));
		texts1.add(new Word("(DELLE", new Rectangle(210, textMarginTop, 80, 30)));
		texts1.add(new Word("MODINE)", new Rectangle(300, textMarginTop, 100, 30)));
		List<Word> texts2 = new ArrayList<>();
		texts2.add(new Word("Alla", new Rectangle(textMarginLeft, 250, 40, 30)));
		texts2.add(new Word("matina,", new Rectangle(100, 250, 75, 25)));
		texts2.add(new Word("appena", new Rectangle(185, 250, 70, 25)));
		texts2.add(new Word("alzata", new Rectangle(265, 250, 60, 25)));
		List<Word> texts3 = new ArrayList<>();
		texts3.add(new Word("o", new Rectangle(textMarginLeft, 300, 10, 25)));
		texts3.add(new Word("bella", new Rectangle(70, 300, 50, 25)));
		texts3.add(new Word("chiao,", new Rectangle(130, 300, 60, 25)));
		texts3.add(new Word("bella", new Rectangle(200, 300, 50, 25)));
		texts3.add(new Word("chiao,", new Rectangle(260, 300, 60, 25)));
		texts3.add(new Word("bella", new Rectangle(330, 300, 50, 25)));
		texts3.add(new Word("chiao", new Rectangle(390, 300, 50, 25)));
		texts3.add(new Word("chiao", new Rectangle(450, 300, 50, 25)));
		texts3.add(new Word("chiao!", new Rectangle(510, 300, 60, 25)));
		List<Textline> lines = List.of(
			new Textline(texts1),
			new Textline(texts2),
			new Textline(texts3));
		return new OCRData(lines, new Dimension(575, 799));
	}

	/**
	 * 
	 * Gather insights on scaling and (especially!) inversion of top-left vs. bottom-left
	 * 
	 */
	@Test
	void inspectScalingInversion() {
		var originalPageHeight = 800;
		var originalLineHeight = 30;

		List<Word> texts1 = new ArrayList<>();
		texts1.add(new Word("BELLA", new Rectangle(textMarginLeft, textMarginTop, 70, originalLineHeight)));
		texts1.add(new Word("CHIAO", new Rectangle(130, textMarginTop, 70, originalLineHeight)));
		texts1.add(new Word("(DELLE", new Rectangle(210, textMarginTop, 80, originalLineHeight)));
		texts1.add(new Word("MODINE)", new Rectangle(300, textMarginTop, 100, originalLineHeight)));
		List<Textline> lines = List.of(new Textline(texts1));

		OCRData ocrData = new OCRData(lines, new Dimension(575, originalPageHeight));
		var originalMax = 400;
		var originalY1 = textMarginTop;
		var scaleRatio = 0.5f;
		var boxOriginal = ocrData.getTextlines().get(0).getBox();
		assertEquals(originalMax, boxOriginal.getMaxX());
		assertEquals(originalY1, boxOriginal.getMinY());

		// act
		ocrData.scale(scaleRatio);

		// scaling only halfes
		var boxScaled = ocrData.getTextlines().get(0).getBox();
		assertEquals(originalMax * scaleRatio, boxScaled.getMaxX());
		assertEquals(originalY1 * scaleRatio, boxScaled.getMinY());
		var lineIn = ocrData.getTextlines().get(0);
		PDFTextElement textElem = new PDFTextElement(lineIn.getText(), lineIn.getBox());
		var scaledTop = textElem.getBox().getMinY();
		var scaledBtm = textElem.getBox().getMaxY();
		assertEquals(originalMax * scaleRatio, textElem.getBox().getMaxX());
		assertEquals(originalY1 * scaleRatio, scaledTop);
		assertEquals(originalY1 * scaleRatio + originalLineHeight * scaleRatio, scaledBtm);

		// re-act
		var newHeight = originalPageHeight * scaleRatio;
		textElem.invert(newHeight);
		assertEquals(originalMax * scaleRatio, textElem.getBox().getMaxX());
		var newTop = textElem.getBox().getMaxY();
		var newBtm = textElem.getBox().getMinY();
		assertEquals(315 - textElem.getBox().getHeight(), newTop);
		assertEquals(300 - textElem.getBox().getHeight(), newBtm);
		assertEquals(288.75, textElem.getBaseline().getY1());
	}
}
