package de.ulb.digital.derivans.generate.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.DigitalDerivansRuntimeException;
import de.ulb.digital.derivans.IDerivans;
import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.generate.GeneratorImageJPG;
import de.ulb.digital.derivans.model.DerivateFS;
import de.ulb.digital.derivans.model.IDerivate;
import de.ulb.digital.derivans.model.step.DerivateStepImage;

/**
 * 
 * Test Specification for {@link ImageDerivatHandler}
 * 
 * @author hartwig
 *
 */
class TestImageGenerator {

	@TempDir
	static Path sharedTempDir;

	static IDerivate testDerivate;

	@BeforeAll
	static void setupBeforeClass() throws IOException, DigitalDerivansException {
		Path imageDir = TestImageGenerator.sharedTempDir.resolve(TestHelper.IMAGE);
		int width = 450;
		int height = 700;
		int number = 4;
		TestHelper.generateImages(imageDir, width, height, number, "%04d.jpg");
		TestImageGenerator.testDerivate = new DerivateFS(sharedTempDir);
		TestImageGenerator.testDerivate.init(Path.of(TestHelper.IMAGE));
	}

	@Test
	void testRenderer80() throws DigitalDerivansException, IOException {
		// arrange
		Path targetPath = TestImageGenerator.sharedTempDir.resolve(IDerivans.IMAGE_Q80);
		DerivateStepImage imageStep = new DerivateStepImage();
		imageStep.setQuality(80);
		imageStep.setInputDir(TestHelper.IMAGE);
		imageStep.setOutputDir(IDerivans.IMAGE_Q80);
		GeneratorImageJPG imgGen = new GeneratorImageJPG();
		imgGen.setDerivate(TestImageGenerator.testDerivate);
		imgGen.setStep(imageStep);

		// act
		int outcome = imgGen.create();

		// assert
		assertEquals(4, outcome);
		Files.list(targetPath).forEach(p -> p.toFile().exists());
	}

	@Test
	void testRenderer70() throws DigitalDerivansException, IOException {
		// arrange
		Path targetPath = TestImageGenerator.sharedTempDir.resolve("image_70");
		Files.createDirectory(targetPath); // mandatory, done in production by Basetype
	
		DerivateStepImage imageStep = new DerivateStepImage();
		imageStep.setInputDir(TestHelper.IMAGE);
		imageStep.setOutputDir("image_70");
		imageStep.setQuality(70);
		GeneratorImageJPG imgGen = new GeneratorImageJPG();
		imgGen.setDerivate(TestImageGenerator.testDerivate);
		imgGen.setStep(imageStep);

		// act
		int outcome = imgGen.create();

		// assert
		assertEquals(4, outcome);
		Files.list(targetPath).forEach(p -> p.toFile().exists());
		List<Path> paths = Files.list(targetPath).collect(Collectors.toList());
		assertEquals(4, paths.size());
		for (Path p : paths) {
			assertTrue(Files.exists(p));
		}
	}

	@Test
	void testRenderer70Maximal1000() throws DigitalDerivansException, IOException {
		// arrange
		Path targetPath = sharedTempDir.resolve("DEFAULT");
		Files.createDirectory(targetPath); // mandatory, done in production by BaseDerivateer

		DerivateStepImage imageStep = new DerivateStepImage();
		imageStep.setMaximal(256);
		imageStep.setInputDir(TestHelper.IMAGE); // critical if missing
		imageStep.setOutputPrefix("BUNDLE_BRANDED_PREVIEW__");
		imageStep.setOutputDir("DEFAULT");
		GeneratorImageJPG derivateer = new GeneratorImageJPG();
		derivateer.setDerivate(TestImageGenerator.testDerivate);
		derivateer.setStep(imageStep);

		// act
		int outcome = derivateer.create();

		// assert
		assertEquals(4, outcome);
		List<Path> paths = Files.list(targetPath).collect(Collectors.toList());
		assertEquals(4, paths.size());
		for (Path p : paths) {
			assertTrue(Files.exists(p));
			BufferedImage bi = ImageIO.read(p.toFile());
			assertEquals(256, bi.getHeight());
			assertEquals(164, bi.getWidth());
			assertTrue(p.toString().contains("BUNDLE_BRANDED"));
		}
	}

