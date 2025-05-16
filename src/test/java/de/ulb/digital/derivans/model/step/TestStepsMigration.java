package de.ulb.digital.derivans.model.step;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.Derivans;
import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.config.DerivansParameter;
import de.ulb.digital.derivans.generate.Generator;

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
class TestStepsMigration {

	private static final String NAME_PDF = "the-name-of-foo";

	private static final String NAME_TEMPLATE = "foo_template_without_bar.png";

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
		TestStepsMigration.workDir = TestHelper.fixturePrint737429(TestStepsMigration.tempDir, "MAX");

		// arrange configuration
		// migration configuration with extended derivates
		Path configTargetDir = TestStepsMigration.tempDir.resolve("config");
		if (Files.exists(configTargetDir)) {
			Files.walk(configTargetDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(configTargetDir);
		}
		TestHelper.copyTree(TestResource.CONFIG_RES_DIR.get(), configTargetDir);
		DerivansParameter dp = new DerivansParameter();
		dp.setPathConfig(configTargetDir.resolve("derivans_ulb_migration.ini"));
		dp.setNamePDF(NAME_PDF);
		dp.setPathFooter(configTargetDir.resolve(NAME_TEMPLATE));
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.init(workDir.resolve("737429.xml"));
		generators = derivans.getDerivateers();
		steps = derivans.getSteps();
	}

	@Test
	void testStepFooter() {
		DerivateStep theStep = TestStepsMigration.steps.stream()
				.filter(DerivateStepImageFooter.class::isInstance)
				.findFirst()
				.get();
		assertEquals(DerivateType.JPG, theStep.getOutputType());
		var footerStep = (DerivateStepImageFooter) theStep;
		assertEquals(NAME_TEMPLATE, footerStep.getPathTemplate().getFileName().toString());
	}

	@Test
	void testStepPDFLabel() {
		DerivateStep theStep = TestStepsMigration.steps.stream()
				.filter(s -> s.getOutputType() == DerivateType.PDF)
				.findFirst()
				.get();
		assertEquals(DerivateType.PDF, theStep.getOutputType());
		var footerStep = (DerivateStepPDF) theStep;
		assertEquals(NAME_PDF, footerStep.getNamePDF().get());
	}

}
