package de.ulb.digital.derivans.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.DerivansParameter;
import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.model.DerivateStep;
import de.ulb.digital.derivans.model.DerivateType;

/**
 * 
 * @author hartwig
 *
 */
public class TestDerivansConfiguration {

	@Test
	void testCommonConfiguration(@TempDir Path tempDir) throws Exception {

		// arrange
		Path targetMetsDir = tempDir.resolve("226134857");
		Files.createDirectory(targetMetsDir);
		Path metsModsTarget = targetMetsDir.resolve("226134857.xml");
		Files.copy(TestResource.HD_Aa_226134857_LEGACY.get(), metsModsTarget);
		Path conf = Path.of("src/test/resources/config/derivans.ini");
		DerivansParameter dp = new DerivansParameter();
		dp.setPathConfig(conf);
		dp.setPathInput(metsModsTarget);

		// act
		DerivansConfiguration dc = new DerivansConfiguration(dp);

		// assert
		assertEquals(80, dc.getQuality());
		assertEquals(4, dc.getPoolsize());

		List<DerivateStep> steps = dc.getDerivateSteps();
		assertEquals(7, steps.size());

		// footer
		assertEquals("jpg", steps.get(0).getOutputType());
		assertEquals(95, steps.get(0).getQuality());
		assertEquals("Universitäts- und Landesbibliothek Sachsen-Anhalt", steps.get(0).getFooterLabel());
		assertEquals("src/test/resources/config/footer_template.png", steps.get(0).getPathTemplate().toString());
		assertEquals(DerivateType.JPG_FOOTER, steps.get(0).getDerivateType());

		// min derivate from footer
		assertEquals("jpg", steps.get(1).getOutputType());
		assertEquals(80, steps.get(1).getQuality());
		assertEquals("IMAGE_FOOTER", steps.get(1).getInputPath().getFileName().toString());
		assertEquals("IMAGE_80", steps.get(1).getOutputPath().getFileName().toString());
		assertEquals(DerivateType.JPG, steps.get(1).getDerivateType());

		// pdf
		assertEquals("pdf", steps.get(2).getOutputType());
		assertEquals(DerivateType.PDF, steps.get(2).getDerivateType());

		// additional assets
		assertEquals("BUNDLE_BRANDED_PREVIEW__", steps.get(5).getOutputPrefix());
		assertEquals(1000, steps.get(5).getMaximal());
		assertEquals(DerivateType.PDF, steps.get(2).getDerivateType());
	}

}
