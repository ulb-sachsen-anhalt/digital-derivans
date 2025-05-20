package de.ulb.digital.derivans.generate.pdf;

import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
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
import com.itextpdf.kernel.pdf.PdfAConformance;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfOutline;
import com.itextpdf.kernel.pdf.PdfOutputIntent;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfVersion;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;
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
import de.ulb.digital.derivans.data.io.JarResource;
import de.ulb.digital.derivans.data.mets.METS;
import de.ulb.digital.derivans.model.DerivateStruct;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.IDerivate;
import de.ulb.digital.derivans.model.IPDFProcessor;
import de.ulb.digital.derivans.model.pdf.MetadataType;
import de.ulb.digital.derivans.model.pdf.PDFOutlineEntry;
import de.ulb.digital.derivans.model.pdf.PDFMetadata;
import de.ulb.digital.derivans.model.pdf.PDFPage;
import de.ulb.digital.derivans.model.pdf.PDFResult;
import de.ulb.digital.derivans.model.pdf.PDFTextElement;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;

/**
 * 
 * iText Implementation of {@link IPDFProcessor PDFProcessor}
 * 
 * @author hartwig
 */
public class ITextProcessor implements IPDFProcessor {

	private static final float DBG_LINEWIDTH_BASE = 2.0f;

	private static final float DBG_LINEWIDTH_WORD = 0.3f;

	private static final float DBG_LINEWIDTH_ROW = 0.6f;

	private static final String PDF_METADATA_LABEL_PUBLISHED = "Published";

	private static final String PDF_METADATA_LABEL_ACCESS_CONDITION = "Access condition";

	private static final Logger LOGGER = LogManager.getLogger(ITextProcessor.class);

	// output resolution for screens
	public static final float ITEXT_ASSUMES_DPI = 72.0f;

	private TypeConfiguration renderLevel;

	private TypeConfiguration renderModus;

	private float dpiScale = 1.0f;

	// high-level access
	private Document document;

	// low-level access
	private PdfDocument pdfDoc;

	private boolean debugRender;

	private DerivateStepPDF pdfStep;

	private DerivateStruct structure;

	private PdfFont font;

	public static final float ITEXT_FONT_STEP = .5f;

	private Style rtlStyle = new Style()
			.setTextAlignment(TextAlignment.RIGHT)
			.setBaseDirection(BaseDirection.RIGHT_TO_LEFT);

	// colors in debug mode
	private Color dbgColorLine = new DeviceCmyk(1.0f, 1.0f, 0.0f, 0.0f);
	private Color dbgColorWord = new DeviceCmyk(0.0f, 1.0f, 1.0f, 0.0f);
	private Color dbgColorBase = new DeviceCmyk(0.0f, 0.0f, 1.0f, 0.0f);

	PDFResult reportDoc = new PDFResult();

	@Override
	public void init(DerivateStepPDF pdfStep, IDerivate derivate)
			throws DigitalDerivansException {
		this.pdfStep = pdfStep;
		this.renderLevel = pdfStep.getRenderLevel();
		this.renderModus = pdfStep.getRenderModus();
		this.debugRender = pdfStep.getDebugRender();
		this.setDpi(pdfStep.getImageDpi());
		this.font = this.loadFont("ttf/DejaVuSans.ttf");
		this.rtlStyle = this.rtlStyle.setFont(this.font);
	}

	public DerivateStruct getStructure() {
		return structure;
	}

	@Override
	public void setStructure(DerivateStruct structure) {
		this.structure = structure;
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
		PdfDocumentInfo docInfo = this.pdfDoc.getDocumentInfo();
		String title = this.pdfStep.getTitle();
		String year = this.pdfStep.getPublicationYear();
		String combinedTitle = String.format("(%s) %s", year, title);
		docInfo.setMoreInfo(PdfName.Title.getValue(), combinedTitle);
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
		docInfo.setMoreInfo("year", combinedTitle);
		PDFMetadata metadata = new PDFMetadata(mdMap);
		this.reportDoc.setMetadata(metadata);
	}

