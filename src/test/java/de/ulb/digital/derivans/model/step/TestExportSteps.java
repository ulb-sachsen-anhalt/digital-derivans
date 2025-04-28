package de.ulb.digital.derivans.model.step;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.Derivans;
import de.ulb.digital.derivans.TestHelper;
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
 * used config: src/test/resources/config/derivans_ulb_export.ini
 * 
 * @author hartwig
 *
 */
class TestExportSteps {

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
	static void setupBeforeClass() throws Exception {

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
		dp.setPathConfig(configTargetDir.resolve("derivans_ulb_export.ini"));
		dp.setPathInput(workDir.resolve("737429.xml"));
		dp.setNamePDF(NAME_PDF);
		dp.setPathFooter(configTargetDir.resolve(NAME_TEMPLATE));
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.init(workDir.resolve("737429.xml"));
		derivateers = derivans.getDerivateers();
		steps = derivans.getSteps();
	}


	@Test
	void testStepFooter() {
		DerivateStep theStep = TestExportSteps.steps.stream()
				.filter(s -> s.getOutputType() == DerivateType.JPG_FOOTER)
				.findFirst()
				.get();
		assertEquals(DerivateType.JPG_FOOTER, theStep.getOutputType());
		var footerStep = (DerivateStepImageFooter) theStep;
		assertEquals(NAME_TEMPLATE, footerStep.getPathTemplate().getFileName().toString());
	}

	@Test
	void testStepPDFLabel() {
		DerivateStep theStep = TestExportSteps.steps.stream()
				.filter(s -> s.getOutputType() == DerivateType.PDF)
				.findFirst()
				.get();
		assertEquals(DerivateType.PDF, theStep.getOutputType());
		var footerStep = (DerivateStepPDF) theStep;
		assertEquals(NAME_PDF, footerStep.getNamePDF().get());
	}


}
