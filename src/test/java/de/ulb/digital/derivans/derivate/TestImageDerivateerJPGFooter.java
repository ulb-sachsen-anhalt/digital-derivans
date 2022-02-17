package de.ulb.digital.derivans.derivate;

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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.DerivansPathResolver;
import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DerivateType;
import de.ulb.digital.derivans.model.DigitalFooter;

/**
 * 
 * Test Specification for {@link ImageDerivatHandler}
 * 
 * @author hartwig
 *
 */
class TestImageDerivateerJPGFooter {

	private static String urn = "my:urn:bibliothek-123-4-1";

	private static int height = 5500;

	@TempDir
	static Path sharedTempDir;

	@BeforeAll
	public static void setupBeforeClass() throws IOException {
		Path defaultMaxDir = sharedTempDir.resolve("IMAGE");
		Files.createDirectory(defaultMaxDir);
		for (int i = 1; i < 5; i++) {
			Path jpgFile = defaultMaxDir.resolve("000" + i + ".jpg");
			BufferedImage bi2 = new BufferedImage(3500, height, BufferedImage.TYPE_3BYTE_BGR);
			ImageIO.write(bi2, "JPG", jpgFile.toFile());
		}
	}

	@Test
	void testRendererSyntheticFooterImages() throws DigitalDerivansException, IOException {
		// arrange
		Path sourcePath = sharedTempDir.resolve("IMAGE");
		Path targetPath = sharedTempDir.resolve("IMAGE_FOOTER");
		Files.createDirectory(targetPath);
		DerivansData input = new DerivansData(sourcePath, DerivateType.JPG);
		DerivansData output = new DerivansData(targetPath, DerivateType.JPG);

		String templatePath = "src/test/resources/config/footer_template.png";
		Path source = Paths.get(templatePath).toAbsolutePath();
		Path targetDir = sharedTempDir.resolve("footer");
		Files.createDirectories(targetDir);
		Path target = targetDir.resolve("footer_template.png");
		Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

		DigitalFooter footer = new DigitalFooter("ULB", urn, target);

		// check template file found
		assertTrue(Files.exists(target));

		DerivansPathResolver resolver = new DerivansPathResolver();
		// empty list at construction time is okay since we re-set the pages immediately
		IDerivateer derivateer = new ImageDerivateerJPGFooter(input, output, footer, new ArrayList<>(), 95);
		derivateer.setDigitalPages(resolver.resolveFromPath(sourcePath));

		// act
		int outcome = derivateer.create();

		// assert
		assertEquals(4, outcome);
		List<Path> resultPaths = Files.list(targetPath).collect(Collectors.toList());
		for (Path p : resultPaths) {
			assertTrue(p.toFile().exists());
			byte[] bytes = Files.readAllBytes(p);
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
			assertNotEquals(height, image.getHeight());
			assertEquals(5675, image.getHeight());
		}
	}

	@Test
	void testRendererLargeFooterImage(@TempDir Path tempDir) throws DigitalDerivansException, IOException {
		// arrange
		Path imagePath = tempDir.resolve("IMAGE2");
		Path targetPath = tempDir.resolve("IMAGE_FOOTER2");
		Files.createDirectory(imagePath);
		String resPath = "src/test/resources/images/1667522809_J_0025_0001.jpg";
		Path sourceImage = Paths.get(resPath).toAbsolutePath();
		Files.copy(sourceImage, imagePath.resolve(Path.of(resPath).getFileName()), 
			StandardCopyOption.REPLACE_EXISTING);
		DerivansData input = new DerivansData(imagePath, DerivateType.JPG);
		DerivansData output = new DerivansData(targetPath, DerivateType.JPG);
		String templatePath = "src/test/resources/config/footer_template.png";
		Path source = Paths.get(templatePath).toAbsolutePath();
		Path targetDir = tempDir.resolve("footer");
		Files.createDirectories(targetDir);
		Path target = targetDir.resolve("footer_template.png");
		Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
		DigitalFooter footer = new DigitalFooter("ULB", urn, target);
		DerivansPathResolver resolver = new DerivansPathResolver();
		// empty list at construction time is okay since we re-set the pages immediately
		IDerivateer derivateer = new ImageDerivateerJPGFooter(input, output, footer, new ArrayList<>(), 95);
		derivateer.setDigitalPages(resolver.resolveFromPath(imagePath));

		// act
		int outcome = derivateer.create();

		// assert
		assertEquals(1, outcome);
		List<Path> resultPaths = Files.list(targetPath).collect(Collectors.toList());
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
	void testRendererFooterFromTIFInput(@TempDir Path tempDir) throws DigitalDerivansException, IOException {
		// arrange
		String img = "src/test/resources/images/3900_00000010x768.tif";
		Path imgSource = Paths.get(img).toAbsolutePath();
		Path targetPath1 = tempDir.resolve("IMAGE3");
		Files.createDirectory(targetPath1);
		Path imgPath = targetPath1.resolve("00000010.tif");
		Path targetPath2 = tempDir.resolve("IMAGE_FOOTER3");
		Files.copy(imgSource, imgPath, StandardCopyOption.REPLACE_EXISTING);
		DerivansData input = new DerivansData(targetPath1, DerivateType.IMAGE);
		DerivansData output = new DerivansData(targetPath2, DerivateType.JPG);
		String templatePath = "src/test/resources/config/footer_template.png";
		Path source = Paths.get(templatePath).toAbsolutePath();
		Path targetDir = tempDir.resolve("footer");
		Files.createDirectories(targetDir);
		Path target = targetDir.resolve("footer_template.png");
		Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
		DigitalFooter footer = new DigitalFooter("ULB", urn, target);
		DerivansPathResolver resolver = new DerivansPathResolver();
		// empty list at construction time is okay since we re-set the pages immediately
		IDerivateer derivateer = new ImageDerivateerJPGFooter(input, output, footer, new ArrayList<>(), 95);
		derivateer.setDigitalPages(resolver.resolveFromPath(targetPath1));

		// act
		int outcome = derivateer.create();

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
}