	@Override
	public PDFResult write(File fileDescriptor) throws DigitalDerivansException {
		try (FileOutputStream fos = new FileOutputStream(fileDescriptor)) {
			PdfWriter writer = new PdfWriter(fos, new WriterProperties()
					.addXmpMetadata().setPdfVersion(PdfVersion.PDF_1_7));
			if (this.pdfStep.getConformanceLevel() != null && !this.debugRender) {
				PdfAConformance conformanceLevel = ITextProcessor
						.determineLevel(this.pdfStep.getConformanceLevel());
				String iccPath = "icc/sRGB_CS_profile.icm";
				InputStream is = this.getClass().getClassLoader().getResourceAsStream(iccPath);
				PdfOutputIntent outputIntent = new PdfOutputIntent("Custom",
						"",
						"http://www.color.org",
						"sRGB IEC61966-2.1", is);
				this.pdfDoc = new PdfADocument(writer, conformanceLevel, outputIntent);
			} else {
				this.pdfDoc = new PdfDocument(writer);
			}
			this.pdfDoc = new PdfDocument(writer);
			this.document = new Document(pdfDoc);
			this.addMetadata();
			var result = this.addContents();
			this.reportDoc.addPages(result);
			this.document.close();
			this.pdfDoc.close();
		} catch (IOException exc) {
			LOGGER.error(exc);
			throw new DigitalDerivansException(exc);
		}
		return this.reportDoc;
	}

	/**
	 * PDF archive conformance level according ISO-standard notation, i.e.
	 * for ISO 19005-2:2011 (PDF 1.7) like PDF/A-2b or PDF/A-3u
	 * 
	 * @param conformance
	 * @return
	 * @throws DigitalDerivansException
	 */
	private static PdfAConformance determineLevel(String conformance) throws DigitalDerivansException {
		int offset = conformance.lastIndexOf('-');
		if (offset > -1) {
			String suffix = conformance.substring(offset + 1);
			String part = String.valueOf(suffix.charAt(0));
			String lvl = String.valueOf(suffix.charAt(1));
			PdfAConformance pdfALevel = PdfAConformance.PDF_A_2B;
			if (part.equals("1") && lvl.equalsIgnoreCase("B")) {
				pdfALevel = PdfAConformance.PDF_A_1B;
			}
			if (part.equals("2") && lvl.equalsIgnoreCase("A")) {
				pdfALevel = PdfAConformance.PDF_A_2A;
			}
			if (part.equals("3")) {
				if (lvl.equalsIgnoreCase("A")) {
					pdfALevel = PdfAConformance.PDF_A_3A;
				} else if (lvl.equalsIgnoreCase("B")) {
					pdfALevel = PdfAConformance.PDF_A_3B;
				}
			}
			if (pdfALevel != null) {
				return pdfALevel;
			}
		}
		throw new DigitalDerivansException("Cant set PDF/A conformance level for " + conformance + "!");
	}

	private List<PDFPage> addContents() throws DigitalDerivansException {
		PdfOutline baseOutline = this.pdfDoc.getOutlines(true);
		String rootLabel = this.structure.getLabel();
		List<PDFPage> processedPdfPages = new ArrayList<>();
		// outline the very first page with logical root
		PdfOutline logRootOutline = baseOutline.addOutline(rootLabel);
		logRootOutline.setTitle(rootLabel);
		if (this.structure.getChildren().isEmpty()) {
			List<PDFPage> processed = this.addPages(this.structure.getPages(), logRootOutline);
			processedPdfPages.addAll(processed);
			PdfPage startPage = this.pdfDoc.getPage(1);
			logRootOutline.addAction(PdfAction.createGoTo(PdfExplicitDestination.createFit(startPage)));
		} else {
			for (DerivateStruct currentStruct : this.structure.getChildren()) {
				String childLabel = currentStruct.getLabel();
				PdfOutline childLine = logRootOutline.addOutline(childLabel);
				this.traverse(childLine, currentStruct, processedPdfPages);
			}
		}
		return processedPdfPages;
	}

	private void traverse(PdfOutline currentOutline, DerivateStruct currStruct, List<PDFPage> processedPdfPages)
			throws DigitalDerivansException {
		if (currStruct.getChildren().isEmpty()) {
			List<PDFPage> processed = this.addPages(currStruct.getPages(), currentOutline);
			processedPdfPages.addAll(processed);
			int firstNumber = processed.get(0).getNumber();
			PdfPage firstStructPage = this.requestDestinationPage(firstNumber);
			currentOutline.addAction(PdfAction.createGoTo(PdfExplicitDestination.createFit(firstStructPage)));
		} else {
			for (DerivateStruct subStruct : currStruct.getChildren()) {
				String childLabel = subStruct.getLabel();
				PdfOutline childLine = currentOutline.addOutline(childLabel);
				traverse(childLine, subStruct, processedPdfPages);
			}
		}
	}

