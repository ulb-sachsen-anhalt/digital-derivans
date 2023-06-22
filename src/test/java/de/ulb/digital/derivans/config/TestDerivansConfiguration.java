package de.ulb.digital.derivans.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.DerivansParameter;
import de.ulb.digital.derivans.TestDerivans;
import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.model.step.DerivateStep;
import de.ulb.digital.derivans.model.step.DerivateStepImage;
import de.ulb.digital.derivans.model.step.DerivateStepImageFooter;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;
import de.ulb.digital.derivans.model.step.DerivateType;

/**
 * 
 * @author hartwig
 *
 */
public class TestDerivansConfiguration {

	/**
	 * 
	 * Common configuration with metadata present
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testCommonConfigurationWithMETS(@TempDir Path tempDir) throws Exception {

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
		assertTrue(steps.get(0) instanceof DerivateStepImageFooter);
		DerivateStepImageFooter stepFooter = (DerivateStepImageFooter)steps.get(0);
		assertEquals("jpg", stepFooter.getOutputType());
		assertEquals(95, stepFooter.getQuality());
		assertEquals("Universitäts- und Landesbibliothek Sachsen-Anhalt", stepFooter.getFooterLabel());
		assertEquals("src/test/resources/config/footer_template.png", stepFooter.getPathTemplate().toString());
		assertEquals(DerivateType.JPG_FOOTER, steps.get(0).getDerivateType());

		// min derivate from footer
		assertTrue(steps.get(1) instanceof DerivateStepImage);
		DerivateStepImage stepMinImage = (DerivateStepImage) steps.get(1);
		assertEquals("jpg", stepMinImage.getOutputType());
		assertEquals(80, stepMinImage.getQuality());
		assertEquals("IMAGE_FOOTER", stepMinImage.getInputPath().getFileName().toString());
		assertEquals("IMAGE_80", stepMinImage.getOutputPath().getFileName().toString());
		assertEquals(DerivateType.JPG, stepMinImage.getDerivateType());

		// pdf
		assertTrue(steps.get(2) instanceof DerivateStepPDF);
		DerivateStepPDF stepPdf = (DerivateStepPDF) steps.get(2);
		assertEquals("pdf", stepPdf.getOutputType());
		assertEquals(DerivateType.PDF, stepPdf.getDerivateType());
		assertTrue(stepPdf.isEnrichMetadata());

		// additional assets
		assertTrue(steps.get(5) instanceof DerivateStepImage);
		DerivateStepImage stepPreviewImage = (DerivateStepImage) steps.get(5);
		assertEquals("BUNDLE_BRANDED_PREVIEW__", stepPreviewImage.getOutputPrefix());
		assertEquals(1000,stepPreviewImage.getMaximal());
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
		assertEquals("pdf", stepPDF.getOutputType());
		assertEquals(DerivateType.PDF, stepPDF.getDerivateType());
		assertFalse(stepPDF.isEnrichMetadata());
	}


	/**
	 * 
	 * Default/Fallback configuration without 
	 * metadata but default image sub dir (MAX)
	 * and PDF introduced into METS
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testDefaultLocalConfiguration(@TempDir Path tempDir) throws Exception {

		// arrange
		var imgDir = DefaultConfiguration.DEFAULT_PATH_INPUT_IMAGES;
		Path pathInput = tempDir.resolve("default_local");
		Path pathImageMax = pathInput.resolve(imgDir);
		Files.createDirectories(pathImageMax);
		TestDerivans.generateJpgs(pathImageMax, 620, 877, 6);
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
		assertEquals("jpg", steps.get(0).getOutputType());
		// assertEquals(80, steps.get(0).getQuality());
		assertEquals("MAX", steps.get(0).getInputPath().getFileName().toString());
		assertEquals("IMAGE_80", steps.get(0).getOutputPath().getFileName().toString());
		assertEquals(DerivateType.JPG, steps.get(0).getDerivateType());

		// pdf
		assertEquals("pdf", steps.get(1).getOutputType());
		assertEquals(DerivateType.PDF, steps.get(1).getDerivateType());
		assertTrue(((DerivateStepPDF)steps.get(1)).isEnrichMetadata());
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
		var customImageSubDir = "ORIGINAL";
		Path pathInput = tempDir.resolve("default_local");
		Path pathImageMax = pathInput.resolve(customImageSubDir);
		Files.createDirectories(pathImageMax);
		TestDerivans.generateJpgs(pathImageMax, 620, 877, 6);
		var params = new DerivansParameter();
		params.setPathInput(pathInput);
		params.setPathDirImages(Path.of(customImageSubDir));

		// act
		DerivansConfiguration dc = new DerivansConfiguration(params);

		// assert
		List<DerivateStep> steps = dc.getDerivateSteps();
		assertEquals(2, steps.size());

		// minimal derivate from images
		assertEquals("jpg", steps.get(0).getOutputType());
		// assertEquals(80, steps.get(0).getQuality());
		assertEquals(customImageSubDir, steps.get(0).getInputPath().getFileName().toString());
		assertEquals("IMAGE_80", steps.get(0).getOutputPath().getFileName().toString());
		assertEquals(DerivateType.JPG, steps.get(0).getDerivateType());

		// pdf
		assertEquals("pdf", steps.get(1).getOutputType());
		assertEquals(DerivateType.PDF, steps.get(1).getDerivateType());
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
		TestDerivans.generateJpgs(pathImageMax, 620, 877, 6);
		var params = new DerivansParameter();
		params.setPathInput(pathInput);
		params.setPathDirImages(pathImageMax);

		// act
		DerivansConfiguration dc = new DerivansConfiguration(params);

		// assert
		List<DerivateStep> steps = dc.getDerivateSteps();
		assertEquals(2, steps.size());

		// minimal derivate from images
		assertEquals("jpg", steps.get(0).getOutputType());
		// assertEquals(80, steps.get(0).getQuality());
		assertEquals(customImageSubDir, steps.get(0).getInputPath().getFileName().toString());
		assertEquals("IMAGE_80", steps.get(0).getOutputPath().getFileName().toString());
		assertEquals(DerivateType.JPG, steps.get(0).getDerivateType());

		// pdf
		assertEquals("pdf", steps.get(1).getOutputType());
		assertEquals(DerivateType.PDF, steps.get(1).getDerivateType());
	}

}
