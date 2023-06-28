package de.ulb.digital.derivans.derivate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.itextpdf.text.pdf.BaseFont;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.config.DefaultConfiguration;
import de.ulb.digital.derivans.data.DerivansPathResolver;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DescriptiveData;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.DigitalStructureTree;
import de.ulb.digital.derivans.model.PDFMetaInformation;
import de.ulb.digital.derivans.model.PDFPageformation;
import de.ulb.digital.derivans.model.ocr.OCRData;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;
import de.ulb.digital.derivans.model.step.DerivateType;

/**
 * 
 * Basic specification for creation of PDF derivates
 * 
 * @author hartwig
 *
 */
public class TestPDFDerivateer {

	private int testPageSize = 20;

	DigitalStructureTree get() {
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
		// BaseDerivateer base = new BaseDerivateer(input, output);
		int pdfImageDpi = DefaultConfiguration.PDF_IMAGE_DPI;
		DerivateStepPDF pdfMeta = new DerivateStepPDF();
		pdfMeta.setImageDpi(pdfImageDpi);
		pdfMeta.mergeDescriptiveData(dd);
		// IDerivateer handler = new PDFDerivateer(base, get(), pages, pdfMeta);
		IDerivateer handler = new PDFDerivateer(input, output, pages, pdfMeta);
		((PDFDerivateer) handler).setStructure(get());
		int result = handler.create();

		PDFInspector inspector = new PDFInspector(outPath);
		PDFMetaInformation pdfMetaInformation = inspector.getPDFMetaInformation();

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
	 * Create PDFs with differernt size requirements
	 * but still without additional metadata
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testCreatePDFWithDifferentSizeImages(@TempDir Path tempDir) throws Exception {

		// arrange
		int n_pages = 10;
		Path pathImages = tempDir.resolve("MAX");
		Files.createDirectory(pathImages);
		List<DigitalPage> pages = new ArrayList<>();
		for (int i = 1; i <= n_pages; i++) {
			String imageName = String.format("%04d.jpg", i);
			Path jpgFile = pathImages.resolve(imageName);
			int width = (i % 2 == 0) ? 500 : 625;
			int heigth = (i % 2 == 1) ? 600 : 700;
			BufferedImage bi2 = new BufferedImage(width, heigth, BufferedImage.TYPE_3BYTE_BGR);
			ImageIO.write(bi2, "JPG", jpgFile.toFile());
			DigitalPage e = new DigitalPage(i, imageName);
			pages.add(e);
		}
		DigitalStructureTree kap1 = new DigitalStructureTree(1, "Teil 1");
		DigitalStructureTree kap21 = new DigitalStructureTree(4, "Abschnitt 2.1");
		DigitalStructureTree kap22 = new DigitalStructureTree(6, "Abschnitt 2.2");
		DigitalStructureTree kap2 = new DigitalStructureTree(4, "Teil 2", List.of(kap21, kap22));
		DigitalStructureTree tree = new DigitalStructureTree(1, "Buch 1", List.of(kap1, kap2));

		// act
		String pdfName = String.format("pdf-image-%04d.pdf", n_pages);
		Path outPath = tempDir.resolve(pdfName);
		DerivansData input = new DerivansData(pathImages, DerivateType.JPG);
		DerivansData output = new DerivansData(outPath, DerivateType.PDF);
		DerivateStepPDF pdfMeta = new DerivateStepPDF();
		pdfMeta.setImageDpi(300);
		IDerivateer handler = new PDFDerivateer(input, output, pages, pdfMeta);
		((PDFDerivateer) handler).setStructure(tree);

		// act
		handler.create();

		PDFInspector inspector = new PDFInspector(outPath);
		List<PDFPageformation> pagesInfo = inspector.getPageInformation();

		// assert
		assertEquals(10, pagesInfo.size());
		assertEquals(1, pagesInfo.get(0).getNumber());
		// adjusted because differences between images and document dimensions
		assertEquals(150, pagesInfo.get(0).getDimension().width); // was: 650
		assertEquals(144, pagesInfo.get(0).getDimension().height); // was: 600
		assertEquals(120, pagesInfo.get(5).getDimension().width); // was: 500
		assertEquals(168, pagesInfo.get(5).getDimension().height); // was: 700
	}

	/**
	 * 
	 * Create PDF with format PDF/A but still no metadata
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
		PDFMetaInformation pdfMetaInformation = inspector.getPDFMetaInformation();

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
	 * Create PDF from local images plus local OCR files
	 * without metadata
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testCreatePDFTextlayer(@TempDir Path tempDir) throws Exception {

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

			// create some ocr data
			List<OCRData.Text> texts1 = new ArrayList<>();
			texts1.add(new OCRData.Text("Hello", new Rectangle(100, 100, 100, 50)));
			texts1.add(new OCRData.Text("World", new Rectangle(300, 100, 100, 50)));
			List<OCRData.Text> texts4 = new ArrayList<>();
			texts4.add(new OCRData.Text("BELLA", new Rectangle(50, 200, 50, 30)));
			texts4.add(new OCRData.Text("CHIAO", new Rectangle(150, 200, 50, 30)));
			texts4.add(new OCRData.Text("(DELLE", new Rectangle(200, 200, 50, 30)));
			texts4.add(new OCRData.Text("MODINE)", new Rectangle(250, 200, 50, 30)));
			List<OCRData.Text> texts2 = new ArrayList<>();
			texts2.add(new OCRData.Text("Alla", new Rectangle(100, 250, 50, 25)));
			texts2.add(new OCRData.Text("matina,", new Rectangle(150, 250, 50, 25)));
			texts2.add(new OCRData.Text("appena", new Rectangle(200, 250, 50, 25)));
			texts2.add(new OCRData.Text("alzata", new Rectangle(250, 250, 50, 25)));
			List<OCRData.Text> texts3 = new ArrayList<>();
			texts3.add(new OCRData.Text("o", new Rectangle(10, 300, 10, 15)));
			texts3.add(new OCRData.Text("bella", new Rectangle(30, 300, 50, 15)));
			texts3.add(new OCRData.Text("chiao,", new Rectangle(100, 300, 50, 15)));
			texts3.add(new OCRData.Text("bella", new Rectangle(160, 300, 50, 15)));
			texts3.add(new OCRData.Text("chiao,", new Rectangle(240, 300, 50, 15)));
			texts3.add(new OCRData.Text("bella", new Rectangle(300, 300, 50, 15)));
			texts3.add(new OCRData.Text("chiao", new Rectangle(360, 300, 50, 15)));
			texts3.add(new OCRData.Text("chiao", new Rectangle(420, 300, 50, 15)));
			texts3.add(new OCRData.Text("chiao!", new Rectangle(480, 300, 50, 15)));
			List<OCRData.Textline> lines = List.of(
					new OCRData.Textline(texts1),
					new OCRData.Textline(texts4),
					new OCRData.Textline(texts2),
					new OCRData.Textline(texts3));
			OCRData ocrData = new OCRData(lines, new Dimension(575, 799));

			e.setOcrData(ocrData);
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
		pdfMeta.setImageDpi(144);
		pdfMeta.mergeDescriptiveData(dd);
		IDerivateer handler = new PDFDerivateer(input, output, pages, pdfMeta);
		((PDFDerivateer)handler).setStructure(get());

		// act
		int result = handler.create();

		// inspect
		PDFInspector inspector = new PDFInspector(outPath);
		PDFMetaInformation pdfMetaInformation = inspector.getPDFMetaInformation();

		// assert
		assertEquals(1, result);
		assertTrue(Files.exists(outPath));
		assertEquals("n.a.", pdfMetaInformation.getTitle());
		assertEquals("n.a.", pdfMetaInformation.getAuthor());
		// no default creator information exists
		assertTrue(pdfMetaInformation.getCreator().isEmpty());
	}

	@Test
	void testReadMetaInformationFromPDF01() throws Exception {
		// act
		Path pdfPath = Paths.get("src/test/resources/pdf/169683404X.pdf");
		PDFInspector inspector = new PDFInspector(pdfPath);
		PDFMetaInformation pdfMetaInformation = inspector.getPDFMetaInformation();

		// assert
		assertEquals(
				"Namensteil: Boethius (Typ: family), 480-524 (Typ: date), Anicius Manlius Severinus (Typ: given), authority URI: , value URI: , Rolle: Rollenbezeichnung: aut (Norm: marcrelator, Typ: code), Anzeigeform: Boethius, Anicius Manlius Severinus, Typ: personal, Norm: gnd",
				pdfMetaInformation.getAuthor());
		assertEquals("[Halle (Saale), Universitäts- und Landesbibliothek Sachsen-Anhalt, Qu. Cod. 77, Fragment 1]",
				pdfMetaInformation.getTitle());
		assertEquals("169683404X", pdfMetaInformation.getXmpMetadata().getElementsByTagNameNS("*", "recordIdentifier")
				.item(0).getChildNodes().item(1).getTextContent());
	}

	/**
	 * 
	 * Ensure that OCR Data is taken into account
	 * if running in local mode without metadata
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testTextlayerFromFulltextDirectoryOnly(@TempDir Path tempDir) throws Exception {

		Path pathTarget = tempDir.resolve("zd1");

		// arrange ocr data
		Path sourceOcr = Path.of("src/test/resources/alto/1667524704_J_0150/1667524704_J_0150_0512.xml");
		assertTrue(Files.exists(sourceOcr, LinkOption.NOFOLLOW_LINKS));
		Path sourceFile = sourceOcr.getFileName();
		Path targetDir = pathTarget.resolve("FULLTEXT");
		Files.createDirectories(targetDir);
		Path targetOcr = targetDir.resolve(sourceFile);
		Files.copy(sourceOcr, targetOcr);

		// arrange image data
		Path pathImageMax = pathTarget.resolve("MAX");
		Files.createDirectory(pathImageMax);
		Path imagePath = pathImageMax.resolve("1667524704_J_0150_0512.jpg");
		// original dimensions: 7544,10536
		TestHelper.writeImage(imagePath, 754, 1053, BufferedImage.TYPE_BYTE_GRAY, "JPG");

		// arrange base derivateer
		DerivansData input = new DerivansData(pathImageMax, DerivateType.JPG);
		DerivansData output = new DerivansData(pathTarget, DerivateType.PDF);
		// BaseDerivateer base = new BaseDerivateer(input, output);

		// arrange pdf path and pages
		DerivansPathResolver resolver = new DerivansPathResolver(pathTarget);
		DerivateStepPDF step = new DerivateStepPDF();
		step.setOutputPath(pathTarget);
		step.setInputPath(pathImageMax);
		DescriptiveData dd = new DescriptiveData();
		List<DigitalPage> pages = resolver.resolveFromStep(step);
		resolver.enrichOCRFromFilesystem(pages, targetDir);
		step.mergeDescriptiveData(dd);
		PDFDerivateer handler = new PDFDerivateer(input, output, pages, step);

		// act
		int result = handler.create();

		// assert
		assertEquals(1, result);
		Path pdfWritten = pathTarget.resolve("zd1.pdf");
		assertTrue(Files.exists(pdfWritten));
		assertEquals(1, handler.getNPagesWithOCR().get());
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
		float size = PDFDerivateer.calculateFontSize(font, text, 14, 12);
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
		float size = PDFDerivateer.calculateFontSize(font, backslash, 45, 11);
		assertTrue(size > 3.0);
	}

	/**
	 * 
	 * check: providing illegal DPI yields exception
	 * 
	 * @throws Exception
	 */
	@Test
	void testInvalidImageDPI() throws Exception {

		// arrange mwe
		DerivansData input = new DerivansData(Path.of("."), DerivateType.JPG);
		DerivansData output = new DerivansData(Path.of("."), DerivateType.PDF);
		DescriptiveData dd = new DescriptiveData();
		DerivateStepPDF pdfMeta = new DerivateStepPDF();
		pdfMeta.mergeDescriptiveData(dd);
		pdfMeta.setImageDpi(1);

		// act
		var thrown = assertThrows(DigitalDerivansException.class, () -> {
			new PDFDerivateer(input, output, new ArrayList<>(), pdfMeta);
		});

		// assert
		assertTrue(thrown.getMessage().contains("invalid dpi: '1'"));
	}


	/**
	 * 
	 * Check: invalid pages provided => yield exception
	 * 
	 * @throws Exception
	 */
	@Test
	void testInvalidPageArg() throws Exception {

		// arrange mwe
		DerivansData input = new DerivansData(Path.of("."), DerivateType.JPG);
		DerivansData output = new DerivansData(Path.of("."), DerivateType.PDF);
		DescriptiveData dd = new DescriptiveData();
		DerivateStepPDF pdfMeta = new DerivateStepPDF();
		pdfMeta.mergeDescriptiveData(dd);

		// act
		var thrown = assertThrows(DigitalDerivansException.class, () -> {
			new PDFDerivateer(input, output, null, pdfMeta);
		});

		// assert
		assertTrue(thrown.getMessage().contains("Invalid pages"));
	}
}
