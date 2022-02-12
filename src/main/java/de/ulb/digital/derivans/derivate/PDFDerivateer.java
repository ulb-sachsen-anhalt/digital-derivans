package de.ulb.digital.derivans.derivate;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

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

	private PDFMetaInformation pdfMeta;
	
//	private String pdfConformanceLevel;

	public static final float ITEXT_ASSUMES_DPI = 72.0f;
	
	private int dpi;

	private float dpiScale = 1.0f;

	/**
	 * 
	 * Create new instance on top of {@link BaseDerivateer}
	 * 
	 * @param basic
	 * @param tree
	 * @param descriptiveData
	 * @param pages
	 */
	public PDFDerivateer(BaseDerivateer basic, DigitalStructureTree tree,
			DescriptiveData descriptiveData, List<DigitalPage> pages, 
			PDFMetaInformation metaInfo) {
		super(basic.getInput(), basic.getOutput());
		this.structure = tree;
//		this.description = descriptiveData;
		this.digitalPages = pages;
		this.pdfMeta = metaInfo;
		this.setDpi(metaInfo.getImageDpi());
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
		
		// push image data as base graphic content
		PdfContentByte over = writer.getDirectContent();
		image.setAbsolutePosition(0f, 0f);
		over.addImage(image);

		// push ocr if any
		Optional<OCRData> optOcr = page.getOcrData();
		if (optOcr.isPresent()) {
			OCRData ocrData = optOcr.get();
			// get to know current image dimensions
			// most likely to differ due subsequent scaling derivation steps
			// height has also changed if additional footer has been applied to image
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
			// page height corresponds to original image height
			float currentImageHeight = image.getScaledHeight();
			float ratio = currentImageHeight / pageHeight;
			if (Math.abs(1.0 - ratio) > 0.01) {
				LOGGER.trace("scale ocr data for '{}' by '{}'", page.getImagePath(), ratio);
				ocrData.scale(ratio);
			}

			// place optional text *behind* image
			PdfContentByte cb = writer.getDirectContentUnder();
			cb.saveState();
			List<OCRData.Textline> ocrLines = ocrData.getTextlines();
			for (OCRData.Textline line : ocrLines) {
				renderLine(font, (int) currentImageHeight, cb, line);
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
			image.scaleAbsolute(image.getWidth() * this.dpiScale, image.getHeight() * this.dpiScale);
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

			if (this.pdfMeta.getConformanceLevel() != null) {
				PdfAConformanceLevel pdfaLevel = PdfAConformanceLevel.valueOf(this.pdfMeta.getConformanceLevel());
				writer = PdfAWriter.getInstance(document, fos, pdfaLevel);
			} else {
				writer = PdfWriter.getInstance(document, fos);
			}

			// metadata must be added afterwards creation of pdfWriter
			document.addTitle(this.pdfMeta.getTitle());
			document.addAuthor(this.pdfMeta.getAuthor());
			Optional<String> optCreator = this.pdfMeta.getCreator();
			if (optCreator.isPresent()) {
				document.addCreator(optCreator.get());
			}
			Optional<String> optKeywords = this.pdfMeta.getKeywords();
			if (optKeywords.isPresent()) {
				document.addKeywords(optKeywords.get());
			}
			// custom metadata entry -> com.itextpdf.text.Header
			Optional<String> optLicense = this.pdfMeta.getLicense();
			if (optLicense.isPresent()) {
				document.add(new Header("Access condition", optLicense.get()));
			}
			document.add(new Header("Published", this.pdfMeta.getPublicationYear()));

			writer.createXmpMetadata();
			document.open();

			// add profile if pdf-a required
			if (this.pdfMeta.getConformanceLevel() != null) {
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

	private void setDpi(int dpi) {
		if (dpi > 0 && dpi <= 600) {
			LOGGER.info("no dpi set, use {}", dpi);
			this.dpi = dpi;
			this.dpiScale = ITEXT_ASSUMES_DPI / (float)dpi;
		} else {
			LOGGER.error("tried to set invalid dpi: '{}' (must be in range 1 - 600)", dpi);
		}
	}

	public AtomicInteger getNPagesWithOCR() {
		return nPagesWithOCR;
	}

}
