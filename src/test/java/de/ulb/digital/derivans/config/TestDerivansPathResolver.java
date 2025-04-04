package de.ulb.digital.derivans.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.Derivans;
import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.data.io.DerivansPathResolver;
import de.ulb.digital.derivans.model.DigitalPage;

/**
 * 
 * @author u.hartwig
 *
 */
class TestDerivansPathResolver {

	@Test
	void testPathResolverDefaultConf(@TempDir Path tempDir) throws Exception {
		// arrange
		Path pathTarget = tempDir.resolve("only_images");
		Path pathImageMax = pathTarget.resolve("MAX");
		Files.createDirectories(pathImageMax);
		TestHelper.generateImages(pathImageMax, 1240, 1754, 6, "%04d.jpg");
		DerivansParameter dp = new DerivansParameter();
		dp.setPathInput(pathTarget);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		dc.setParamImages(pathImageMax.toString());
		Derivans derivans = new Derivans(dc);
		// DerivansPathResolver resolver = new DerivansPathResolver(dc.getPathDir());

		// // act
		// List<DigitalPage> actuals = resolver.resolveFromStep(derivans.getSteps().get(0));
		
		// // assert
		// assertNotNull(actuals);
		// assertEquals(6, actuals.size());
	}
}
