package de.ulb.digital.derivans.generate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.model.DerivateFS;
import de.ulb.digital.derivans.model.DerivateStruct;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.IDerivate;
import de.ulb.digital.derivans.model.step.DerivateStepImageFooter;

/**
 * 
 * Test Specification for {@link GeneratorImageJPGFooter}
 * when dealing scan images with small edges
 * 
 * @author u.hartwig
 *
 */
class TestImageGeneratorFooterGranularWithEdges {

	private static IDerivate testDerivate;

	private static DerivateStruct testStruct;

	@TempDir
	static Path sharedTempDir;

	static Path sourcePath;

	static Path targetPath;

	static Path pathTemplate;

	static String footerDirectory = "FOOTER";

	/**
	 * 
	 * Setup synthetic derivate with digital pages with granular URNs having edges
	 * 
	 * @throws IOException
	 * @throws DigitalDerivansException
	 */
	@BeforeAll
	static void setupBeforeClass() throws IOException, DigitalDerivansException {
		testStruct = new DerivateStruct(1, "0001");
		Path defaultMaxDir = sharedTempDir.resolve("IMAGE");
		Files.createDirectory(defaultMaxDir);
		int w = 500;
		int h = 750;
		for (int i = 1; i < 10; i++) {
			Path jpgFile = defaultMaxDir.resolve("000" + i + ".jpg");
			if ( i > 1) {
				// simulate narrow edge images to the end
				w -= 25;
			}
			BufferedImage bi2 = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int gray = (x + y) % 256;
					int rgb = (gray << 16) | (gray << 8) | gray;
					bi2.setRGB(x, y, rgb);
				}
			}
			ImageIO.write(bi2, "JPG", jpgFile.toFile());
			String currId = String.format("FILE_%04d", i);
			DigitalPage page = new DigitalPage(currId, i, jpgFile);
			String currentURN = String.format("urn:nbn:3:3-1192015415-%02d", i);
			page.setContentIds(currentURN);
			testStruct.getPages().add(page);
		}
		sourcePath = sharedTempDir.resolve(TestHelper.IMAGE);
		testDerivate = new DerivateFS(sharedTempDir);
		testDerivate.init(Path.of(TestHelper.IMAGE));
		Path source = TestResource.RES_FOOTER.get().toAbsolutePath();
		Path footerDir = sharedTempDir.resolve("footer");
		Files.createDirectories(footerDir);
		pathTemplate = footerDir.resolve("footer_template.png");
		Files.copy(source, pathTemplate, StandardCopyOption.REPLACE_EXISTING);
	}

	/**
	 * 
	 * Ensure even for images with small edges images can be processed,
	 * even though some footers may be skipped because they would become too small.
	 * 
	 * @param tempDir
	 * @throws DigitalDerivansException
	 */
	@Test
	void rendererFooterGranular(@TempDir Path tempDir) throws DigitalDerivansException {
		// arrange
		DerivateStepImageFooter footerStep = new DerivateStepImageFooter(TestHelper.IMAGE, footerDirectory);
		footerStep.setPathTemplate(pathTemplate);
		footerStep.setQuality(95);
		footerStep.setFooterLabel("Universitäts- und Landesbibliothek Sachsen-Anhalt");
		DerivateFS derivate = new DerivateFS(tempDir);
		derivate.setStructure(testStruct);
		Generator derivateerGranular = new GeneratorImageJPGFooter();
		derivateerGranular.setDerivate(derivate);
		derivateerGranular.setStep(footerStep);
		
		// act
		assertDoesNotThrow(derivateerGranular::create);
	}

}
