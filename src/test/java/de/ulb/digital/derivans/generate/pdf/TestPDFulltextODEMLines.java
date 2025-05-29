package de.ulb.digital.derivans.generate.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.ValidationResult;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.IDerivans;
import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.config.TypeConfiguration;
import de.ulb.digital.derivans.generate.GeneratorPDF;
import de.ulb.digital.derivans.model.DerivateMD;
import de.ulb.digital.derivans.model.pdf.PDFPage;
import de.ulb.digital.derivans.model.pdf.PDFResult;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;

/**
 * 
 * MWE: Artificial collection of 3 pages with METS/MODS
 * Used config: src/test/resources/config/derivans.ini
 * 
 * @author hartwig
 *
 */
class TestPDFulltextODEMLines {

	@TempDir
	static Path tempDir;

	static Path workDir;

	static PDFResult resultDoc;

	static Path pdfPath;

	@BeforeAll
	static void setupBeforeClass() throws Exception {
		workDir = tempDir.resolve("148811035");
		// use existing images
		Path imageDir = workDir.resolve(TestHelper.ULB_MAX_PATH);
		Files.createDirectories(imageDir);
		Path sourceImageDir = Path.of("src/test/resources/ocr/alto/148811035/MAX");
		TestHelper.copyTree(sourceImageDir, imageDir);
		Path sourceMets = Path.of("src/test/resources/ocr/alto/148811035/mets.xml");
		Path targetMets = workDir.resolve(Path.of("mets.xml"));
		Files.copy(sourceMets, targetMets);
		Path sourceOcr = Path.of("src/test/resources/ocr/alto/148811035/FULLTEXT");
		Path targetOcr = workDir.resolve("FULLTEXT");
		TestHelper.copyTree(sourceOcr, targetOcr);
		DerivateStepPDF pdfStep = new DerivateStepPDF();
		pdfStep.setImageDpi(300);
		pdfStep.setRenderLevel(TypeConfiguration.RENDER_LEVEL_LINE);
		pdfStep.setDebugRender(true);
		pdfStep.setConformance("PDF/A-1B");
		pdfStep.setInputDir(IDerivans.IMAGE_DIR_MAX);
		pdfStep.setOutputDir(".");
		pdfStep.setPathPDF(workDir.resolve("148811035.pdf"));
		DerivateMD testDerivate = new DerivateMD(targetMets);
		testDerivate.init(TestHelper.ULB_MAX_PATH);
		GeneratorPDF generator = new GeneratorPDF();
		generator.setDerivate(testDerivate);
		generator.setStep(pdfStep);
		generator.create();
		resultDoc = generator.getPDFResult();
		pdfPath = resultDoc.getPath();
	}

	@Test
	void testPDFWritten() {
		assertTrue(Files.exists(pdfPath));
	}

	@Test
	void testPDFResultDocPage5() {
		PDFPage page6 = resultDoc.getPdfPages().get(5);
		assertTrue(page6.getImagePath().endsWith("MAX/00000006.jpg"));
		assertEquals(6, page6.getNumber());
	}

	/**
	 * 
	 * Usefullness of output depends on actual PDF processing library
	 * for example, iText5 delivered good results, PDFBox3 not
	 * 
	 * prev: "an den Grantzen des Hertzogthums Florentz"
	 * curr: "ChriſtlicheMa"
	 * 
	 * @throws Exception
	 */
	@Test
	void testPage07ContainsText() throws Exception {
		var textPage07 = TestHelper.getTextAsSingleLine(pdfPath, 7);
		assertTrue(textPage07.contains("Christliche Majest")); // PDFBox
	}

	/**
	 * 
	 * Test total length of result text including whitespaces from page No 320809
	 * 
	 * was: 3381, 3446, 3501
	 * 
	 * @throws Exception
	 */
	@Test
	void testPage08TextLength() throws Exception {
		var textPage07 = TestHelper.getTextAsSingleLine(pdfPath, 7);
		assertEquals(3501, textPage07.length());
	}

	@Test
	void testValidity() throws Exception {
		VeraGreenfieldFoundryProvider.initialise();
		PDFAFlavour flavour = PDFAFlavour.fromString("1b");
		try (PDFAParser parser = Foundries.defaultInstance().createParser(new FileInputStream(pdfPath.toString()), flavour)) {
			PDFAValidator validator = Foundries.defaultInstance().createValidator(flavour, false);
			ValidationResult result = validator.validate(parser);
			assertFalse(result.isCompliant());
		}

	}
}
