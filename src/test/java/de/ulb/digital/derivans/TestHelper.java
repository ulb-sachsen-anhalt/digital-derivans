package de.ulb.digital.derivans;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

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

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.xml.sax.SAXException;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfOutline;
import com.itextpdf.kernel.pdf.PdfReader;

import de.ulb.digital.derivans.data.mets.METS;
import de.ulb.digital.derivans.data.xml.XMLHandler;
import de.ulb.digital.derivans.model.ocr.OCRData;
import de.ulb.digital.derivans.model.pdf.MetadataType;
import de.ulb.digital.derivans.model.pdf.PDFMetadata;
import de.ulb.digital.derivans.model.pdf.PDFOutlineEntry;
import de.ulb.digital.derivans.model.text.Textline;
import de.ulb.digital.derivans.model.text.Word;

/**
 * 
 * Little test helpers assembled adhere
 * 
 * @author u.hartwig
 */
public class TestHelper {

	static String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
	static String METS_SCHEMA = "http://www.loc.gov/METS/";

	public static final Path ULB_MAX_PATH = Path.of("MAX");

	public static final String IMAGE = "IMAGE";

	public static int textMarginLeft = 50;

	public static int textMarginTop = 200;

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
		builder.setNamespace(METS.NS_METS);
		return builder.compileWith(XPathFactory.instance());
	}

	public static List<String> fixture737429ImageLabel = List.of("737434", "737436", "737437", "737438");

	public static Path fixturePrint737429(Path tempDir, Path srcMets, String imageSubDir) throws IOException {
		Path pathTarget = tempDir.resolve("737429");
		if (Files.exists(pathTarget)) {
			Files.walk(pathTarget).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			Files.delete(pathTarget);
		}
		Files.createDirectory(pathTarget);
		Path metsTarget = pathTarget.resolve("737429.xml");
		Files.copy(srcMets, metsTarget);
		Path imagePath = pathTarget.resolve(imageSubDir);
		generateJpgsFromList(imagePath, 500, 800, fixture737429ImageLabel);
		return pathTarget;
	}

	public static Path fixturePrint737429(Path tempDir) throws IOException {
		return fixturePrint737429(tempDir, TestResource.VLS_HD_Aa_737429.get(), "MAX");
	}

	public static Path fixturePrint737429(Path tempDir, String imageSubDir) throws IOException {
		return fixturePrint737429(tempDir, TestResource.VLS_HD_Aa_737429.get(), imageSubDir);
	}

	public static String getText(Path writtenData, int pageNr) throws Exception {
		PDFInspector inspector = new PDFInspector(writtenData);
		return inspector.getPageText(pageNr);
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

	public static class PDFInspector {

		private Path pdfPath;

		public PDFInspector(Path pdfPath) {
			this.pdfPath = pdfPath;
		}

		public Path getPdfPath() {
			return pdfPath;
		}

		public String getPageTextLinebreaksReplaced(int pageNr) throws DigitalDerivansException {
			var someText = getPageText(pageNr);
			if (!someText.isEmpty()) {
				return someText.replace("\n", " ");
			}
			return someText;
		}

		/**
		 * 
		 * Simple approach to check if page contains any textual data
		 * Please note: Page count starts by "1", this is the very first page.
		 * Textual representation is actually not 1:1 identical
		 * 
		 * @param pageNr
		 * @return
		 * @throws DigitalDerivansException
		 */
		public String getPageText(int pageNr) throws DigitalDerivansException {
			if (!Files.exists(this.pdfPath)) {
				throw new DigitalDerivansException("Invalid PDF file path " + pdfPath);
			}
			try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(pdfPath))) {
				var ts = new DerivansPDFStripper();
				ts.setStartPage(pageNr);
				ts.setEndPage(pageNr);
				return ts.getText(document);
			} catch (IOException e) {
				var msg = this.pdfPath + ": " + e.getMessage();
				throw new DigitalDerivansException(msg);
			}
		}

		public String getImageInfo(int pageNr) throws DigitalDerivansException {
			try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(pdfPath))) {
				PDPage page = document.getPage(pageNr - 1); // PDFBox uses 0-based indexing
				var resources = page.getResources();
				var xObjects = resources.getXObjectNames();
				for (var xObjName : xObjects) {
					var xObj = resources.getXObject(xObjName);
					if (xObj instanceof org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject) {
						var img = (org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject) xObj;
						long imageSize = 0;
						var imageStream = img.getStream();
						if (imageStream != null) {
							imageSize = imageStream.getLength();
						}
						return String.format("Image %s: %s, %d x %d px, %d bytes",
								xObjName.getName(),
								img.getSuffix(),
								img.getWidth(),
								img.getHeight(),
								imageSize);
					}
				}
			} catch (IOException e) {
				var msg = this.pdfPath + ": " + e.getMessage();
				throw new DigitalDerivansException(msg);
			}
			return "No image found";
		}

		/**
		 * 
		 * Retrive information concerning PDF Metadata
		 * 
		 * @return
		 * @throws IOException
		 */
		public PDFMetadata getPDFMetaInformation() throws IOException,
				DigitalDerivansException {
			try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(pdfPath))) {
				PDDocumentInformation pdfInfo = document.getDocumentInformation();
				String author = pdfInfo.getAuthor();
				String title = pdfInfo.getTitle();
				String creator = pdfInfo.getCreator();
				EnumMap<MetadataType, String> mdMap = new EnumMap<>(MetadataType.class);
				mdMap.put(MetadataType.AUTHOR, author);
				mdMap.put(MetadataType.TITLE, title);
				mdMap.put(MetadataType.CREATOR, creator);
				var pdfMeta = new PDFMetadata(mdMap);
				PDDocumentCatalog catalog = document.getDocumentCatalog();
				try {
					PDMetadata meta = catalog.getMetadata();
					InputStream xmpStream = meta.exportXMPMetadata();
					var handler = new XMLHandler(xmpStream);
					pdfMeta.setXmpMetadata(handler.getDocument());
				} catch (IOException | NullPointerException e) {
					// pass
				}
				return pdfMeta;
			}
		}

		public PDFOutlineEntry getOutline() throws IOException {
			PdfReader reader = new PdfReader(this.pdfPath.toFile());
			PdfOutline pdfOutline = null;
			try (PdfDocument pdfDocument = new PdfDocument(reader)) {
				pdfOutline = pdfDocument.getOutlines(true);
			}
			String label = pdfOutline.getTitle();
			String dest = METS.UNSET;
			if (pdfOutline.getDestination() != null) {
				dest = pdfOutline.getDestination().getPdfObject().toString();
			}
			PDFOutlineEntry root = new PDFOutlineEntry(label, dest);
			if (!pdfOutline.getAllChildren().isEmpty()) {
				for (var child : pdfOutline.getAllChildren()) {
					traverseOutline(root, child);
				}
			}
			return root;
		}

		public static void traverseOutline(PDFOutlineEntry currParent, PdfOutline currChild) {
			String label = currChild.getTitle();
			String dest = METS.UNSET;
			if (currChild.getDestination() != null) {
				dest = currChild.getDestination().getPdfObject().toString();
			}
			PDFOutlineEntry nextChild = new PDFOutlineEntry(label, dest);
			currParent.getOutlineEntries().add(nextChild);
			List<PdfOutline> nextChildren = currChild.getAllChildren();
			for (var nextOutlineChild : nextChildren) {
				traverseOutline(nextChild, nextOutlineChild);
			}
		}

		public int countPages() throws DigitalDerivansException {
			if (!Files.exists(this.pdfPath)) {
				throw new DigitalDerivansException("Invalid PDF file path " + pdfPath);
			}
			try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(pdfPath))) {
				return document.getNumberOfPages();
			} catch (IOException e) {
				var msg = this.pdfPath + ": " + e.getMessage();
				throw new DigitalDerivansException(msg);
			}
		}
	}

	/**
	 * 
	 * Create artificial OCR data
	 * (Between word/token on-a-line +10 pixel gap)
	 * 
	 * @return
	 */
	public static OCRData italianOCR() {
		List<Word> texts1 = new ArrayList<>();
		texts1.add(new Word("BELLA", new Rectangle(textMarginLeft, textMarginTop, 70, 30)));
		texts1.add(new Word("CHIAO", new Rectangle(130, textMarginTop, 70, 30)));
		texts1.add(new Word("(DELLE", new Rectangle(210, textMarginTop, 80, 30)));
		texts1.add(new Word("MODINE)", new Rectangle(300, textMarginTop, 100, 30)));
		List<Word> texts2 = new ArrayList<>();
		texts2.add(new Word("Alla", new Rectangle(textMarginLeft, 250, 40, 30)));
		texts2.add(new Word("matina,", new Rectangle(100, 250, 75, 25)));
		texts2.add(new Word("appena", new Rectangle(185, 250, 70, 25)));
		texts2.add(new Word("alzata", new Rectangle(265, 250, 60, 25)));
		List<Word> texts3 = new ArrayList<>();
		texts3.add(new Word("o", new Rectangle(textMarginLeft, 300, 10, 25)));
		texts3.add(new Word("bella", new Rectangle(70, 300, 50, 25)));
		texts3.add(new Word("chiao,", new Rectangle(130, 300, 60, 25)));
		texts3.add(new Word("bella", new Rectangle(200, 300, 50, 25)));
		texts3.add(new Word("chiao,", new Rectangle(260, 300, 60, 25)));
		texts3.add(new Word("bella", new Rectangle(330, 300, 50, 25)));
		texts3.add(new Word("chiao", new Rectangle(390, 300, 50, 25)));
		texts3.add(new Word("chiao", new Rectangle(450, 300, 50, 25)));
		texts3.add(new Word("chiao!", new Rectangle(510, 300, 60, 25)));
		List<Textline> lines = List.of(
				new Textline(texts1),
				new Textline(texts2),
				new Textline(texts3));
		return new OCRData(lines, new Dimension(575, 799));
	}

	/**
	 * 
	 * Assuming char width = 1/2 height
	 * 
	 * @return
	 */
	public static OCRData arabicOCR() {
		var w1 = new Word("٨", new Rectangle(400, textMarginTop, 15, 30)); // ٨
		assertEquals(1, w1.getText().length());
		var w2 = new Word("ديبا", new Rectangle(200, textMarginTop, 60, 30));
		assertEquals(4, w2.getText().length());
		var w3 = new Word("جه", new Rectangle(160, textMarginTop, 30, 30));
		assertEquals(2, w3.getText().length());
		//
		var w4 = new Word("\u0627\u0644\u0633\u0639\u0631",
				new Rectangle(400, textMarginTop + 40, 75, 30)); // السعر
		assertEquals(5, w4.getText().length());
		var w5 = new Word("\u0627\u0644\u0627\u062c\u0645\u0627\u0644\u064a",
				new Rectangle(250, textMarginTop + 40, 120, 30)); // الاجمالي
		assertEquals(8, w5.getText().length());
		List<Textline> lines = List.of(
				new Textline(List.of(w1, w2, w3)),
				new Textline(List.of(w4, w5)));
		return new OCRData(lines, new Dimension(600, 800));
	}
}

/**
 * cf.
 * https://stackoverflow.com/questions/32978179/using-pdfbox-to-get-location-of-line-of-text
 */
class DerivansPDFStripper extends PDFTextStripper {

	static final Locale loc = Locale.forLanguageTag("de-De");

	boolean startOfLine = true;

	@Override
	protected void startPage(PDPage page) throws IOException {
		this.startOfLine = true;
		super.startPage(page);
	}

	@Override
	protected void writeLineSeparator() throws IOException {
		this.startOfLine = true;
		super.writeLineSeparator();
	}

	@Override
	protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
		if (this.startOfLine) {
			TextPosition tPos = textPositions.get(0);
			writeString(String.format(DerivansPDFStripper.loc, "[x:%.2f y:%.2f %spt]", tPos.getXDirAdj(),
					tPos.getYDirAdj(), tPos.getFontSize()));
			this.startOfLine = false;
		}
		super.writeString(text, textPositions);
	}

}
