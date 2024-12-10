package de.ulb.digital.derivans.derivate.pdf;

import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceCmyk;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Point;
import com.itextpdf.kernel.pdf.PdfAConformanceLevel;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfOutline;
import com.itextpdf.kernel.pdf.PdfOutputIntent;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.PdfCanvasConstants;
import com.itextpdf.kernel.pdf.navigation.PdfExplicitDestination;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.Style;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.BaseDirection;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.pdfa.PdfADocument;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.config.TypeConfiguration;
import de.ulb.digital.derivans.data.io.TmpJarResource;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.DigitalStructureTree;
import de.ulb.digital.derivans.model.IPDFProcessor;
import de.ulb.digital.derivans.model.IVisualElement;
import de.ulb.digital.derivans.model.pdf.MetadataType;
import de.ulb.digital.derivans.model.pdf.PDFMetadata;
import de.ulb.digital.derivans.model.pdf.PDFPage;
import de.ulb.digital.derivans.model.pdf.PDFResult;
import de.ulb.digital.derivans.model.pdf.PDFTextElement;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;

/**
 * 
 * iText Implementation of {@link IPDFProcessor PDFProcessor API}
 * 
 * @author hartwig
 */
public class ITextProcessor implements IPDFProcessor {

	private static final String PDF_METADATA_LABEL_PUBLISHED = "Published";

	private static final String PDF_METADATA_LABEL_ACCESS_CONDITION = "Access condition";

	private static final Logger LOGGER = LogManager.getLogger(ITextProcessor.class);

	// Assumed output resolution
	public static final float ITEXT_ASSUMES_DPI = 72.0f;

	private TypeConfiguration renderLevel;

	private TypeConfiguration renderModus;

	private float dpiScale = 1.0f;

	private DigitalStructureTree structure = new DigitalStructureTree();

	// high-level access
	private Document document;

	// low-level access
	private PdfDocument iTextPDFDocument;

	private boolean debugRender;

	private DerivateStepPDF pdfStep;

	private List<DigitalPage> pages;

	public static final float ITEXT_FONT_STEP = .5f;

	private Style rtlStyle = new Style().setTextAlignment(TextAlignment.RIGHT)
			.setBaseDirection(BaseDirection.RIGHT_TO_LEFT);

