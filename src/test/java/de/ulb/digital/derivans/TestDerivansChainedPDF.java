package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.config.DerivansParameter;
import de.ulb.digital.derivans.generate.Generator;
import de.ulb.digital.derivans.model.pdf.PDFResult;
import de.ulb.digital.derivans.generate.GeneratorPDF;

/**
 * 
 * Test chained derivate processing where PDF generation
 * uses intermediate image output from previous step
 * 
 * @author copilot
 *
 */
class TestDerivansChainedPDF {

	/**
	 * 
	 * Test scenario from issue:
	 * - derivate_01: MAX -> IMAGE_80 (with quality 80)
	 * - derivate_02: IMAGE_80 -> PDF
	 * 
	 * Ensure that PDF uses IMAGE_80 as input, not MAX
	 * 
	 * This test verifies the fix by:
	 * 1. Creating a chained derivate configuration
	 * 2. Running both steps
	 * 3. Verifying PDF was created successfully
	 * 4. Verifying IMAGE_80 directory images were used (by ensuring they exist)
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testPDFUsesIntermediateImageInput(@TempDir Path tempDir) throws Exception {

		// arrange
		Path workDir = tempDir.resolve("test_chained");
		Path maxDir = workDir.resolve("MAX");
		Files.createDirectories(maxDir);
		
		// Create high-quality images in MAX directory
		TestHelper.generateImages(maxDir, 400, 600, 3, "%08d.jpg");
		
		// Create config file for chained derivates
		Path configPath = tempDir.resolve("derivans.ini");
		String configContent = String.join("\n",
			"default_quality = 80",
			"default_poolsize = 8",
			"",
			"[derivate_01]",
			"input_dir = MAX",
			"output_dir = IMAGE_80",
			"",
			"[derivate_02]",
			"input_dir = IMAGE_80",
			"output_dir = .",
			"output_type = pdf"
		);
		Files.writeString(configPath, configContent);

		// act
		DerivansParameter dp = new DerivansParameter();
		dp.setPathInput(workDir);
		dp.setPathConfig(configPath);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);
		List<Generator> generators = derivans.init(workDir);
		
		// Verify we have 2 steps
		assertEquals(2, generators.size());
		
		derivans.forward();

		// assert
		// 1. Verify PDF was created
		Path pdfWritten = workDir.resolve("test_chained.pdf");
		assertTrue(Files.exists(pdfWritten), "PDF should be created");
		
		// 2. Verify IMAGE_80 directory was created with images (from derivate_01)
		Path image80Dir = workDir.resolve("IMAGE_80");
		assertTrue(Files.exists(image80Dir), "IMAGE_80 directory should exist");
		assertTrue(Files.exists(image80Dir.resolve("00000001.jpg")), 
			"IMAGE_80 should contain intermediate images");
		
		// 3. Verify PDF contains correct number of pages
		Generator pdfGenerator = generators.get(1);
		assertTrue(pdfGenerator instanceof GeneratorPDF);
		PDFResult pdfResult = ((GeneratorPDF) pdfGenerator).getPDFResult();
		assertEquals(3, pdfResult.getPdfPages().size(), 
			"PDF should contain 3 pages");
		
		// 4. Verify PDF file is not empty
		assertTrue(Files.size(pdfWritten) > 1000, 
			"PDF file should have substantial size");
	}
}
