package de.ulb.digital.derivans.derivate.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.data.DerivansPathResolver;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DescriptiveData;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.pdf.PDFDocument;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;
import de.ulb.digital.derivans.model.step.DerivateType;

/**
 * 
 * MWE:
 *  PDF with 1 page, Textlayer, no METS/MODS-metadata
 * 
 * @author hartwig
 *
 */
class TestPDFNewspaperWithOCRFromDir {

	static PDFDocument resultDoc;

	@BeforeAll
	static void initAll(@TempDir Path tempDir) throws Exception {
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
		assertNotEquals(-1, pageOne.getNumber());
		assertEquals(1, pageOne.getNumber());
	}

	/**
	 * Image dimension greatly reduced by rendering since iText
	 * assumes we input 72 DPI data, but for real we handle
	 * 300 DPI scanner data, therefore scale by 72/300
	 */
	@Test
	void inspectPageOneDimensions() {
		var pageOne = resultDoc.getPdfPages().get(0);
		assertEquals(180, pageOne.getDimension().getWidth());
		assertEquals(252, pageOne.getDimension().getHeight());
	}

	/**
	 * 
	 * Applied pretty low scale due original page was 8.000 x 10.000
	 * and now only 750 x 1.000 (tenth in each dimension) and again
	 * due 72 DPI vs. 300 DPI issue scaled even more (approx 1/42)
	 * 
	 */
	@Test
	void inspectScale() {
		float scale = resultDoc.getPdfPages().get(0).getScale();
		assertEquals(0.0239, scale, 0.01);
	}

	/**
	 * First line "" never printed since it is originally 17px in height,
	 * after scaling with ratio 0.02 only 0.34 pixel which is way too small
	 */
	@Test
	void inspectPageOne01stLine() {
		var firstLine = resultDoc.getPdfPages().get(0).getlines().get(0);
		assertFalse(firstLine.isPrinted());
	}

	@Test
	void inspectPageOne02ndLine() {
		var firstLine = resultDoc.getPdfPages().get(0).getlines().get(1);
		assertTrue(firstLine.isPrinted());
		assertEquals("Nr. 296 Seite 2",firstLine.getText());
		assertEquals(2, Math.round(firstLine.getFontSize()));
		assertEquals(17, firstLine.getBox().x);
		assertEquals(8, firstLine.getBox().y);
	}
}
