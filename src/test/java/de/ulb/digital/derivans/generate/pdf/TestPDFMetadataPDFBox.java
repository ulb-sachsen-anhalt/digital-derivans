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
import de.ulb.digital.derivans.model.pdf.DescriptiveMetadata;
import de.ulb.digital.derivans.model.pdf.PDFMetadata;
import de.ulb.digital.derivans.model.pdf.PDFResult;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;

/**
 * 
 * Test PDF metadata handling with PDFBox processor
 * 
 * @author hartwig
 *
 */
class TestPDFMetadataPDFBox {

	static Path pdfPath;
	static TestHelper.PDFInspector inspector;

	@BeforeAll
	static void initAll(@TempDir Path tempDir) throws Exception {
		// Create simple test PDF with metadata
		Path pathImages = tempDir.resolve("MAX");
		Files.createDirectory(pathImages);
		Path jpgFile = pathImages.resolve("00001.jpg");
		
		// Create test image
		BufferedImage bi = new BufferedImage(800, 600, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g2d = bi.createGraphics();
		g2d.setColor(Color.WHITE);
		g2d.fillRect(0, 0, 800, 600);
		ImageIO.write(bi, "JPG", jpgFile.toFile());
		
		// Create digital page
		DigitalPage digiPage = new DigitalPage("MAX_0001", 1, jpgFile);
		List<DigitalPage> pages = new ArrayList<>();
		pages.add(digiPage);
		
		DerivateStruct testStruct = new DerivateStruct(1, "0001");
		testStruct.getPages().add(digiPage);
		DerivateFS testDerivate = new DerivateFS(tempDir);
		testDerivate.setStructure(testStruct);

		// Create PDF with metadata
		Path outputPath = tempDir.resolve("test-metadata-pdfbox.pdf");
		DerivateStepPDF pdfStep = new DerivateStepPDF("MAX", ".");
		pdfStep.setImageDpi(DefaultConfiguration.DEFAULT_IMAGE_DPI);
		pdfStep.setPathPDF(outputPath);
		
		// Set metadata
		DescriptiveMetadata metadata = new DescriptiveMetadata();
		metadata.setTitle("Test PDF Document with PDFBox");
		metadata.setPerson("Test Author");
		metadata.setYearPublished("2026");
		pdfStep.mergeDescriptiveData(metadata);
		
		GeneratorPDF generator = new GeneratorPDF();
		generator.setPDFProcessor(new PDFBoxProcessor());
		generator.setDerivate(testDerivate);
		generator.setStep(pdfStep);
		generator.create();
		
		PDFResult result = generator.getPDFResult();
		pdfPath = result.getPath();
		inspector = new TestHelper.PDFInspector(pdfPath);
	}

	@Test
	void testPDFExists() {
		assertTrue(Files.exists(pdfPath));
	}

	@Test
	void testPDFMetadataTitle() throws Exception {
		PDFMetadata pdfMetadata = inspector.getPDFMetaInformation();
		assertEquals("(2026) Test PDF Document with PDFBox", pdfMetadata.getTitle());
	}

	@Test
	void testPDFMetadataAuthor() throws Exception {
		PDFMetadata pdfMetadata = inspector.getPDFMetaInformation();
		assertEquals("Test Author", pdfMetadata.getAuthor());
	}

	@Test
	void testPDFHasOneCorePage() throws Exception {
		int nPages = inspector.countPages();
		assertEquals(1, nPages);
	}
}
