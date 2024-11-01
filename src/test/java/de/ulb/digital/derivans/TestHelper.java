package de.ulb.digital.derivans;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.xml.sax.SAXException;

import de.ulb.digital.derivans.data.IMetadataStore;
import de.ulb.digital.derivans.derivate.pdf.PDFInspector;

/**
 * 
 * Some little test helpers assembled adhere
 * 
 * @author u.hartwig
 */
public class TestHelper {

	public static void generateImages(Path imageDir, int width, int height, int number, String labelFormat)
			throws IOException {
		if (Files.exists(imageDir)) {
			Files.delete(imageDir);
		}
		Files.createDirectory(imageDir);
		var imageFormat = labelFormat.substring(labelFormat.lastIndexOf('.') + 1).toUpperCase();
		for (int i = 1; i <= number; i++) {
			String imageLabel = String.format(labelFormat, i);
			Path imagePath = imageDir.resolve(imageLabel);
			writeImage(imagePath, width, height, BufferedImage.TYPE_3BYTE_BGR, imageFormat);
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
			Graphics2D g2d = (Graphics2D) bi2.getGraphics();
			g2d.setColor(Color.LIGHT_GRAY);
			g2d.fillRect(0, 0, width, height);
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

	public static XPathExpression<Element> generateXpression(String xpathStr) {
		XPathBuilder<Element> builder = new XPathBuilder<Element>(xpathStr, Filters.element());
		builder.setNamespace(IMetadataStore.NS_METS);
		return builder.compileWith(XPathFactory.instance());
	}

	public static List<String> fixture737429ImageLabel = List.of("737434", "737436", "737437", "737438");

	public static Path fixturePrint737429(Path tempDir, Path srcMets) throws IOException {
		Path pathTarget = tempDir.resolve("737429");
		if (Files.exists(pathTarget)) {
			Files.walk(pathTarget).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(pathTarget);
		}
		Files.createDirectory(pathTarget);
		Path metsTarget = pathTarget.resolve("737429.xml");
		Files.copy(srcMets, metsTarget);
		Path imagePath = pathTarget.resolve("MAX");
		generateJpgsFromList(imagePath, 475, 750, fixture737429ImageLabel);
		return pathTarget;
	}

	public static Path fixturePrint737429(Path tempDir) throws IOException {
		return fixturePrint737429(tempDir, TestResource.HD_Aa_737429.get());
	}

	public static String getText(Path writtenData, int pageNr) throws Exception {
		PDFInspector inspector = new PDFInspector(writtenData);
		return inspector.getPageText(pageNr, "", "");
	}

	public static String getTextAsSingleLine(Path writtenData, int pageNr) throws Exception {
		PDFInspector inspector = new PDFInspector(writtenData);
		return inspector.getPageTextLinebreaksReplaced(pageNr);
	}

	/**
	 * 
	 * In case of invalid Documents {@link org.xml.sax.SAXParseException}
	 * will be thrown.
	 * In case of valid Documents, nothing is thrown but "true" returned.
	 * 
	 */
	public static boolean validateXML(Path xmlPath, Path xsdPath) throws SAXException, IOException,
			ParserConfigurationException {
		var dbf = DocumentBuilderFactory.newInstance();
		String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
		String METS_SCHEMA = "http://www.loc.gov/METS/";
		dbf.setAttribute(JAXP_SCHEMA_LANGUAGE, METS_SCHEMA);
		dbf.setNamespaceAware(true);
		DocumentBuilder parser = dbf.newDocumentBuilder();
		org.w3c.dom.Document document = parser.parse(xmlPath.toFile());
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Source schemaFile = new StreamSource(xsdPath.toFile());
		Schema schema = factory.newSchema(schemaFile);
		Validator validator = schema.newValidator();
		validator.validate(new DOMSource(document));
		return true;
	}
}
