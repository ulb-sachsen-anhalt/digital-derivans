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
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

		for (int i = 1; i < 9; i++) {
			Path jpgFile = defaultMaxDir.resolve("000" + i + ".jpg");
			BufferedImage bi2 = new BufferedImage(3500, height, BufferedImage.TYPE_3BYTE_BGR);
			ImageIO.write(bi2, "JPG", jpgFile.toFile());
		}
	}

	@Test
	void testRendererFooterImages() throws DigitalDerivansException, IOException {
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

		IDerivateer jpgs = new ImageDerivateerJPGFooter(input, output, 95, footer);

		// act
		boolean outcome = jpgs.create();

		// assert
		assertTrue(outcome);
		List<Path> resultPaths = Files.list(targetPath).collect(Collectors.toList());
		for (Path p : resultPaths) {
			assertTrue(p.toFile().exists());
			byte[] bytes = Files.readAllBytes(p);
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
			assertNotEquals(height, image.getHeight());
			assertEquals(5675, image.getHeight());
		}
	}
}
