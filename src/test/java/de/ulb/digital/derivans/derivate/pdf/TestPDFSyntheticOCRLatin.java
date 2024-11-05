package de.ulb.digital.derivans.derivate.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.ocr.OCRData;
import de.ulb.digital.derivans.model.pdf.PDFDocument;
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
 * @author hartwig
 *
 */
class TestPDFSyntheticOCRLatin {

	private static int testPageSize = 1;

	static PDFDocument resultDoc;

	static int topRightY = 200;
	static int marginLeftX = 50;

	@BeforeAll
	static void initAll(@TempDir Path tempDir) throws Exception {
	Path pathImages = tempDir.resolve("MAX");
		Files.createDirectory(pathImages);
		List<DigitalPage> pages = new ArrayList<>();
		String imageName = String.format("%04d.jpg", 1);
		Path jpgFile = pathImages.resolve(imageName);
		BufferedImage bi2 = new BufferedImage(575, 799, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g2d = bi2.createGraphics();
		g2d.setColor(Color.ORANGE);
		g2d.fillRect(0, 0, 575, 799);
		ImageIO.write(bi2, "JPG", jpgFile.toFile());
		DigitalPage e = new DigitalPage(1, imageName);

		// create some ocr data
		List<Word> texts1 = new ArrayList<>();
		texts1.add(new Word("BELLA", new Rectangle(marginLeftX, topRightY, 50, 30)));
		texts1.add(new Word("CHIAO", new Rectangle(110, topRightY, 50, 30)));
		texts1.add(new Word("(DELLE", new Rectangle(170, topRightY, 60, 30)));
		texts1.add(new Word("MODINE)", new Rectangle(230, topRightY, 70, 30)));
		List<Word> texts2 = new ArrayList<>();
		texts2.add(new Word("Alla", new Rectangle(marginLeftX, 250, 40, 30)));
		texts2.add(new Word("matina,", new Rectangle(100, 250, 70, 25)));
		texts2.add(new Word("appena", new Rectangle(180, 250, 60, 25)));
		texts2.add(new Word("alzata", new Rectangle(240, 250, 60, 25)));
		List<Word> texts3 = new ArrayList<>();
		texts3.add(new Word("o", new Rectangle(marginLeftX, 300, 10, 25)));
		texts3.add(new Word("bella", new Rectangle(70, 300, 50, 25)));
		texts3.add(new Word("chiao,", new Rectangle(130, 300, 60, 25)));
		texts3.add(new Word("bella", new Rectangle(200, 300, 50, 25)));
		texts3.add(new Word("chiao,", new Rectangle(260, 300, 60, 25)));
		texts3.add(new Word("bella", new Rectangle(330, 300, 50, 25)));
		texts3.add(new Word("chiao", new Rectangle(390, 300, 50, 25)));
		texts3.add(new Word("chiao", new Rectangle(450, 300, 50, 25)));
		texts3.add(new Word("chiao!", new Rectangle(500, 300, 60, 25)));
		List<Textline> lines = List.of(
				new Textline(texts1),
				new Textline(texts2),
				new Textline(texts3));
		OCRData ocrData = new OCRData(lines, new Dimension(575, 799));

		e.setOcrData(ocrData);
		pages.add(e);

		// act
		String pdfName = String.format("pdfa-image-%04d.pdf", testPageSize);
		Path outPath = tempDir.resolve(pdfName);
		DerivansData input = new DerivansData(pathImages, DerivateType.JPG);
		DerivansData output = new DerivansData(outPath, DerivateType.PDF);
		DerivateStepPDF pdfStep = new DerivateStepPDF();
		pdfStep.setImageDpi(144);
		pdfStep.setRenderModus(DefaultConfiguration.DEFAULT_RENDER_LEVEL);
		pdfStep.setDebugRender(true);
		var handler = new PDFDerivateer(input, output, pages, pdfStep);

		// act
		handler.create();
		resultDoc = handler.getPDFDocument();
	}
	
	@Test
	void documentInstanceExists() {
		assertNotNull(resultDoc);
	}

	@Test
	void numberOfAddedPages() {
		assertEquals(1, resultDoc.getPdfPages().size());
	}

	/**
	 * Since there was no structural metadata present,
	 * there can't be a valid outline with links
	 */
	@Test
	void ensureNoOutlinePresent() {
		assertEquals(0, resultDoc.getOutline().size());
	}

	@Test
	void inspectPageOneNumber() {
		var pageOne = resultDoc.getPdfPages().get(0);
		assertEquals(1, pageOne.getNumber());
	}

	/**
	 * Image dimension greatly reduced by rendering since iText
	 * assumes we handle 72 DPI
	 */
	@Test
	void inspectPageOneDimension() {
		var pageOne = resultDoc.getPdfPages().get(0);
		assertEquals(287, pageOne.getDimension().getWidth());
		assertEquals(399, pageOne.getDimension().getHeight());
	}


	/**
	 * Since iText5 assumes 72 DPI desolution and we enforce 144 DPI
	 * input dimension is half-size only, so instead of starting at
	 * 50,200 it's actually 25,100
	 */
	@Test
	void inspectPageOneFirstLine() {
		var pageOne = resultDoc.getPdfPages().get(0);
		var firstline = pageOne.getlines().get(0);
		assertEquals(marginLeftX / 2, firstline.getBox().x);
		assertEquals(topRightY / 2, firstline.getBox().y);
	}
}
