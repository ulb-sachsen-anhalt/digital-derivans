package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.xpath.XPathExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.config.DerivansParameter;

/**
 * 
 * Kitodo3 exporter setup for newspaper issue
 * with TIF images and METS/MODS metadata
 * Ensure resulting METS conforms to METS XSD
 * 
 * used config: src/test/resources/config/derivans.ini
 * 
 * @author hartwig
 *
 */
public class TestDerivansExportKitodo3Issue {

	@TempDir
	static Path tempDir;

	static Path workDir;

	static int nExpectedImages = 17;

	static String issueLabel = "253780594-18920720";

	@BeforeAll
	public static void setupBeforeClass() throws Exception {

		workDir = tempDir.resolve(issueLabel);
		Path pathTargetMets = workDir.resolve(issueLabel + ".xml");
		// usually Kitodo3 has it's images in "images/max"
		// but due export workflow images are moved to "MAX"
		var pathImageDir = workDir.resolve("MAX");
		Files.createDirectories(pathImageDir);
		var pathRes = TestResource.K3_ZD2_253780594.get();
		Files.copy(pathRes, pathTargetMets);
		TestHelper.generateImages(pathImageDir, 120, 200, 7, "%08d.tif");
		DerivansParameter dp = new DerivansParameter();
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.init(pathTargetMets);
		derivans.create();
	}

	@Test
	void testPDFWritten() throws Exception {
		Path pdfWritten = workDir.resolve("25378059418920720.pdf");
		assertTrue(Files.exists(pdfWritten));
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

}
