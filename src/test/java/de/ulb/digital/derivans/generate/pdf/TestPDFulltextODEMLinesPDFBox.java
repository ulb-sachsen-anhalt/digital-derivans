package de.ulb.digital.derivans.generate.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.IDerivans;
import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.config.TypeConfiguration;
import de.ulb.digital.derivans.generate.GeneratorPDF;
import de.ulb.digital.derivans.model.DerivateMD;
import de.ulb.digital.derivans.model.pdf.PDFResult;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;

/**
 * 
 * MWE: Artificial collection of 3 pages
 * Used config: src/test/resources/config/derivans.ini
 * 
 * @author hartwig
 *
 */
@Disabled
class TestPDFulltextODEMLinesPDFBox {

	@TempDir
	static Path tempDir;

	static Path workDir;

	static PDFResult resultDoc;

	static Path pdfPath;

	@BeforeAll
	static void setupBeforeClass() throws Exception {
		workDir = tempDir.resolve("148811035");
		// use existing images
		Path imageDir = workDir.resolve(IDerivans.IMAGE_DIR_MAX);
		Files.createDirectories(imageDir);
		Path sourceImageDir = Path.of("src/test/resources/ocr/alto/148811035/IMAGE_80");
		TestHelper.copyTree(sourceImageDir, imageDir);
		Path sourceMets = Path.of("src/test/resources/ocr/alto/148811035/mets.xml");
		Path targetMets = workDir.resolve(Path.of("mets.xml"));
		Files.copy(sourceMets, targetMets);
		Path sourceOcr = Path.of("src/test/resources/ocr/alto/148811035/FULLTEXT");
		Path targetOcr = workDir.resolve("FULLTEXT");
		TestHelper.copyTree(sourceOcr, targetOcr);
		DerivateStepPDF pdfStep = new DerivateStepPDF(imageDir.toString(), workDir.toString());
		pdfStep.setImageDpi(300);
		pdfStep.setRenderLevel(TypeConfiguration.RENDER_LEVEL_WORD);
		pdfStep.setDebugRender(true);
		DerivateMD derivate = new DerivateMD(targetMets);
		derivate.init(imageDir);
		var handler = new GeneratorPDF();
		var processor = new PDFBoxProcessor();
		handler.setPDFProcessor(processor);
		handler.setDerivate(derivate);
		handler.setStep(pdfStep);

		// act
		handler.create();
		resultDoc = handler.getPDFResult();
		pdfPath = resultDoc.getPath();
	}

	@Test
	void testPDFWritten() {
		assertTrue(Files.exists(pdfPath));
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
	@Disabled
	void testPage07ContainsText() throws Exception {
		var textPage07 = TestHelper.getTextAsSingleLine(pdfPath, 2);
		assertTrue(textPage07.contains("ChriſtlicheMa ")); // PDFBox
	}

	/**
	 * 
	 * Test total length of result text including whitespaces
	 * 
	 * @throws Exception
	 */
	@Test
	@Disabled
	void testPage07HasCertainLength() throws Exception {
		var textPage07 = TestHelper.getTextAsSingleLine(pdfPath, 2);
		assertEquals(1017, textPage07.length());
	}

}
