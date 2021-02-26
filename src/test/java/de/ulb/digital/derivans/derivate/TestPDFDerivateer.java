package de.ulb.digital.derivans.derivate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.derivate.IDerivateer;
import de.ulb.digital.derivans.derivate.PDFDerivateer;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DerivateType;
import de.ulb.digital.derivans.model.DescriptiveData;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.DigitalStructureTree;
import de.ulb.digital.derivans.model.PDFMetaInformation;

/**
 * 
 * @author hartwig
 *
 */
public class TestPDFDerivateer {

	String post737429 = "./src/test/resources/metsmods/737429_post.xml";

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
			BufferedImage bi2 = new BufferedImage(2500, 4000, BufferedImage.TYPE_3BYTE_BGR);
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
				pages);
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
		assertEquals(pdfMetaInformation.getAuthor(),
				"Namensteil: Boethius (Typ: family), 480-524 (Typ: date), Anicius Manlius Severinus (Typ: given), authority URI: , value URI: , Rolle: Rollenbezeichnung: aut (Norm: marcrelator, Typ: code), Anzeigeform: Boethius, Anicius Manlius Severinus, Typ: personal, Norm: gnd");
		assertEquals(pdfMetaInformation.getTitle(),
				"[Halle (Saale), Universitäts- und Landesbibliothek Sachsen-Anhalt, Qu. Cod. 77, Fragment 1]");
		assertEquals(pdfMetaInformation.getXmpMetadata().getElementsByTagNameNS("*", "recordIdentifier").item(0)
				.getChildNodes().item(1).getTextContent(), "169683404X");
	}

}