	/**
	 * 
	 * Ensure that cascading works as expected, i.e. prefixes are being
	 * handled properly between different derivate steps
	 * 
	 * @throws DigitalDerivansException
	 * @throws IOException
	 */
	@Test
	void testRendererCascadingwithAttributes() throws DigitalDerivansException, IOException {
		// arrange
		Path targetPath = sharedTempDir.resolve("PREVIEW");
		Path finalPath = sharedTempDir.resolve("FINAL");
		Files.createDirectory(targetPath);
		Files.createDirectory(finalPath);

		DerivateStepImage previewStep = new DerivateStepImage();
		previewStep.setInputDir(TestHelper.IMAGE);
		previewStep.setOutputDir("PREVIEW");
		previewStep.setQuality(50);
		previewStep.setMaximal(256);
		previewStep.setOutputPrefix("BUNDLE_BRANDED_PREVIEW__");
		GeneratorImageJPG previewGen = new GeneratorImageJPG();
		previewGen.setDerivate(TestImageGenerator.testDerivate);
		previewGen.setStep(previewStep);

		DerivateStepImage humbleStep = new DerivateStepImage();
		humbleStep.setInputPrefix("BUNDLE_BRANDED_PREVIEW__");
		humbleStep.setOutputPrefix("BUNDLE_HUMBLE__");
		humbleStep.setInputDir("PREVIEW");
		humbleStep.setOutputDir("FINAL");
		humbleStep.setMaximal(128);
		humbleStep.setQuality(50);
		GeneratorImageJPG finalGen = new GeneratorImageJPG();
		finalGen.setDerivate(TestImageGenerator.testDerivate);
		finalGen.setStep(humbleStep);

		// act
		int nPreviews = previewGen.create();
		int nFinals = finalGen.create();

		// assert
		assertEquals(4, nFinals);
		assertEquals(nPreviews, nFinals);
		List<Path> paths = Files.list(finalPath).sorted().collect(Collectors.toList());
		assertEquals(4, paths.size());
		for (int i = 0; i < paths.size(); i++) {
			Path p = paths.get(i);
			assertTrue(Files.exists(p));
			BufferedImage bi = ImageIO.read(p.toFile());
			assertEquals(128, bi.getHeight());
			String fileName = p.getFileName().toString();
			assertEquals(fileName, String.format("BUNDLE_HUMBLE__000%d.jpg", i + 1));
		}
	}

	/**
	 * 
	 * Check behavor if no output dir parameter provided
	 * Actually the output check preceedes reading of input
	 * 
	 */
	@Test
	void testBehavorMissingOutput() {
		DerivateStepImage incompleteStep01 = new DerivateStepImage();
		GeneratorImageJPG invalidGen = new GeneratorImageJPG();
		invalidGen.setDerivate(TestImageGenerator.testDerivate);
		invalidGen.setStep(incompleteStep01);

		// act
		var exc = assertThrows(DigitalDerivansRuntimeException.class, invalidGen::create);

		// assert 
		assertEquals("No outputDir: null!", exc.getMessage());
	}

	/**
	 * 
	 * Check behavor if no input dir parameter present
	 * 
	 * @throws IOException
	 */
	@Test
	void testBehavorMissingInput() throws IOException {
		Path path = sharedTempDir.resolve("MISSING_INPUT");
		Files.createDirectory(path);
		DerivateStepImage incompleteStep01 = new DerivateStepImage();
		incompleteStep01.setOutputDir("MISSING_INPUT");
		GeneratorImageJPG invalidGen = new GeneratorImageJPG();
		invalidGen.setDerivate(TestImageGenerator.testDerivate);
		invalidGen.setStep(incompleteStep01);

		// act
		var exc = assertThrows(DigitalDerivansRuntimeException.class, invalidGen::create);

		// assert 
		assertEquals("No inputDir: null!", exc.getMessage());
	}
}
