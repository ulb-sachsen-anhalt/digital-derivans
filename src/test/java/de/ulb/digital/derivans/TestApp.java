package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 
 * @author hartwig
 *
 */
class TestApp {

	/**
	 * 
	 * Testing the application bare boned (=config missing)
	 * using default, implicite steps
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testAppOdeFallbackConfiguration(@TempDir Path tempDir) throws Exception {

		// arrange metadata and images
		Path pathTarget = TestDerivans.arrangeMetsAndMAXImagesFor737429(tempDir);

		// act
		String[] args = { pathTarget.resolve("737429.xml").toString() };
		App.main(args);

		// assert
		Path pathPdf = pathTarget.resolve("191092622.pdf");
		assertTrue(Files.exists(pathPdf));
	}

	@Test
	void testAppOdeWithConfigurationFile(@TempDir Path tempDir) throws Exception {

		// arrange
		Path pathTarget = TestDerivans.arrangeMetsAndMAXImagesFor737429(tempDir);
		
		Path configDir = Path.of("src/test/resources/config");
		Path configTempDir = tempDir.resolve("config");
		Path configTemp = configTempDir.resolve("derivans.ini");
		TestHelper.copyTree(configDir, configTempDir);

		// act
		String[] args = { pathTarget.resolve("737429.xml").toString(), "-c", configTemp.toString() };
		App.main(args);

		// assert
		Path pathPdf = pathTarget.resolve("191092622.pdf");
		assertTrue(Files.exists(pathPdf));
	}
}
