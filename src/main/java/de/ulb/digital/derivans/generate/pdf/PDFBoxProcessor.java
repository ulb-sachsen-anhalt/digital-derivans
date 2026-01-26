package de.ulb.digital.derivans.generate.pdf;

import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceCMYK;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.util.Matrix;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.config.TypeConfiguration;
import de.ulb.digital.derivans.data.io.JarResource;
import de.ulb.digital.derivans.model.DerivateStruct;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.IDerivate;
import de.ulb.digital.derivans.model.IPDFProcessor;
import de.ulb.digital.derivans.model.pdf.PDFPage;
import de.ulb.digital.derivans.model.pdf.PDFResult;
import de.ulb.digital.derivans.model.pdf.PDFTextElement;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;

/**
 * 
 * Apache PDFBox Implementation of {@link IPDFProcessor PDFProcessor}
 * 
 * @author hartwig
 */
public class PDFBoxProcessor implements IPDFProcessor {

	private static final float DBG_LINEWIDTH_BASE = 2.0f;

	private static final float DBG_LINEWIDTH_WORD = 0.3f;

	private static final float DBG_LINEWIDTH_ROW = 0.6f;

	private static final String PDF_METADATA_LABEL_PUBLISHED = "Published";

	private static final String PDF_METADATA_LABEL_ACCESS_CONDITION = "Access condition";

	private static final Logger LOGGER = LogManager.getLogger(PDFBoxProcessor.class);

	// output resolution for screens
	public static final float PDFBOX_ASSUMES_DPI = 72.0f;

	private TypeConfiguration renderLevel;

	private TypeConfiguration renderModus;

	private float dpiScale = 1.0f;

	private PDDocument document;

	private boolean debugRender;

	private DerivateStepPDF pdfStep;

	private IDerivate derivate;

	private DerivateStruct structure;

	private PDFont font;

	public static final float PDFBOX_FONT_STEP = .5f;

	private Map<String, PDFPage> renderedPages = new HashMap<>();

	// colors in debug mode
	private PDColor dbgColorLine;
	private PDColor dbgColorWord;
	private PDColor dbgColorBase;

	PDFResult reportDoc = new PDFResult();

	@Override
	public void init(DerivateStepPDF pdfStep, IDerivate derivate)
			throws DigitalDerivansException {
		this.pdfStep = pdfStep;
		this.renderLevel = pdfStep.getRenderLevel();
		this.renderModus = pdfStep.getRenderModus();
		this.debugRender = pdfStep.getDebugRender();
		this.setDpi(pdfStep.getImageDpi());
		this.derivate = derivate;
		
		// Initialize debug colors (CMYK)
		float[] cyan = {1.0f, 1.0f, 0.0f, 0.0f};
		float[] magenta = {0.0f, 1.0f, 1.0f, 0.0f};
		float[] yellow = {0.0f, 0.0f, 1.0f, 0.0f};
		this.dbgColorLine = new PDColor(cyan, PDDeviceCMYK.INSTANCE);
		this.dbgColorWord = new PDColor(magenta, PDDeviceCMYK.INSTANCE);
		this.dbgColorBase = new PDColor(yellow, PDDeviceCMYK.INSTANCE);
	}

	public DerivateStruct getStructure() {
		return structure;
	}

	@Override
	public void setStructure(DerivateStruct structure) {
		this.structure = structure;
	}

	private void setDpi(int dpi) throws DigitalDerivansException {
		if (dpi < 72 || dpi > 600) {
			String msg = String.format("tried to set invalid dpi: '%s' (must be in range 72 - 600)", dpi);
			LOGGER.error(msg);
			throw new DigitalDerivansException(msg);
		} else {
			LOGGER.info("set dpi for image scaling {}", dpi);
			this.dpiScale = PDFBOX_ASSUMES_DPI / dpi;
		}
	}

	/**
	 * 
	 * Called implicitly by #write
	 * metadata must be added before content
	 * 
	 */
	@Override
	public void addMetadata() {
		PDDocumentInformation docInfo = this.document.getDocumentInformation();
		String title = this.pdfStep.getTitle();
		String year = this.pdfStep.getPublicationYear();
		String combinedTitle = String.format("(%s) %s", year, title);
		docInfo.setTitle(combinedTitle);
		docInfo.setAuthor(this.pdfStep.getAuthor());
		Optional<String> optCreator = this.pdfStep.getCreator();
		if (optCreator.isPresent()) {
			docInfo.setCreator(optCreator.get());
		}
		Optional<String> optKeywords = this.pdfStep.getKeywords();
		if (optKeywords.isPresent()) {
			docInfo.setKeywords(optKeywords.get());
		}
		Optional<String> optLicense = this.pdfStep.getLicense();
		if (optLicense.isPresent()) {
			docInfo.setCustomMetadataValue(PDF_METADATA_LABEL_ACCESS_CONDITION, optLicense.get());
			docInfo.setCustomMetadataValue(PDF_METADATA_LABEL_PUBLISHED, this.pdfStep.getPublicationYear());
		}
		docInfo.setCustomMetadataValue("year", combinedTitle);
	}

