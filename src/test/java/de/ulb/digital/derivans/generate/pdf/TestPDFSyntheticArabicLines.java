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

import de.ulb.digital.derivans.config.DefaultConfiguration;
import de.ulb.digital.derivans.generate.GeneratorPDF;
import de.ulb.digital.derivans.IDerivans;
import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.model.DerivateFS;
import de.ulb.digital.derivans.model.DerivateStruct;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.pdf.PDFTextElement;
import de.ulb.digital.derivans.model.pdf.PDFResult;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;

/**
 * 
 * MWE PDF on line level, no metadata
 * 
 * @author hartwig
 *
 */
class TestPDFSyntheticArabicLines {

	private static final int N_PAGES = 1;

	private static final int TEST_DPI = 144;

	static PDFResult pdfWithLines;

	static PDFTextElement pdfLinelvlFirstLine;

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
		dp.setOcrData(TestHelper.arabicOCR());
		List<DigitalPage> pages = new ArrayList<>();
		pages.add(dp);
		DerivateStruct struct = new DerivateStruct(1, "00001");
		struct.getPages().add(dp);
		DerivateFS theDerivate = new DerivateFS(tempDir);
		theDerivate.setStructure(struct);

		// act
		String pdfLineName = String.format("pdf-linelevel-%04d.pdf", N_PAGES);
		Path outputLinePath = tempDir.resolve(pdfLineName);
		DerivateStepPDF pdfStep = new DerivateStepPDF();
		pdfStep.setImageDpi(TEST_DPI);
		pdfStep.setRenderLevel(DefaultConfiguration.DEFAULT_RENDER_LEVEL);
		pdfStep.setDebugRender(true);
		pdfStep.setPathPDF(outputLinePath);

		// act once
		var handlerOne = new GeneratorPDF();
		handlerOne.setDerivate(theDerivate);
		handlerOne.setStep(pdfStep);
		handlerOne.create();
		pdfWithLines = handlerOne.getPDFResult();
		pdfLinelvlFirstLine = pdfWithLines.getPdfPages().get(0).getTextcontent().get().get(0);

	}

	@Test
	void pdfLinelevelExists() {
		assertTrue(Files.exists(pdfWithLines.getPath()));
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
		assertEquals(128, pdfLinelvlFirstLine.getBaseline().length());
	}

}
