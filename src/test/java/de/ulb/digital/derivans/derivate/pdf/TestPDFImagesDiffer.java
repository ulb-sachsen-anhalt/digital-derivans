package de.ulb.digital.derivans.derivate.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.DigitalStructureTree;
import de.ulb.digital.derivans.model.pdf.PDFDocument;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;
import de.ulb.digital.derivans.model.step.DerivateType;

/**
 * 
 * MWE:
 *  PDF with 10 pages, no Textlayer, no METS/MODS-metadata
 *  synthetic structure tree, images differ in dimension
 * 
 * @author hartwig
 *
 */
class TestPDFImagesDiffer {

	static PDFDocument resultDoc;

	@BeforeAll
	static void initAll(@TempDir Path tempDir) throws Exception {
		// arrange
		int nPages = 10;
		Path pathImages = tempDir.resolve("MAX");
		Files.createDirectory(pathImages);
		List<DigitalPage> pages = new ArrayList<>();
		for (int i = 1; i <= nPages; i++) {
			String imageName = String.format("%04d.jpg", i);
			Path jpgFile = pathImages.resolve(imageName);
			int width = (i % 2 == 0) ? 1000 : 700;
			int heigth = 600;
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
		String pdfName = String.format("pdf-image-%04d.pdf", nPages);
		Path outPath = tempDir.resolve(pdfName);
		DerivansData input = new DerivansData(pathImages, DerivateType.JPG);
		DerivansData output = new DerivansData(outPath, DerivateType.PDF);
		DerivateStepPDF pdfMeta = new DerivateStepPDF();
		pdfMeta.setImageDpi(300);
		var handler = new PDFDerivateer(input, output, pages, pdfMeta);
		handler.setStructure(tree);

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
		assertEquals(10, resultDoc.getPdfPages().size());
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
		assertNotEquals(-1, pageOne.getNumber());
		assertEquals(1, pageOne.getNumber());
	}

	/**
	 * Image dimension greatly reduced by rendering since iText
	 * assumes we input 72 DPI data, but for real we handle
	 * 300 DPI scanner data
	 * 
	 * 168: was 700
	 * 144: was 600
	 */
	@Test
	void inspectPageOneDimension() {
		var pageOne = resultDoc.getPdfPages().get(0);
		assertEquals(168, pageOne.getDimension().getWidth());
		assertEquals(144, pageOne.getDimension().getHeight());
	}

	/**
	 * cf. {@link #inspectPageOneDimension()}
	 * 240: was 1000
	 * 144: was 600
	 */
	@Test
	void inspectPageTwoDimension() {
		var pageOne = resultDoc.getPdfPages().get(1);
		assertEquals(240, pageOne.getDimension().getWidth());
		assertEquals(144, pageOne.getDimension().getHeight());
	}
}
