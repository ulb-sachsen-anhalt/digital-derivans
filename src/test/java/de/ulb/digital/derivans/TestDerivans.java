package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.xml.XMLConstants;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.ElementFilter;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.util.IteratorIterable;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.data.IMetadataStore;

/**
 * 
 * @author hartwig
 *
 */
public class TestDerivans {

	public static final Namespace NS_METS = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");

	@Test
	@Order(1)
	void testConfig(@TempDir Path tempDir) throws Exception {

		// arrange metadata and images
		Path pathTarget = arrangeMetsAndImagesFor737429(tempDir);

		// arrange configuration
		// migration configuration with extended derivates
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

	@Test
	@Order(2)
	void testDerivatesFrom737429Defaults(@TempDir Path tempDir) throws Exception {

		Path pathTarget = arrangeMetsAndImagesFor737429(tempDir);

		// act
		DerivansParameter dp = new DerivansParameter();
		Path metadataPath = pathTarget.resolve("737429.xml");
		dp.setPathInput(metadataPath);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);
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

		Path targetMets = pathTarget.resolve(Path.of("737429.mets.xml"));
		Files.copy(TestResource.HD_Aa_737429.get(), targetMets);

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

	@Test
	@Order(5)
	void testDerivateWithFulltext(@TempDir Path tempDir) throws Exception {

		// arrange migration configuration with extended derivates
		Path configSourceDir = Path.of("src/test/resources/config");
		Path configTargetDir = tempDir.resolve("config");
		if (Files.exists(configTargetDir)) {
			Files.walk(configTargetDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(configTargetDir);
		}
		copyTree(configSourceDir, configTargetDir);
		Path pathTarget = tempDir.resolve("148811035");
		Path pathImageMax = pathTarget.resolve("MAX");
		Files.createDirectories(pathImageMax);
		Path sourceImageDir = Path.of("src/test/resources/alto/148811035/MAX");
		copyTree(sourceImageDir, pathImageMax);

		// create artificial testimages
		// List<String> ids = IntStream.range(1, 17).mapToObj(i -> String.format("%08d",
		// i)).collect(Collectors.toList());
		// generateJpgsFromList(pathImageMax, 2164, 2448, ids);

		Path sourceMets = Path.of("src/test/resources/alto/148811035/mets.xml");
		Path targetMets = pathTarget.resolve(Path.of("mets.xml"));
		Files.copy(sourceMets, targetMets);
		Path sourceOcr = Path.of("src/test/resources/alto/148811035/FULLTEXT");
		Path targetOcr = pathTarget.resolve("FULLTEXT");
		copyTree(sourceOcr, targetOcr);

		DerivansParameter dp = new DerivansParameter();
		dp.setPathInput(targetMets);
		dp.setPathConfig(configTargetDir.resolve("derivans.ini"));
		DerivansConfiguration dc = new DerivansConfiguration(dp);

		// apply some scaling, too
		int maximal = 2339; // A4 200 DPI ok
//		int maximal = 1754; // A4 150 DPI tw, print vanishes over top up to "Sero ..."
//		int maximal = 1170; // A4 100 DPI ok with smaller text
		dc.getDerivateSteps().get(1).setMaximal(maximal);
		Derivans derivans = new Derivans(dc);

		// act
		derivans.create();

		// assert
		Path pdfWritten = pathTarget.resolve("148811035.pdf");
		assertTrue(Files.exists(pdfWritten));
	}

	@Test
	@Order(6)
	void testDerivatesOnlyWithPathZD(@TempDir Path tempDir) throws Exception {

		// arrange
		Path pathTarget = tempDir.resolve("zd1");
		// ocr data
		Path sourceOcr = Path.of("src/test/resources/alto/1667524704_J_0150/1667524704_J_0150_0512.xml");
		assertTrue(Files.exists(sourceOcr, LinkOption.NOFOLLOW_LINKS));
		Path sourceFile = sourceOcr.getFileName();
		Path targetDir = pathTarget.resolve("FULLTEXT");
		Files.createDirectories(targetDir);
		Path targetOcr = targetDir.resolve(sourceFile);
		Files.copy(sourceOcr, targetOcr);

		// image data
		Path pathImageMax = pathTarget.resolve("TIF");
		Files.createDirectory(pathImageMax);
		Path imagePath = pathImageMax.resolve("1667524704_J_0150_0512.tif");
		writeImage(imagePath, 7544, 10536, BufferedImage.TYPE_BYTE_GRAY, "TIFF");

		// act
		DerivansParameter dp = new DerivansParameter();
		dp.setPathInput(pathTarget);
		Path configPath = Path.of("src/test/resources/config3/derivans-tif.ini");
		dp.setPathConfig(configPath);
		DerivansConfiguration dc = new DerivansConfiguration(dp);
		Derivans derivans = new Derivans(dc);
		derivans.create();

		// assert
		Path pdfWritten = pathTarget.resolve("zd1.pdf");
		assertTrue(Files.exists(pdfWritten));
	}

	public static Path arrangeMetsAndImagesFor737429(Path tempDir) throws IOException {
		Path pathTarget = tempDir.resolve("737429");
		if (Files.exists(pathTarget)) {
			Files.walk(pathTarget).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(pathTarget);
		}
		Files.createDirectory(pathTarget);
		Path metsTarget = pathTarget.resolve("737429.xml");
		Files.copy(TestResource.HD_Aa_737429.get(), metsTarget);
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
			String imagePath = String.format("%04d.jpg", i);
			Path jpgFile = imageDir.resolve(imagePath);
			writeImage(jpgFile, width, height, BufferedImage.TYPE_3BYTE_BGR, "JPG");
		}
	}

	public static void writeImage(Path imagePath, int width, int height, int type, String format) throws IOException {
		BufferedImage bi2 = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		Font theFont = new Font("Helvetica", Font.BOLD, 84);
		Graphics2D g2d = bi2.createGraphics();
		g2d.setColor(Color.WHITE);
		g2d.setFont(theFont);
		FontMetrics fontMetrics = g2d.getFontMetrics();
		String imageName = imagePath.getFileName().toString();
		Rectangle2D rect = fontMetrics.getStringBounds(imageName, g2d);
		int centerX = (bi2.getWidth() - (int) rect.getWidth()) / 2;
		g2d.drawString(imageName, centerX, 500);
		g2d.dispose();
		ImageIO.write(bi2, format, imagePath.toFile());
	}

	public static void generateJpgsFromList(Path imageDir, int width, int height, List<String> labels)
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

	public static void copyTree(Path pathSource, Path pathTarget) {
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
