package de.ulb.digital.derivans.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
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

import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.IDerivans;
import de.ulb.digital.derivans.model.step.DerivateStep;
import de.ulb.digital.derivans.model.step.DerivateStepImage;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;
import de.ulb.digital.derivans.model.step.DerivateType;

/**
 * 
 * Test specification for different
 * Derivans Configurations
 * 
 * @author hartwig
 *
 */
public class TestDerivansConfiguration {

	private static final String ORIGINAL_SUB_DIR = "ORIGINAL";

	/**
	 * 
	 * Test minimal configuration to produce
	 * PDF with metadata file present
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testMinimalPDFConfigurationWithMETS(@TempDir Path tempDir) throws Exception {

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
		assertEquals(2, steps.size());

		// min derivate
		assertTrue(steps.get(0) instanceof DerivateStepImage);
		DerivateStepImage stepMinImage = (DerivateStepImage) steps.get(0);
		assertEquals(80, stepMinImage.getQuality());
		assertEquals("DEFAULT", stepMinImage.getInputDir());
		assertEquals("IMAGE_80", stepMinImage.getOutputDir());
		assertEquals(DerivateType.JPG, stepMinImage.getOutputType());

		// pdf
		assertTrue(steps.get(1) instanceof DerivateStepPDF);
		DerivateStepPDF stepPdf = (DerivateStepPDF) steps.get(1);
		assertEquals(DerivateType.PDF, stepPdf.getOutputType());
		assertTrue(stepPdf.isEnrichMetadata());
	}

	/**
	 * 
	 * Common configuration with metadata present
	 * and PDF enrichment set to "false", i.e.
	 * no insertions into METS
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testConfigurationWithDisabledPDFEnrichment(@TempDir Path tempDir) throws Exception {

		// arrange
		Path targetMetsDir = tempDir.resolve("226134857");
		Files.createDirectory(targetMetsDir);
		Path metsModsTarget = targetMetsDir.resolve("226134857.xml");
		Files.copy(TestResource.HD_Aa_226134857_LEGACY.get(), metsModsTarget);
		Path conf = Path.of("src/test/resources/config/derivans-pdf.ini");
		DerivansParameter dp = new DerivansParameter();
		dp.setPathConfig(conf);
		dp.setPathInput(metsModsTarget);

		// act
		DerivansConfiguration dc = new DerivansConfiguration(dp);

		// assert
		assertEquals(80, dc.getQuality());
		assertEquals(4, dc.getPoolsize());
		List<DerivateStep> steps = dc.getDerivateSteps();
		assertEquals(3, steps.size());
		assertTrue(steps.get(2) instanceof DerivateStepPDF);
		var stepPDF = (DerivateStepPDF) steps.get(2);
		assertEquals(DerivateType.PDF, stepPDF.getOutputType());
		assertFalse(stepPDF.isEnrichMetadata());
	}

	/**
	 * 
	 * Default/Fallback configuration without
	 * metadata but default image sub dir (DEFAULT)
	 * and PDF enriched into METS
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testDefaultLocalConfiguration(@TempDir Path tempDir) throws Exception {

		// arrange
		var imgDir = IDerivans.IMAGE_DIR_DEFAULT;
		Path pathInput = tempDir.resolve("default_local");
		Path pathImageDir = pathInput.resolve(imgDir);
		Files.createDirectories(pathImageDir);
		TestHelper.generateImages(pathImageDir, 620, 877, 6, "%04d.jpg");
		var params = new DerivansParameter();
		params.setPathInput(pathInput);

		// act
		DerivansConfiguration dc = new DerivansConfiguration(params);

		// assert
		assertEquals(80, dc.getQuality());
		assertEquals(2, dc.getPoolsize());

		List<DerivateStep> steps = dc.getDerivateSteps();
		assertEquals(2, steps.size());

		// minimal derivate from images
		// assertEquals(80, steps.get(0).getQuality());
		assertEquals(IDerivans.IMAGE_DIR_DEFAULT, steps.get(0).getInputDir());
		assertEquals("IMAGE_80", steps.get(0).getOutputDir());
		assertEquals(DerivateType.JPG, steps.get(0).getOutputType());

		// pdf
		assertEquals(DerivateType.PDF, steps.get(1).getOutputType());
		assertTrue(((DerivateStepPDF) steps.get(1)).isEnrichMetadata());
	}

	/**
	 * 
	 * Test behavior if image sub directory
	 * was set as relative path
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testConfigurationRelativeImageDir(@TempDir Path tempDir) throws Exception {
		// arrange
		var customImageSubDir = ORIGINAL_SUB_DIR;
		Path pathInput = tempDir.resolve("default_local");
		Path pathImageMax = pathInput.resolve(customImageSubDir);
		Files.createDirectories(pathImageMax);
		TestHelper.generateImages(pathImageMax, 620, 877, 6, "%04d.jpg");
		var params = new DerivansParameter();
		params.setPathInput(pathInput);
		params.setImages(customImageSubDir);

		// act
		DerivansConfiguration dc = new DerivansConfiguration(params);

		// assert
		List<DerivateStep> steps = dc.getDerivateSteps();
		assertEquals(2, steps.size());

		// minimal derivate from images
		// assertEquals(80, steps.get(0).getQuality());
		assertEquals(customImageSubDir, steps.get(0).getInputDir());
		assertEquals("IMAGE_80", steps.get(0).getOutputDir());
		assertEquals(DerivateType.JPG, steps.get(0).getOutputType());

		// pdf
		assertEquals(DerivateType.PDF, steps.get(1).getOutputType());
	}

	/**
	 * 
	 * Test behavior if image sub directory
	 * was set as absolute path which is *not*
	 * sub dir of pathInput
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testConfigurationAbsoluteImageDir(@TempDir Path tempDir) throws Exception {
		// arrange
		var customImageSubDir = "MY_WAY";
		Path pathInput = tempDir.resolve("custom_images_local");
		Files.createDirectory(pathInput);
		Path pathImageMax = tempDir.resolve(customImageSubDir);
		Files.createDirectories(pathImageMax);
		// TestHelper.generateImages(pathImageMax, 620, 877, 6, "%04d.jpg");
		var params = new DerivansParameter();
		params.setPathInput(pathInput);
		params.setImages(pathImageMax.toString());

		// act
		DerivansConfiguration dc = new DerivansConfiguration(params);

		// assert
		List<DerivateStep> steps = dc.getDerivateSteps();
		assertEquals(2, steps.size());

		// minimal derivate from images
		assertEquals(80, ((DerivateStepImage)steps.get(0)).getQuality());
		assertEquals(pathImageMax.toString(), steps.get(0).getInputDir());
		assertEquals("IMAGE_80", steps.get(0).getOutputDir());
		assertEquals(DerivateType.JPG, steps.get(0).getOutputType());

		// pdf
		assertEquals(DerivateType.PDF, steps.get(1).getOutputType());
	}

	/**
	 * 
	 * Behavior if ULB config, but images are in group
	 * 'ORIGINAL' (rather kitodo.presentation like)
	 * for VL ID 16359604
	 * 
	 * => Ensure, overwriting config via CLI works!
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testConfigULBOverwriteImageGroup(@TempDir Path tempDir) throws Exception {

		// arrange
		Path configSourceDir = Path.of("src/test/resources/config");
		Path configTargetDir = tempDir.resolve("config");
		if (Files.exists(configTargetDir)) {
			Files.walk(configTargetDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(configTargetDir);
		}
		Files.createDirectories(configTargetDir);
		Path testConfig = configSourceDir.resolve("derivans_ulb_odem.ini");
		Files.copy(testConfig, configTargetDir.resolve("derivans.ini"));
		DerivansParameter dp = new DerivansParameter();
		dp.setPathConfig(testConfig);
		// this is the mandatory point
		dp.setImages(ORIGINAL_SUB_DIR);
		Path pathTarget = tempDir.resolve("16359604");
		dp.setPathInput(pathTarget.resolve("16359604.mets.xml"));
		Path sourceImageDir = Path.of("src/test/resources/16359604");
		TestHelper.copyTree(sourceImageDir, pathTarget);
		// create artificial "ORIGINAL" testimages
		// Path imageDirOriginal = pathTarget.resolve(ORIGINAL_SUB_DIR);
		// List<String> ids = IntStream.range(5, 13)
		// 		.mapToObj(i -> String.format("163310%02d", i)).collect(Collectors.toList());
		// // these are the least dimensions a newspaper page
		// // shall shrink to which was originally 7000x10000
		// TestHelper.generateJpgsFromList(imageDirOriginal, 700, 1000, ids);

		// act
		DerivansConfiguration dc = new DerivansConfiguration(dp);

		// assert
		var dSteps = dc.getDerivateSteps();
		assertEquals(3, dSteps.size());
		assertSame(DerivateType.PDF, dSteps.get(2).getOutputType());
		DerivateStepPDF pdfStep = (DerivateStepPDF) dSteps.get(2);
		assertEquals(ORIGINAL_SUB_DIR, pdfStep.getParamImages());
		assertEquals(ORIGINAL_SUB_DIR, dSteps.get(0).getInputDir());
	}

	/**
	 * Behavior if no explicite config => 2 fallback steps
	 * just to generate a PDF from an image input group
	 * 
	 * If now images are in different group
	 * 'ORIGINAL' instead of default 'MAX',
	 * ensure, overwriting fallback via CLI works too!
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testFallbackButOverwriteImageGroup(@TempDir Path tempDir) throws Exception {

		// arrange
		DerivansParameter dp = new DerivansParameter();
		// this is the mandatory point
		dp.setImages(ORIGINAL_SUB_DIR);
		Path pathTarget = tempDir.resolve("16359604");
		dp.setPathInput(pathTarget.resolve("16359604.mets.xml"));
		Path sourceImageDir = Path.of("src/test/resources/16359604");
		TestHelper.copyTree(sourceImageDir, pathTarget);
		// create artificial "ORIGINAL" testimages
		Path imageDirOriginal = pathTarget.resolve(ORIGINAL_SUB_DIR);
		List<String> ids = IntStream.range(5, 13)
				.mapToObj(i -> String.format("163310%02d", i)).collect(Collectors.toList());
		// these are the least dimensions a newspaper page
		// shall shrink to which was originally 7000x10000
		TestHelper.generateJpgsFromList(imageDirOriginal, 700, 1000, ids);
		// Derivans derivans = new Derivans(dc);

		// act
		DerivansConfiguration dc = new DerivansConfiguration(dp);

		// assert
		var dSteps = dc.getDerivateSteps();
		assertEquals(2, dSteps.size());
		assertEquals(ORIGINAL_SUB_DIR, dSteps.get(0).getInputDir());
		assertSame(DerivateType.PDF, dSteps.get(1).getOutputType());
		DerivateStepPDF pdfStep = (DerivateStepPDF) dSteps.get(1);
		assertEquals("IMAGE_80", pdfStep.getInputDir());
		assertEquals(ORIGINAL_SUB_DIR, pdfStep.getParamImages());
		assertEquals(ORIGINAL_SUB_DIR, dSteps.get(0).getInputDir());
	}
}
