package de.ulb.digital.derivans.derivate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.config.DefaultConfiguration;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DerivateType;
import de.ulb.digital.derivans.model.DescriptiveData;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.DigitalStructureTree;
import de.ulb.digital.derivans.model.PDFMetaInformation;
import de.ulb.digital.derivans.model.ocr.OCRData;

/**
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

	@Test
	void testCreateFromPath01_0025(@TempDir Path tempDir) throws Exception {

		// arrange
		Path pathImages = tempDir.resolve("MAX");
		Files.createDirectory(pathImages);
		List<DigitalPage> pages = new ArrayList<>();
		for (int i = 1; i <= testPageSize; i++) {
			String imageName = String.format("%04d.jpg", i);
			Path jpgFile = pathImages.resolve(imageName);
			BufferedImage bi2 = new BufferedImage(1050, 1498, BufferedImage.TYPE_3BYTE_BGR);
			ImageIO.write(bi2, "JPG", jpgFile.toFile());
			DigitalPage e = new DigitalPage(i, imageName);
			pages.add(e);
		}

		// act
		String pdfName = String.format("pdf-image-%04d.pdf", testPageSize);
		Path outPath = tempDir.resolve(pdfName);
		DescriptiveData dd = new DescriptiveData();
		dd.setYearPublished("2020");

		DerivansData output = new DerivansData(outPath, DerivateType.PDF);
		IDerivateer handler = new PDFDerivateer(new DerivansData(pathImages, DerivateType.JPG), output, get(), dd,
				pages, null);
		boolean result = handler.create();

		PDFMetaInformation pdfMetaInformation = PDFDerivateer.getPDFMetaInformation(outPath);

		// assert
		assertTrue(result);
		assertTrue(Files.exists(outPath));
		assertEquals("n.a.", pdfMetaInformation.getTitle());
		assertEquals("n.a.", pdfMetaInformation.getAuthor());
		// no default creator information exists
		assertNull(pdfMetaInformation.getCreator());
	}

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

		DerivansData output = new DerivansData(outPath, DerivateType.PDF);
		IDerivateer handler = new PDFDerivateer(new DerivansData(pathImages, DerivateType.JPG), output, get(), dd,
				pages, level);

		boolean result = handler.create();

		PDFMetaInformation pdfMetaInformation = PDFDerivateer.getPDFMetaInformation(outPath);

		// assert
		assertTrue(result);
		assertTrue(Files.exists(outPath));
		assertEquals("n.a.", pdfMetaInformation.getTitle());
		assertEquals("n.a.", pdfMetaInformation.getAuthor());
		// no default creator information exists
		assertNull(pdfMetaInformation.getCreator());
	}

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

		DerivansData output = new DerivansData(outPath, DerivateType.PDF);
		IDerivateer handler = new PDFDerivateer(new DerivansData(pathImages, DerivateType.JPG), output, get(), dd,
				pages, level);

		boolean result = handler.create();

		PDFMetaInformation pdfMetaInformation = PDFDerivateer.getPDFMetaInformation(outPath);
		
		// assert
		assertTrue(result);
		assertTrue(Files.exists(outPath));
		assertEquals("n.a.", pdfMetaInformation.getTitle());
		assertEquals("n.a.", pdfMetaInformation.getAuthor());
		// no default creator information exists
		assertNull(pdfMetaInformation.getCreator());
	}

	@Test
	void testGetPDFMetaInformation01() throws Exception {
		// act
		PDFMetaInformation pdfMetaInformation = null;
		Path pdfPath = Paths.get("src/test/resources/pdf/169683404X.pdf");
		pdfMetaInformation = PDFDerivateer.getPDFMetaInformation(pdfPath);

		// assert
		assertEquals(
				"Namensteil: Boethius (Typ: family), 480-524 (Typ: date), Anicius Manlius Severinus (Typ: given), authority URI: , value URI: , Rolle: Rollenbezeichnung: aut (Norm: marcrelator, Typ: code), Anzeigeform: Boethius, Anicius Manlius Severinus, Typ: personal, Norm: gnd",
				pdfMetaInformation.getAuthor());
		assertEquals("[Halle (Saale), Universitäts- und Landesbibliothek Sachsen-Anhalt, Qu. Cod. 77, Fragment 1]",
				pdfMetaInformation.getTitle());
		assertEquals("169683404X", pdfMetaInformation.getXmpMetadata().getElementsByTagNameNS("*", "recordIdentifier")
				.item(0).getChildNodes().item(1).getTextContent());
	}

}
