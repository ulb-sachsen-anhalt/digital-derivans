package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.xpath.XPathExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.config.DerivansParameter;
import de.ulb.digital.derivans.generate.Generator;
import de.ulb.digital.derivans.model.pdf.PDFOutlineEntry;

/**
 * 
 * Kitodo3 exporter setup for newspaper issue with rather unordinary structure
 * i.e. in METS after page 4 goes page 7 (colorchecker) and then pages 5 and 6
 * which occour again as part of sub-structure "Feuilleton-Beilage. Nr. 20."
 * therefore the structmap links contains 9 links (1 top link, 4 pages, 1
 * colorchecker,
 * 3 pages) for only 7 actual pages and 1 sub-structure.
 * 
 * used config: src/test/resources/config/derivans_ulb_export.ini
 * 
 * @author hartwig
 *
 */
class TestDerivansExportKitodo3Issue {

	@TempDir
	static Path tempDir;

	static Path workDir;

	static String issueLabel = "253780594-18920720";

	static List<Generator> generators;

	static PDFOutlineEntry pdfOutline;

	@BeforeAll
	static void setupBeforeClass() throws Exception {

		workDir = tempDir.resolve(issueLabel);
		Path pathTargetMets = workDir.resolve(issueLabel + ".xml");
		// usually Kitodo3 has it's images in "images/max"
		// but due export workflow images are moved to "MAX"
		var pathImageDir = workDir.resolve("MAX");
		Files.createDirectories(pathImageDir);
		var pathRes = TestResource.K3_ZD2_253780594.get();
		Files.copy(pathRes, pathTargetMets);
		TestHelper.generateImages(pathImageDir, 120, 200, 7, "%08d.tif");

		Path configTargetDir = tempDir.resolve("config");
		if (Files.exists(configTargetDir)) {
			Files.walk(configTargetDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(configTargetDir);
		}
		TestHelper.copyTree(TestResource.CONFIG_RES_DIR.get(), configTargetDir);
		DerivansParameter dp = new DerivansParameter();
		dp.setPathConfig(configTargetDir.resolve("derivans_ulb_export.ini"));
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.init(pathTargetMets);
		generators = derivans.getGenerators();
		derivans.forward();
		Path pdfWritten = workDir.resolve("25378059418920720.pdf");
		TestHelper.PDFInspector pdfInspector = new TestHelper.PDFInspector(pdfWritten);
		pdfOutline = pdfInspector.getOutline();
	}

	@Test
	void testPDFWritten() {
		Path pdfWritten = workDir.resolve("25378059418920720.pdf");
		assertTrue(Files.exists(pdfWritten));
	}

	@Test
	void checkGeneratorClazzes() {
		assertEquals(5, generators.size());
		assertEquals("GeneratorImageJPGFooter", generators.get(0).getClass().getSimpleName());
		assertEquals("GeneratorImageJPG", generators.get(1).getClass().getSimpleName());
		assertEquals("GeneratorPDF", generators.get(2).getClass().getSimpleName());
		assertEquals("GeneratorImageJPG", generators.get(3).getClass().getSimpleName());
		assertEquals("GeneratorImageJPG", generators.get(4).getClass().getSimpleName());
	}

	@Test
	void testPageCount() throws Exception {
		Path pdfWritten = workDir.resolve("25378059418920720.pdf");
		TestHelper.PDFInspector pdfInspector = new TestHelper.PDFInspector(pdfWritten);
		int nExpectedImages = 7;
		int nPages = pdfInspector.countPages();
		assertEquals(nExpectedImages, nPages, "Expected PDF page count " + nExpectedImages + " but got " + nPages);
	}

	@Test
	void testPDFFilePointerWritten() throws Exception {
		Document doc = TestHelper.readXMLDocument(workDir.resolve(issueLabel + ".xml"));
		var xprFilePtr = ".//mets:fptr[@FILEID='PDF_25378059418920720']";
		XPathExpression<Element> xpath = TestHelper.generateXpression(xprFilePtr);
		Element el = xpath.evaluateFirst(doc);
		assertNotNull(el);
	}

	@Test
	void testResultXMLvalid() throws Exception {
		var resultXML = workDir.resolve(issueLabel + ".xml");
		var pathMETSXSD = TestResource.METS_1_12_XSD.get();
		assertTrue(TestHelper.validateXML(resultXML, pathMETSXSD));
	}

	/**
	 * 
	 * Assume following outline structure:
	 * - Level 1: "Zeitung"
	 * - Level 2: "Nr. 58."
	 * - Level 3: "Seite 1" - "Seite 4" + "Beilage" + Colorchecker => 6 entries
	 * 
	 * Issue with multi-linked pages resulting in wrong outline structure
	 * having 8 linked structures (duplicate links "Seite 5" and "Seite 6")
	 * which should only appear as part of "Feuilleton-Beilage. Nr. 20." entry.
	 * 
	 * @throws Exception
	 */
	@Test
	void outlineIssueTopStructs() {
		assertEquals("Zeitung", pdfOutline.getLabel());
		assertEquals(1, pdfOutline.getOutlineEntries().size());
		PDFOutlineEntry issueOutline = pdfOutline.getOutlineEntries().get(0);
		assertEquals("Nr. 58.", issueOutline.getLabel());
		var nTopStructs = issueOutline.getOutlineEntries().size();
		assertEquals(6, nTopStructs, "Expected 6 top level structs under issue but got " + nTopStructs);
		assertEquals("[Colorchecker]", issueOutline.getOutlineEntries().get(nTopStructs - 1).getLabel());
	}

	@Test
	void outlineIssueFeuilletonBeilage() {
		PDFOutlineEntry outlineIssue = pdfOutline.getOutlineEntries().get(0);
		var fStructs = outlineIssue.getOutlineEntries().get(4);
		assertEquals("Feuilleton-Beilage. Nr. 20.", fStructs.getLabel());
		assertEquals(2, fStructs.getOutlineEntries().size());
		var page5 = fStructs.getOutlineEntries().get(0);
		var page6 = fStructs.getOutlineEntries().get(1);
		assertEquals("[Seite 5]", page5.getLabel());
		assertEquals(5, page5.getPageNumber());
		assertEquals(6, page6.getPageNumber());
	}
}