	@Override
	public PDFResult write(File fileDescriptor) throws DigitalDerivansException {
		try {
			this.document = new PDDocument();
			this.font = this.loadFont("ttf/DejaVuSans.ttf");
			this.addMetadata();
			
			List<DigitalPage> allPages = this.derivate.allPagesSorted();
			List<PDFPage> processedPdfPages = this.addPages(allPages);
			this.reportDoc.addPages(processedPdfPages);
			this.addOutline();
			
			this.document.save(fileDescriptor);
			this.document.close();
			
			LOGGER.info("done creating pdf {}, checking readability", fileDescriptor);
			PDFBoxProcessor.checkReadability(fileDescriptor);
		} catch (IOException exc) {
			LOGGER.error(exc);
			throw new DigitalDerivansException(exc);
		}
		LOGGER.info("create report object with {} pages", this.reportDoc.getPdfPages().size());
		return this.reportDoc;
	}

	@Override
	public void addOutline() throws DigitalDerivansException {
		if (this.structure == null) {
			LOGGER.debug("no structure available, skipping outline creation");
			return;
		}
		
		PDDocumentOutline outline = new PDDocumentOutline();
		this.document.getDocumentCatalog().setDocumentOutline(outline);
		
		String rootLabel = this.structure.getLabel();
		PDOutlineItem rootOutlineItem = new PDOutlineItem();
		rootOutlineItem.setTitle(rootLabel);
		outline.addLast(rootOutlineItem);
		
		this.traverse(rootOutlineItem, structure);
	}

	private void traverse(PDOutlineItem currentOutline, DerivateStruct currStruct)
			throws DigitalDerivansException {
		if (currStruct.getChildren().isEmpty()) {
			int order = currStruct.getOrder();
			// corner case: take care if very first level struct is unset
			if (order < 1) {
				order = 1;
			}
			PDPage firstStructPage = this.requestDestinationPage(order);
			currentOutline.setDestination(firstStructPage);
		} else {
			for (DerivateStruct subStruct : currStruct.getChildren()) {
				String childLabel = subStruct.getLabel();
				PDOutlineItem childLine = new PDOutlineItem();
				childLine.setTitle(childLabel);
				currentOutline.addLast(childLine);
				
				int childOrder = subStruct.getOrder();
				// PDFBox will throw exception if requested page not in range 1 .. max
				try {
					PDPage referencedPage = this.document.getPage(childOrder - 1); // PDFBox uses 0-based index
					childLine.setDestination(referencedPage);
				} catch (IndexOutOfBoundsException ioe) {
					var msg = String.format("while adding outline entry '%s' for order %d: %s",
							childLabel, childOrder, ioe.getMessage());
					throw new DigitalDerivansException(msg, ioe);
				}
				traverse(childLine, subStruct);
			}
		}
	}

	private PDPage requestDestinationPage(int n) throws DigitalDerivansException {
		// pre-check
		int nPdfPages = this.document.getNumberOfPages();
		if (nPdfPages < n) {
			String msg = String.format("request invalid page number %02d (total: %02d)", n, nPdfPages);
			throw new DigitalDerivansException(msg);
		}
		return this.document.getPage(n - 1); // PDFBox uses 0-based index
	}

	/**
	 * Construct the proper input image path for a given digital page,
	 * taking into account the inputDir from the PDF step configuration.
	 * This ensures that in chained derivate scenarios, the correct
	 * intermediate image directory is used.
	 * 
	 * @param page the digital page
	 * @return the path to the input image file
	 */
	private Path getInputImagePath(DigitalPage page) {
		String dirName = this.pdfStep.getInputDir();
		DigitalPage.File currentFile = page.getFile();
		return currentFile.using(dirName);
	}

