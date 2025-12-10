package de.ulb.digital.derivans.generate.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.generate.Generator;
import de.ulb.digital.derivans.generate.GeneratorImageJPGFooter;
import de.ulb.digital.derivans.model.DerivateFS;
import de.ulb.digital.derivans.model.DerivateStruct;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.DigitalType;
import de.ulb.digital.derivans.model.IDerivate;
import de.ulb.digital.derivans.model.step.DerivateStepImageFooter;

/**
 * 
 * Test Specification for {@link GeneratorImageJPGFooter}
 * 
 * @author u.hartwig
 *
 */
class TestImageGeneratorFooter {

	private static int height = 5500;

	private static IDerivate testDerivate;

	@TempDir
	static Path sharedTempDir;

	static Path sourcePath;

	static Path targetPath;

	@BeforeAll
	static void setupBeforeClass() throws IOException, DigitalDerivansException {
		Path defaultMaxDir = sharedTempDir.resolve("IMAGE");
		Files.createDirectory(defaultMaxDir);

		for (int i = 1; i < 4; i++) {
			Path jpgFile = defaultMaxDir.resolve("000" + i + ".jpg");
			BufferedImage bi2 = new BufferedImage(250, 375, BufferedImage.TYPE_3BYTE_BGR);
			ImageIO.write(bi2, "JPG", jpgFile.toFile());
		}
		sourcePath = sharedTempDir.resolve(TestHelper.IMAGE);
		targetPath = sharedTempDir.resolve("IMAGE_FOOTER_GRANULAR");
		Files.createDirectory(targetPath);
		TestImageGeneratorFooter.testDerivate = new DerivateFS(sharedTempDir);
		testDerivate.init(Path.of(TestHelper.IMAGE));
	}

	@Test
	void testRendererMediumFooter(@TempDir Path tempDir) throws DigitalDerivansException, IOException {
		// arrange
		Path inputImageDir = tempDir.resolve("IMAGE1");
		Files.createDirectory(inputImageDir);
		for (int i = 1; i < 3; i++) {
			Path jpgFile = inputImageDir.resolve("000" + i + ".jpg");
			BufferedImage bi2 = new BufferedImage(3500, height, BufferedImage.TYPE_3BYTE_BGR);
			ImageIO.write(bi2, "JPG", jpgFile.toFile());
		}
		IDerivate derivateFS = new DerivateFS(tempDir);
		derivateFS.init(Path.of("IMAGE1"));
		assertEquals(2, derivateFS.allPagesSorted().size());
		Path trgPath = tempDir.resolve("IMAGE_FOOTER1");
		Files.createDirectory(trgPath);

		String templatePath = "src/test/resources/config/footer_template.png";
		Path source = Paths.get(templatePath).toAbsolutePath();
		Path targetDir = tempDir.resolve("footer");
		Files.createDirectories(targetDir);
		Path footerTarget = targetDir.resolve("footer_template.png");
		Files.copy(source, footerTarget, StandardCopyOption.REPLACE_EXISTING);
		assertTrue(Files.exists(footerTarget));

		DerivateStepImageFooter footerStep = new DerivateStepImageFooter("IMAGE1", "IMAGE_FOOTER1");
		footerStep.setQuality(95);
		footerStep.setFooterLabel("Testlabel footer regular");
		footerStep.setPathTemplate(footerTarget);

		Generator footerGen = new GeneratorImageJPGFooter();
		footerGen.setDerivate(derivateFS);
		footerGen.setStep(footerStep);

		// act
		int outcome = footerGen.create();

		// assert
		assertEquals(2, outcome);
		List<Path> resultPaths = Files.list(trgPath).collect(Collectors.toList());
		for (Path p : resultPaths) {
			assertTrue(p.toFile().exists());
			byte[] bytes = Files.readAllBytes(p);
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
			assertNotEquals(height, image.getHeight());
			assertEquals(5675, image.getHeight());
		}
	}

