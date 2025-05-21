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
import de.ulb.digital.derivans.config.DefaultConfiguration;
import de.ulb.digital.derivans.generate.GeneratorPDF;
import de.ulb.digital.derivans.model.DerivateFS;
import de.ulb.digital.derivans.model.DerivateStruct;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.ITextElement;
import de.ulb.digital.derivans.model.pdf.PDFTextElement;
import de.ulb.digital.derivans.model.pdf.PDFResult;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;

/**
 * 
 * MWE
 * PDF with 1 page, 1 synthetic Textlayer, no METS/MODS-metadata,
 * no outline
 * 
 * ONCE for line level
 * THEN for word level
 * 
 * @author hartwig
 *
 */
class TestPDFSyntheticWesteuropeLines {

	private static final int N_PAGES = 1;

	private static final int TEST_DPI = 144;

	static PDFResult pdfLines;

	static PDFTextElement pdfLinelvlFirstLine;

	private static int orgwidth = 575;

	private static int orgHeight = 800;

	private static int textMarginLeft = 50;

	private static int textMarginTop = 200;

	@BeforeAll
	static void initAll(@TempDir Path tempDir) throws Exception {
		Path pathImages = tempDir.resolve("MAX");
		Files.createDirectory(pathImages);
		Path jpgFile = pathImages.resolve("00001.jpg");
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

		String pdfLineName = String.format("pdf-line-%04d.pdf", N_PAGES);
		Path outputLinePath = tempDir.resolve(pdfLineName);
		DerivateStepPDF pdfStep1 = new DerivateStepPDF();
		pdfStep1.setImageDpi(TEST_DPI);
		pdfStep1.setRenderLevel(DefaultConfiguration.DEFAULT_RENDER_LEVEL);
		pdfStep1.setConformance("PDF/A-1B");
		pdfStep1.setDebugRender(false);
		pdfStep1.setPathPDF(outputLinePath);
		GeneratorPDF generatorLine = new GeneratorPDF();
		generatorLine.setDerivate(testDerivate);
		generatorLine.setStep(pdfStep1);
		generatorLine.create();
		pdfLines = generatorLine.getPDFResult();
		pdfLinelvlFirstLine = pdfLines.getPdfPages().get(0).getTextcontent().get().get(0);
	}

	@Test
	void pdfLinelevelExists() {
		assertTrue(Files.exists(pdfLines.getPath()));
	}

	@Test
	void numberOfAddedPages() {
		assertEquals(1, pdfLines.getPdfPages().size());
	}

	/**
	 * Since there was no structural metadata present,
	 * there can't be a valid outline with links
	 */
	@Test
	void ensureNoOutlinePresent() {
		assertEquals(0, pdfLines.getOutline().size());
	}

	@Test
	void inspectPageOneNumber() {
		var pageOne = pdfLines.getPdfPages().get(0);
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
		var pageOne = pdfLines.getPdfPages().get(0);
		assertEquals(orgwidth / 2, pageOne.getDimension().getWidth());
		assertEquals(orgHeight / 2, pageOne.getDimension().getHeight());
	}

	/**
	 * iText assumes 72 DPI resolution, we enforce 144 DPI
	 * => dimensions 1/2:
	 * height 400 instead of 800
	 * y 100 instead of 200
	 * => PDF Rendering starts relative to bottom
	 * y 300 instead of 100: scaled page hight - org_size * 0.5
	 * 
	 * width is 130 px instead of 260 (in OCR)
	 * height is 15 px instead of 30 (in OCR)
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
		assertEquals(11.25, ((PDFTextElement) pdfLinelvlFirstLine).getFontSize());
	}

	@Test
	void inspectTextualContent() {
		assertEquals("BELLA CHIAO (DELLE MODINE)", ((ITextElement) pdfLinelvlFirstLine).getText());
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

}
