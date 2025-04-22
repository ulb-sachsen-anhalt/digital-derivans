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
import de.ulb.digital.derivans.config.TypeConfiguration;
import de.ulb.digital.derivans.data.io.DerivansPathResolver;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.pdf.PDFResult;
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
class TestPDFLevelLineOCRFromDir {

	static PDFResult resultDoc;

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
		step.setOutputSubDir(pathTarget);
		step.setInputSubDir(pathImageMax);
		step.setDebugRender(true);
		step.setRenderLevel(TypeConfiguration.RENDER_LEVEL_WORD);
		// DescriptiveData dd = new DescriptiveData();
		List<DigitalPage> pages = resolver.resolveFromStep(step);
		resolver.enrichOCRFromFilesystem(pages, targetDir);
		// step.mergeDescsriptiveData(dd);
		PDFDerivateer handler = new PDFDerivateer(input, output, pages, step);
	
		// act
		handler.create();
		resultDoc = handler.getPDFResult();
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
	 * Image dimension depends on actual
	 * PDF rendering backend
	 * 
	 * iText5 assumed input 72 DPI data, even for
	 * 300 DPI scanner data
	 * 
	 * 180 vs. 754
	 * 252 vs. 1053
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
	 * and now approx. 750 x 1.000 (tenth in each dimension) and again
	 * due 72 DPI vs. 300 DPI issue scaled even more (approx 1/42)
	 * 
	 */
	@Test
	void inspectScale() {
		float scale = resultDoc.getPdfPages().get(0).getScale();
		assertEquals(0.023, scale, 0.01);
	}

	/**
	 * First line "" never printed since it is originally 17px in height,
	 * after scaling with ratio 0.02 only 0.34 pixel which is way too small
	 */
	@Test
	void inspectPageOne01stLine() {
		var optFirstLine = resultDoc.getPdfPages().get(0).getTextcontent();
		assertTrue(optFirstLine.isPresent());
		var firstLine = optFirstLine.get().get(0);
		assertFalse(firstLine.isPrinted());
	}

	@Test
	void inspectPageOne02ndLine() {
		var optFirstLine = resultDoc.getPdfPages().get(0).getTextcontent();
		assertTrue(optFirstLine.isPresent());
		var firstLine = optFirstLine.get().get(1);
		assertTrue(firstLine.isPrinted());
		assertEquals("Nr. 296 Seite 2",firstLine.getText());
		assertEquals(2, Math.round(firstLine.getFontSize()));
		assertEquals(17, (int)firstLine.getBox().getX());
		assertEquals(241, (int)firstLine.getBox().getY());
	}
}
