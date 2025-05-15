package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
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
import de.ulb.digital.derivans.model.step.DerivateStep;
import de.ulb.digital.derivans.model.step.DerivateType;

/**
 * 
 * Common Kitodo2 exporter setup 
 * with TIF images, METS/MODS metadata
 * and additional DSpace derivates
 * 
 * used config: src/test/resources/config/derivans_ulb_export.ini
 * 
 * @author hartwig
 *
 */
class TestDerivansExportKitodo2 {

	@TempDir
	static Path tempDir;

	static Path workDir;

	static int nExpectedImages = 17;

	static List<DerivateStep> steps;

	@BeforeAll
	static void setupBeforeClass() throws Exception {

		// arrange metadata and images
		var pathRes = Path.of("src/test/resources/mets/kitodo2/058141367.xml");
		TestDerivansExportKitodo2.workDir = fixtureMetadataTIFK2(tempDir, pathRes);

		// arrange configuration
		Path configSourceDir = Path.of("src/test/resources/config");
		Path configTargetDir = tempDir.resolve("config");
		if (Files.exists(configTargetDir)) {
			Files.walk(configTargetDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(configTargetDir);
		}
		TestHelper.copyTree(configSourceDir, configTargetDir);
		DerivansParameter dp = new DerivansParameter();
		dp.setPathConfig(configTargetDir.resolve("derivans_ulb_export.ini"));
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		Path input = workDir.resolve("058141367.xml");
		assertTrue(Files.exists(input));
		derivans.init(input);
		TestDerivansExportKitodo2.steps = derivans.getSteps();
		derivans.forward();
	}

	@Test
	void testStep01() {
		var step01 = TestDerivansExportKitodo2.steps.get(0);
		assertEquals(IDerivans.IMAGE_DIR_MAX, step01.getInputDir());
		assertEquals(IDerivans.IMAGE_FOOTER, step01.getOutputDir());
		assertEquals(DerivateType.TIF, step01.getInputType());
		assertEquals(DerivateType.JPG, step01.getOutputType());
	}

	@Test
	void testStep04() {
		var theStep = TestDerivansExportKitodo2.steps.get(3);
		assertEquals(IDerivans.IMAGE_FOOTER, theStep.getInputDir());
		assertEquals(IDerivans.IMAGE_PREVIEW, theStep.getOutputDir());
		assertEquals(DerivateType.JPG, theStep.getInputType());
		assertEquals(DerivateType.JPG, theStep.getOutputType());
		assertEquals(IDerivans.IMAGE_PREVIEW, theStep.getOutputPrefix());
	}

	@Test
	void testStep05() {
		var theStep = TestDerivansExportKitodo2.steps.get(4);
		assertEquals(IDerivans.IMAGE_PREVIEW, theStep.getInputDir());
		assertEquals(IDerivans.IMAGE_THUMBNAIL, theStep.getOutputDir());
		assertEquals(DerivateType.JPG, theStep.getInputType());
		assertEquals(IDerivans.IMAGE_PREVIEW, theStep.getInputPrefix());
	}

	@Test
	void testDerivateJPGsWithFooterWritten() throws Exception {
		Path footerDir = TestDerivansExportKitodo2.workDir.resolve("IMAGE_FOOTER");
		assertTrue(Files.exists(footerDir));
		for (int i = 1; i < nExpectedImages; i++) {
			var imageLabel = String.format("%08d.jpg", i);
			assertTrue(footerDir.resolve(imageLabel).toFile().exists());
		}
	}

	@Test
	void testPreviewImagesWritten() throws Exception {
		Path previewDir = TestDerivansExportKitodo2.workDir.resolve(IDerivans.IMAGE_PREVIEW);
		assertTrue(Files.exists(previewDir));
		for (int i = 1; i < nExpectedImages; i++) {
			var imageLabel = String.format("%s%08d.jpg", IDerivans.IMAGE_PREVIEW, i);
			assertTrue(previewDir.resolve(imageLabel).toFile().exists());
		}
	}

	@Test
	void testThumbnailsWritten() throws Exception {
		Path thumbsDir = TestDerivansExportKitodo2.workDir.resolve(IDerivans.IMAGE_THUMBNAIL);
		assertTrue(Files.exists(thumbsDir));
		for (int i = 1; i < nExpectedImages; i++) {
			var imageLabel = String.format("%s%08d.jpg", IDerivans.IMAGE_THUMBNAIL, i);
			assertTrue(thumbsDir.resolve(imageLabel).toFile().exists());
		}
	}

	@Test
	void testPDFWritten() throws Exception {
		Path pdfWritten = TestDerivansExportKitodo2.workDir.resolve("058141367.pdf");
		assertTrue(Files.exists(pdfWritten));
	}

	@Test
	void testMETSUpdateSuccess() throws Exception {
		// assert proper pdf-metadata integration
		Document doc = TestHelper.readXMLDocument(TestDerivansExportKitodo2.workDir.resolve("058141367.xml"));
		XPathExpression<Element> xpath = TestHelper.generateXpression(".//mets:fptr[@FILEID='PDF_058141367']");
		Element el = xpath.evaluateFirst(doc);
		assertNotNull(el);

		// inspect parent element
		Element parent = el.getParentElement();
		assertEquals("div", parent.getName());
		assertEquals("monograph", parent.getAttribute("TYPE").getValue());
		assertEquals("LOG_0000", parent.getAttribute("ID").getValue());

		// assert PDF is inserted as first child
		Element firstChild = parent.getChildren().get(0);
		assertNotNull(firstChild.getAttribute("FILEID"));
		assertEquals("PDF_058141367", firstChild.getAttribute("FILEID").getValue());
	}

	@Test
	void testResultXMLvalid() throws Exception {
		var resultXML = TestDerivansExportKitodo2.workDir.resolve("058141367.xml");
		var pathMETSXSD = TestResource.METS_1_12_XSD.get();
		assertTrue(TestHelper.validateXML(resultXML, pathMETSXSD));
	}

	public static Path fixtureMetadataTIFK2(Path tempDir, Path srcMets) throws IOException {
		Path pathTarget = tempDir.resolve("058141367");
		if (Files.exists(pathTarget)) {
			Files.walk(pathTarget).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(pathTarget);
		}
		Path imagesDst = pathTarget.resolve("MAX");
		Files.createDirectories(imagesDst);
		// for (int i=1; i<18; i++) {
			TestHelper.generateImages(imagesDst, 140, 200, 17, "%08d.tif");
		// }
		Path metsTarget = pathTarget.resolve("058141367.xml");
		Files.copy(srcMets, metsTarget);
		return pathTarget;
	}
}
