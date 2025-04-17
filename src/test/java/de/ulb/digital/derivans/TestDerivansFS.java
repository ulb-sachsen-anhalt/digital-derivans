package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.config.DerivansParameter;

/**
 * 
 * @author hartwig
 *
 */
class TestDerivansFS {

	/**
	 * 
	 * Start images set flat in start directory
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testDerivatesFSEnforceFlat(@TempDir Path tempDir) throws Exception {

		// arrange
		Path pathTarget = tempDir.resolve("only_images");
		TestHelper.generateImages(pathTarget, 300, 420, 4, "%04d.jpg");

		// act
		DerivansParameter dp = new DerivansParameter();
		dp.setImages(".");
		dp.setPathInput(pathTarget);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);
		derivans.create(pathTarget);

		// assert
		Path pdfWritten = pathTarget.resolve("only_images.pdf");
		assertTrue(Files.exists(pdfWritten));
	}

	/**
	 * 
	 * Start images one level below actual start directory
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testDerivatesFSDefaultsToMAXPath(@TempDir Path tempDir) throws Exception {

		// arrange
		Path pathTarget = tempDir.resolve("only_images");
		Path pathImageMax = pathTarget.resolve("MAX");
		Files.createDirectories(pathImageMax);
		TestHelper.generateImages(pathImageMax, 300, 420, 4, "%04d.jpg");

		// act
		DerivansParameter dp = new DerivansParameter();
		dp.setPathInput(pathTarget);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);
		derivans.create(pathTarget);

		// assert
		Path pdfWritten = pathTarget.resolve("only_images.pdf");
		assertTrue(Files.exists(pdfWritten));
	}

	/**
	 * 
	 * Start images one level below actual start directory
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testDerivatesFSDefaultsToDEFAULTPath(@TempDir Path tempDir) throws Exception {

		// arrange
		Path pathTarget = tempDir.resolve("only_images");
		Path pathImageMax = pathTarget.resolve("DEFAULT");
		Files.createDirectories(pathImageMax);
		TestHelper.generateImages(pathImageMax, 300, 420, 4, "%04d.jpg");

		// act
		DerivansParameter dp = new DerivansParameter();
		dp.setPathInput(pathTarget);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);
		derivans.create(pathTarget);

		// assert
		Path pdfWritten = pathTarget.resolve("only_images.pdf");
		assertTrue(Files.exists(pdfWritten));
	}

	
	@Test
	void testDerivatesFSFiveSteps(@TempDir Path tempDir) throws Exception {

		// arrange
		Path configSourceDir = Path.of("src/test/resources/config");
		Path configTargetDir = tempDir.resolve("config");
		Files.createDirectories(configTargetDir);
		Path testConfig = configSourceDir.resolve("derivans-5steps.ini");
		Files.copy(testConfig, configTargetDir.resolve("derivans-5steps.ini"));
		DerivansParameter dp = new DerivansParameter();
		dp.setPathConfig(testConfig);
		Path pathTarget = tempDir.resolve("only_images");
		Path pathImageMax = pathTarget.resolve("MAX");
		Files.createDirectories(pathImageMax);
		TestHelper.generateImages(pathImageMax, 300, 420, 4, "%04d.jpg");

		// act
		dp.setPathInput(pathTarget);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);
		derivans.create(pathTarget);

		// assert
		Path pdfWritten = pathTarget.resolve("only_images.pdf");
		assertTrue(Files.exists(pdfWritten));
	}

	/**
	 * 
	 * Behavior if OCR data present in sub directory 'ALTO3' and 
	 * images in directory 'ORIGINAL'
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testConfigCustomWithImagesAndPartialOCR(@TempDir Path tempDir) throws Exception {

		// arrange
		Path configSourceDir = Path.of("src/test/resources/config");
		Path configTargetDir = tempDir.resolve("config");
		if (Files.exists(configTargetDir)) {
			Files.walk(configTargetDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(configTargetDir);
		}
		Files.createDirectories(configTargetDir);
		Path testConfig = configSourceDir.resolve("derivans-custom.ini");
		Files.copy(testConfig, configTargetDir.resolve("derivans-custom.ini"));
		DerivansParameter dp = new DerivansParameter();
		dp.setPathConfig(testConfig);
		Path pathTarget = tempDir.resolve("16359604");
		dp.setPathInput(pathTarget);
		Path sourceImageDir = Path.of("src/test/resources/16359604");
		TestHelper.copyTree(sourceImageDir, pathTarget);
		// create artificial "ORIGINAL" testimages
		Path imageOriginal = pathTarget.resolve("ORIGINAL");
		List<String> ids = IntStream.range(4, 8).mapToObj(i -> String.format("163310%02d", i))
				.collect(Collectors.toList());
		// these are the small dimensions a newspaper page
		// shall shrink to which was originally 7000x10000
		TestHelper.generateJpgsFromList(imageOriginal, 1400, 2000, ids);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.create(pathTarget);

		// assert
		String pdfName = "16359604.pdf";
		Path pdfWritten = pathTarget.resolve(pdfName);
		assertTrue(Files.exists(pdfWritten));
	}

	/**
	 * 
	 * Behavior if ULB config, but images are in group 'ORIGINAL' (rather
	 * kitodo.presentation like) for VL ID 16359604
	 * 
	 * => Ensure, overwriting config via CLI works!
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	// @Test
	// void testConfigULBOverwriteImageGroup(@TempDir Path tempDir) throws Exception
	// {

	// // arrange
	// Path configSourceDir = Path.of("src/test/resources/config");
	// Path configTargetDir = tempDir.resolve("config");
	// if (Files.exists(configTargetDir)) {
	// Files.walk(configTargetDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
	// Files.delete(configTargetDir);
	// }
	// Files.createDirectories(configTargetDir);
	// Path testConfig = configSourceDir.resolve("derivans.ini");
	// Files.copy(testConfig, configTargetDir.resolve("derivans.ini"));
	// DerivansParameter dp = new DerivansParameter();
	// dp.setPathConfig(testConfig);
	// // this is the mandatory point
	// dp.setImages("ORIGINAL");
	// Path pathTarget = tempDir.resolve("16359604");
	// dp.setPathInput(pathTarget.resolve("16359604.mets.xml"));
	// Path sourceImageDir = Path.of("src/test/resources/16359604");
	// TestHelper.copyTree(sourceImageDir, pathTarget);
	// // create artificial "ORIGINAL" testimages
	// Path imageOriginal = pathTarget.resolve("ORIGINAL");
	// List<String> ids = IntStream.range(5, 13)
	// .mapToObj(i -> String.format("163310%02d", i)).collect(Collectors.toList());
	// // these are the least dimensions a newspaper page
	// // shall shrink to which was originally 7000x10000
	// TestHelper.generateJpgsFromList(imageOriginal, 700, 1000, ids);
	// DerivansConfiguration dc = new DerivansConfiguration(dp);
	// ((DerivateStepPDF)
	// dc.getDerivateSteps().get(2)).setModsIdentifierXPath("//mods:title");
	// Derivans derivans = new Derivans(dc);

	// // act
	// derivans.create();

	// // assert
	// String pdfName = "General-Anzeiger_f\u00FCr_Halle_und_den_Saalkreis.pdf";
	// Path pdfWritten = pathTarget.resolve(pdfName);
	// assertTrue(Files.exists(pdfWritten));
	// }

}
