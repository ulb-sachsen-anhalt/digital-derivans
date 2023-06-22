package de.ulb.digital.derivans.derivate;

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

import de.ulb.digital.derivans.DerivansPathResolver;
import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestDerivans;
import de.ulb.digital.derivans.model.DerivansData;
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

		int width = 3500;
		int height = 5500;
		int number = 8;

		TestDerivans.generateJpgs(imageDir, width, height, number);
	}

	@Test
	void testRenderer80() throws DigitalDerivansException, IOException {
		// arrange
		Path sourcePath = sharedTempDir.resolve("IMAGE");
		Path targetPath = sharedTempDir.resolve("IMAGE_80");
		Files.createDirectory(targetPath);
		DerivansData input = new DerivansData(sourcePath, DerivateType.JPG);
		DerivansData output = new DerivansData(targetPath, DerivateType.JPG);
		IDerivateer jpgs = new ImageDerivateerJPG(input, output, 80);
		DerivansPathResolver resolver = new DerivansPathResolver();
		jpgs.setDigitalPages(resolver.resolveFromPath(sourcePath));

		// act
		int outcome = jpgs.create();

		// assert
		assertEquals(8, outcome);
		Files.list(targetPath).forEach(p -> p.toFile().exists());
	}

	@Test
	void testRenderer70() throws DigitalDerivansException, IOException {
		// arrange
		Path sourcePath = sharedTempDir.resolve("IMAGE");
		Path targetPath = sharedTempDir.resolve("IMAGE_70");
		Files.createDirectory(targetPath);
		DerivansData input = new DerivansData(sourcePath, DerivateType.JPG);
		DerivansData output = new DerivansData(targetPath, DerivateType.JPG);
		IDerivateer jpgs = new ImageDerivateerJPG(input, output, 70);
		DerivansPathResolver resolver = new DerivansPathResolver();
		jpgs.setDigitalPages(resolver.resolveFromPath(sourcePath));

		// act
		int outcome = jpgs.create();

		// assert
		assertEquals(8, outcome);
		Files.list(targetPath).forEach(p -> p.toFile().exists());
		List<Path> paths = Files.list(targetPath).collect(Collectors.toList());
		assertEquals(8, paths.size());
		for (Path p : paths) {
			assertTrue(Files.exists(p));
		}
	}

	@Test
	void testRenderer70Maximal1000() throws DigitalDerivansException, IOException {
		// arrange
		Path sourcePath = sharedTempDir.resolve("IMAGE");
		Path targetPath = sharedTempDir.resolve("DEFAULT");
		Files.createDirectory(targetPath);
		DerivansData input = new DerivansData(sourcePath, DerivateType.JPG);
		DerivansData output = new DerivansData(targetPath, DerivateType.JPG);
		ImageDerivateerJPG derivateer = new ImageDerivateerJPG(input, output, 70);
		DerivansPathResolver resolver = new DerivansPathResolver();
		derivateer.setDigitalPages(resolver.resolveFromPath(sourcePath));
		derivateer.setMaximal(1000);
		derivateer.setOutputPrefix("BUNDLE_BRANDED_PREVIEW__");

		// act
		int outcome = derivateer.create();

		// assert
		assertEquals(8, outcome);
		List<Path> paths = Files.list(targetPath).collect(Collectors.toList());
		assertEquals(8, paths.size());
		for (Path p : paths) {
			assertTrue(Files.exists(p));
			BufferedImage bi = ImageIO.read(p.toFile());
			assertEquals(1000, bi.getHeight());
			assertEquals(636, bi.getWidth());
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

		DerivansData input = new DerivansData(sourcePath, DerivateType.JPG);
		DerivansData output = new DerivansData(targetPath, DerivateType.JPG);
		ImageDerivateerJPG id1 = new ImageDerivateerJPG(input, output, 50);
		DerivansPathResolver resolver = new DerivansPathResolver();
		id1.setDigitalPages(resolver.resolveFromPath(sourcePath));
		id1.setMaximal(800);
		id1.setOutputPrefix("BUNDLE_BRANDED_PREVIEW__");

		DerivansData input2 = new DerivansData(targetPath, DerivateType.JPG);
		DerivansData output2 = new DerivansData(finalPath, DerivateType.JPG);
		ImageDerivateerJPG id2 = new ImageDerivateerJPG(input2, output2, 50);
		resolver = new DerivansPathResolver();
		id2.setDigitalPages(resolver.resolveFromPath(sourcePath));
		id2.setMaximal(640);
		id2.setOutputPrefix("BUNDLE_HUMBLE__");

		// act
		id1.create();
		int outcome = id2.create();

		// assert
		assertEquals(8, outcome);
		List<Path> paths = Files.list(finalPath).sorted().collect(Collectors.toList());
		assertEquals(8, paths.size());
		for (int i = 0; i < paths.size(); i++) {
			Path p = paths.get(i);
			assertTrue(Files.exists(p));
			BufferedImage bi = ImageIO.read(p.toFile());
			assertEquals(640, bi.getHeight());
			String fileName = p.getFileName().toString();
			assertEquals(fileName, String.format("BUNDLE_HUMBLE__000%d.jpg", i + 1));
		}
	}
}
