package de.ulb.digital.derivans.derivate.image;

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
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.derivate.IDerivateer;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DigitalFooter;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.step.DerivateType;

/**
 * 
 * Test Specification for {@link ImageDerivateerJPGFooterGranular}
 * 
 * @author hartwig
 *
 */
class TestImageDerivateerJPGFooterGranular {

	private static String urn = "my:urn:bibliothek-123-4-1";

	@TempDir
	static Path sharedTempDir;
	
	static Path sourcePath;
	
	static Path targetPath;

	@BeforeAll
	public static void setupBeforeClass() throws IOException {
		Path defaultMaxDir = sharedTempDir.resolve("IMAGE");
		Files.createDirectory(defaultMaxDir);

		for (int i = 1; i < 4; i++) {
			Path jpgFile = defaultMaxDir.resolve("000" + i + ".jpg");
			BufferedImage bi2 = new BufferedImage(250, 375, BufferedImage.TYPE_3BYTE_BGR);
			ImageIO.write(bi2, "JPG", jpgFile.toFile());
		}
		sourcePath = sharedTempDir.resolve("IMAGE");
		targetPath = sharedTempDir.resolve("IMAGE_FOOTER_GRANULAR");
		Files.createDirectory(targetPath);
	}

	@Test
	@Order(1)
	void testRendererFooterGranular() throws DigitalDerivansException, IOException {
		// arrange
		DerivansData input = new DerivansData(sharedTempDir, "IMAGE", DerivateType.JPG);
		DerivansData output = new DerivansData(sharedTempDir, "IMAGE_FOOTER_GRANULAR", DerivateType.JPG);
		String templatePath = "src/test/resources/config/footer_template.png";
		Path source = Paths.get(templatePath).toAbsolutePath();
		Path targetDir = sharedTempDir.resolve("footer");
		Files.createDirectories(targetDir);
		Path target = targetDir.resolve("footer_template.png");
		Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
		DigitalFooter footer = new DigitalFooter("ULB", urn, target);
		// check template file found
		assertTrue(Files.exists(target));

		// generate synthetic digital pages with granular URNs
		List<DigitalPage> pages = new ArrayList<>();
		for (int i = 1; i < 4; i++) {
			String currId = String.format("FILE_%04d", i);
			Path currPath = sharedTempDir.resolve("IMAGE").resolve(String.format("%04d.jpg", i));
			DigitalPage page = new DigitalPage(currId, i, currPath);
			String currentURN = String.format("urn:nbn:3:1-123-%02d", i);
			page.setContentIds(currentURN);
			pages.add(page);
		}
		
		// act
		IDerivateer derivateerGranular = new ImageDerivateerJPGFooterGranular(input, output, footer, pages, 95);
		int outcome = derivateerGranular.create();

		// assert
		assertEquals(3, outcome);
		List<Path> resultPaths = Files.list(targetPath).collect(Collectors.toList());
		assertEquals(3, resultPaths.size());
		for (Path p : resultPaths) {
			assertTrue(p.toFile().exists());
			byte[] bytes = Files.readAllBytes(p);
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
			assertNotEquals(375, image.getHeight());
			assertEquals(387, image.getHeight());
			assertEquals(250, image.getWidth());
		}

		assertEquals(3, ((ImageDerivateerJPGFooterGranular) derivateerGranular).getNumberOfGranularIdentifiers());
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
	@Order(2)
	void testRendererFooterGranularPartial() throws DigitalDerivansException, IOException {
		// arrange
		DerivansData input = new DerivansData(sharedTempDir, "IMAGE", DerivateType.JPG);
		DerivansData output = new DerivansData(sharedTempDir, "IMAGE_FOOTER2", DerivateType.JPG);

		String templatePath = "src/test/resources/config/footer_template.png";
		Path source = Paths.get(templatePath).toAbsolutePath();
		Path targetDir = sharedTempDir.resolve("footer");
		Files.createDirectories(targetDir);
		Path target = targetDir.resolve("footer_template.png");
		Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

		DigitalFooter footer = new DigitalFooter("ULB", urn, target);

		// check template file found
		assertTrue(Files.exists(target));

		// generate synthetic digital pages with granular URNs
		List<DigitalPage> pages = new ArrayList<>();
		for (int i = 1; i < 4; i++) {
			String currId = String.format("FILE_%04d", i);
			Path currPath = sharedTempDir.resolve("IMAGE").resolve(String.format("%04d.jpg", i));
			DigitalPage page = new DigitalPage(currId, i, currPath);
			String currentURN = String.format("urn:nbn:3:1-123-%02d", i);
			// skip page 2 
			if (i % 2 != 0) {
				page.setContentIds(currentURN);
			}
			pages.add(page);
		}
		
		// enrich target path
		IDerivateer jpgs = new ImageDerivateerJPGFooterGranular(input, output, footer, pages, 95);

		// act
		int outcome = jpgs.create();

		// assert
		assertEquals(3, outcome);
		List<Path> resultPaths = Files.list(sharedTempDir.resolve("IMAGE_FOOTER2")).collect(Collectors.toList());

		// difference since there are 3 images, but only two of them have granular urn
		assertEquals(2, ((ImageDerivateerJPGFooterGranular) jpgs).getNumberOfGranularIdentifiers());

		// ToDo: Specify behavior
		assertEquals(3, resultPaths.size());
	}
}
