package de.ulb.digital.derivans.derivate.pdf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.itextpdf.awt.geom.Point2D;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Header;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.ICC_Profile;
import com.itextpdf.text.pdf.PdfAConformanceLevel;
import com.itextpdf.text.pdf.PdfAWriter;
import com.itextpdf.text.pdf.PdfAction;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfDestination;
import com.itextpdf.text.pdf.PdfOutline;
import com.itextpdf.text.pdf.PdfWriter;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.config.DefaultConfiguration;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.DigitalStructureTree;
import de.ulb.digital.derivans.model.ICharacterToken;
import de.ulb.digital.derivans.model.IPDFProcessor;
import de.ulb.digital.derivans.model.ocr.OCRData;
import de.ulb.digital.derivans.model.pdf.PDFDocument;
import de.ulb.digital.derivans.model.pdf.PDFPage;
import de.ulb.digital.derivans.model.pdf.PDFElement;
import de.ulb.digital.derivans.model.pdf.PDFMetadata;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;
import de.ulb.digital.derivans.model.text.Textline;

/**
 * @author hartwig
 */
public class IText5Processor implements IPDFProcessor {

	private static final Logger LOGGER = LogManager.getLogger(IText5Processor.class);

	// what IText(5) assumes for resolution
	public static final float ITEXT_ASSUMES_DPI = 72.0f;

	private String renderLevel;

	private String renderModus;

	private float dpiScale = 1.0f;

	private DigitalStructureTree structure = new DigitalStructureTree();

	private Document document;

	private boolean debugRender;

	private DerivateStepPDF pdfStep;

	private List<DigitalPage> pages;

	public static final float ITEXT_FONT_STEP = .5f;

	// for introspection
	PDFDocument pdfDoc = new PDFDocument();

	public IText5Processor() {
		this.document = new Document();
	}

	@Override
	public void init(DerivateStepPDF pdfStep, List<DigitalPage> pages, DigitalStructureTree structure)
			throws DigitalDerivansException {
		this.pdfStep = pdfStep;
		this.renderLevel = pdfStep.getRenderLevel();
		this.renderModus = pdfStep.getRenderModus();
		this.debugRender = pdfStep.getDebugRender();
		this.setDpi(pdfStep.getImageDpi());
		this.structure = structure;
		this.pages = pages;
	}

	private void setDpi(int dpi) throws DigitalDerivansException {
		if (dpi >= ITEXT_ASSUMES_DPI && dpi <= 600) {
			LOGGER.info("set dpi for image scaling {}", dpi);
			this.dpiScale = ITEXT_ASSUMES_DPI / dpi;
		} else {
			String msg = String.format("tried to set invalid dpi: '%s' (must be in range 72 - 600)", dpi);
			LOGGER.error(msg);
			throw new DigitalDerivansException(msg);
		}
	}

	@Override
	public void addMetadata() {
		// create introspection object
		PDFMetadata metadata = new PDFMetadata();
		// metadata must be added before content
		document.addTitle(this.pdfStep.getTitle());
		metadata.setTitle(this.pdfStep.getTitle());
		document.addAuthor(this.pdfStep.getAuthor());
		metadata.setAuthor(this.pdfStep.getAuthor());
		Optional<String> optCreator = this.pdfStep.getCreator();
		if (optCreator.isPresent()) {
			document.addCreator(optCreator.get());
			metadata.setCreator(optCreator);
		}
		Optional<String> optKeywords = this.pdfStep.getKeywords();
		if (optKeywords.isPresent()) {
			document.addKeywords(optKeywords.get());
		}
		// custom metadata entry -> com.itextpdf.text.Header
		Optional<String> optLicense = this.pdfStep.getLicense();
		if (optLicense.isPresent()) {
			try {
				document.add(new Header("Access condition", optLicense.get()));
				document.add(new Header("Published", this.pdfStep.getPublicationYear()));
			} catch (DocumentException e) {
				e.printStackTrace();
			}
		}
		metadata.setTitle(this.pdfStep.getTitle());
		this.pdfDoc.setMetadata(metadata);
	}