	PDFResult reportDoc = new PDFResult();

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
		if (dpi <= 300 && dpi >= 600) {
			String msg = String.format("tried to set invalid dpi: '%s' (must be in range 72 - 600)", dpi);
			LOGGER.error(msg);
			throw new DigitalDerivansException(msg);
		} else {
			LOGGER.info("set dpi for image scaling {}", dpi);
			this.dpiScale = ITEXT_ASSUMES_DPI / dpi;
		}
	}

	/**
	 * 
	 * Called implicitely by #write
	 * metadata must be added before content
	 * 
	 */
	@Override
	public void addMetadata() {
		EnumMap<MetadataType, String> mdMap = new EnumMap<>(MetadataType.class);
		var docInfo = this.iTextPDFDocument.getDocumentInfo();
		docInfo.setMoreInfo(PdfName.Title.getValue(), this.pdfStep.getTitle());
		mdMap.put(MetadataType.TITLE, this.pdfStep.getTitle());
		docInfo.setMoreInfo(PdfName.Author.getValue(), this.pdfStep.getAuthor());
		mdMap.put(MetadataType.AUTHOR, this.pdfStep.getAuthor());
		Optional<String> optCreator = this.pdfStep.getCreator();
		if (optCreator.isPresent()) {
			docInfo.setMoreInfo(PdfName.Creator.getValue(), optCreator.get());
			mdMap.put(MetadataType.CREATOR, optCreator.get());
		}
		Optional<String> optKeywords = this.pdfStep.getKeywords();
		if (optKeywords.isPresent()) {
			docInfo.setMoreInfo(PdfName.Keywords.getValue(), optKeywords.get());
			mdMap.put(MetadataType.KEYWORDS, optKeywords.get());
		}
		Optional<String> optLicense = this.pdfStep.getLicense();
		if (optLicense.isPresent()) {
			docInfo.setMoreInfo(PDF_METADATA_LABEL_ACCESS_CONDITION, optLicense.get());
			docInfo.setMoreInfo(PDF_METADATA_LABEL_PUBLISHED, this.pdfStep.getPublicationYear());
		}
		PDFMetadata metadata = new PDFMetadata(mdMap);
		this.reportDoc.setMetadata(metadata);
	}

	@Override
	public PDFResult write(File fileDescriptor) throws DigitalDerivansException {
		try (FileOutputStream fos = new FileOutputStream(fileDescriptor)) {
			PdfWriter writer = new PdfWriter(fileDescriptor);
			if (this.pdfStep.getConformanceLevel() != null && !this.debugRender) {
				PdfAConformanceLevel conformanceLevel = PdfAConformanceLevel.getConformanceLevel("1", "B");
				String iccPath = "icc/sRGB_CS_profile.icm";
				InputStream is = this.getClass().getClassLoader().getResourceAsStream(iccPath);
				PdfOutputIntent outputIntent = new PdfOutputIntent("Custom", "",
						"http://www.color.org", "sRGB IEC61966-2.1", is);
				this.iTextPDFDocument = new PdfADocument(writer, conformanceLevel, outputIntent);
			} else {
				this.iTextPDFDocument = new PdfDocument(writer);
			}
			this.iTextPDFDocument = new PdfDocument(writer);
			this.document = new Document(iTextPDFDocument);
			this.addMetadata();
			var resultPages = this.addPages(this.pages);
			this.reportDoc.addPages(resultPages);
			if (this.structure != null && this.structure.getLabel() != null) {
				this.addOutline(resultPages.size());
			}
			this.document.close();
			this.iTextPDFDocument.close();
		} catch (IOException exc) {
			LOGGER.error(exc);
			throw new DigitalDerivansException(exc);
		}
		return this.reportDoc;
	}

	/**
	 * 
	 * Optional: set font style in advance for arabic and farsi
	 * 
	 * @param pages
	 * @return
	 * @throws DigitalDerivansException
	 */
	public List<PDFPage> addPages(List<DigitalPage> pages) throws DigitalDerivansException {
		List<PDFPage> resultPages = new ArrayList<>();
		LOGGER.trace("render pages at {}", this.renderLevel);
		try {
			PdfFont font = this.loadFont("ttf/DejaVuSans.ttf");
			this.rtlStyle = this.rtlStyle.setFont(font);
			for (int i = 0; i < pages.size(); i++) {
				DigitalPage pageIn = pages.get(i);
				String imagePath = pageIn.getImagePath().toString();
				Image image = new Image(ImageDataFactory.create(imagePath));
				float imageWidth = image.getImageWidth();
				float imageHeight = image.getImageHeight();
				if (Math.abs(1.0 - this.dpiScale) > 0.01) {
					image.scaleAbsolute(imageWidth * this.dpiScale, imageHeight * this.dpiScale);
					imageWidth = image.getImageScaledWidth();
					imageHeight = image.getImageScaledHeight();
					LOGGER.debug("rescale image: {}x{}", imageWidth, imageHeight);
				}
				PDFPage pdfPage = new PDFPage(new Dimension((int) imageWidth, (int) imageHeight), i);
				pdfPage.setNumber(i + 1);
				pdfPage.setImageDimensionOriginal((int) image.getImageWidth(), (int) image.getImageHeight());
				pdfPage.passOCR(pageIn);
				append(image, font, pdfPage);
				resultPages.add(pdfPage);
			}
		} catch (IOException e) {
			throw new DigitalDerivansException(e);
		}
		return resultPages;
	}

	/**
	 * 
	 * Append page and re-calculate actual dimension for
	 * each page because different pages sizes due different
	 * formats like inlay maps, illustrations, etc.
	 * 
	 * @param image
	 * @param font
	 * @param page
	 * @return
	 * @throws IOException
	 */
	private PDFPage append(Image image, PdfFont font, PDFPage page) {
		PageSize pageSize = new PageSize(image.getImageScaledWidth(), image.getImageScaledHeight());
		var nextPage = this.iTextPDFDocument.addNewPage(pageSize);
		nextPage.getPdfObject().get(PdfName.PageNum);
		image.setFixedPosition(page.getNumber(), 0, 0);
		this.document.add(image);
		if (page.getTextcontent().isPresent()) {
			List<PDFTextElement> txtContents = page.getTextcontent().get();
			for (var line : txtContents) {
				if (this.renderLevel == TypeConfiguration.RENDER_LEVEL_LINE) {
					render(font, line);
				} else if (this.renderLevel == TypeConfiguration.RENDER_LEVEL_WORD) {
					var tokens = line.getChildren();
					for (var word : tokens) {
						render(font, word);
					}
				}
			}
			if (this.debugRender) {
				LOGGER.debug("render ocr outlines {} (height: {})", txtContents, image.getImageHeight());
				this.renderOcrOutlines(txtContents);
			}
		} else {
			LOGGER.info("no ocr data present for '{}'", page.getImagePath());
		}
		return page;
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
	private PDFTextElement render(PdfFont font, PDFTextElement token) {
		String text = token.forPrint();
		float fontSize = token.getFontSize();
		if (fontSize < IPDFProcessor.MIN_CHAR_SIZE) {
			LOGGER.warn("font too small: '{}'(min:{}) resist to render text '{}'", fontSize,
					IPDFProcessor.MIN_CHAR_SIZE, text);
			return token;
		}
		Rectangle2D box = token.getBox();
		float leftMargin = (float) box.getMinX();
		float verticalBaseline = token.getBaseline().getY1();
		var page = this.iTextPDFDocument.getLastPage();
		PdfCanvas pdfCanvas = new PdfCanvas(page);
		var iTextRect = new com.itextpdf.kernel.geom.Rectangle(
				leftMargin, verticalBaseline, (float) box.getWidth(), (float) box.getHeight());
		if ((!this.debugRender) && this.renderModus == TypeConfiguration.RENDER_MODUS_HIDE) {
			pdfCanvas.setTextRenderingMode(PdfCanvasConstants.TextRenderingMode.INVISIBLE);
		}
		float hScale = calculateHorizontalScaling(font, token);
		if (this.debugRender) {
			LOGGER.trace("put '{}' at {}x{} size:{}, scale:{})",
					text, leftMargin, verticalBaseline, fontSize, hScale);
			drawLine(leftMargin, (float) (leftMargin + box.getWidth()), verticalBaseline);
		}
		Text txt = new Text(text).setFont(font).setFontSize(fontSize).setHorizontalScaling(hScale);
		if (token.isRTL()) {
			txt.addStyle(rtlStyle);
		}
		Canvas canvas = new Canvas(pdfCanvas, iTextRect);
		canvas.add(new Paragraph(txt));
		canvas.close();
		pdfCanvas.release();
		token.setPrinted(true);
		return token;
	}

	/**
	 * 
	 * Calculate font scaling to fit given text into the specified width.
	 * cf. 
	 * https://kb.itextpdf.com/itext/how-to-choose-the-optimal-size-for-a-font
	 * 
	 * According to
	 * https://opensource.adobe.com/dc-acrobat-sdk-docs/standards/pdfstandards/pdf/PDF32000_2008.pdf#page=253
	 * value expected to be in PERCENTS, i.e. 0-100
	 * 
	 * iText8: 100 => 1.0
	 * 
	 * @param text
	 * @param width
	 * @param height
	 * @return
	 * @throws IOException
	 */
	float calculateHorizontalScaling(PdfFont font, PDFTextElement token) {
		String text = token.getText();
		float glyphWidth = font.getWidth(text) * .001f * token.getFontSize();
		float totalGlyphWidth = glyphWidth;
		float tokenLenght = token.getBaseline().length();
		return tokenLenght / totalGlyphWidth;
	}

	private void renderOcrOutlines(List<PDFTextElement> ocrLines) {
		Color colorLine = new DeviceCmyk(1.0f, 1.0f, 0.0f, 0.0f);
		Color colorKids = new DeviceCmyk(0.0f, 1.0f, 1.0f, 0.0f);
		for (PDFTextElement line : ocrLines) {
			drawOutline(line.getBox(), colorLine, 1.0f);
			for (IVisualElement txt : line.getChildren()) {
				drawOutline(txt.getBox(), colorKids, 0.4f);
			}
		}
	}

	private void drawLine(float leftBottomX, float rightBottomX, float bottomY) {
		var page = this.iTextPDFDocument.getLastPage();
		var canvas = new PdfCanvas(page);
		var bottomColor = new DeviceCmyk(0.0f, 0.0f, 1.0f, 0.0f);
		canvas.setStrokeColor(bottomColor).setLineWidth(2.0f)
				.moveTo(leftBottomX, bottomY)
				.lineTo(rightBottomX, bottomY).closePathStroke();
	}

	private void drawOutline(Rectangle2D rectangle, Color c, float lineWidth) {
		List<Point> points = asPoints(rectangle);
		this.drawPointsWith(points, lineWidth, c);
	}

	private void drawPointsWith(List<Point> points, float lineWidth, Color c) {
		var page = this.iTextPDFDocument.getLastPage();
		var canvas = new PdfCanvas(page);
		canvas.setStrokeColor(c);
		canvas.setLineWidth(lineWidth);
		Point pLast = points.get(points.size() - 1);
		canvas.moveTo(pLast.x, pLast.y);
		for (int i = 0; i < points.size(); i++) {
			Point p = points.get(i);
			canvas.lineTo(p.x, p.y);
		}
		canvas.closePathStroke();
	}

	private List<Point> asPoints(Rectangle2D rect) {
		List<Point> points = new ArrayList<>();
		float minX = (float) rect.getMinX();
		float minY = (float) rect.getMinY();
		float maxX = (float) rect.getMaxX();
		float maxY = (float) rect.getMaxY();
		points.add(new Point(minX, minY));
		points.add(new Point(maxX, minY));
		points.add(new Point(maxX, maxY));
		points.add(new Point(minX, maxY));
		return points;
	}

	@Override
	public boolean addOutline(int maxPages) {
		PdfOutline rootOutline = this.iTextPDFDocument.getOutlines(true);
		for (int i = 1; i <= maxPages; i++) {
			if (structure.getPage() == i) {
				traverseStructure(this.iTextPDFDocument, rootOutline, structure);
			}
		}
		return true;
	}

	static void traverseStructure(PdfDocument pdfDocument, PdfOutline currOutline, DigitalStructureTree structure) {
		String label = structure.getLabel();
		int pageN = structure.getPage();
		PdfPage destPage = pdfDocument.getPage(pageN);
		LOGGER.debug("[PAGE {}] created outline label {}", pageN, label);
		if (structure.hasSubstructures()) {
			var newParent = currOutline.addOutline(label);
			newParent.addAction(PdfAction.createGoTo(PdfExplicitDestination.createFit(destPage)));
			for (DigitalStructureTree sub : structure.getSubstructures()) {
				traverseStructure(pdfDocument, newParent, sub);
			}
		} else {
			currOutline.addOutline(label).addAction(PdfAction.createGoTo(PdfExplicitDestination.createFit(destPage)));
		}
	}

	public PdfFont loadFont(String path) throws DigitalDerivansException {
		try {
			TmpJarResource tmpResHandler = new TmpJarResource(path);
			String resPath = tmpResHandler.extract("derivans-tmp-font-", ".ttf");
			return PdfFontFactory.createFont(resPath, PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_EMBEDDED);
		} catch (IOException e) {
			throw new DigitalDerivansException(e);
		}
	}
}
