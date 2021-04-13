package de.ulb.digital.derivans.derivate;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Header;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ICC_Profile;
import com.itextpdf.text.pdf.PdfAConformanceLevel;
import com.itextpdf.text.pdf.PdfAWriter;
import com.itextpdf.text.pdf.PdfAction;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfDestination;
import com.itextpdf.text.pdf.PdfOutline;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DescriptiveData;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.DigitalStructureTree;
import de.ulb.digital.derivans.model.PDFMetaInformation;
import de.ulb.digital.derivans.model.ocr.OCRData;
import de.ulb.digital.derivans.model.ocr.OCRData.Textline;

/**
 * 
 * Create PDF derivate from
 * <ul>
 * <li>image data in JPG format (and extension *.jpg)</li>
 * <li>XML OCR-Data (ALTO)</li>
 * </ul>
 * 
 * @author hartwig
 *
 */
public class PDFDerivateer extends BaseDerivateer {

	private static final Logger LOGGER = LogManager.getLogger(PDFDerivateer.class);

	private DigitalStructureTree structure;

	private DescriptiveData description;

	private AtomicInteger nPagesWithOCR = new AtomicInteger();

	private String pdfConformanceLevel;

	public PDFDerivateer(DerivansData input, DerivansData output, DigitalStructureTree tree,
			DescriptiveData descriptiveData, List<DigitalPage> pages, String conformanceLevel) {
		super(input, output);
		this.structure = tree;
		this.description = descriptiveData;
		this.digitalPages = pages;
		this.pdfConformanceLevel = conformanceLevel;
	}

	/**
	 * 
	 * Create new instance on top of {@link BaseDerivateer}
	 * 
	 * @param basic
	 * @param tree
	 * @param descriptiveData
	 * @param pages
	 */
	public PDFDerivateer(BaseDerivateer basic, Path outputPath, DigitalStructureTree tree,
			DescriptiveData descriptiveData, List<DigitalPage> pages, String conformanceLevel) {
		super(basic.getInput(), basic.getOutput());
		// necessary to set specific filename in output dir
		this.getOutput().setPath(outputPath);
		this.structure = tree;
		this.description = descriptiveData;
		this.digitalPages = pages;
		this.pdfConformanceLevel = conformanceLevel;
	}

	private int addPages(Document document, PdfWriter writer, List<DigitalPage> pages) throws DigitalDerivansException {
		int pagesAdded = 0;

		try {
			// get font
			FontHandler handler = new FontHandler();
			BaseFont font = handler.forPDF("ttf/DejaVuSans.ttf");
			// process each page
			for (DigitalPage page : pages) {
				document.newPage();
				addPage(writer, font, page);
				pagesAdded++;
			}
		} catch (IOException | DocumentException e) {
			throw new DigitalDerivansException(e);
		}

		return pagesAdded;
	}

	private void addPage(PdfWriter writer, BaseFont font, DigitalPage page) throws IOException, DocumentException {

		PdfContentByte over = writer.getDirectContent();
		String imagePath = page.getImagePath().toString();
		Image image = Image.getInstance(imagePath);
		float actualImageDerivateHeight = image.getHeight();
		image.setAbsolutePosition(0f, 0f);
		over.addImage(image);

		// some ocr, if any
		Optional<OCRData> optOcr = page.getOcrData();
		if (optOcr.isPresent()) {

			OCRData ocrData = optOcr.get();

			// get to know current image dimensions
			// most likely to differ due subsequent scaling derivation steps
			int pageHeight = ocrData.getPageHeight();
			int footerHeight = 0;
			Optional<Integer> optFooterHeight = page.getFooterHeight();
			if (optFooterHeight.isPresent()) {
				footerHeight = optFooterHeight.get();
				pageHeight += footerHeight;
				LOGGER.debug("add footerHeight '{}' to original pageHeight '{}' from OCR-time", footerHeight,
						pageHeight);
			}
			// need to scale?
			float ratio = actualImageDerivateHeight / pageHeight;
			if (Math.abs(1.0 - ratio) > 0.01) {
				LOGGER.trace("scale ocr data for '{}' by '{}'", page.getImagePath(), ratio);
				ocrData.scale(ratio);
			}

			// place optional text *behind* image
			PdfContentByte cb = writer.getDirectContentUnder();
			cb.saveState();
			List<OCRData.Textline> ocrLines = ocrData.getTextlines();
			for (OCRData.Textline line : ocrLines) {
				renderLine(font, (int) actualImageDerivateHeight, cb, line);
			}
			cb.restoreState();

			// increment number of ocr-ed pages for each PDF
			nPagesWithOCR.getAndIncrement();

		} else {
			LOGGER.info("no ocr data present for '{}'", page.getImagePath());
		}
	}