	@Override
	public PDFDocument write(File fileDescriptor) throws DigitalDerivansException {
		
		try (FileOutputStream fos = new FileOutputStream(fileDescriptor)) {
			// must call for writer before add metadata
			PdfWriter writer = null;
			if (this.pdfStep.getConformanceLevel() != null && !this.debugRender) {
				PdfAConformanceLevel pdfaLevel = PdfAConformanceLevel.valueOf(this.pdfStep.getConformanceLevel());
				writer = PdfAWriter.getInstance(document, fos, pdfaLevel);
			} else {
				writer = PdfWriter.getInstance(document, fos);
			}

			this.addMetadata();
			writer.createXmpMetadata();

			// open document to add content data
			document.open();

			// add pages
			var resultPages = this.addPages(document, writer, pages);
			pdfDoc.addPages(resultPages);
			document.setPageCount(resultPages.size());

			// add outline from structure
			if (structure != null) {
				this.addOutline(writer, resultPages.size());
			}

			// add some icc-profile, hopefully matching included images
			if (this.pdfStep.getConformanceLevel() != null && !this.debugRender) {
				String iccPath = "icc/sRGB_CS_profile.icm";
				InputStream is = this.getClass().getClassLoader().getResourceAsStream(iccPath);
				ICC_Profile icc = ICC_Profile.getInstance(is);
				writer.setOutputIntents("Custom", "", "http://www.color.org", "sRGB IEC61966-2.1", icc);
			}

			// finally close resources
			document.close();
			writer.close();

		} catch (DocumentException | IOException exc) {
			LOGGER.error(exc);
			throw new DigitalDerivansException(exc);
		}

		return pdfDoc;
	}

	public List<PDFPage> addPages(Document document, PdfWriter writer, List<DigitalPage> pages) throws DigitalDerivansException {
		List<PDFPage> resultPages = new ArrayList<>();
		try {
			// get font
			FontHandler handler = new FontHandler();
			BaseFont font = handler.forPDF("ttf/DejaVuSans.ttf");
			// process each page
			for (int i = 0; i < pages.size(); i++) {
				String imagePath = pages.get(i).getImagePath().toString();
				Image image = Image.getInstance(imagePath);
				image.scaleAbsolute(image.getWidth() * this.dpiScale, image.getHeight() * this.dpiScale);
				float imageHeight = image.getScaledHeight();
				float imageWidth = image.getScaledWidth();
				// re-set page document dimension for *each* image page since there
				// occour different page size due inlay maps, illustrations, etc.
				if (document.setPageSize(new Rectangle(imageWidth, imageHeight))) {
					LOGGER.debug("re-set document pageSize {}x{}", imageWidth, imageHeight);
				}
				// init next page of document
				document.newPage();
				var result = addPage(writer, font, pages.get(i));
				result.setDimension((int)imageWidth, (int)imageHeight);
				result.setNumber(i+1);
				resultPages.add(result);
			}
		} catch (IOException | DocumentException e) {
			throw new DigitalDerivansException(e);
		}
		return resultPages;
	}

