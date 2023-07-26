package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

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
 * Common ULB Sachsen-Anhalt data setup
 * with images and METS/MODS metadata
 * from legacy vlserver to attach
 * footnote and generate some
 * additional derivates for dspace
 * (= 7 steps)
 * 
 * @author hartwig
 *
 */
public class TestDerivansCommonULB {

	@TempDir
	static Path tempDir;

	static Path pathTarget;

	@BeforeAll
	public static void setupBeforeClass() throws Exception {

		// arrange metadata and images
		pathTarget = TestHelper.fixturePrint737429(tempDir);

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
		dp.setPathConfig(configTargetDir.resolve("derivans-7steps.ini"));
		dp.setPathInput(pathTarget.resolve("737429.xml"));
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.create();
	}

	@Test
	void testPDFWritten() throws Exception {
		Path pdfWritten = pathTarget.resolve("191092622.pdf");
		assertTrue(Files.exists(pdfWritten));
	}

	@Test
	void testMETSUpdateSuccess() throws Exception {
		// assert proper pdf-metadata integration
		Document doc = TestHelper.readXMLDocument(pathTarget.resolve("737429.xml"));
		XPathExpression<Element> xpath = TestHelper.generateXpression(".//mets:fptr[@FILEID='PDF_191092622']");
		Element el = xpath.evaluateFirst(doc);
		assertNotNull(el);

		// inspect parent element
		Element parent = el.getParentElement();
		assertEquals("div", parent.getName());
		assertEquals("monograph", parent.getAttribute("TYPE").getValue());
		assertEquals("log737429", parent.getAttribute("ID").getValue());

		// assert PDF is inserted as first child
		Element firstChild = parent.getChildren().get(0);
		assertNotNull(firstChild.getAttribute("FILEID"));
		assertEquals("PDF_191092622", firstChild.getAttribute("FILEID").getValue());
	}

	@Test
	void testThumbnailDerivatesGenerated() throws Exception {
		Path bundle2 = pathTarget.resolve("BUNDLE_THUMBNAIL__");
		assertTrue(Files.exists(bundle2));
		List<Path> b2ps = Files.list(bundle2).sorted().collect(Collectors.toList());
		Path p = b2ps.get(0);
		assertTrue(Files.exists(p));
		BufferedImage bi = ImageIO.read(p.toFile());
		assertEquals(128, bi.getHeight());
		String fileName = p.getFileName().toString();
		assertEquals("BUNDLE_THUMBNAIL__737434.jpg", fileName);
	}

}