	/**
	 * 
	 * Render a single row of textual tokens, if a valid fontSize can be calculated
	 * 
	 * @param font
	 * @param pageHeight
	 * @param cb
	 * @param line
	 */
	private static void renderLine(BaseFont font, int pageHeight, PdfContentByte cb, Textline line) {
		String text = line.getText();
		Rectangle box = toItextBox(line.getBounds());
		float fontSize = calculateFontSize(font, text, box.getWidth(), box.getHeight());
		if (fontSize < 1.0) {
			LOGGER.warn("attenzione - font to small: '{}' for text '{}' - resist to render", fontSize, line);
			return;
		}
		float x = box.getLeft();
		float y = pageHeight - box.getBottom();
		// looks like we need to go down a bit because
		// font seems to be rendered not from baseline but from v with shall be
		// v = y - fontSize
		float v = y - fontSize;
		LOGGER.trace("put '{}' at {}x{} (fontsize:{})", text, x, v, fontSize);
		cb.beginText();
		cb.setFontAndSize(font, fontSize);
		cb.showTextAligned(Element.ALIGN_LEFT, text, x, v, 0);
		cb.endText();
	}

	/**
	 * 
	 * Calculates font size to fit the given text into the specified width. Not
	 * exact but the best we have so far.
	 * 
	 * @param text
	 * @param width
	 * @param height
	 * @return
	 * @throws IOException
	 */
	static float calculateFontSize(BaseFont font, String text, float width, float height) {
		float sw = font.getWidth(text);
		float fontSizeX = sw / 1000.0f * height;
		Chunk chunk = new Chunk(text, new Font(font, fontSizeX));
		while (chunk.getWidthPoint() > width) {
			fontSizeX -= 3f;
			chunk = new Chunk(text, new Font(font, fontSizeX));
		}
		return fontSizeX;
	}

	private static Rectangle toItextBox(java.awt.Rectangle r) {
		com.itextpdf.awt.geom.Point tPoint = new com.itextpdf.awt.geom.Point(r.x, r.y);
		com.itextpdf.awt.geom.Dimension tDim = new com.itextpdf.awt.geom.Dimension(r.width, r.height);
		com.itextpdf.awt.geom.Rectangle tmp = new com.itextpdf.awt.geom.Rectangle(tPoint, tDim);
		return new Rectangle(tmp);
	}

	static boolean buildOutline(PdfWriter pdfWriter, int nPages, DigitalStructureTree structure) {
		PdfOutline rootOutline = pdfWriter.getRootOutline();
		rootOutline.setTitle(structure.getLabel());
		for (int i = 1; i <= nPages; i++) {
			if (structure.getPage() == i) {
				traverseStructure(pdfWriter, rootOutline, structure);
			}
		}
		return true;
	}

	static void traverseStructure(PdfWriter pdfWriter, PdfOutline rootOutline, DigitalStructureTree structure) {
		String label = structure.getLabel();
		int page = structure.getPage();
		PdfDestination dest = new PdfDestination(PdfDestination.FITB);
		PdfAction action = PdfAction.gotoLocalPage(page, dest, pdfWriter);
		LOGGER.debug("[PAGE {}] outlineEntry '{}'", page, label);
		if (structure.hasSubstructures()) {
			PdfOutline outline = new PdfOutline(rootOutline, action, label);
			for (DigitalStructureTree sub : structure.getSubstructures()) {
				traverseStructure(pdfWriter, outline, sub);
			}
		} else {
			new PdfOutline(rootOutline, action, label);
		}
	}

	static PDFMetaInformation getPDFMetaInformation(Path pdfPath) throws IOException {
		FileInputStream fis = new FileInputStream(pdfPath.toAbsolutePath().toString());
		PdfReader reader = new PdfReader(fis);

		Map<String, String> metadata = reader.getInfo();
		String author = metadata.get("Author");
		String title = metadata.get("Title");

		byte[] xmpMetadataBytes = reader.getMetadata();
		org.w3c.dom.Document xmpMetadata = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			// please sonarqube "Disable XML external entity (XXE) processing"
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setErrorHandler(null);
			xmpMetadata = builder.parse(new ByteArrayInputStream(xmpMetadataBytes));
		} catch (Exception e) {
			// no xmpMetadata is fine. so setting it to null.
		}
		fis.close();

