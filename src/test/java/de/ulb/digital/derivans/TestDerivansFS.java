package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.config.DerivansParameter;
import de.ulb.digital.derivans.generate.Generator;
import de.ulb.digital.derivans.model.pdf.PDFOutlineEntry;
import de.ulb.digital.derivans.model.step.DerivateStep;
import de.ulb.digital.derivans.model.step.DerivateType;

/**
 * 
 * @author hartwig
 *
 */
class TestDerivansFS {

	/**
	 * 
	 * Start images set flat in start directory
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testDerivatesResolveFlat(@TempDir Path tempDir) throws Exception {

		// arrange
		Path pathTarget = tempDir.resolve("only_images");
		TestHelper.generateImages(pathTarget, 300, 420, 4, "%04d.jpg");

		// act
		DerivansParameter dp = new DerivansParameter();
		dp.setImages(".");
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);
		List<Generator> des = derivans.init(pathTarget);
		assertEquals(2, des.size());
		derivans.forward();

		// assert
		Path pdfWritten = pathTarget.resolve("only_images.pdf");
		assertTrue(Files.exists(pdfWritten));
	}

	/**
	 * 
	 * Start images one level below actual start directory
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testDerivatesResolveMAXPath(@TempDir Path tempDir) throws Exception {

		// arrange
		Path pathTarget = tempDir.resolve("only_images");
		Path pathImageMax = pathTarget.resolve("MAX");
		Files.createDirectories(pathImageMax);
		TestHelper.generateImages(pathImageMax, 300, 420, 4, "%04d.jpg");

		// act
		DerivansParameter dp = new DerivansParameter();
		dp.setPathInput(pathTarget);
		dp.setImages("MAX");
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);
		derivans.init(pathTarget);
		derivans.forward();

		// assert
		Path pdfWritten = pathTarget.resolve("only_images.pdf");
		assertTrue(Files.exists(pdfWritten));

		PDFOutlineEntry outline = new TestHelper.PDFInspector(pdfWritten).getOutline();
		assertNotNull(outline);
		assertEquals("Outlines", outline.getLabel());
		assertEquals(1, outline.getOutlineEntries().size());
		PDFOutlineEntry logRoot = outline.getOutlineEntries().get(0);
		assertEquals("only_images", logRoot.getLabel());
		assertEquals(4, logRoot.getOutlineEntries().size());
	}

	/**
	 * 
	 * Start images one level below actual start directory
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testDerivatesResolveTIFInput(@TempDir Path tempDir) throws Exception {

		// arrange
		Path configTargetDir = tempDir.resolve("config");
		Files.createDirectories(configTargetDir);
		Path srcConfig = TestResource.CONFIG_RES_DIR.get().resolve("derivans_ulb_export.ini");
		Path trgConfig = configTargetDir.resolve("derivans_ulb_export.ini");
		Files.copy(srcConfig, trgConfig);
		DerivansParameter dp = new DerivansParameter();
		dp.setPathConfig(trgConfig);
		Path pathTarget = tempDir.resolve("only_images");
		Path pathImageMax = pathTarget.resolve("MAX");
		Files.createDirectories(pathImageMax);
		TestHelper.generateImages(pathImageMax, 150, 210, 2, "%04d.tif");

		// act
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);
		derivans.init(pathTarget);

		// assert
		List<DerivateStep> steps = derivans.getSteps();
		assertEquals(5, steps.size());
		assertEquals(DerivateType.TIF, steps.get(0).getInputType());
		assertEquals(DerivateType.JPG, steps.get(1).getInputType());

		derivans.forward();
		// assert
		Path pdfWritten = pathTarget.resolve("only_images.pdf");
		assertTrue(Files.exists(pdfWritten));

		PDFOutlineEntry outline = new TestHelper.PDFInspector(pdfWritten).getOutline();
		assertNotNull(outline);
		assertEquals("Outlines", outline.getLabel());
		assertEquals(1, outline.getOutlineEntries().size());
		PDFOutlineEntry logRoot = outline.getOutlineEntries().get(0);
		assertEquals("only_images", logRoot.getLabel());
		assertEquals(2, logRoot.getOutlineEntries().size());
	}

	/**
	 * 
	 * Start images one level below actual start directory
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testDerivatesResolveDEFAULTPath(@TempDir Path tempDir) throws Exception {

		// arrange
		Path pathTarget = tempDir.resolve("only_images");
		Path pathImageMax = pathTarget.resolve("DEFAULT");
		Files.createDirectories(pathImageMax);
		TestHelper.generateImages(pathImageMax, 300, 420, 4, "%04d.jpg");

		// act
		DerivansParameter dp = new DerivansParameter();
		dp.setPathInput(pathTarget);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);
		derivans.init(pathTarget);
		derivans.forward();

		// assert
		Path pdfWritten = pathTarget.resolve("only_images.pdf");
		assertTrue(Files.exists(pdfWritten));
	}

	/**
	 * 
	 * Behavior if OCR data present in sub directory 'ALTO3' and
	 * images in directory 'ORIGINAL'
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testConfigCustomWithImagesAndPartialOCR(@TempDir Path tempDir) throws Exception {

		// arrange
		Path configTargetDir = tempDir.resolve("config");
		if (Files.exists(configTargetDir)) {
			Files.walk(configTargetDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(configTargetDir);
		}
		Files.createDirectories(configTargetDir);
		DerivansParameter dp = new DerivansParameter();
		dp.setPathConfig(TestResource.CONFIG_ODEM_CUSTOM.get());
		Path pathTarget = tempDir.resolve("16359604");
		dp.setPathInput(pathTarget);
		Path sourceImageDir = Path.of("src/test/resources/ocr/16359604");
		TestHelper.copyTree(sourceImageDir, pathTarget);
		// create artificial "ORIGINAL" testimages
		Path imageOriginal = pathTarget.resolve("ORIGINAL");
		List<String> ids = IntStream.range(4, 8).mapToObj(i -> String.format("163310%02d", i))
				.collect(Collectors.toList());
		// these are the small dimensions a newspaper page
		// shall shrink to which was originally 7000x10000
		TestHelper.generateJpgsFromList(imageOriginal, 1400, 2000, ids);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.init(pathTarget);
		derivans.forward();

		// assert
		String pdfName = "16359604.pdf";
		Path pdfWritten = pathTarget.resolve(pdfName);
		assertTrue(Files.exists(pdfWritten));
	}

}