	@Test
	void testRendererLargeFooter(@TempDir Path tempDir) throws DigitalDerivansException, IOException {
		// arrange
		Path imagePath = tempDir.resolve("IMAGE2");
		Path dstPath = tempDir.resolve("IMAGE_FOOTER2");
		Files.createDirectory(imagePath);
		String resPath = "src/test/resources/images/1667522809_J_0025_0001.jpg";
		Path sourceImage = Paths.get(resPath).toAbsolutePath();
		Files.copy(sourceImage, imagePath.resolve(Path.of(resPath).getFileName()),
				StandardCopyOption.REPLACE_EXISTING);
		String templatePath = "src/test/resources/config/footer_template.png";
		Path tplSourcePath = Paths.get(templatePath).toAbsolutePath();
		Path targetDir = tempDir.resolve("footer");
		Files.createDirectories(targetDir);
		Path tplTarget = targetDir.resolve("footer_template.png");
		Files.copy(tplSourcePath, tplTarget, StandardCopyOption.REPLACE_EXISTING);
		DerivateStepImageFooter footerStep = new DerivateStepImageFooter("IMAGE2", "IMAGE_FOOTER2");
		footerStep.setFooterLabel("Newspaper Footer");
		footerStep.setPathTemplate(tplTarget);

		DerivateFS dFs = new DerivateFS(sharedTempDir);
		dFs.init(imagePath);
		Generator gen = new GeneratorImageJPGFooter();
		gen.setDerivate(dFs);
		gen.setStep(footerStep);

		// act
		int outcome = gen.create();

		// assert
		assertEquals(1, outcome);
		List<Path> resultPaths = Files.list(dstPath).collect(Collectors.toList());
		for (Path p : resultPaths) {
			assertTrue(p.toFile().exists());
			byte[] bytes = Files.readAllBytes(p);
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
			assertNotEquals(height, image.getHeight());
			assertEquals(11167, image.getHeight());
		}
	}

	/**
	 * 
	 * Testdata with invalid TIF Metadata
	 * 
	 * @param tempDir
	 * @throws DigitalDerivansException
	 * @throws IOException
	 */
	@Test
	void testRendererFooterTIF(@TempDir Path tempDir) throws DigitalDerivansException, IOException {
		// arrange
		String img = "src/test/resources/images/3900_00000010x768.tif";
		Path imgSource = Paths.get(img).toAbsolutePath();
		Path targetPath1 = tempDir.resolve("IMAGE3");
		Files.createDirectory(targetPath1);
		Path imgPath = targetPath1.resolve("00000010.tif");
		Path targetPath2 = tempDir.resolve("IMAGE_FOOTER3");
		Files.copy(imgSource, imgPath, StandardCopyOption.REPLACE_EXISTING);
		String templatePath = "src/test/resources/config/footer_template.png";
		Path source = Paths.get(templatePath).toAbsolutePath();
		Path targetDir = tempDir.resolve("footer");
		Files.createDirectories(targetDir);
		Path target = targetDir.resolve("footer_template.png");
		Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

		DerivateStepImageFooter footerStep = new DerivateStepImageFooter("IMAGE3", "IMAGE_FOOTER3");
		footerStep.setInputType(DigitalType.TIF);
		footerStep.setOutputType(DigitalType.JPG_FOOTER);
		footerStep.setPathTemplate(target);
		footerStep.setQuality(95);

		Generator footerGen = new GeneratorImageJPGFooter();
		DerivateFS theDerivate = new DerivateFS(sharedTempDir);
		theDerivate.setStartFileExtension(".tif");
		theDerivate.init(targetPath1);
		footerGen.setDerivate(theDerivate);
		footerGen.setStep(footerStep);

		// act
		int outcome = footerGen.create();

		// assert
		assertEquals(1, outcome);
		List<Path> resultPaths = Files.list(targetPath2).collect(Collectors.toList());
		assertEquals(1, resultPaths.size());
		for (Path p : resultPaths) {
			assertTrue(p.toFile().exists());
			byte[] bytes = Files.readAllBytes(p);
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
			assertNotEquals(height, image.getHeight());
			assertEquals(796, image.getHeight());
		}
	}

