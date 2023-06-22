package de.ulb.digital.derivans.derivate;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.itextpdf.awt.geom.Point2D;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
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
import de.ulb.digital.derivans.config.DefaultConfiguration;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.DigitalStructureTree;
import de.ulb.digital.derivans.model.PDFMetaInformation;
import de.ulb.digital.derivans.model.ocr.OCRData;
import de.ulb.digital.derivans.model.ocr.OCRToken;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;

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

	private AtomicInteger nPagesWithOCR = new AtomicInteger();

	// private PDFMetaInformation pdfMeta;

	public static final float ITEXT_ASSUMES_DPI = 72.0f;

	private int dpi;

	private float dpiScale = 1.0f;

	private boolean debugRender;

	private String renderLevel;

	private String renderModus;

	private DerivateStepPDF derivateStep;

	/**
	 * 
	 * Create new instance on top of {@link BaseDerivateer}
	 * 
	 * @param basic
	 * @param tree
	 * @param descriptiveData
	 * @param pages
	 */
	public PDFDerivateer(BaseDerivateer basic, DigitalStructureTree tree, List<DigitalPage> pages,
			DerivateStepPDF derivateStep) throws DigitalDerivansException {
		super(basic.getInput(), basic.getOutput());
		this.structure = tree;
		this.digitalPages = pages;
		this.derivateStep = derivateStep;
		this.setDpi(derivateStep.getImageDpi());
		this.debugRender = derivateStep.getDebugRender();
		this.renderLevel = derivateStep.getRenderLevel();
		this.renderModus = derivateStep.getRenderModus();
		LOGGER.info("debugRender: {}", this.debugRender);
	}

	public DerivateStepPDF getConfig() {
		return this.derivateStep;
	}

	private void setDpi(int dpi) throws DigitalDerivansException {
		if (dpi >= ITEXT_ASSUMES_DPI && dpi <= 600) {
			LOGGER.info("set dpi for image scaling {}", dpi);
			this.dpi = dpi;
			this.dpiScale = ITEXT_ASSUMES_DPI / dpi;
		} else {
			String msg = String.format("tried to set invalid dpi: '%s' (must be in range 72 - 600)", dpi);
			LOGGER.error(msg);
			throw new DigitalDerivansException(msg);
		}
	}

	public AtomicInteger getNPagesWithOCR() {
		return nPagesWithOCR;
	}

	private int addPages(Document document, PdfWriter writer, List<DigitalPage> pages) throws DigitalDerivansException {
		int pagesAdded = 0;

		try {
			// get font
			FontHandler handler = new FontHandler();
			BaseFont font = handler.forPDF("ttf/DejaVuSans.ttf");
			// process each page
			for (DigitalPage page : pages) {
				String imagePath = page.getImagePath().toString();
				Image image = Image.getInstance(imagePath);
				image.scaleAbsolute(image.getWidth() * this.dpiScale, image.getHeight() * this.dpiScale);
				float imageHeight = image.getScaledHeight();
				float imageWidth = image.getScaledWidth();
				// re-set page document dimension for *each* page since
				if (document.setPageSize(new Rectangle(imageWidth, imageHeight))) {
					LOGGER.debug("re-set document pageSize {}x{}", imageWidth, imageHeight);
				}
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
			LOGGER.debug("handle optional ocr for {}", page.getImagePath());
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
			}
			// place optional text *behind* image
			PdfContentByte cb = writer.getDirectContent();
			// optional: if layer shall be visible, show words too on top
			if (this.debugRender) {
				cb = writer.getDirectContent();
			}
			cb.saveState();
			List<OCRData.Textline> ocrLines = ocrData.getTextlines();
			// communicate once
			if (this.renderLevel.equalsIgnoreCase(DefaultConfiguration.DEFAULT_RENDER_LEVEL)) {
				LOGGER.trace("render text at line-level");
			} else if(this.renderLevel.equalsIgnoreCase("word")) {
				LOGGER.trace("render text at word-level");
			}
			// actual rendering about to start
			for (OCRData.Textline line : ocrLines) {
				if (this.renderLevel.equalsIgnoreCase(DefaultConfiguration.DEFAULT_RENDER_LEVEL)) {
					render(font, (int) currentImageHeight, cb, line);
				} else if (this.renderLevel.equalsIgnoreCase("word")) {
					List<OCRData.Text> tokens = line.getTokens();
					for (var word : tokens) {
						render(font, (int) currentImageHeight, cb, word);
					}
				}
			} 
			cb.restoreState();
			// optional: render ocr data outlines for debugging visualization
			if (this.debugRender) {
				this.renderOutlines(writer, ocrLines, currentImageHeight);
			}
			// increment number of ocr-ed pages for each PDF
			nPagesWithOCR.getAndIncrement();
		} else {
			LOGGER.info("no ocr data present for '{}'", page.getImagePath());
		}
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
	private void render(BaseFont font, int pageHeight, PdfContentByte cb, OCRToken token) {
		String text = token.getText();
		java.awt.Rectangle b = token.getBox();
		Rectangle box = toItextBox(token.getBox());
		float fontSize = calculateFontSize(font, text, box.getWidth(), box.getHeight());
		if (fontSize < .75f) {
			LOGGER.warn("font too small: '{}'(min:{}) for text '{}' - resist to render", fontSize, .75, text);
			return;
		}
		float x = box.getLeft();
		float y = pageHeight - box.getBottom();
		// looks like we need to go down a bit because
		// font seems to be rendered not from baseline, therefore introduce v
		// v = y - fontSize * x, with X is were the magic starts: x=1 too low, x=0.5 too
		// high
		float v = y - (fontSize * .75f);
		if (this.debugRender) {
			LOGGER.trace("put '{}' at {}x{}(x:{},w:{}) (fontsize:{})", text, x, v, b.x, b.width, fontSize);
		}
		// propably hide text layer font
		if(this.renderModus.equalsIgnoreCase("invisible")) {
			cb.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_INVISIBLE);
		}
		cb.beginText();
		cb.setFontAndSize(font, fontSize);
		cb.showTextAligned(Element.ALIGN_BOTTOM, text, x, v, 0);
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
			fontSizeX -= .5f;
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

	private void renderOutlines(PdfWriter writer, List<OCRData.Textline> ocrLines, float theHeight) {
		BaseColor c1 = new BaseColor(.12f, 0.0f, .5f, .6f); // dark blue
		BaseColor c2 = new BaseColor(.5f, 0.0f, .12f, .6f);
		PdfContentByte cb2 = writer.getDirectContent();
		cb2.saveState();
		for (OCRData.Textline line : ocrLines) {
			for (OCRData.Text txt : line.getTokens()) {
				drawOutline(txt, cb2, theHeight, c2, .25f);
			}
			drawOutline(line, cb2, theHeight, c1, .5f);
		}
		cb2.restoreState();
	}

	private void drawOutline(OCRToken tkn, PdfContentByte cb, float pageHeight, BaseColor c, float lineWidth) {
		List<Point2D.Float> points = asPoints(tkn.getBox());

		cb.setColorStroke(c);
		cb.setLineWidth(lineWidth);
		LOGGER.info("draw outline for {}({}) with {} ({})", tkn, points, c, lineWidth);

		// move to end, then draw the lines
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
	public int create() throws DigitalDerivansException {

		// resolve image paths
		resolver.enrichWithPath(getDigitalPages(), this.getInput().getPath());

		// get dimension of first page
		Image image = null;
		try {
			image = Image.getInstance(digitalPages.get(0).getImagePath().toString());
			if (this.dpi == 0) {
				LOGGER.debug("read xDPI {} from first image {}", image.getDpiX(), digitalPages.get(0).getImagePath());
				this.setDpi(image.getDpiX());
			}
			LOGGER.info("PDF scale {}, dpi: {} (orig.: {}x{})", this.dpiScale, this.dpi, image.getWidth(),
					image.getHeight());
			LOGGER.info("Firstpage: {}x{})", image.getWidth(), image.getHeight());
			image.scaleAbsolute(image.getWidth() * this.dpiScale, image.getHeight() * this.dpiScale);
			LOGGER.info("Firstpage scaled: {}x{})", image.getScaledWidth(), image.getScaledHeight());
		} catch (BadElementException | IOException e) {
			throw new DigitalDerivansException(e);
		}
		Rectangle firstPageSize = new Rectangle(0, 0, image.getScaledWidth(), image.getScaledHeight());
		Document document = new Document(firstPageSize, 0f, 0f, 0f, 0f);

		boolean hasPagesAdded = false;
		boolean hasOutlineAdded = false;
		Path pathToPDF = this.output.getPath();
		// if output path points to a directory, use it's name for PDF-file
		if (Files.isDirectory(pathToPDF)) {
			pathToPDF = pathToPDF.resolve(pathToPDF.getFileName() + ".pdf");
		}

		PdfWriter writer = null;
		try (FileOutputStream fos = new FileOutputStream(pathToPDF.toFile())) {
			if (this.derivateStep.getConformanceLevel() != null && !this.debugRender) {
				PdfAConformanceLevel pdfaLevel = PdfAConformanceLevel.valueOf(this.derivateStep.getConformanceLevel());
				writer = PdfAWriter.getInstance(document, fos, pdfaLevel);
			} else {
				writer = PdfWriter.getInstance(document, fos);
			}

			// metadata must be added afterwards creation of pdfWriter
			document.addTitle(this.derivateStep.getTitle());
			document.addAuthor(this.derivateStep.getAuthor());
			Optional<String> optCreator = this.derivateStep.getCreator();
			if (optCreator.isPresent()) {
				document.addCreator(optCreator.get());
			}
			Optional<String> optKeywords = this.derivateStep.getKeywords();
			if (optKeywords.isPresent()) {
				document.addKeywords(optKeywords.get());
			}
			// custom metadata entry -> com.itextpdf.text.Header
			Optional<String> optLicense = this.derivateStep.getLicense();
			if (optLicense.isPresent()) {
				document.add(new Header("Access condition", optLicense.get()));
			}
			document.add(new Header("Published", this.derivateStep.getPublicationYear()));

			writer.createXmpMetadata();
			document.open();
			// add profile if pdf-a required
			// not possible in debug mode
			if (this.derivateStep.getConformanceLevel() != null && !this.debugRender) {
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
		return result ? 1 : 0;
	}
}
