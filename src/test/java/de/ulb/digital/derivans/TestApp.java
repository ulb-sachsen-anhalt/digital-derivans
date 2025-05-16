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
	 * using implicite default 2step flow compliant with DFG viewer
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testFallbackConfiguration(@TempDir Path tempDir) throws Exception {

		// arrange metadata and images
		Path pathTarget = TestHelper.fixturePrint737429(tempDir, "DEFAULT");
		Path metsFile = pathTarget.resolve("737429.xml");
		var allInLines = Files.readAllLines(metsFile);
		StringBuffer outlines = new StringBuffer();
		for(int i=0; i < allInLines.size(); i++) {
			String currLine = allInLines.get(i);
			if ( currLine.contains("MAX")) {
				currLine = currLine.replace("MAX", "DEFAULT");
			}
			outlines.append(currLine);
		}
		Files.writeString(metsFile, outlines.toString());

		// act
		String[] args = { metsFile.toString() };
		App.main(args);

		// assert
		Path pathPdf = pathTarget.resolve("191092622.pdf");
		assertTrue(Files.exists(pathPdf));
	}

	/**
	 * 
	 * Behavor using common 2step ULB configuration
	 * using "MAX" images, not "DEFAULT"
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testWithSampleConfigurationFile(@TempDir Path tempDir) throws Exception {

		// arrange
		Path pathTarget = TestHelper.fixturePrint737429(tempDir, IDerivans.IMAGE_DIR_MAX);
		
		Path configTempDir = tempDir.resolve("config");
		TestHelper.copyTree(TestResource.CONFIG_RES_DIR.get(), configTempDir);
		Path configTemp = configTempDir.resolve(TestResource.CONFIG_ULB_ODEM.get().getFileName());

		// act
		String[] args = { pathTarget.resolve("737429.xml").toString(), "-c", configTemp.toString() };
		App.main(args);

		// assert
		Path pathPdf = pathTarget.resolve("191092622.pdf");
		assertTrue(Files.exists(pathPdf));
	}
}