	private PDFPage addPage(PdfWriter writer, BaseFont font, DigitalPage page) throws IOException, DocumentException {
		PDFPage resultPage = new PDFPage();
		String imagePath = page.getImagePath().toString();
		Image image = Image.getInstance(imagePath);
		image.scaleAbsolute(image.getWidth() * this.dpiScale, image.getHeight() * this.dpiScale);
		LOGGER.info("addPage rescale: {}x{}", image.getScaledWidth(), image.getScaledHeight());

		// push image data as base graphic content
		PdfContentByte over = writer.getDirectContent();
		image.setAbsolutePosition(0f, 0f);
		over.addImage(image);

		// push ocr if any
		Optional<OCRData> optOcr = page.getOcrData();
		if (optOcr.isPresent()) {
			LOGGER.debug("handle optional ocr for {} (render: {})", page.getImagePath(), this.debugRender);
			OCRData ocrData = optOcr.get();
			// get to know current image dimensions
			// most likely to differ due subsequent scaling derivation steps
			// height has also changed if additional footer has been applied to image
			int pageHeight = ocrData.getPageHeight();
			int footerHeight = 0;
			Optional<Integer> optFooterHeight = page.getFooterHeight();
			if (optFooterHeight.isPresent()) {
				footerHeight = optFooterHeight.get();
				LOGGER.debug("add footerHeight '{}' to original pageHeight '{}' from OCR-time", footerHeight,
						pageHeight);
				pageHeight += footerHeight;
			}
			// need to scale?
			// page height corresponds to original image height
			float currentImageHeight = image.getScaledHeight();
			float ratio = currentImageHeight / pageHeight;
			if (Math.abs(1.0 - ratio) > 0.01) {
				LOGGER.trace("scale ocr data for '{}' by '{}'", page.getImagePath(), ratio);
				ocrData.scale(ratio);
				resultPage.setScale(ratio);
			}
			// place optional text *behind* image
			PdfContentByte cb = writer.getDirectContent();
			// optional: if layer shall be visible, show words too on top
			if (this.debugRender) {
				cb = writer.getDirectContent();
			}
			cb.saveState();
			List<Textline> ocrLines = ocrData.getTextlines();
			// communicate once
			if (this.renderLevel.equalsIgnoreCase(DefaultConfiguration.DEFAULT_RENDER_LEVEL)) {
				LOGGER.trace("render text at line-level");
			} else if (this.renderLevel.equalsIgnoreCase("word")) {
				LOGGER.trace("render text at word-level");
			}
			// actual rendering about to start
			for (Textline line : ocrLines) {
				if (this.renderLevel.equalsIgnoreCase(DefaultConfiguration.DEFAULT_RENDER_LEVEL)) {
					var resultElement = render(font, (int) currentImageHeight, cb, line);
					resultPage.add(resultElement);
				} else if (this.renderLevel.equalsIgnoreCase("word")) {
					List<ICharacterToken> tokens = line.getTokens();
					for (ICharacterToken word : tokens) {
						var resultElement = render(font, (int) currentImageHeight, cb, word);
						resultPage.add(resultElement);
					}
				}
			}
			cb.restoreState();
			// optional: render ocr data outlines for debugging visualization
			// *must not* be done if PDF A Conformance requested - doesn't
			// permit Transparency
			if (this.debugRender) {
				LOGGER.debug("render ocr outlines {} (height: {})", ocrLines, currentImageHeight);
				this.renderOcrOutlines(writer, ocrLines, currentImageHeight);
			}
		} else {
			LOGGER.info("no ocr data present for '{}'", page.getImagePath());
		}
		return resultPage;
	}
	
