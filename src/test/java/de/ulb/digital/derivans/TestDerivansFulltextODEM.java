package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.config.DerivansParameter;
import de.ulb.digital.derivans.model.pdf.PDFMetadata;
import de.ulb.digital.derivans.model.step.DerivateStepImage;

/**
 * 
 * Used config: src/test/resources/config/derivans.ini
 * 
 * @author hartwig
 *
 */
class TestDerivansFulltextODEM {

	@TempDir
	static Path tempDir;

	static Path workDir;

	static Path pdfPath;

	static TestHelper.PDFInspector inspector;

	static int nImages = 17;

	@BeforeAll
	static void setupBeforeClass() throws Exception {
		// arrange migration configuration with extended derivates
		Path configTargetDir = tempDir.resolve("config");
		if (Files.exists(configTargetDir)) {
			Files.walk(configTargetDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(configTargetDir);
		}
		TestHelper.copyTree(TestResource.CONFIG_RES_DIR.get(), configTargetDir);
		workDir = tempDir.resolve("148811035");

		// re-use images
		Path pathImageMax = workDir.resolve(TestHelper.ULB_MAX_PATH);
		Files.createDirectories(pathImageMax);
		Path sourceImageDir = Path.of("src/test/resources/ocr/alto/148811035/MAX");
		TestHelper.copyTree(sourceImageDir, pathImageMax);
		Path sourceMets = Path.of("src/test/resources/ocr/alto/148811035/mets.xml");
		Path targetMets = workDir.resolve(Path.of("mets.xml"));
		Files.copy(sourceMets, targetMets);
		Path sourceOcr = Path.of("src/test/resources/ocr/alto/148811035/FULLTEXT");
		Path targetOcr = workDir.resolve("FULLTEXT");
		TestHelper.copyTree(sourceOcr, targetOcr);

		DerivansParameter dp = new DerivansParameter();
		dp.setPathInput(targetMets);
		dp.setPathConfig(configTargetDir.resolve("derivans_ulb_odem.ini"));
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		// apply some scaling, too
		int maximal = 2339; // A4 200 DPI ok
		// int maximal = 1754; // A4 150 DPI tw, print vanishes over top up to "Sero
		// ..."
		// int maximal = 1170; // A4 100 DPI ok with smaller text
		((DerivateStepImage) dc.getDerivateSteps().get(1)).setMaximal(maximal);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.init(targetMets);
		derivans.forward();
		pdfPath = workDir.resolve("148811035.pdf");
		TestDerivansFulltextODEM.inspector = new TestHelper.PDFInspector(pdfPath);
	}

	@Test
	void testDerivateJPGsWithFooterWritten() {
		Path footerDir = workDir.resolve("IMAGE_FOOTER");
		assertTrue(Files.exists(footerDir));
		for (int i = 1; i < nImages; i++) {
			var imageLabel = String.format("%08d.jpg", i);
			assertTrue(footerDir.resolve(imageLabel).toFile().exists());
		}
	}

	@Test
	void testDerivatesForPDFWritten() {
		Path image80Dir = workDir.resolve(IDerivans.IMAGE_Q80);
		assertTrue(Files.exists(image80Dir));
		for (int i = 1; i < nImages; i++) {
			var imageLabel = String.format("%08d.jpg", i);
			assertTrue(image80Dir.resolve(imageLabel).toFile().exists());
		}
	}

	@Test
	void testPDFWritten() {
		assertTrue(Files.exists(pdfPath));
	}

	@Test
	void testPage01NoContents() throws Exception {
		assertEquals("", TestHelper.getTextAsSingleLine(pdfPath, 1).strip());
	}

	/**
	 * 
	 * Test total length of result text including whitespaces
	 * iText5 : 1327
	 * PDFBox3: 1038
	 * iText8 : 3372
	 * 2025 : 3382
	 * 
	 * @throws Exception
	 */
	@Test
	void testPage07HasCertainLength() throws Exception {
		var textPage07 = TestHelper.getTextAsSingleLine(pdfPath, 7);
		assertEquals(3382, textPage07.length());

	}

	@Test
	void testResultXMLvalid() throws Exception {
		var resultXML = workDir.resolve("mets.xml");
		var pathMETSXSD = TestResource.METS_1_12_XSD.get();
		assertTrue(TestHelper.validateXML(resultXML, pathMETSXSD));
	}

	@Test
	void testPDFMetadataAuthor() throws IOException, DigitalDerivansException {
		PDFMetadata pdfMD = inspector.getPDFMetaInformation();
		assertEquals("n.a.", pdfMD.getAuthor());
	}

	@Test
	void testPDFMetadataTitle() throws IOException, DigitalDerivansException {
		PDFMetadata pdfMD = inspector.getPDFMetaInformation();
		assertTrue(pdfMD.getTitle().startsWith("(1712) Neue Friedens-Vorschläge"));
	}
}
