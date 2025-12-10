package de.ulb.digital.derivans.generate.pdf;

import java.awt.Dimension;
import java.io.File;
// import java.io.FileOutputStream;
import java.io.IOException;
// import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
// import java.util.Map;
// import java.util.Optional;

// import org.apache.fontbox.ttf.TTFParser;
// import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.ulb.digital.derivans.DigitalDerivansException;
// import de.ulb.digital.derivans.config.DefaultConfiguration;
import de.ulb.digital.derivans.config.TypeConfiguration;
import de.ulb.digital.derivans.data.io.JarResource;
import de.ulb.digital.derivans.model.DerivateStruct;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.IDerivate;
// import de.ulb.digital.derivans.model.ITextElement;
import de.ulb.digital.derivans.model.IPDFProcessor;
// import de.ulb.digital.derivans.model.ocr.OCRData;
import de.ulb.digital.derivans.model.pdf.PDFResult;
import de.ulb.digital.derivans.model.pdf.PDFPage;
import de.ulb.digital.derivans.model.pdf.PDFTextElement;
import de.ulb.digital.derivans.model.pdf.PDFMetadata;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;
// import de.ulb.digital.derivans.model.text.Textline;

// import org.apache.pdfbox.contentstream.PDContentStream;
// import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
// import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
// import org.apache.pdfbox.pdmodel.ResourceCache;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
// import org.apache.pdfbox.pdmodel.font.PDFontFactory;
// import org.apache.pdfbox.pdmodel.font.PDTrueTypeFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
// import org.apache.pdfbox.pdmodel.font.PDType1CFont;
// import org.apache.pdfbox.pdmodel.font.PDType1Font;
// import org.apache.pdfbox.pdmodel.font.encoding.Encoding;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
// import org.apache.pdfbox.pdmodel.graphics.image.PDInlineImage;


/**
 * @author hartwig
 */
public class PDFBoxProcessor implements IPDFProcessor {

	private static final Logger LOGGER = LogManager.getLogger(PDFBoxProcessor.class);

	// what IText assumes as input resolution
	public static final float ITEXT_ASSUMES_DPI = 72.0f;

	private TypeConfiguration renderLevel;

	private TypeConfiguration renderModus;

	private float dpiScale = 1.0f;

	private PDDocument document;

	private boolean debugRender;

	private DerivateStepPDF pdfStep;

	private List<DigitalPage> pages;

	public static final float ITEXT_FONT_STEP = .5f;

	// for introspection
	PDFResult reportDoc = new PDFResult();

