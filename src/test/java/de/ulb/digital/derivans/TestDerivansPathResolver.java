package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.model.DerivateStep;
import de.ulb.digital.derivans.model.DerivateType;
import de.ulb.digital.derivans.model.DigitalPage;

/**
 * 
 * @author u.hartwig
 *
 */
public class TestDerivansPathResolver {

	@Test
	void testPathResolverDefaultConf(@TempDir Path tempDir) throws Exception {
		// arrange
		Path pathTarget = tempDir.resolve("only_images");
		Path pathImageMax = pathTarget.resolve("MAX");
		Files.createDirectories(pathImageMax);
		TestDerivans.generateJpgs(pathImageMax, 1240, 1754, 6);
		DerivansParameter dp = new DerivansParameter();
		dp.setPathInput(pathTarget);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);
		DerivansPathResolver resolver = new DerivansPathResolver();

		// act
		List<DerivateStep> steps = derivans.steps;
		assertEquals(DerivateType.JPG, steps.get(0).getDerivateType());
		List<DigitalPage> actuals = resolver.resolveFromStep(steps.get(0));
		
		// assert
		assertNotNull(actuals);
		assertEquals(6, actuals.size());
	}
}
