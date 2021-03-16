package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.config.DerivansConfiguration;

/**
 * 
 * @author hartwig
 *
 */
public class TestDerivans {

	public static final Namespace NS_METS = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");

	@Test
	@Order(1)
	void testDerivatesWithConfigurationFile(@TempDir Path tempDir) throws Exception {

		// arrange metadata and images
		Path pathTarget = arrangeMetaddatenAndImagesFor737429(tempDir);

		// arrange configuration
		Path configSourceDir = Path.of("src/test/resources/config");
		Path configTargetDir = tempDir.resolve("config");
		if (Files.exists(configTargetDir)) {
			Files.walk(configTargetDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(configTargetDir);
		}
		copyTree(configSourceDir, configTargetDir);

		// act
		DerivansParameter dp = new DerivansParameter();
		dp.setPathConfig(configTargetDir.resolve("derivans.ini"));
		dp.setPathInput(pathTarget.resolve("737429.xml"));
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);
		derivans.create();

		// assert pdf exists
		Path pdfWritten = pathTarget.resolve("191092622.pdf");
		assertTrue(Files.exists(pdfWritten));

		// assert proper pdf-metadata integration
		Document doc = readXMLDocument(pathTarget.resolve("737429.xml"));
		XPathExpression<Element> xpath = generateXpression(".//mets:fptr[@FILEID='PDF_191092622']");
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
	@Order(2)
	void testDerivatesFrom737429Defaults(@TempDir Path tempDir) throws Exception {

		Path pathTarget = arrangeMetaddatenAndImagesFor737429(tempDir);

		// act
		DerivansParameter dp = new DerivansParameter();
		dp.setPathInput(pathTarget.resolve("737429.xml"));
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);
		derivans.create();

		// assert
		Path pdfWritten = pathTarget.resolve("191092622.pdf");
		assertTrue(Files.exists(pdfWritten));
	}

	@Test
	@Order(3)
	void testDerivatesOnlyWithPath(@TempDir Path tempDir) throws Exception {

		// arrange
		Path pathTarget = tempDir.resolve("only_images");
		Path pathImageMax = pathTarget.resolve("MAX");
		Files.createDirectories(pathImageMax);
		generateJpgs(pathImageMax, 1240, 1754, 6);

		// act
		DerivansParameter dp = new DerivansParameter();
		dp.setPathInput(pathTarget);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);
		derivans.create();

		// assert
		Path pdfWritten = pathTarget.resolve("only_images.pdf");
		assertTrue(Files.exists(pdfWritten));
	}

	@Test
	@Order(4)
	void testDerivatesWithGranularImages(@TempDir Path tempDir) throws Exception {

		// arrange
		Path pathTarget = tempDir.resolve("only_images");
		Path pathImageMax = pathTarget.resolve("MAX");
		Files.createDirectories(pathImageMax);
		generateJpgsFromList(pathImageMax, 1240, 1754, List.of("737434", "737436", "737437", "737438"));

		Path sourceMets = Path.of("src/test/resources/metadata/vls/737429.mets.xml");
		assertTrue(Files.exists(sourceMets));
		Path targetMets = pathTarget.resolve(Path.of("737429.mets.xml"));
		Files.copy(sourceMets, targetMets);

		DerivansParameter dp = new DerivansParameter();
		dp.setPathInput(pathTarget);
		dp.setPathInput(targetMets);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.create();

		// assert
		Path pdfWritten = pathTarget.resolve("191092622.pdf");
		assertTrue(Files.exists(pdfWritten));
	}

	public static Path arrangeMetaddatenAndImagesFor737429(Path tempDir) throws IOException {
		Path pathTarget = tempDir.resolve("737429");
		if (Files.exists(pathTarget)) {
			Files.walk(pathTarget).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(pathTarget);
		}
		Files.createDirectory(pathTarget);
		Path mets = Path.of("src/test/resources/metadata/vls/737429.xml");
		Path metsTarget = pathTarget.resolve("737429.xml");
		Files.copy(mets, metsTarget);
		Path imagePath = pathTarget.resolve("MAX");
		generateJpgsFromList(imagePath, 2367, 3737, List.of("737434", "737436", "737437", "737438"));
		return pathTarget;
	}

	public static void generateJpgs(Path imageDir, int width, int height, int number) throws IOException {
		if (Files.exists(imageDir)) {
			Files.delete(imageDir);
		}
		Files.createDirectory(imageDir);
		for (int i = 1; i <= number; i++) {
			String imageName = String.format("%04d.jpg", i);
			writeImage(imageDir, width, height, imageName);
		}
	}

	private static void writeImage(Path imageDir, int width, int height, String imageName) throws IOException {
		Path jpgFile = imageDir.resolve(imageName);
		BufferedImage bi2 = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		Font theFont = new Font("Helvetica", Font.BOLD, 84);
		Graphics2D g2d = bi2.createGraphics();
		g2d.setColor(Color.WHITE);
		g2d.setFont(theFont);
		FontMetrics fontMetrics = g2d.getFontMetrics();
		Rectangle2D rect = fontMetrics.getStringBounds(imageName, g2d);
		int centerX = (bi2.getWidth() - (int) rect.getWidth()) / 2;
		g2d.drawString(imageName, centerX, 500);
		g2d.dispose();
		ImageIO.write(bi2, "JPG", jpgFile.toFile());
	}

	private static void generateJpgsFromList(Path imageDir, int width, int height, List<String> labels)
			throws IOException {
		if (Files.exists(imageDir)) {
			Files.delete(imageDir);
		}
		Files.createDirectory(imageDir);
		for (int i = 0; i < labels.size(); i++) {
			Path jpgFile = imageDir.resolve(labels.get(i) + ".jpg");
			BufferedImage bi2 = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
			ImageIO.write(bi2, "JPG", jpgFile.toFile());
		}
	}

	static void copyTree(Path pathSource, Path pathTarget) {
		try {
			Files.walk(pathSource).forEach(s -> {
				try {
					Path d = pathTarget.resolve(pathSource.relativize(s));
					if (Files.isDirectory(s)) {
						if (!Files.exists(d))
							Files.createDirectory(d);
						return;
					}
					Files.copy(s, d);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static Document readXMLDocument(Path filePath) throws JDOMException, IOException {
		SAXBuilder builder = new SAXBuilder();
		return builder.build(filePath.toFile());
	}

	public XPathExpression<Element> generateXpression(String xpathStr) {
		XPathBuilder<Element> builder = new XPathBuilder<Element>(xpathStr, Filters.element());
		builder.setNamespace(NS_METS);
		return builder.compileWith(XPathFactory.instance());
	}

}