	/**
	 * @param pages
	 * @return
	 * @throws DigitalDerivansException
	 */
	public List<PDFPage> addPages(List<DigitalPage> pages) throws DigitalDerivansException {
		List<PDFPage> resultPages = new ArrayList<>();
		LOGGER.debug("render {} pages at {}", pages.size(), this.renderLevel);
		try {
			for (int i = 0; i < pages.size(); i++) {
				DigitalPage pageIn = pages.get(i);
				String pageId = pageIn.getPageId();
				PDFPage pdfPage = null;
				if (this.renderedPages.containsKey(pageId)) {
					LOGGER.warn("skip already rendered page '{}'", pageId);
					pdfPage = this.renderedPages.get(pageId);
				} else {
					int orderN = pageIn.getOrderNr();
					String imagePath = this.getInputImagePath(pageIn).toString();
					LOGGER.debug("render page {} image {}", i + 1, imagePath);
					
					PDImageXObject image = PDImageXObject.createFromFile(imagePath, this.document);
					float imageWidth = image.getWidth();
					float imageHeight = image.getHeight();
					
					if (Math.abs(1.0 - this.dpiScale) > 0.01) {
						imageWidth = imageWidth * this.dpiScale;
						imageHeight = imageHeight * this.dpiScale;
						LOGGER.trace("rescale image: {}x{}", imageWidth, imageHeight);
					}
					pdfPage = new PDFPage(new Dimension((int) imageWidth, (int) imageHeight), orderN);
					pdfPage.passOCRFrom(pageIn);
					this.append(image, pdfPage, imageWidth, imageHeight);
				}
				resultPages.add(pdfPage);
				this.renderedPages.put(pageId, pdfPage);
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
	 * @param page
	 * @param imageWidth
	 * @param imageHeight
	 * @return
	 * @throws DigitalDerivansException
	 * @throws IOException
	 */
	private PDFPage append(PDImageXObject image, PDFPage page, float imageWidth, float imageHeight) 
			throws IOException, DigitalDerivansException {
		PDRectangle pageSize = new PDRectangle(imageWidth, imageHeight);
		PDPage pdfPage = new PDPage(pageSize);
		this.document.addPage(pdfPage);
		
		try (PDPageContentStream contentStream = new PDPageContentStream(this.document, pdfPage)) {
			// Draw image
			contentStream.drawImage(image, 0, 0, imageWidth, imageHeight);
			
			if (page.getTextcontent().isPresent()) {
				List<PDFTextElement> txtContents = page.getTextcontent().get();
				if (!txtContents.isEmpty()) {
					for (var line : txtContents) {
						if (this.renderLevel == TypeConfiguration.RENDER_LEVEL_LINE) {
							render(contentStream, line, imageHeight);
							if (this.debugRender) {
								this.drawBoundingBox(contentStream, line.getBox(), this.dbgColorLine, DBG_LINEWIDTH_ROW);
							}
						} else if (this.renderLevel == TypeConfiguration.RENDER_LEVEL_WORD) {
							var tokens = line.getChildren();
							for (var word : tokens) {
								render(contentStream, word, imageHeight);
								if (this.debugRender) {
									this.drawBoundingBox(contentStream, word.getBox(), this.dbgColorWord, DBG_LINEWIDTH_WORD);
								}
							}
						}
					}
				}
			} else {
				LOGGER.info("no ocr data present for '{}'", page.getImagePath());
			}
		}
		return page;
	}

	/**
	 * 
	 * Render single textual token(s) if valid fontSize can be calculated.
	 * Depending on render level "token" means line or word.
	 * 
	 * @param contentStream
	 * @param token
	 * @param pageHeight
	 * @throws DigitalDerivansException
	 * @throws IOException
	 */
	private PDFTextElement render(PDPageContentStream contentStream, PDFTextElement token, float pageHeight) 
			throws IOException {
		float fontSize = token.getFontSize();
		if (fontSize < IPDFProcessor.MIN_CHAR_SIZE) {
			String missedText = token.forPrint();
			LOGGER.warn("font too small: '{}'(min:{}) resist to render text '{}'", fontSize,
					IPDFProcessor.MIN_CHAR_SIZE, missedText);
			return null;
		}
		
		String text = harmonizeText(token);
		if (text == null) {
			return token;
		}
		
		Rectangle2D box = token.getBox();
		float leftMargin = (float) box.getMinX();
		float baselineY = pageHeight - token.getBaseline().getY1(); // PDFBox origin is bottom-left
		float hScale = calculateHorizontalScaling(token);
		
		if (this.debugRender) {
			LOGGER.trace("put '{}' at baseline {}x{} size:{}, scale:{})",
					text, leftMargin, baselineY, fontSize, hScale);
			var rightMargin = (float) (leftMargin + box.getWidth());
			contentStream.setStrokingColor(dbgColorBase);
			contentStream.setLineWidth(DBG_LINEWIDTH_BASE);
			contentStream.moveTo(leftMargin, baselineY);
			contentStream.lineTo(rightMargin, baselineY);
			contentStream.stroke();
		}
		
		try {
			contentStream.beginText();
			contentStream.setFont(this.font, fontSize);
			
			// Note: PDFBox 3.0 doesn't have direct text rendering mode control like iText
			// For hidden text, we would need to set the text rendering mode using raw PDF operators
			// For now, text will always be visible
			
			// Handle RTL text
			if (token.isRTL()) {
				leftMargin = (float) box.getMaxX();
				// For RTL, we might need to adjust positioning
			}
			
			// Set horizontal scaling
			contentStream.setTextMatrix(new Matrix(hScale, 0, 0, 1, leftMargin, baselineY));
			contentStream.showText(text);
			contentStream.endText();
			
			token.setPrinted(true);
		} catch (Exception exc) {
			LOGGER.warn("While rendering {} : {}", text, exc.getMessage());
		}
		return token;
	}

	/**
	 * 
	 * Critical for PDF/A conformant output since strange Errors
	 * concerning gStack accumulate when try to render chars
	 * which are *not* renderable with given font
	 * 
	 * Prepend UTF normalization for regular historical ger/fre/lat prints
	 *
	 * @param token
	 * @return harmonized String or null
	 */
	public String harmonizeText(PDFTextElement token) {
		String originalText = token.forPrint();
		if (!token.isRTL() && !Normalizer.isNormalized(originalText, Normalizer.Form.NFKD)) {
			originalText = Normalizer.normalize(originalText, Normalizer.Form.NFKD);
		}
		
		String fontLabel = this.font.getName();
		StringBuilder harmonized = new StringBuilder();
		
		try {
			for (int i = 0; i < originalText.length(); i++) {
				char c = originalText.charAt(i);
				// PDFBox doesn't have containsGlyph method, so we try encoding
				try {
					this.font.encode(String.valueOf(c));
					harmonized.append(c);
				} catch (Exception e) {
					LOGGER.trace("char '{}' of '{}' not contained in font {}", c, originalText, fontLabel);
					if (c == 11799) { // "⸗" Double Oblique Hyphen 0x2E17 (UTF-16)
						harmonized.append('-');
					} else if (c == 868) { // " ͤ" Combining Latin Small Letter E
						if (i == 0) {
							LOGGER.error("can't render '{}': starts with '{}' not in font '{}'",
									originalText, c, fontLabel);
							return null;
						}
						String prev = originalText.substring(i - 1, i + 1);
						if (prev.charAt(0) == 'a') {
							harmonized.setCharAt(harmonized.length() - 1, 'ä');
						} else if (prev.charAt(0) == 'o') {
							harmonized.setCharAt(harmonized.length() - 1, 'ö');
						} else if (prev.charAt(0) == 'u') {
							harmonized.setCharAt(harmonized.length() - 1, 'ü');
						}
					} else {
						LOGGER.debug("can't render '{}': char '{}' not in font '{}'", originalText, c, fontLabel);
						return null;
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error harmonizing text: {}", e.getMessage());
			return null;
		}
		
		return harmonized.toString();
	}

	/**
	 * 
	 * Calculate font scaling to fit given text into the specified width.
	 * 
	 * @param token
	 * @return
	 * @throws IOException
	 */
	float calculateHorizontalScaling(PDFTextElement token) {
		String text = token.getText();
		try {
			float glyphWidth = this.font.getStringWidth(text) / 1000f * token.getFontSize();
			float tokenLength = token.getBaseline().length();
			return tokenLength / glyphWidth;
		} catch (Exception e) {
			LOGGER.warn("Error calculating horizontal scaling: {}", e.getMessage());
			return 1.0f;
		}
	}

	private void drawBoundingBox(PDPageContentStream contentStream, Rectangle2D rectangle, 
			PDColor color, float lineWidth) throws IOException {
		float minX = (float) rectangle.getMinX();
		float minY = (float) rectangle.getMinY();
		float maxX = (float) rectangle.getMaxX();
		float maxY = (float) rectangle.getMaxY();
		
		contentStream.setStrokingColor(color);
		contentStream.setLineWidth(lineWidth);
		contentStream.addRect(minX, minY, maxX - minX, maxY - minY);
		contentStream.stroke();
	}

	public PDFont loadFont(String path) throws DigitalDerivansException {
		try {
			JarResource tmpResHandler = new JarResource(path);
			String resPath = tmpResHandler.extract("derivans-tmp-font-", ".ttf");
			this.font = PDType0Font.load(this.document, new File(resPath));
		} catch (IOException e) {
			throw new DigitalDerivansException(e);
		}
		return this.font;
	}

	private static void checkReadability(File fileDescriptor) throws DigitalDerivansException {
		try (PDDocument pdfOut = Loader.loadPDF(new RandomAccessReadBufferedFile(fileDescriptor))) {
			int nPages = pdfOut.getNumberOfPages();
			LOGGER.info("pdf {} contains {} pages", fileDescriptor, nPages);
		} catch (Exception e) {
			LOGGER.error("fail read {}: {}", fileDescriptor, e.getMessage());
			throw new DigitalDerivansException(e);
		}
	}
}