	private PdfPage requestDestinationPage(int n) throws DigitalDerivansException {
		// pre-check
		int nPdfPages = this.pdfDoc.getNumberOfPages();
		if (nPdfPages < n) {
			String msg = String.format("request invalid page number %02d (total: %02d)", n, nPdfPages);
			throw new DigitalDerivansException(msg);
		}
		return this.pdfDoc.getPage(n);
	}

	/**
	 * @param pages
	 * @return
	 * @throws DigitalDerivansException
	 */
	public List<PDFPage> addPages(List<DigitalPage> pages, PdfOutline currentOutline) throws DigitalDerivansException {
		List<PDFPage> resultPages = new ArrayList<>();
		LOGGER.debug("render pages at {}", this.renderLevel);
		try {
			for (int i = 0; i < pages.size(); i++) {
				DigitalPage pageIn = pages.get(i);
				int orderN = pageIn.getOrderNr();
				String imagePath = pageIn.getFile().getPath().toString();
				Image image = new Image(ImageDataFactory.create(imagePath));
				float imageWidth = image.getImageWidth();
				float imageHeight = image.getImageHeight();
				if (Math.abs(1.0 - this.dpiScale) > 0.01) {
					image.scaleAbsolute(imageWidth * this.dpiScale, imageHeight * this.dpiScale);
					imageWidth = image.getImageScaledWidth();
					imageHeight = image.getImageScaledHeight();
					LOGGER.trace("rescale image: {}x{}", imageWidth, imageHeight);
				}
				PDFPage pdfPage = new PDFPage(new Dimension((int) imageWidth, (int) imageHeight), orderN);
				pdfPage.setImageDimensionOriginal((int) image.getImageWidth(), (int) image.getImageHeight());
				pdfPage.passOCR(pageIn);
				this.append(image, pdfPage);
				resultPages.add(pdfPage);
				var optPageLabel = pageIn.getPageLabel();
				if (optPageLabel.isPresent()) {
					String label = optPageLabel.get();
					PdfPage thisPage = this.pdfDoc.getPage(pageIn.getOrderNr());
					currentOutline.addOutline(label)
							.addAction(PdfAction.createGoTo(PdfExplicitDestination.createFit(thisPage)));
				}
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
	private PDFPage append(Image image, PDFPage page) {
		PageSize pageSize = new PageSize(image.getImageScaledWidth(), image.getImageScaledHeight());
		var nextPage = this.pdfDoc.addNewPage(pageSize);
		nextPage.getPdfObject().get(PdfName.PageNum);
		image.setFixedPosition(page.getNumber(), 0, 0);
		this.document.add(image);
		if (page.getTextcontent().isPresent()) {
			List<PDFTextElement> txtContents = page.getTextcontent().get();
			for (var line : txtContents) {
				if (this.renderLevel == TypeConfiguration.RENDER_LEVEL_LINE) {
					render(line);
					if (this.debugRender) {
						this.drawBoundingBox(line.getBox(), this.dbgColorLine, DBG_LINEWIDTH_ROW);
					}
				} else if (this.renderLevel == TypeConfiguration.RENDER_LEVEL_WORD) {
					var tokens = line.getChildren();
					for (var word : tokens) {
						render(word);
						if (this.debugRender) {
							this.drawBoundingBox(word.getBox(), this.dbgColorWord, DBG_LINEWIDTH_WORD);
						}
					}
				}
			}
		} else {
			LOGGER.info("no ocr data present for '{}'", page.getImagePath());
		}
		return page;
	}

	/**
	 * 
	 * Render single textual token(s) if valid fontSize can be calculated.
	 * Depending on render level "token" means line or word.
	 * 
	 * @param font
	 * @param pageHeight
	 * @param cb
	 * @param line
	 */
	private PDFTextElement render(PDFTextElement token) {
		String text = token.forPrint();
		float fontSize = token.getFontSize();
		if (fontSize < IPDFProcessor.MIN_CHAR_SIZE) {
			LOGGER.warn("font too small: '{}'(min:{}) resist to render text '{}'", fontSize,
					IPDFProcessor.MIN_CHAR_SIZE, text);
			return token;
		}
		Rectangle2D box = token.getBox();
		float leftMargin = (float) box.getMinX();
		float baselineY = token.getBaseline().getY1();
		var page = this.pdfDoc.getLastPage();
		PdfCanvas pdfCanvas = new PdfCanvas(page);
		var iTextRect = new com.itextpdf.kernel.geom.Rectangle(
				leftMargin, baselineY, (float) box.getWidth(), (float) box.getHeight());
		if ((!this.debugRender) && this.renderModus == TypeConfiguration.RENDER_MODUS_HIDE) {
			pdfCanvas.setTextRenderingMode(PdfCanvasConstants.TextRenderingMode.INVISIBLE);
		}
		float hScale = calculateHorizontalScaling(token);
		if (this.debugRender) {
			LOGGER.trace("put '{}' at baseline {}x{} size:{}, scale:{})",
					text, leftMargin, baselineY, fontSize, hScale);
			var rightMargin = (float) (leftMargin + box.getWidth());
			pdfCanvas.setStrokeColor(dbgColorBase).setLineWidth(DBG_LINEWIDTH_BASE)
					.moveTo(leftMargin, baselineY)
					.lineTo(rightMargin, baselineY).closePathStroke();
		}
		Text txt = new Text(text).setFont(this.font).setFontSize(fontSize).setHorizontalScaling(hScale);
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
	 * cf. https://kb.itextpdf.com/itext/how-to-choose-the-optimal-size-for-a-font
	 * Value range unclear; according to Specs it's percents (i.e. 0-100)
	 * https://opensource.adobe.com/dc-acrobat-sdk-docs/standards/pdfstandards/pdf/PDF32000_2008.pdf#page=253
	 * but with iText8: 100 => 1.0
	 * 
	 * @param text
	 * @param width
	 * @param height
	 * @return
	 * @throws IOException
	 */
	float calculateHorizontalScaling(PDFTextElement token) {
		String text = token.getText();
		float glyphWidth = this.font.getWidth(text) * .001f * token.getFontSize();
		float totalGlyphWidth = glyphWidth;
		float tokenLenght = token.getBaseline().length();
		return tokenLenght / totalGlyphWidth;
	}

	private void drawBoundingBox(Rectangle2D rectangle, Color c, float lineWidth) {
		List<Point> points = new ArrayList<>();
		float minX = (float) rectangle.getMinX();
		float minY = (float) rectangle.getMinY();
		float maxX = (float) rectangle.getMaxX();
		float maxY = (float) rectangle.getMaxY();
		points.add(new Point(minX, minY));
		points.add(new Point(maxX, minY));
		points.add(new Point(maxX, maxY));
		points.add(new Point(minX, maxY));
		this.drawPointsWith(points, lineWidth, c);
	}

	private void drawPointsWith(List<Point> points, float lineWidth, Color c) {
		var page = this.pdfDoc.getLastPage();
		var canvas = new PdfCanvas(page);
		canvas.setStrokeColor(c).setLineWidth(lineWidth);
		Point pLast = points.get(points.size() - 1);
		canvas.moveTo(pLast.getX(), pLast.getY());
		for (int i = 0; i < points.size(); i++) {
			Point p = points.get(i);
			canvas.lineTo(p.getX(), p.getY());
		}
		canvas.closePathStroke();
	}

	public PdfFont loadFont(String path) throws DigitalDerivansException {
		try {
			JarResource tmpResHandler = new JarResource(path);
			String resPath = tmpResHandler.extract("derivans-tmp-font-", ".ttf");
			return PdfFontFactory.createFont(resPath, PdfEncodings.IDENTITY_H, EmbeddingStrategy.FORCE_EMBEDDED);
		} catch (IOException e) {
			throw new DigitalDerivansException(e);
		}
	}

	public static PDFOutlineEntry readOutline(Path pathPdf) throws IOException {
		PdfReader reader = new PdfReader(pathPdf.toFile());
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
}
