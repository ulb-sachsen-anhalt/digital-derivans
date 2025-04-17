package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import de.ulb.digital.derivans.derivate.IDerivateer;
import de.ulb.digital.derivans.derivate.pdf.ITextProcessor;
import de.ulb.digital.derivans.model.pdf.PDFOutlineEntry;
import de.ulb.digital.derivans.model.step.DerivateStep;

/**
 * 
 * Common ULB Sachsen-Anhalt data setup
 * with images and METS/MODS metadata
 * from legacy vlserver to attach
 * footnote and generate some
 * additional derivates for dspace
 * (= 7 steps)
 * 
 * used config: src/test/resources/config/derivans-5steps.ini
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

	static List<IDerivateer> derivateers;

	@BeforeAll
	static void setupBeforeClass() throws Exception {

		// arrange metadata and images
		workDir = TestHelper.fixturePrint737429(tempDir);

		// arrange configuration
		// migration configuration with extended derivates
		Path configSourceDir = Path.of("src/test/resources/config");
		Path configTargetDir = tempDir.resolve("config");
		if (Files.exists(configTargetDir)) {
			Files.walk(configTargetDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(configTargetDir);
		}
		TestHelper.copyTree(configSourceDir, configTargetDir);
		DerivansParameter dp = new DerivansParameter();
		dp.setPathConfig(configTargetDir.resolve("derivans-5steps.ini"));
		Path input = workDir.resolve("737429.xml");
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.create(input);
		derivateers = derivans.getDerivateers();
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
	}

	@Test
	void testNumberOfCreatedDerivateers() {
		assertEquals(5, derivateers.size());
	}

	/**
	 * 
	 * Due Bug "Fail to render granular URN #51"
	 * https://github.com/ulb-sachsen-anhalt/digital-derivans/issues/51
	 * extended tests for clazz names
	 * 
	 */
	@Test
	void testTypesOfDerivateers()  {
		assertEquals("PDFDerivateer", derivateers.get(2).getClass().getSimpleName());
		assertEquals("ImageDerivateerJPG", derivateers.get(1).getClass().getSimpleName());
		assertNotEquals("ImageDerivateerJPGFooter", derivateers.get(0).getClass().getSimpleName());
		assertEquals("ImageDerivateerJPGFooterGranular", derivateers.get(0).getClass().getSimpleName());
	}

	@Test
	void testDerivateJPGsWithFooterWritten() {
		Path footerDir = workDir.resolve("IMAGE_FOOTER");
		assertTrue(Files.exists(footerDir));
		for (int i=1; i < TestHelper.fixture737429ImageLabel.size(); i++) {
			var imageLabel = String.format("%s.jpg", TestHelper.fixture737429ImageLabel.get(i));
			assertTrue(footerDir.resolve(imageLabel).toFile().exists());
		}
	}

	@Test
	void testPreviewImagesWritten() {
		Path previewDir = workDir.resolve(prefixPreview);
		assertTrue(Files.exists(previewDir));
		for (int i=1; i < TestHelper.fixture737429ImageLabel.size(); i++) {
			var imageLabel = String.format("%s%s.jpg", prefixPreview, TestHelper.fixture737429ImageLabel.get(i));
			assertTrue(previewDir.resolve(imageLabel).toFile().exists());
		}
	}

	@Test
	void testThumbnailsWritten()  {
		Path thumbsDir = workDir.resolve(prefixThumbs);
		assertTrue(Files.exists(thumbsDir));
		for (int i=1; i < TestHelper.fixture737429ImageLabel.size(); i++) {
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
	void testPDFOutline() throws Exception{
		Path pdfWritten = workDir.resolve("737429.pdf");
		assertTrue(Files.exists(pdfWritten));
		PDFOutlineEntry outline = ITextProcessor.readOutline(pdfWritten);
		assertNotNull(outline);
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
