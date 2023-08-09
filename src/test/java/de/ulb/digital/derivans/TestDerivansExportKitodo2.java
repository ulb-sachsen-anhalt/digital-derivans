package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.xpath.XPathExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.config.DerivansParameter;

/**
 * 
 * Common Kitodo2 exporter setup
 * with TIF images and METS/MODS metadata
 * 
 * used config: src/test/resources/config/derivans-5steps.ini
 * 
 * @author hartwig
 *
 */
public class TestDerivansExportKitodo2 {

	@TempDir
	static Path tempDir;

	static Path workDir;

	static String prefixPreview = "BUNDLE_BRANDED_PREVIEW__";

	static String prefixThumbs = "BUNDLE_THUMBNAIL__";
	
	static int nExpectedImages = 17;

	@BeforeAll
	public static void setupBeforeClass() throws Exception {

		// arrange metadata and images
		var pathRes = Path.of("src/test/resources/058141367/058141367.xml");
		workDir = fixtureMetadataTIFK2(tempDir, pathRes);

		// arrange configuration
		// migration configuration with extended derivates
		Path configSourceDir = Path.of("src/test/resources/config");
		Path configTargetDir = tempDir.resolve("config");
		if (Files.exists(configTargetDir)) {
			Files.walk(configTargetDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(configTargetDir);
		}
		TestHelper.copyTree(configSourceDir, configTargetDir);
		DerivansParameter dp = new DerivansParameter();
		dp.setPathConfig(configTargetDir.resolve("derivans-5steps.ini"));
		dp.setPathInput(workDir.resolve("058141367.xml"));
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.create();
	}

	@Test
	void testDerivateJPGsWithFooterWritten() throws Exception {
		Path footerDir = workDir.resolve("IMAGE_FOOTER");
		assertTrue(Files.exists(footerDir));
		for (int i=1; i < nExpectedImages; i++) {
			var imageLabel = String.format("%08d.jpg", i);
			assertTrue(footerDir.resolve(imageLabel).toFile().exists());
		}
	}

	@Test
	void testPreviewImagesWritten() throws Exception {
		Path previewDir = workDir.resolve(prefixPreview);
		assertTrue(Files.exists(previewDir));
		for (int i=1; i < nExpectedImages; i++) {
			var imageLabel = String.format("%s%08d.jpg", prefixPreview, i);
			assertTrue(previewDir.resolve(imageLabel).toFile().exists());
		}
	}

	@Test
	void testThumbnailsWritten() throws Exception {
		Path thumbsDir = workDir.resolve(prefixThumbs);
		assertTrue(Files.exists(thumbsDir));
		for (int i=1; i < nExpectedImages; i++) {
			var imageLabel = String.format("%s%08d.jpg", prefixThumbs, i);
			assertTrue(thumbsDir.resolve(imageLabel).toFile().exists());
		}
	}

	@Test
	void testPDFWritten() throws Exception {
		Path pdfWritten = workDir.resolve("058141367.pdf");
		assertTrue(Files.exists(pdfWritten));
	}

	@Test
	void testMETSUpdateSuccess() throws Exception {
		// assert proper pdf-metadata integration
		Document doc = TestHelper.readXMLDocument(workDir.resolve("058141367.xml"));
		XPathExpression<Element> xpath = TestHelper.generateXpression(".//mets:fptr[@FILEID='PDF_058141367']");
		Element el = xpath.evaluateFirst(doc);
		assertNotNull(el);

		// inspect parent element
		Element parent = el.getParentElement();
		assertEquals("div", parent.getName());
		assertEquals("monograph", parent.getAttribute("TYPE").getValue());
		assertEquals("LOG_0000", parent.getAttribute("ID").getValue());

		// assert PDF is inserted as first child
		Element firstChild = parent.getChildren().get(0);
		assertNotNull(firstChild.getAttribute("FILEID"));
		assertEquals("PDF_058141367", firstChild.getAttribute("FILEID").getValue());
	}


	public static Path fixtureMetadataTIFK2(Path tempDir, Path srcMets) throws IOException {
		Path pathTarget = tempDir.resolve("058141367");
		if (Files.exists(pathTarget)) {
			Files.walk(pathTarget).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(pathTarget);
		}
		var maxImages = srcMets.getParent().resolve("MAX");
		Path imagesDst = pathTarget.resolve("MAX");
		Files.createDirectories(imagesDst);
		Files.walk(maxImages, 1).forEach(src -> {
			try {
				var dst = imagesDst.resolve(src.getFileName());
				if(Files.isRegularFile(src)) {
					Files.copy(src, dst);
				}
			} catch (IOException e) {
			}
		});		
		Path metsTarget = pathTarget.resolve("058141367.xml");
		Files.copy(srcMets, metsTarget);
		return pathTarget;
	}
}
