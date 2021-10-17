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
import org.junit.jupiter.api.Disabled;
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
@Disabled
class TestImageDerivateerTIFFooter {

	private static String urn = "my:urn:bibliothek-123-4-1";

	private static int height = 3086;

	@TempDir
	static Path sharedTempDir;

	@BeforeAll
	public static void setupBeforeClass() throws IOException {
		Path defaultMaxDir = sharedTempDir.resolve("IMAGE");
		Files.createDirectory(defaultMaxDir);

		for (int i = 1; i <= 100; i++) {
			String fileName = String.format("%04d.tif", i);
			Path imgPath = defaultMaxDir.resolve(fileName);
			String imgSource = "src/test/resources/3900/00000010.tif";
			Path source = Paths.get(imgSource).toAbsolutePath();
			Files.copy(source, imgPath, StandardCopyOption.REPLACE_EXISTING);
//			BufferedImage bi2 = new BufferedImage(1050, height, BufferedImage.TYPE_3BYTE_BGR);
//			ImageIO.write(bi2, "TIF", imgPath.toFile());
		}
	}

	@Test
	void testRendererFooterImagesFromTIFInput() throws DigitalDerivansException, IOException {
		// arrange
		Path sourcePath = sharedTempDir.resolve("IMAGE");
		Path targetPath = sharedTempDir.resolve("IMAGE_FOOTER");
		Files.createDirectory(targetPath);
		DerivansData input = new DerivansData(sourcePath, DerivateType.IMAGE);
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
		IDerivateer derivateer = new ImageDerivateerJPGFooter(input, output, 95, footer, new ArrayList<>());
		derivateer.setDigitalPages(resolver.resolveFromPath(sourcePath));

		// act
		int outcome = derivateer.create();

		// assert
		assertEquals(100, outcome);
		List<Path> resultPaths = Files.list(targetPath).collect(Collectors.toList());
		for (Path p : resultPaths) {
			assertTrue(p.toFile().exists());
			byte[] bytes = Files.readAllBytes(p);
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
			assertNotEquals(height, image.getHeight());
			assertEquals(3200, image.getHeight());
		}
	}
}