	@Test
	void testRendererFooterGranular(@TempDir Path tempDir) throws DigitalDerivansException, IOException {
		// arrange
		String templatePath = "src/test/resources/config/footer_template.png";
		Path source = Paths.get(templatePath).toAbsolutePath();
		Path footerDir = tempDir.resolve("footer");
		Files.createDirectories(footerDir);
		Path target = footerDir.resolve("footer_template.png");
		Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

		// generate synthetic digital pages with granular URNs
		DerivateStruct testStruct = new DerivateStruct(1, "0001");
		for (int i = 1; i < 4; i++) {
			String currId = String.format("FILE_%04d", i);
			Path currPath = tempDir.resolve(TestHelper.IMAGE).resolve(String.format("%04d.jpg", i));
			DigitalPage page = new DigitalPage(currId, i, currPath);
			String currentURN = String.format("urn:nbn:3:1-123-%02d", i);
			page.setContentIds(currentURN);
			testStruct.getPages().add(page);
		}

		DerivateStepImageFooter footerStep = new DerivateStepImageFooter(TestHelper.IMAGE, "IMAGE_FOOTER4");
		footerStep.setPathTemplate(target);
		footerStep.setQuality(95);

		DerivateFS derivate = new DerivateFS(tempDir);
		derivate.setStructure(testStruct);
		Generator derivateerGranular = new GeneratorImageJPGFooter();
		derivateerGranular.setDerivate(derivate);
		derivateerGranular.setStep(footerStep);
		
		// act
		int outcome = derivateerGranular.create();

		// assert
		assertEquals(3, outcome);
		List<Path> resultPaths = Files.list(tempDir.resolve("IMAGE_FOOTER4")).collect(Collectors.toList());
		assertEquals(3, resultPaths.size());
		for (Path p : resultPaths) {
			assertTrue(p.toFile().exists());
			byte[] bytes = Files.readAllBytes(p);
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
			assertNotEquals(375, image.getHeight());
			assertEquals(387, image.getHeight());
			assertEquals(250, image.getWidth());
		}

		assertEquals(3, ((GeneratorImageJPGFooter) derivateerGranular).getNumberOfGranularIdentifiers());
	}

	/**
	 * 
	 * Scenario when in-between a single pages misses granular URN (here: page 2)
	 * Is it expected that 2 pages render granular URN and 1 page only work URN?
	 * 
	 * @throws DigitalDerivansException
	 * @throws IOException
	 */
	@Test
	void testRendererFooterGranularPartial(@TempDir Path tempDir) throws DigitalDerivansException, IOException {
		// arrange
		String templatePath = "src/test/resources/config/footer_template.png";
		Path source = Paths.get(templatePath).toAbsolutePath();
		Path targetDir = sharedTempDir.resolve("footer");
		Files.createDirectories(targetDir);
		Path target = targetDir.resolve("footer_template.png");
		Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

		// generate synthetic digital pages with granular URNs
		DerivateStruct testStruct = new DerivateStruct(1, "0001");
		for (int i = 1; i < 4; i++) {
			String currId = String.format("FILE_%04d", i);
			Path currPath = sharedTempDir.resolve(TestHelper.IMAGE).resolve(String.format("%04d.jpg", i));
			DigitalPage page = new DigitalPage(currId, i, currPath);
			String currentURN = String.format("urn:nbn:3:1-123-%02d", i);
			// skip page 2
			if (i % 2 != 0) {
				page.setContentIds(currentURN);
			}
			testStruct.getPages().add(page);
		}

		// enrich target path
		DerivateStepImageFooter footerStep = new DerivateStepImageFooter(TestHelper.IMAGE, "IMAGE_FOOTER5");
		footerStep.setPathTemplate(target);
		footerStep.setQuality(95);

		DerivateFS derivate = new DerivateFS(tempDir);
		derivate.setStructure(testStruct);
		GeneratorImageJPGFooter footerGen = new GeneratorImageJPGFooter();
		footerGen.setDerivate(derivate);
		footerGen.setStep(footerStep);

		// act
		int outcome = footerGen.create();

		// assert
		assertEquals(3, outcome);
		List<Path> resultPaths = Files.list(sharedTempDir.resolve("IMAGE_FOOTER5")).collect(Collectors.toList());

		// difference since there are 3 images, but only two of them have granular urn
		assertEquals(2, footerGen.getNumberOfGranularIdentifiers());
		assertEquals(3, resultPaths.size());
	}
}
