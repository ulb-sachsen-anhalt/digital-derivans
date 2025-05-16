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
import de.ulb.digital.derivans.model.step.DerivateStep;

/**
 * 
 * MWE for ULB Sachsen-Anhalt
 * Setup contains JPG images + METS/MODS metadata
 * generate images, attach footnote, create PDF
 * (= 5 steps)
 * 
 * used config: src/test/resources/config/derivans_ulb_migration.ini
 * 
 * @author hartwig
 *
 */
class TestDerivansLegacyULB {

	@TempDir
	static Path tempDir;

	static Path workDir;

	static String prefixPreview = "BUNDLE_BRANDED_PREVIEW__";

	static String prefixThumbs = "BUNDLE_THUMBNAIL__";

	static List<DerivateStep> steps;

	static List<Generator> generators;

	@BeforeAll
	static void setupBeforeClass() throws Exception {

		// arrange metadata and images
		workDir = TestHelper.fixturePrint737429(tempDir);

		// arrange configuration
		// migration configuration with extended derivates
		Path configTargetDir = tempDir.resolve("config");
		if (Files.exists(configTargetDir)) {
			Files.walk(configTargetDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(configTargetDir);
		}
		TestHelper.copyTree(TestResource.CONFIG_RES_DIR.get(), configTargetDir);
		DerivansParameter dp = new DerivansParameter();
		dp.setPathConfig(configTargetDir.resolve("derivans_ulb_migration.ini"));
		Path input = workDir.resolve("737429.xml");
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.init(input);
		derivans.forward();
		generators = derivans.getDerivateers();
		steps = derivans.getSteps();
	}

	@Test
	void testNumberOfParsedDerivansSteps() {
		assertEquals(5, steps.size());
	}

	@Test
	void testTypesOfDerivansSteps() {
		assertEquals("DerivateStepImageFooter", steps.get(0).getClass().getSimpleName());
		assertEquals("DerivateStepImage", steps.get(1).getClass().getSimpleName());
		assertEquals("DerivateStepPDF", steps.get(2).getClass().getSimpleName());
		assertEquals("GeneratorImageJPG", generators.get(3).getClass().getSimpleName());
		assertEquals("GeneratorImageJPG", generators.get(4).getClass().getSimpleName());
	}

	@Test
	void testNumberOfCreatedDerivateers() {
		assertEquals(5, generators.size());
	}

	@Test
	void ensurePathsDerivateer01() {
		var gen01 = generators.get(0);
		assertTrue(gen01.getDerivate().getRootDir().endsWith("737429"));
		assertEquals("MAX", gen01.getStep().getInputDir());
		assertEquals("IMAGE_FOOTER", gen01.getStep().getOutputDir());
	}

	@Test
	void ensurePathsDerivateer02() {
		var gen02 = generators.get(1);
		assertTrue(gen02.getDerivate().getRootDir().endsWith("737429"));
		assertEquals("IMAGE_FOOTER", gen02.getStep().getInputDir());
		assertEquals("IMAGE_80", gen02.getStep().getOutputDir());
	}

	@Test
	void testDerivateJPGsWithFooterWritten() {
		Path footerDir = workDir.resolve("IMAGE_FOOTER");
		assertTrue(Files.exists(footerDir));
		for (int i = 1; i < TestHelper.fixture737429ImageLabel.size(); i++) {
			var imageLabel = String.format("%s.jpg", TestHelper.fixture737429ImageLabel.get(i));
			assertTrue(footerDir.resolve(imageLabel).toFile().exists());
		}
	}

	@Test
	void testPreviewImagesWritten() {
		Path previewDir = workDir.resolve(prefixPreview);
		assertTrue(Files.exists(previewDir));
		for (int i = 1; i < TestHelper.fixture737429ImageLabel.size(); i++) {
			var imageLabel = String.format("%s%s.jpg", prefixPreview, TestHelper.fixture737429ImageLabel.get(i));
			assertTrue(previewDir.resolve(imageLabel).toFile().exists());
		}
	}

	@Test
	void testThumbnailsWritten() {
		Path thumbsDir = workDir.resolve(prefixThumbs);
		assertTrue(Files.exists(thumbsDir));
		for (int i = 1; i < TestHelper.fixture737429ImageLabel.size(); i++) {
			var imageLabel = String.format("%s%s.jpg", prefixThumbs, TestHelper.fixture737429ImageLabel.get(i));
			assertTrue(thumbsDir.resolve(imageLabel).toFile().exists());
		}
	}

	@Test
	void testPDFNamedLikePPNWritten() {
		Path pdfWritten = workDir.resolve("191092622.pdf");
		assertTrue(Files.exists(pdfWritten));
	}

	@Test
	void testPDFOutlineTree() throws Exception {
		Path pdfWritten = workDir.resolve("191092622.pdf");
		PDFOutlineEntry outline = new TestHelper.PDFInspector(pdfWritten).getOutline();
		assertNotNull(outline);
		assertEquals("Outlines", outline.getLabel());
		assertEquals(1, outline.getOutlineEntries().size());
		PDFOutlineEntry logRoot = outline.getOutlineEntries().get(0);
		assertEquals("Ode In Solemni", logRoot.getLabel().substring(0, 14));
		assertEquals(2, logRoot.getOutlineEntries().size());
		var childOne = logRoot.getOutlineEntries().get(0);
		var childTwo = logRoot.getOutlineEntries().get(1);
		assertEquals("Titelblatt", childOne.getLabel());
		assertEquals("[Ode]", childTwo.getLabel());
		assertEquals(1, childOne.getOutlineEntries().size());
		assertEquals("[Seite 2]", childOne.getOutlineEntries().get(0).getLabel());
		assertEquals(3, childTwo.getOutlineEntries().size());
		assertEquals("[Seite 3]", childTwo.getOutlineEntries().get(0).getLabel());
		assertEquals("[Seite 5]", childTwo.getOutlineEntries().get(2).getLabel());
	}

	@Test
	void testMETSUpdateSuccess() throws Exception {
		// assert proper pdf-metadata integration
		Document doc = TestHelper.readXMLDocument(workDir.resolve("737429.xml"));
		XPathExpression<Element> xpath = TestHelper.generateXpression(".//mets:fptr[@FILEID='PDF_191092622']");
		Element el = xpath.evaluateFirst(doc);
		assertNotNull(el);

		// inspect parent element
		Element parent = el.getParentElement();
		assertEquals("div", parent.getName());
		assertEquals("monograph", parent.getAttribute("TYPE").getValue());
		assertEquals("log737429", parent.getAttribute("ID").getValue());

		// assert PDF is inserted as first child
		Element firstChild = parent.getChildren().get(0);
		assertNotNull(firstChild.getAttribute("FILEID"));
		assertEquals("PDF_191092622", firstChild.getAttribute("FILEID").getValue());
	}

}
