package de.ulb.digital.derivans.derivate.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.itextpdf.text.pdf.BaseFont;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.config.DefaultConfiguration;
import de.ulb.digital.derivans.derivate.IDerivateer;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DescriptiveData;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.DigitalStructureTree;
import de.ulb.digital.derivans.model.pdf.PDFMetadata;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;
import de.ulb.digital.derivans.model.step.DerivateType;

/**
 * 
 * Basic specification for creation of PDF derivates
 * 
 * @author hartwig
 *
 */
class TestPDFProcessorIText5 {

	private int testPageSize = 20;

	private DigitalStructureTree get() {
		DigitalStructureTree kap1 = new DigitalStructureTree(1, "Teil 1");
		DigitalStructureTree kap21 = new DigitalStructureTree(4, "Abschnitt 2.1");
		DigitalStructureTree kap22 = new DigitalStructureTree(6, "Abschnitt 2.2");
		DigitalStructureTree kap2 = new DigitalStructureTree(4, "Teil 2", List.of(kap21, kap22));
		DigitalStructureTree kap3 = new DigitalStructureTree(10, "Teil 3");
		DigitalStructureTree kap41 = new DigitalStructureTree(12, "Abschnitt 4.1");
		DigitalStructureTree kap421 = new DigitalStructureTree(14, "Abschnitt 4.2.1");
		DigitalStructureTree kap422 = new DigitalStructureTree(16, "Abschnitt 4.2.2");
		DigitalStructureTree kap42 = new DigitalStructureTree(14, "Abschnitt 4.2", List.of(kap421, kap422));
		DigitalStructureTree kap4 = new DigitalStructureTree(12, "Teil 4", List.of(kap41, kap42));
		DigitalStructureTree kap5 = new DigitalStructureTree(18, "Teil 5");
		return new DigitalStructureTree(1, "Buch 1", List.of(kap1, kap2, kap3, kap4, kap5));
	}

