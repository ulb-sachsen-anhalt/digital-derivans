package de.ulb.digital.derivans.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.Derivans;
import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.model.step.DerivateStep;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;

/**
 * 
 * Common ULB Sachsen-Anhalt data setup
 * with images and METS/MODS metadata
 * from legacy vlserver to attach
 * footnote and generate some
 * additional derivates for dspace
 * (= 7 steps)
 * 
 * used config: src/test/resources/config/derivans_ulb_export.ini
 * 
 * @author hartwig
 *
 */
class TestDerivansParameter {

	private static final String NAME_PDF = "the-name-of-the";

	private static final String NAME_PDF_FILE = NAME_PDF + ".pdf";

	/**
	 * 
	 * Simulate scenario with condfigured XPath identifier
	 * which preceedes default ULB PDF label (from mods:identifier@type=gbv)
	 * 
	 */
	@Test
	void testConfiguredXPathUsed(@TempDir Path tmpDir) throws Exception {
		var thisRoot = tmpDir.resolve("forceNameXPath");
		Files.createDirectories(thisRoot);
		var thisDir = TestHelper.fixturePrint737429(thisRoot);

		// arrange configuration
		// migration configuration with extended derivates
		Path configSourceDir = Path.of("src/test/resources/config");
		Path configTargetDir = thisDir.resolve("config");
		TestHelper.copyTree(configSourceDir, configTargetDir);

		// alter config
		String xPath = "//mods:mods/mods:titleInfo/mods:title";
		var pathConfig = configTargetDir.resolve("derivans_ulb_odem.ini");
		var identConfLine = "mods_identifier_xpath = " + xPath;
		Files.writeString(pathConfig, identConfLine + System.lineSeparator(), StandardOpenOption.APPEND);

		DerivansParameter dp = new DerivansParameter();
		dp.setPathConfig(pathConfig);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.init(thisDir.resolve("737429.xml"));
		DerivateStep step = derivans.getSteps().get(2);
		assertTrue(step instanceof DerivateStepPDF);
		DerivateStepPDF stepPdf = (DerivateStepPDF) step;
		assertEquals(xPath, stepPdf.getModsIdentifierXPath().get());
		derivans.forward();

		// assert
		var optPdf = Files.list(thisDir)
				.filter(p -> p.getFileName().toString().endsWith(".pdf")).findFirst();
		assertTrue(optPdf.isPresent());
		assertTrue(optPdf.get().getFileName().toString().startsWith("Ode_In_Solemni_"));
	}

	/**
	 * 
	 * Simulate scenario with condfigured XPath identifier and
	 * additional param for PDF name => ensure value from
	 * CLI overrides configured value
	 * 
	 */
	@Test
	void testConfiguredXPathOverwrittenByArg(@TempDir Path tmpDir) throws Exception {
		var thisRoot = tmpDir.resolve("forceNameCLI");
		Files.createDirectories(thisRoot);
		var thisDir = TestHelper.fixturePrint737429(thisRoot);

		// arrange configuration
		// migration configuration with extended derivates
		Path configSourceDir = Path.of("src/test/resources/config");
		Path configTargetDir = thisDir.resolve("config");
		TestHelper.copyTree(configSourceDir, configTargetDir);

		// alter config
		var pathConfig = configTargetDir.resolve("derivans_ulb_odem.ini");
		var identConfLine = "mods_identifier_xpath = //mods:mods/mods:titleInfo/mods:title";
		Files.writeString(pathConfig, identConfLine + System.lineSeparator(), StandardOpenOption.APPEND);

		DerivansParameter dp = new DerivansParameter();
		dp.setPathConfig(pathConfig);
		dp.setNamePDF(NAME_PDF);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.init(thisDir.resolve("737429.xml"));
		derivans.forward();

		// assert
		Path pdfWritten = thisDir.resolve(NAME_PDF_FILE);
		assertTrue(Files.exists(pdfWritten));
	}
}
