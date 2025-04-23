package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
import de.ulb.digital.derivans.model.step.DerivateStep;
import de.ulb.digital.derivans.model.step.DerivateStepImageFooter;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;
import de.ulb.digital.derivans.model.step.DerivateType;

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
class TestDerivansArguments {

	private static final String NAME_PDF = "the-name-of-the";

	private static final String NAME_PDF_FILE = NAME_PDF + ".pdf";

	private static final String NAME_TEMPLATE = "footer_template_without_dfg_logo.png";

	@TempDir
	static Path tempDir;

	static Path workDir;

	static String prefixPreview = "BUNDLE_BRANDED_PREVIEW__";

	static String prefixThumbs = "BUNDLE_THUMBNAIL__";

	static List<DerivateStep> steps;

	static List<IDerivateer> derivateers;

	@BeforeAll
	public static void setupBeforeClass() throws Exception {

		// arrange metadata and images
		workDir = TestHelper.fixturePrint737429(tempDir, "MAX");

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
		dp.setPathInput(workDir.resolve("737429.xml"));
		dp.setNamePDF(NAME_PDF);
		dp.setPathFooter(configTargetDir.resolve(NAME_TEMPLATE));
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.init(workDir.resolve("737429.xml"));
		derivans.create();
		derivateers = derivans.getDerivateers();
		steps = derivans.getSteps();
	}

	@Test
	void testPDFWritten() {
		Path pdfWritten = workDir.resolve(NAME_PDF_FILE);
		assertTrue(Files.exists(pdfWritten));
	}

	@Test
	void testMETSUpdateSuccess() throws Exception {
		// assert proper pdf-metadata integration
		var pdfMark = "PDF_" + NAME_PDF;
		Document doc = TestHelper.readXMLDocument(workDir.resolve("737429.xml"));
		XPathExpression<Element> xpath = TestHelper.generateXpression(".//mets:fptr[@FILEID='" + pdfMark + "']");
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
		assertEquals(pdfMark, firstChild.getAttribute("FILEID").getValue());
	}

	@Test
	void testStepPDFLabel() {
		DerivateStep theStep = steps.stream()
				.filter(s -> s.getDerivateType() == DerivateType.PDF)
				.findFirst()
				.get();
		assertEquals(DerivateType.PDF, theStep.getDerivateType());
		var footerStep = (DerivateStepPDF) theStep;
		assertEquals(NAME_PDF, footerStep.getNamePDF().get());
	}

	@Test
	void testStepFooter() {
		DerivateStep theStep = steps.stream()
				.filter(s -> s.getDerivateType() == DerivateType.JPG_FOOTER)
				.findFirst()
				.get();
		assertEquals(DerivateType.JPG_FOOTER, theStep.getDerivateType());
		var footerStep = (DerivateStepImageFooter) theStep;
		assertEquals(NAME_TEMPLATE, footerStep.getPathTemplate().getFileName().toString());
	}

	/**
	 * 
	 * Simulate scenario with condfigured XPath identifier
	 * which preceedes default ULB PDF label (from mods:identifier@type=gbv)
	 * 
	 */
	@Test
	void testConfiguredXPathUsed(@TempDir Path tmpDir) throws Exception {
		var thisRoot = tmpDir.resolve("forceNameXPath");
		Files.createDirectories(thisRoot);
		var thisDir = TestHelper.fixturePrint737429(thisRoot);

		// arrange configuration
		// migration configuration with extended derivates
		Path configSourceDir = Path.of("src/test/resources/config");
		Path configTargetDir = thisDir.resolve("config");
		TestHelper.copyTree(configSourceDir, configTargetDir);

		// alter config
		var pathConfig = configTargetDir.resolve("derivans_ulb.ini");
		var identConfLine = "mods_identifier_xpath = //mods:mods/mods:titleInfo/mods:title";
		Files.writeString(pathConfig, identConfLine + System.lineSeparator(), StandardOpenOption.APPEND);

		DerivansParameter dp = new DerivansParameter();
		dp.setPathConfig(pathConfig);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.init(thisDir.resolve("737429.xml"));
		derivans.create();

		// assert
		var optPdf = Files.list(thisDir)
			.filter(p -> p.getFileName().toString().endsWith(".pdf")).findFirst();
		assertTrue(optPdf.isPresent());
		assertTrue(optPdf.get().getFileName().toString().startsWith("Ode_In_Solemni_"));
	}

	/**
	 * 
	 * Simulate scenario with condfigured XPath identifier and
	 * additional param fpr PDF name => ensure the value from
	 * CLI overrides configured value
	 * 
	 */
	@Test
	void testConfiguredXPathCollidesWithParam(@TempDir Path tmpDir) throws Exception {
		var thisRoot = tmpDir.resolve("forceNameCLI");
		Files.createDirectories(thisRoot);
		var thisDir = TestHelper.fixturePrint737429(thisRoot);

		// arrange configuration
		// migration configuration with extended derivates
		Path configSourceDir = Path.of("src/test/resources/config");
		Path configTargetDir = thisDir.resolve("config");
		TestHelper.copyTree(configSourceDir, configTargetDir);

		// alter config
		var pathConfig = configTargetDir.resolve("derivans_ulb.ini");
		var identConfLine = "mods_identifier_xpath = //mods:mods/mods:titleInfo/mods:title";
		Files.writeString(pathConfig, identConfLine + System.lineSeparator(), StandardOpenOption.APPEND);

		DerivansParameter dp = new DerivansParameter();
		dp.setPathConfig(pathConfig);
		dp.setNamePDF(NAME_PDF);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.init(thisDir.resolve("737429.xml"));
		derivans.create();

		// assert
		Path pdfWritten = thisDir.resolve(NAME_PDF_FILE);
		assertTrue(Files.exists(pdfWritten));
	}
}