	/**
	 * 
	 * Create Derivates from plain root dir without any further metadata
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testCreateFromPath01_0025(@TempDir Path tempDir) throws Exception {

		// arrange
		Path pathImages = tempDir.resolve("MAX");
		Files.createDirectory(pathImages);
		List<DigitalPage> pages = new ArrayList<>();
		for (int i = 1; i <= testPageSize; i++) {
			String imageName = String.format("%04d.jpg", i);
			Path jpgFile = pathImages.resolve(imageName);
			BufferedImage bi2 = new BufferedImage(500, 750, BufferedImage.TYPE_3BYTE_BGR);
			ImageIO.write(bi2, "JPG", jpgFile.toFile());
			DigitalPage e = new DigitalPage(i, imageName);
			pages.add(e);
		}

		// act
		String pdfName = String.format("pdf-image-%04d.pdf", testPageSize);
		Path outPath = tempDir.resolve(pdfName);
		DescriptiveData dd = new DescriptiveData();
		dd.setYearPublished("2020");

		DerivansData input = new DerivansData(pathImages, DerivateType.JPG);
		DerivansData output = new DerivansData(outPath, DerivateType.PDF);
		int pdfImageDpi = DefaultConfiguration.PDF_IMAGE_DPI;
		DerivateStepPDF pdfMeta = new DerivateStepPDF();
		pdfMeta.setImageDpi(pdfImageDpi);
		pdfMeta.mergeDescriptiveData(dd);
		var handler = new PDFDerivateer(input, output, pages, pdfMeta);
		handler.setStructure(get());
		int result = handler.create();
		var doc = handler.getPDFDocument();

		// assert
		assertEquals(1, result);
		assertTrue(Files.exists(outPath));

		assertEquals("n.a.", doc.getMetadata().getTitle());
		// no default creator information exists
		assertTrue(doc.getMetadata().getCreator().isEmpty());
		assertEquals("n.a.", doc.getMetadata().getAuthor());
	}

	/**
	 * 
	 * Create PDF with format PDF/A but no metadata
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testCreatePDFA(@TempDir Path tempDir) throws Exception {

		// arrange
		Path pathImages = tempDir.resolve("MAX");
		Files.createDirectory(pathImages);
		List<DigitalPage> pages = new ArrayList<>();
		for (int i = 1; i <= testPageSize; i++) {
			String imageName = String.format("%04d.jpg", i);
			Path jpgFile = pathImages.resolve(imageName);
			BufferedImage bi2 = new BufferedImage(575, 799, BufferedImage.TYPE_3BYTE_BGR);
			Graphics2D g2d = bi2.createGraphics();
			g2d.setColor(Color.ORANGE);
			g2d.fillRect(0, 0, 575, 799);
			ImageIO.write(bi2, "JPG", jpgFile.toFile());
			DigitalPage e = new DigitalPage(i, imageName);
			pages.add(e);
		}
		String level = DefaultConfiguration.PDFA_CONFORMANCE_LEVEL;

		// act
		String pdfName = String.format("pdfa-image-%04d.pdf", testPageSize);
		Path outPath = tempDir.resolve(pdfName);
		DescriptiveData dd = new DescriptiveData();
		dd.setYearPublished("2020");

		DerivansData input = new DerivansData(pathImages, DerivateType.JPG);
		DerivansData output = new DerivansData(outPath, DerivateType.PDF);
		DerivateStepPDF pdfMeta = new DerivateStepPDF();
		pdfMeta.setConformanceLevel(level);
		pdfMeta.setImageDpi(300);
		pdfMeta.mergeDescriptiveData(dd);
		IDerivateer handler = new PDFDerivateer(input, output, pages, pdfMeta);

		// act
		var result = handler.create();
		PDFInspector inspector = new PDFInspector(outPath);
		PDFMetadata pdfMetaInformation = inspector.getPDFMetaInformation();

		// assert
		assertEquals(1, result);
		assertTrue(Files.exists(outPath));
		assertEquals("n.a.", pdfMetaInformation.getTitle());
		assertEquals("n.a.", pdfMetaInformation.getAuthor());
		// no default creator information exists
		assertTrue(pdfMetaInformation.getCreator().isEmpty());
	}

	/**
	 * 
	 * Testdata from VL HD ID 369765, page 316642
	 * 
	 * <String CONTENT="⸗" HEIGHT="12" HPOS="939" ID="region0000_line0021_word0000"
	 * VPOS="1293" WIDTH="14"/>
	 * 
	 * Please note:
	 * iText5 is not able to calculate a valid length for this char, therefore it
	 * crashes
	 * 
	 * @throws Exception
	 */
	@Test
	void testFontSizeForBadLines() throws Exception {
		String fontPath = "src/main/resources/ttf/DejaVuSans.ttf";
		assumeTrue(Files.exists(Path.of(fontPath)));
		BaseFont font = new FontHandler().forPDF(fontPath);

		// 31662.xml
		String text = "⸗";

		assertFalse(Character.isAlphabetic(text.toCharArray()[0]));
		// act
		float size = IText5Processor.calculateFontSize(font, text, 14, 12);
		assertEquals(0.0, size);
	}

	/**
	 * 
	 * Check: how gets a backslash rendered?
	 * OCR data from VL HD ID 369765, page 316642
	 * 
	 * <String CONTENT="/" HEIGHT="45" HPOS="394" ID="region0000_line0023_word0001"
	 * VPOS="1372" WIDTH="11"/>
	 * 
	 * @throws Exception
	 */
	@Test
	void testFontSizeForBackslash() throws Exception {
		String fontPath = "src/main/resources/ttf/DejaVuSans.ttf";
		assumeTrue(Files.exists(Path.of(fontPath)));
		BaseFont font = new FontHandler().forPDF(fontPath);

		// 316642.xml
		String backslash = "/";

		// act
		assertFalse(Character.isAlphabetic(backslash.toCharArray()[0]));
		float size = IText5Processor.calculateFontSize(font, backslash, 45, 11);
		assertTrue(size > 3.0);
	}

	/**
	 * 
	 * Minimum check: enforce illegal DPI yields exception
	 * 
	 * @throws Exception
	 */
	@Test
	void testInvalidImageDPI() {

		// arrange mwe
		DescriptiveData dd = new DescriptiveData();
		DerivateStepPDF pdfMeta = new DerivateStepPDF();
		pdfMeta.mergeDescriptiveData(dd);
		pdfMeta.setImageDpi(1);

		// act
		var proc = new IText5Processor();
		var thrown = assertThrows(DigitalDerivansException.class, () -> {
			proc.init(pdfMeta, null, get());
		});

		// assert
		assertTrue(thrown.getMessage().contains("invalid dpi: '1'"));
	}

}
