package de.ulb.digital.derivans.derivate.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.config.TypeConfiguration;
import de.ulb.digital.derivans.derivate.IDerivateer;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DerivateFS;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.pdf.PDFResult;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;
import de.ulb.digital.derivans.model.step.DerivateType;

/**
 * 
 * MWE: Artificial collection of 3 pages
 * Used config: src/test/resources/config/derivans.ini
 * 
 * @author hartwig
 *
 */
public class TestPDFulltextODEMLines {

	@TempDir
	static Path tempDir;

	static Path workDir;

	static PDFResult resultDoc;

	static Path pdfPath;

	@BeforeAll
	public static void setupBeforeClass() throws Exception {
		workDir = tempDir.resolve("148811035");
		// use existing images
		Path imageDir = workDir.resolve(IDerivateer.IMAGE_DIR_MAX);
		Files.createDirectories(imageDir);
		Path sourceImageDir = Path.of("src/test/resources/alto/148811035/IMAGE_80");
		TestHelper.copyTree(sourceImageDir, imageDir);
		Path sourceMets = Path.of("src/test/resources/alto/148811035/mets.xml");
		Path targetMets = workDir.resolve(Path.of("mets.xml"));
		Files.copy(sourceMets, targetMets);
		Path sourceOcr = Path.of("src/test/resources/alto/148811035/FULLTEXT");
		Path targetOcr = workDir.resolve("FULLTEXT");
		TestHelper.copyTree(sourceOcr, targetOcr);
		DerivansData input = new DerivansData(workDir, IDerivateer.IMAGE_DIR_MAX, DerivateType.JPG);
		DerivansData output = new DerivansData(workDir, ".", DerivateType.PDF);
		DerivateStepPDF pdfStep = new DerivateStepPDF();
		pdfStep.setImageDpi(300);
		pdfStep.setRenderLevel(TypeConfiguration.RENDER_LEVEL_WORD);
		pdfStep.setDebugRender(true);
		pdfStep.setInputDir(IDerivateer.IMAGE_DIR_MAX);
		pdfStep.setOutputDir(".");
		pdfStep.setPathPDF(workDir.resolve("148811035.pdf"));
		DerivateFS derivate = new DerivateFS(workDir);
		derivate.init(TestHelper.ULB_MAX_PATH);
		List<DigitalPage> pages = derivate.getAllPages();
		// List<DigitalPage> pages = resolver.resolveFromStep(pdfStep);
		// resolver.enrichOCRFromFilesystem(pages, targetOcr);
		var handler = new PDFDerivateer(input, output, pages, pdfStep);
		handler.setDerivate(derivate);

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
	void testPage07ContainsText() throws Exception {
		var textPage07 = TestHelper.getTextAsSingleLine(pdfPath, 2);
		assertTrue(textPage07.contains("Chriſtliche Majeſt")); // PDFBox
	}

	/**
	 * 
	 * Test total length of result text including whitespaces
	 * 
	 * @throws Exception
	 */
	@Test
	void testPage07HasCertainLength() throws Exception {
		var textPage07 = TestHelper.getTextAsSingleLine(pdfPath, 2);
		assertEquals(3446, textPage07.length());
	}

}
