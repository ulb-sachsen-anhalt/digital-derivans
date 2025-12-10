package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.xpath.XPathExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.config.DerivansParameter;
import de.ulb.digital.derivans.model.pdf.PDFOutlineEntry;

/**
 * 
 * Digital object with rather complicated structure, i.e. wild
 * mixture of pages, sub-structures and ongoing structures
 * from Kitodo2 VD18 phase IV
 * 
 * used config: src/test/resources/config/derivans_ulb_odem.ini
 * 
 * @author hartwig
 *
 */
class TestDerivansComplexVolume {

	@TempDir
	static Path tempDir;

	static Path workDir;

	static String issueLabel = "1981185920_35126";

	static PDFOutlineEntry outline;

	@BeforeAll
	static void setupBeforeClass() throws Exception {

		workDir = tempDir.resolve(issueLabel);
		Path pathTargetMets = workDir.resolve(issueLabel + ".xml");
		// usually Kitodo3 has it's images in "images/max"
		// but due export workflow images are moved to "MAX"
		var pathImageDir = workDir.resolve("MAX");
		Files.createDirectories(pathImageDir);
		var pathRes = TestResource.SHARE_IT_VD18_1981185920_35126.get();
		Files.copy(pathRes, pathTargetMets);
		TestHelper.generateImages(pathImageDir, 120, 200, 415, "%08d.jpg");

		Path configTargetDir = tempDir.resolve("config");
		if (Files.exists(configTargetDir)) {
			Files.walk(configTargetDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(configTargetDir);
		}
		TestHelper.copyTree(TestResource.CONFIG_RES_DIR.get(), configTargetDir);
		DerivansParameter dp = new DerivansParameter();
		dp.setPathConfig(configTargetDir.resolve("derivans_ulb_odem.ini"));
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.init(pathTargetMets);
		derivans.forward();
		Path pdfWritten = workDir.resolve("168566600011796.pdf");
		TestHelper.PDFInspector pdfInspector = new TestHelper.PDFInspector(pdfWritten);
		outline = pdfInspector.getOutline();
	}

	@Test
	void testPDFWritten() {
		Path pdfWritten = workDir.resolve("168566600011796.pdf");
		assertTrue(Files.exists(pdfWritten));
	}

	@Test
	void testExpectedPDFPageCount() throws Exception {
		Path pdfWritten = workDir.resolve("168566600011796.pdf");
		TestHelper.PDFInspector pdfInspector = new TestHelper.PDFInspector(pdfWritten);
		int nExpectedImages = 415;
		int nPages = pdfInspector.countPages();
		assertEquals(nExpectedImages, nPages, "Expected PDF page count " + nExpectedImages + " but got " + nPages);
	}

	@Test
	void pdfRootOutline() {
		assertNotNull(outline);
		assertEquals("Nouveaux cahiers de lecture", outline.getLabel());
	}

	/**
	 * 
	 * This tacles the top structure entries (= volumes) issue, where due multiple
	 * linked structures in METS more than the given 415 pages appear as entries
	 * but 426 : 415 pages + 11 top structures
	 * 
	 */
	@Test
	void outlineTopStructs() {
		var volumeOutline = outline.getOutlineEntries().get(0);
		assertEquals(11, volumeOutline.getOutlineEntries().size());
	}

	@Test
	void testPDFFilePointerWritten() throws Exception {
		Document doc = TestHelper.readXMLDocument(workDir.resolve(issueLabel + ".xml"));
		var xprFilePtr = ".//mets:fptr[@FILEID='PDF_168566600011796']";
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

}
