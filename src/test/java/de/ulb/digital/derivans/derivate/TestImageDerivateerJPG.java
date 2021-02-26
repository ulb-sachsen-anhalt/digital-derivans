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

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestDerivans;
import de.ulb.digital.derivans.derivate.IDerivateer;
import de.ulb.digital.derivans.derivate.ImageDerivateerToJPG;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DerivateType;

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
		IDerivateer jpgs = new ImageDerivateerToJPG(input, output, 80);

		// act
		boolean outcome = jpgs.create();

		// assert
		assertTrue(outcome);
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
		IDerivateer jpgs = new ImageDerivateerToJPG(input, output, 70);

		// act
		boolean outcome = jpgs.create();

		// assert
		assertTrue(outcome);
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
		ImageDerivateerToJPG jpgs = new ImageDerivateerToJPG(input, output, 70);
		jpgs.setMaximal(1000);
		jpgs.setOutputPrefix("BUNDLE_BRANDED_PREVIEW__");

		// act
		boolean outcome = jpgs.create();

		// assert
		assertTrue(outcome);
		Files.list(targetPath).forEach(p -> p.toFile().exists());
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

}
