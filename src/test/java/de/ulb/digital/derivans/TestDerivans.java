package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.xml.XMLConstants;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.util.IteratorIterable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.config.DerivansParameter;
import de.ulb.digital.derivans.data.IMetadataStore;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;

/**
 * 
 * @author hartwig
 *
 */
public class TestDerivans {

	/**
	 * 
	 * Create Derivates with metadata
	 * and default settings but no explicite
	 * configuration present
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testDerivatesFrom737429Defaults(@TempDir Path tempDir) throws Exception {

		// arrange
		Path pathTarget = TestHelper.fixturePrint737429(tempDir);
		DerivansParameter dp = new DerivansParameter();
		Path metadataPath = pathTarget.resolve("737429.xml");
		dp.setPathInput(metadataPath);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.create();

		// assert
		Path pdfWritten = pathTarget.resolve("191092622.pdf");
		assertTrue(Files.exists(pdfWritten));

		// check metadata
		SAXBuilder builder = new SAXBuilder();
		// please sonarqube "Disable XML external entity (XXE) processing"
		builder.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		Document document = builder.build(new FileInputStream(metadataPath.toString()));
		List<Content> cs = document.getContent();
		Element r = (Element) cs.get(0);
		IteratorIterable<Element> es = r.getDescendants(new ElementFilter("agent"));
		for (Element e : es) {
			List<Element> kids = e.getChildren("note", IMetadataStore.NS_METS);
			if (!kids.isEmpty()) {
				for (Element kid : kids) {
					if (kid.getText().startsWith("PDF FileGroup")) {
						DateTimeFormatter dtf = new DateTimeFormatterBuilder()
								.appendPattern("YYYY-MM-dd")
								.toFormatter();
						String theDateTime = LocalDateTime.now().format(dtf);
						String entry = kid.getTextNormalize();
						assertTrue(entry.contains(theDateTime));
					}
				}
			}
		}
	}

	@Test
	void testDerivatesOnlyWithPath(@TempDir Path tempDir) throws Exception {

		// arrange
		Path pathTarget = tempDir.resolve("only_images");
		Path pathImageMax = pathTarget.resolve("MAX");
		Files.createDirectories(pathImageMax);
		TestHelper.generateImages(pathImageMax, 1240, 1754, 6, "%04d.jpg");

		// act
		DerivansParameter dp = new DerivansParameter();
		dp.setPathInput(pathTarget);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		// dc.setInputDirImages(pathImageMax);
		Derivans derivans = new Derivans(dc);
		derivans.create();

		// assert
		Path pdfWritten = pathTarget.resolve("only_images.pdf");
		assertTrue(Files.exists(pdfWritten));
	}

	@Test
	void testDerivatesWithGranularImages(@TempDir Path tempDir) throws Exception {

		// arrange
		Path pathTarget = tempDir.resolve("only_images");
		Path pathImageMax = pathTarget.resolve("MAX");
		Files.createDirectories(pathImageMax);
		TestHelper.generateJpgsFromList(pathImageMax, 1240, 1754, List.of("737434", "737436", "737437", "737438"));

		Path targetMets = pathTarget.resolve(Path.of("737429.mets.xml"));
		Files.copy(TestResource.HD_Aa_737429.get(), targetMets);

		DerivansParameter dp = new DerivansParameter();
		// dp.setPathInput(pathTarget);
		dp.setPathInput(targetMets);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.create();

		// assert
		Path pdfWritten = pathTarget.resolve("191092622.pdf");
		assertTrue(Files.exists(pdfWritten));
	}

	/**
	 * 
	 * Behavior if custom image dir set and only images present
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testDerivatesWithCLIsetImages(@TempDir Path tempDir) throws Exception {

		// arrange
		var imgDir = "ORIGINAL";
		Path pathTarget = tempDir.resolve("conf_images");
		Path pathImageMax = pathTarget.resolve(imgDir);
		Files.createDirectories(pathImageMax);
		TestHelper.generateImages(pathImageMax, 620, 877, 6, "%04d.jpg");
		DerivansParameter dp = new DerivansParameter();
		dp.setPathInput(pathTarget);
		dp.setImages(imgDir);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.create();

		// assert
		Path pdfWritten = pathTarget.resolve("conf_images.pdf");
		assertTrue(Files.exists(pdfWritten));
	}

	/**
	 * 
	 * Behavior if OCR referenced by URL without file extension
	 * resides in file group named 'ALTO3' and also images
	 * are in group 'ORIGINAL' (rather kitodo.presentation like)
	 * for VL ID 16359604
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testConfigCustomWithImagesAndPartialOCR(@TempDir Path tempDir) throws Exception {

		// arrange
		Path configSourceDir = Path.of("src/test/resources/config");
		Path configTargetDir = tempDir.resolve("config");
		if (Files.exists(configTargetDir)) {
			Files.walk(configTargetDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(configTargetDir);
		}
		Files.createDirectories(configTargetDir);
		Path testConfig = configSourceDir.resolve("derivans-custom.ini");
		Files.copy(testConfig, configTargetDir.resolve("derivans-custom.ini"));
		DerivansParameter dp = new DerivansParameter();
		dp.setPathConfig(testConfig);
		Path pathTarget = tempDir.resolve("16359604");
		dp.setPathInput(pathTarget.resolve("16359604.mets.xml"));
		Path sourceImageDir = Path.of("src/test/resources/16359604");
		TestHelper.copyTree(sourceImageDir, pathTarget);
		// create artificial "ORIGINAL" testimages
		Path imageOriginal = pathTarget.resolve("ORIGINAL");
		List<String> ids = IntStream.range(5, 13)
				.mapToObj(i -> String.format("163310%02d", i)).collect(Collectors.toList());
		// these are the least dimensions a newspaper page
		// shall shrink to which was originally 7000x10000
		TestHelper.generateJpgsFromList(imageOriginal, 700, 1000, ids);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.create();

		// assert
		String pdfName = "General-Anzeiger_f\u00FCr_Halle_und_den_Saalkreis.pdf";
		Path pdfWritten = pathTarget.resolve(pdfName);
		assertTrue(Files.exists(pdfWritten));
	}

	/**
	 * 
	 * Behavior if ULB config, but images are in group
	 * 'ORIGINAL' (rather kitodo.presentation like)
	 * for VL ID 16359604
	 * 
	 * => Ensure, overwriting config via CLI works!
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testConfigULBOverwriteImageGroup(@TempDir Path tempDir) throws Exception {

		// arrange
		Path configSourceDir = Path.of("src/test/resources/config");
		Path configTargetDir = tempDir.resolve("config");
		if (Files.exists(configTargetDir)) {
			Files.walk(configTargetDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(configTargetDir);
		}
		Files.createDirectories(configTargetDir);
		Path testConfig = configSourceDir.resolve("derivans.ini");
		Files.copy(testConfig, configTargetDir.resolve("derivans.ini"));
		DerivansParameter dp = new DerivansParameter();
		dp.setPathConfig(testConfig);
		// this is the mandatory point
		dp.setImages("ORIGINAL");
		Path pathTarget = tempDir.resolve("16359604");
		dp.setPathInput(pathTarget.resolve("16359604.mets.xml"));
		Path sourceImageDir = Path.of("src/test/resources/16359604");
		TestHelper.copyTree(sourceImageDir, pathTarget);
		// create artificial "ORIGINAL" testimages
		Path imageOriginal = pathTarget.resolve("ORIGINAL");
		List<String> ids = IntStream.range(5, 13)
				.mapToObj(i -> String.format("163310%02d", i)).collect(Collectors.toList());
		// these are the least dimensions a newspaper page
		// shall shrink to which was originally 7000x10000
		TestHelper.generateJpgsFromList(imageOriginal, 700, 1000, ids);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		((DerivateStepPDF) dc.getDerivateSteps().get(2)).setModsIdentifierXPath("//mods:title");
		Derivans derivans = new Derivans(dc);

		// act
		derivans.create();

		// assert
		String pdfName = "General-Anzeiger_f\u00FCr_Halle_und_den_Saalkreis.pdf";
		Path pdfWritten = pathTarget.resolve(pdfName);
		assertTrue(Files.exists(pdfWritten));
	}

	/**
	 * 
	 * Ensure Exception is thrown if images are missing
	 * because they are in a different file group
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testConfigULBButDifferentImageGroup(@TempDir Path tempDir) throws Exception {

		// arrange
		Path configSourceDir = Path.of("src/test/resources/config");
		Path configTargetDir = tempDir.resolve("config");
		if (Files.exists(configTargetDir)) {
			Files.walk(configTargetDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(configTargetDir);
		}
		Files.createDirectories(configTargetDir);
		Path testConfig = configSourceDir.resolve("derivans.ini");
		Files.copy(testConfig, configTargetDir.resolve("derivans.ini"));
		DerivansParameter dp = new DerivansParameter();
		dp.setPathConfig(testConfig);
		// dp.setImages("ORIGINAL");
		Path pathTarget = tempDir.resolve("16359604");
		dp.setPathInput(pathTarget.resolve("16359604.mets.xml"));
		Path sourceImageDir = Path.of("src/test/resources/16359604");
		TestHelper.copyTree(sourceImageDir, pathTarget);
		// create artificial "ORIGINAL" testimages
		Path imageOriginal = pathTarget.resolve("ORIGINAL");
		List<String> ids = IntStream.range(5, 13)
				.mapToObj(i -> String.format("163310%02d", i)).collect(Collectors.toList());
		// these are the least dimensions a newspaper page
		// shall shrink to which was originally 7000x10000
		TestHelper.generateJpgsFromList(imageOriginal, 700, 1000, ids);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		var excResult = assertThrows(DigitalDerivansException.class, () -> derivans.create(), "foo");

		// assert
		String pdfName = "General-Anzeiger_f\u00FCr_Halle_und_den_Saalkreis.pdf";
		Path pdfWritten = pathTarget.resolve(pdfName);
		assertFalse(Files.exists(pdfWritten));
		assertTrue(excResult.getMessage().startsWith("No images for "));
	}

	@Test
	void test737429EnforcePDFTextlayer(@TempDir Path tempDir) throws Exception {

		// arrange
		Path pathTarget = TestHelper.fixturePrint737429(tempDir, TestResource.HD_Aa_737429_OCR.get());
		DerivansParameter dp = new DerivansParameter();
		Path metadataPath = pathTarget.resolve("737429.xml");
		TestHelper.copyTree(TestResource.OCR_737429.get(), pathTarget);
		dp.setPathInput(metadataPath);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.create();
		Path pdfWritten = pathTarget.resolve("191092622.pdf");

		// assert
		assertTrue(Files.exists(pdfWritten));
		var textPageOne = TestHelper.getTextAsSingleLine(pdfWritten, 1);
		assertFalse(textPageOne.isBlank());
		assertTrue(textPageOne.contains("SOLEMNI PANEGYRI AVGVSTISSIMO"));
	}
}