	/**
	 * 
	 * Render single textual tokens, if a valid fontSize can be calculated,
	 * which acutally means a line or a word token
	 * 
	 * @param font
	 * @param pageHeight
	 * @param cb
	 * @param line
	 */
	private PDFElement render(BaseFont font, int pageHeight, PdfContentByte cb, ICharacterToken token) {
		String text = token.getText();
		var result = new PDFElement(text, token.getBox());
		Rectangle box = toItextBox(token.getBox());
		float fontSize = calculateFontSize(font, text, box.getWidth(), box.getHeight());
		if (fontSize < IPDFProcessor.MIN_CHAR_SIZE) {
			LOGGER.warn("font too small: '{}'(min:{}) resist to render text '{}'", fontSize, IPDFProcessor.MIN_CHAR_SIZE, text);
			return result;
		}
		result.setFontSize(fontSize);
		float x = box.getLeft();
		float y = pageHeight - box.getBottom();
		// looks like we need to go down a bit because
		// font seems to be rendered not from bottom, therefore introduce v
		// v = y - fontSize * MIN_CHAR_SIZE x, with X is were the magic starts: x=1 too low, x=0.5 too
		// high
		float v = y - (fontSize * IPDFProcessor.MIN_CHAR_SIZE);
		if (this.debugRender) {
			LOGGER.trace("put '{}' at {}x{} (fontsize:{})", text, x, v, fontSize);
		}
		// propably hide text layer font
		if (this.renderModus.equalsIgnoreCase("invisible")) {
			cb.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_INVISIBLE);
		}
		// rendering depends on text orientation
		if (token.isLTR()) {
			cb.beginText();
			cb.setFontAndSize(font, fontSize);
			cb.showTextAligned(Element.ALIGN_BOTTOM, text, x, v, 0);
			cb.endText();
		} else {
			Font f = new Font(font, fontSize);
			float leading = .0f;
			ColumnText ct = new ColumnText(cb);
			float llx = x;
			float lly = v;
			float urx = box.getRight();
			float ury = pageHeight - box.getTop();
			java.awt.Rectangle r = new java.awt.Rectangle((int) llx, (int) box.getBottom(),
					(int) (urx - llx),
					(int) (box.getTop() - box.getBottom()));
			result.setBox(r);
			result.setLeading(leading);
			if(this.debugRender) {
				BaseColor c3 = new BaseColor(.0f, 0.5f, .0f, .6f);
				drawOutline(r, cb, pageHeight, c3, 0.3f);
			}
			ct.setSimpleColumn(llx, lly, urx, ury);
			ct.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
			ct.setAlignment(Element.ALIGN_BOTTOM);
			ct.setLeading(leading); // cf. https://de.wikipedia.org/wiki/Zeilendurchschuss
			ct.addElement(new Paragraph(leading, text, f)); // pass leading otherwise data loss
			try {
				ct.go();
			} catch (DocumentException e) {
				LOGGER.error(e.getMessage());
			}
		}
		result.setPrinted(true);
		return result;
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
			fontSizeX -= IText5Processor.ITEXT_FONT_STEP;
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
	
	private void renderOcrOutlines(PdfWriter writer, List<Textline> ocrLines, float theHeight) {
		BaseColor c1 = new BaseColor(.2f, 0.0f, .5f, .6f); // dark blue
		BaseColor c2 = new BaseColor(.5f, 0.0f, .2f, .6f); // dark red
		PdfContentByte cb2 = writer.getDirectContent();
		cb2.saveState();
		for (Textline line : ocrLines) {
			for (ICharacterToken txt : line.getTokens()) {
				drawOutline(txt.getBox(), cb2, theHeight, c2, .25f);
			}
			drawOutline(line.getBox(), cb2, theHeight, c1, .5f);
		}
		cb2.restoreState();
	}

	private void drawOutline(java.awt.Rectangle rectangle, PdfContentByte cb, float pageHeight, BaseColor c,
			float lineWidth) {
		List<Point2D.Float> points = asPoints(rectangle);
		cb.setColorStroke(c);
		cb.setLineWidth(lineWidth);
		// move to end, then draw lines
		Point2D.Float pLast = points.get(points.size() - 1);
		cb.moveTo(pLast.x, pageHeight - pLast.y);
		for (int i = 0; i < points.size(); i++) {
			Point2D.Float p = points.get(i);
			cb.lineTo(p.x, pageHeight - p.y);
		}
		cb.stroke();
	}

	static List<Point2D.Float> asPoints(java.awt.Rectangle rect) {
		List<Point2D.Float> points = new ArrayList<>();
		float minX = (float) rect.getMinX();
		float minY = (float) rect.getMinY();
		float maxX = (float) rect.getMaxX();
		float maxY = (float) rect.getMaxY();
		points.add(new Point2D.Float(minX, minY));
		points.add(new Point2D.Float(maxX, minY));
		points.add(new Point2D.Float(maxX, maxY));
		points.add(new Point2D.Float(minX, maxY));
		return points;
	}

	public boolean addOutline(PdfWriter pdfWriter, int nPages) {
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
}
