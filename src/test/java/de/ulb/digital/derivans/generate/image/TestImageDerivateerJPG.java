package de.ulb.digital.derivans.generate.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.generate.GeneratorImageJPG;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DerivateFS;
import de.ulb.digital.derivans.model.step.DerivateType;

/**
 * 
 * Test Specification for {@link ImageDerivatHandler}
 * 
 * @author hartwig
 *
 */
class TestImageDerivateerJPG {

	@TempDir
	static Path sharedTempDir;

	@BeforeAll
	public static void setupBeforeClass() throws IOException {
		Path imageDir = sharedTempDir.resolve("IMAGE");
		int width = 450;
		int height = 700;
		int number = 4;
		TestHelper.generateImages(imageDir, width, height, number, "%04d.jpg");
	}

	@Test
	void testRenderer80() throws DigitalDerivansException, IOException {
		// arrange
		Path sourcePath = sharedTempDir.resolve("IMAGE");
		Path targetPath = sharedTempDir.resolve("IMAGE_80");
		Files.createDirectory(targetPath); // mandatory, done in production by BaseDerivateer
		DerivateFS dFs = new DerivateFS(targetPath);
		dFs.init(sourcePath);

		DerivansData input = new DerivansData(sharedTempDir, "IMAGE", DerivateType.JPG);
		DerivansData output = new DerivansData(sharedTempDir, "IMAGE_80", DerivateType.JPG);
		GeneratorImageJPG jpgs = new GeneratorImageJPG(input, output, 80);
		jpgs.setDigitalPages(dFs.getAllPages());

		// act
		int outcome = jpgs.create();

		// assert
		assertEquals(4, outcome);
		Files.list(targetPath).forEach(p -> p.toFile().exists());
	}

	@Test
	void testRenderer70() throws DigitalDerivansException, IOException {
		// arrange
		Path sourcePath = sharedTempDir.resolve("IMAGE");
		Path targetPath = sharedTempDir.resolve("image_70");
		Files.createDirectory(targetPath); // mandatory, done in production by BaseDerivateer
		DerivateFS dFs = new DerivateFS(targetPath);
		dFs.init(sourcePath);
		DerivansData input = new DerivansData(sharedTempDir, "IMAGE", DerivateType.JPG);
		DerivansData output = new DerivansData(sharedTempDir, "image_70", DerivateType.JPG);
		GeneratorImageJPG jpgs = new GeneratorImageJPG(input, output, 70);
		jpgs.setDigitalPages(dFs.getAllPages());

		// act
		int outcome = jpgs.create();

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
		Path sourcePath = sharedTempDir.resolve("IMAGE");
		Path targetPath = sharedTempDir.resolve("DEFAULT");
		Files.createDirectory(targetPath); // mandatory, done in production by BaseDerivateer
		DerivateFS dFs = new DerivateFS(targetPath);
		dFs.init(sourcePath);
		DerivansData input = new DerivansData(sharedTempDir, "IMAGE", DerivateType.JPG);
		DerivansData output = new DerivansData(sharedTempDir, "DEFAULT", DerivateType.JPG);
		GeneratorImageJPG derivateer = new GeneratorImageJPG(input, output, 70);
		derivateer.setMaximal(256);
		derivateer.setOutputPrefix("BUNDLE_BRANDED_PREVIEW__");
		derivateer.setDigitalPages(dFs.getAllPages());

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
		Path sourcePath = sharedTempDir.resolve("IMAGE");
		Path targetPath = sharedTempDir.resolve("PREVIEW");
		Path finalPath = sharedTempDir.resolve("FINAL");
		Files.createDirectory(targetPath);
		DerivateFS dFs = new DerivateFS(sharedTempDir);
		dFs.init(sourcePath);
		DerivansData input = new DerivansData(sharedTempDir, "IMAGE", DerivateType.JPG);
		DerivansData output = new DerivansData(sharedTempDir, "PREVIEW", DerivateType.JPG);
		GeneratorImageJPG id1 = new GeneratorImageJPG(input, output, 50);
		id1.setMaximal(256);
		id1.setDigitalPages(dFs.getAllPages());
		id1.setOutputPrefix("BUNDLE_BRANDED_PREVIEW__");

		DerivansData input2 = new DerivansData(sharedTempDir, "PREVIEW", DerivateType.JPG);
		DerivansData output2 = new DerivansData(sharedTempDir, "FINAL", DerivateType.JPG);
		GeneratorImageJPG id2 = new GeneratorImageJPG(input2, output2, 50);
		id2.setMaximal(128);
		id2.setInputPrefix("BUNDLE_BRANDED_PREVIEW__");
		id2.setOutputPrefix("BUNDLE_HUMBLE__");
		id2.setDigitalPages(dFs.getAllPages());

		// act
		id1.create();
		int outcome = id2.create();

		// assert
		assertEquals(4, outcome);
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
}