	@Override
	public void init(DerivateStepPDF pdfStep, IDerivate derivate)
			throws DigitalDerivansException {
		this.pdfStep = pdfStep;
		this.renderLevel = pdfStep.getRenderLevel();
		this.renderModus = pdfStep.getRenderModus();
		this.debugRender = pdfStep.getDebugRender();
		this.setDpi(pdfStep.getImageDpi());
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

	@Override
	public void addMetadata() {
	}

	@Override
	public PDFResult write(File fileDescriptor) throws DigitalDerivansException {
		try (PDDocument pdDoc = new PDDocument()) {
		 	this.document = pdDoc;
		 	this.addMetadata();
		 	var resultPages = this.addPages(pages);
		 	this.reportDoc.addPages(resultPages);
			//this.addOutline(writer, resultPages.size());
		 	this.document.save(fileDescriptor);
		 	this.document.close();
		} catch (IOException exc) {
			LOGGER.error(exc);
			throw new DigitalDerivansException(exc);
		}
		return this.reportDoc;
	}

	public List<PDFPage> addPages(List<DigitalPage> pages) throws DigitalDerivansException {
		List<PDFPage> resultPages = new ArrayList<>();
		LOGGER.trace("render pages at {}", this.renderLevel);
		try {
			String fontTmp = this.loadFont("ttf/DejaVuSans.ttf");
			PDType0Font font = PDType0Font.load(this.document, new File(fontTmp));
			for (int i = 0; i < this.pages.size(); i++) {
				var result = addPageAt(pages.get(i), i);
				//result.setDimension((int)imageWidth, (int)imageHeight);
				result.setNumber(i + 1);
				resultPages.add(result);
			}
		} catch (IOException e) {
			throw new DigitalDerivansException(e);
		}
		return resultPages;
	}

	private PDFPage addPageAt(DigitalPage page, int pageNumber) throws IOException {
		PDFPage resultPage = new PDFPage();
		String imagePath = page.getFile().getPath().toString();
		PDImageXObject img = PDImageXObject.createFromFile(imagePath, this.document);
		float imageWidth = img.getWidth();
		float imageHeight = img.getHeight();
		if (Math.abs(1.0 - this.dpiScale) > 0.01) {
			LOGGER.debug("rescale image: {}x{}", imageWidth, imageHeight);
		}
		resultPage = new PDFPage(new Dimension((int) imageWidth, 
			(int) imageHeight), pageNumber);
		var pdPage = new PDPage(new PDRectangle(0, 0, imageWidth, imageHeight));
		this.document.addPage(pdPage);
		try(PDPageContentStream cStream = new PDPageContentStream(document, pdPage)) {
			cStream.drawImage(img, 0, 0);
		//	Optional<OCRData> optOcr = page.getOcrData();
		// 	if (optOcr.isPresent()) {
		// 		LOGGER.debug("handle optional ocr for {} (render: {})", page.getImagePath(), this.debugRender);
		// 		OCRData ocrData = optOcr.get();
		// 		int pageHeight = ocrData.getPageHeight();
		// 		int footerHeight = 0;
		// 		Optional<Integer> optFooterHeight = page.getFooterHeight();
		// 		if (optFooterHeight.isPresent()) {
		// 			footerHeight = optFooterHeight.get();
		// 			LOGGER.debug("add footerHeight '{}' to original pageHeight '{}' from OCR-time", footerHeight,
		// 					pageHeight);
		// 			pageHeight += footerHeight;
		// 		}
		// 		// need to scale page height corresponds to original image height
		// 		float currentImageHeight = imageHeight;
		// 		float ratio = currentImageHeight / pageHeight;
		// 		if (Math.abs(1.0 - ratio) > 0.01) {
		// 			LOGGER.trace("scale ocr data for '{}' by '{}'", page.getImagePath(), ratio);
		// 			ocrData.scale(ratio);
		// 			resultPage.setScale(ratio);
		// 		}
		// 		List<Textline> ocrLines = ocrData.getTextlines();
		// 		if (this.debugRender) {
		// 			LOGGER.debug("render ocr outlines {} (height: {})", ocrLines, currentImageHeight);
		// 		}
		// 	} else {
		// 		LOGGER.info("no ocr data present for '{}'", page.getImagePath());
		// 	}
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
	private PDFTextElement render(int pageHeight, PDFTextElement token)
	// private PDFTextElement render(PDPageContentStream stream, int pageHeight, PDFTextElement token)
		throws IOException {
		String text = token.getText();
		float fontSize = token.getFontSize();
		if (fontSize < IPDFProcessor.MIN_CHAR_SIZE) {
			LOGGER.warn("font too small: '{}'(min:{}) resist to render text '{}'", fontSize,
			IPDFProcessor.MIN_CHAR_SIZE, text);
			return token;
		}
	// 	float descent = (boxHeight * .25f); // distance between lowest descending glyph and baseline (Unterlänge) 
	// 	float fontHeight = boxHeight - descent;
	// // 	if (fontSize < IPDFProcessor.MIN_CHAR_SIZE) {
	// // 		LOGGER.warn("font too small: '{}'(min:{}) resist to render text '{}'", fontSize,
	// // 				IPDFProcessor.MIN_CHAR_SIZE, text);
	// // 		return result;
	// // 	}
	// // 	result.setFontSize(fontSize);
	// float x = token.getBox().x;
	// float v = token.getBox().y + descent;
	// stream.addRect(x, token.getBox().y, (float)token.getBox().getWidth(), (float)token.getBox().getHeight());
	// stream.stroke();
	// 	float y = pageHeight - box.getBottom();
	// 	// looks like we need to go down a bit because
	// 	// font seems to be rendered not from bottom, therefore introduce v
	// 	// v = y - fontSize * MIN_CHAR_SIZE x, with X is were the magic starts: x=1 too
	// 	// low, x=0.5 too
	// 	// high
	// 	float v = y - (fontSize * IPDFProcessor.MIN_CHAR_SIZE);
	// 	if (this.debugRender) {
	// 		LOGGER.trace("put '{}' at {}x{} (fontsize:{})", text, x, v, fontSize);
	// 	}
	// 	// hide text layer if configured and *not* in debugMode
	// 	if ((!this.debugRender) && this.renderModus.equalsIgnoreCase("invisible")) {
	// 		//cb.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_INVISIBLE);
	// 	}
	// 	// rendering depends on text orientation
	// 	var page = this.iTextPDFDocument.getLastPage();
	// 	var pdfCanvas = new PdfCanvas(page);
	// 	var canvas = new Canvas(page, box);
	// 	if (token.isLTR()) {
	// 		pdfCanvas.beginText();
	// 		pdfCanvas.setFontAndSize(font, fontSize);
	// 		//canvas.setHorizontalScaling(v); // like horizontal stretch?
	// 		result.lowerLeftX = x;
	// 		result.lowerLeftY = v;
	// 		pdfCanvas.moveTo(x, v);
	// 		bottomLine(x, x + box.getRight(), y, pageHeight, new DeviceCmyk(.1f, .0f, .5f, .62f), v);
	// 		pdfCanvas.showText(text); //(text, x, v, 0);
	// 		pdfCanvas.endText();
	// 		canvas.setBackgroundColor(new DeviceCmyk(.3f, .3f, .3f, .31f),
	// 		 0, 0, 0, 0);
	// 		 canvas.close();
	// 	} else {
	// 		this.document.setFont(font);
	// 		this.document.setFontSize(fontSize);
	// 		Paragraph p = new Paragraph(text);
	// 		p.setTextAlignment(TextAlignment.RIGHT);
	// 		p.setFixedPosition(x, y, box.getWidth());
	// 		this.document.add(p);
	// 	}
		// var fontRes = this.getClass().getClassLoader().getResource("ttf/DejaVuSans.ttf").getFile();
		// PDType0Font pdFont = PDType0Font.load(this.document, new File(fontRes));
		// stream.beginText();
		// stream.setFont(pdFont, fontSize);
		// stream.newLineAtOffset((float)token.getBox().getMinX(), token.getBaseline().getY1());
		// try {
		// 	stream.showText(text);
		// } catch (IllegalArgumentException agsex) {
		// 	LOGGER.warn("Fail to render {}", text);
		// }
		// stream.endText();
		// result.setPrinted(true);
		// return result;
		return null;
	}

	public boolean addOutline(int nPages) {
	// PdfOutline rootOutline = pdfWriter.getRootOutline();
	// rootOutline.setTitle(structure.getLabel());
	// for (int i = 1; i <= nPages; i++) {
	// if (structure.getPage() == i) {
	// traverseStructure(pdfWriter, rootOutline, structure);
	// }
	// }
		return true;
	}

	public String loadFont(String path) throws DigitalDerivansException {
		JarResource tmpResHandler = new JarResource(path);
		return tmpResHandler.extract("derivans-tmp-font-", ".ttf");
	}

	@Override
	public void setStructure(DerivateStruct struct) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'setStructure'");
	}

	@Override
	public void addOutline() throws DigitalDerivansException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'addOutline'");
	}
}