		PDFMetaInformation pmi = new PDFMetaInformation(author, title, metadata, xmpMetadata);
		String creator = metadata.get("Creator");
		pmi.setCreator(creator);

		return pmi;
	}

	/**
	 * Use with caution as the PDF File needs to be read entirely in memory.
	 * 
	 * @param pdfPath
	 * @param pdfMetaInformation
	 * @throws IOException
	 * @throws DocumentException
	 */
	static void setPDFMetaInformation(Path pdfPath, PDFMetaInformation pdfMetaInformation)
			throws IOException, DocumentException {
		String pdfPathString = pdfPath.toAbsolutePath().toString();

		// load PDF to memory
		PdfReader reader = new PdfReader(Files.readAllBytes(pdfPath));
		FileOutputStream fos = new FileOutputStream(pdfPathString);
		PdfStamper stamper = new PdfStamper(reader, fos);
		stamper.setMoreInfo(pdfMetaInformation.getMetadata());
		stamper.close();
		reader.close();
	}

	@Override
	public boolean create() throws DigitalDerivansException {

		// resolve image paths
		resolver.enrichWithPath(getDigitalPages(), this.getInput().getPath());

		// get dimension of first page
		Image image = null;
		try {
			image = Image.getInstance(digitalPages.get(0).getImagePath().toString());
		} catch (BadElementException | IOException e) {
			throw new DigitalDerivansException(e);
		}
		Rectangle firstPageSize = new Rectangle(0, 0, image.getWidth(), image.getHeight());
		Document document = new Document(firstPageSize, 0f, 0f, 0f, 0f);

		boolean hasPagesAdded = false;
		boolean hasOutlineAdded = false;
		Path pathToPDF = this.output.getPath();

		PdfWriter writer = null;
		try (FileOutputStream fos = new FileOutputStream(pathToPDF.toFile())) {

			if (pdfConformanceLevel != null) {
				PdfAConformanceLevel pdfaLevel = PdfAConformanceLevel.valueOf(pdfConformanceLevel);
				writer = PdfAWriter.getInstance(document, fos, pdfaLevel);

			} else {
				writer = PdfWriter.getInstance(document, fos);
			}

			// metadata must be added afterwards creation of pdfWriter
			document.addTitle(this.description.getTitle());
			document.addAuthor(this.description.getPerson());
			Optional<String> optCreator = this.description.getCreator();
			if (optCreator.isPresent()) {
				document.addCreator(optCreator.get());
			}
			Optional<String> optKeywords = this.description.getKeywords();
			if (optKeywords.isPresent()) {
				document.addKeywords(optKeywords.get());
			}
			// custom metadata entry -> com.itextpdf.text.Header
			Optional<String> optLicense = this.description.getLicense();
			if (optLicense.isPresent()) {
				document.add(new Header("Access condition", optLicense.get()));
			}
			document.add(new Header("Published", this.description.getYearPublished()));

			writer.createXmpMetadata();
			document.open();

			// add profile if pdf-a required
			if (pdfConformanceLevel != null) {
				String iccPath = "icc/sRGB_CS_profile.icm";
				InputStream is = this.getClass().getClassLoader().getResourceAsStream(iccPath);
				ICC_Profile icc = ICC_Profile.getInstance(is);
				writer.setOutputIntents("Custom", "", "http://www.color.org", "sRGB IEC61966-2.1", icc);
			}

			int nPagesAdded = addPages(document, writer, digitalPages);

			// inform doc how many pages it holds
			document.setPageCount(nPagesAdded);
			hasPagesAdded = nPagesAdded == digitalPages.size();

			// process outline structure
			if (structure != null) {
				hasOutlineAdded = buildOutline(writer, nPagesAdded, structure);
			}
			// finally close resources
			document.close();
			writer.close();

		} catch (DocumentException | IOException exc) {
			LOGGER.error(exc);
			throw new DigitalDerivansException(exc);
		}

		boolean result = hasPagesAdded && hasOutlineAdded;
		LOGGER.info("created pdf '{}' with {} pages (outline:{})", pathToPDF, document.getPageNumber(),
				hasOutlineAdded);
		return result;
	}

	public AtomicInteger getNPagesWithOCR() {
		return nPagesWithOCR;
	}

}
