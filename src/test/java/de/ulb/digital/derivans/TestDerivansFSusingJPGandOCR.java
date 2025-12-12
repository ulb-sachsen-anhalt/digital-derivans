package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.config.DerivansParameter;

/**
 * 
 * @author hartwig
 *
 */
class TestDerivansFSusingJPGandOCR {

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
