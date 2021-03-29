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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
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
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DescriptiveData;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.DigitalStructureTree;
import de.ulb.digital.derivans.model.PDFMetaInformation;
import de.ulb.digital.derivans.model.ocr.OCRData;

/**
 * 
 * Create PDF derivate
 * 
 * @author hartwig
 *
 */
public class PDFDerivateer extends BaseDerivateer {

	private static final Logger LOGGER = LogManager.getLogger(PDFDerivateer.class);

	private DigitalStructureTree structure;

	private List<DigitalPage> pages;

	private DescriptiveData description;

	private String pdfConformanceLevel;

	/**
	 * 
	 * Default constructor
	 * 
	 * @param input
	 * @param output
	 * @param tree
	 * @param descriptiveData
	 * @param pages
	 */
	public PDFDerivateer(DerivansData input, DerivansData output, DigitalStructureTree tree,
			DescriptiveData descriptiveData, List<DigitalPage> pages) {
		super(input, output);
		this.structure = tree;
		this.description = descriptiveData;
		this.pages = pages;
	}

	public PDFDerivateer(DerivansData input, DerivansData output, DigitalStructureTree tree,
			DescriptiveData descriptiveData, List<DigitalPage> pages, String conformanceLevel) {
		super(input, output);
		this.structure = tree;
		this.description = descriptiveData;
		this.pages = pages;
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
		this.getOutput().setPath(outputPath);
		this.structure = tree;
		this.description = descriptiveData;
		this.pages = pages;
		this.pdfConformanceLevel = conformanceLevel;
	}

	private List<DigitalPage> resolvePaths() throws IOException {

		// check input data dir exists
		if (!Files.exists(input.getPath())) {
			throw new IllegalArgumentException("Invalid inputDir '" + input.getPath() + "'");
		}
		Path pathInput = input.getPath();

		// enrich path from inputPath + filePointer (METS)
		if (pages != null && !pages.isEmpty()) {
			for (DigitalPage page : pages) {
				String filePointer = page.getFilePointer();
				page.setPath(pathInput.resolve(filePointer));
			}
			return pages;
		} else {
			// try to read PDF input images (JPG)from inputPath
			try (Stream<Path> filesList = Files.list(pathInput)) {
				List<DigitalPage> digitalPages = filesList.map(pathInput::resolve)
						.filter((Path p) -> p.toString().endsWith(".jpg")).sorted().map(DigitalPage::new)
						.collect(Collectors.toList());
				if (digitalPages.isEmpty()) {
					throw new IllegalArgumentException("No Images in '" + pathInput + "'");
				}

				return digitalPages;
			}
		}
	}

	static int addPages(Document document, PdfWriter writer, List<DigitalPage> pages, String conformance)
			throws DigitalDerivansException {
		Integer defaultFontSize = DefaultConfiguration.DEFAULT_FONT_SIZE;

		int pagesAdded = 0;
		BaseFont font = null;

		try {
			// get font
			font = determineFont(conformance, defaultFontSize);

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

	private static BaseFont determineFont(String conformance, Integer defaultFontSize)
			throws DocumentException, IOException {
		if (conformance != null) {
//			return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED);
			Font f = new Font(BaseFont.createFont("ttf/FreeMonoBold.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED), 10);
			return f.getBaseFont();
		} else {
			Font f = FontFactory.getFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED, defaultFontSize);
			return f.getBaseFont();
		}
	}

	private static void addPage(PdfWriter writer, BaseFont font, DigitalPage page)
			throws IOException, DocumentException {

		// save state because to do some graphics stuff
		PdfContentByte cb = writer.getDirectContentUnder();
		cb.saveState();

		int pageHeight = 800;

		// some text, if any
		if (page.getOcrData() != null) {
			List<OCRData.Textline> ocrLines = page.getOcrData().getTextlines();
			for (int i = 0; i < ocrLines.size(); i++) {
				OCRData.Text line = ocrLines.get(i).getLine();
				String text = line.getText();
				float fontSize = line.getHeight();
				com.itextpdf.awt.geom.Point tPoint = new com.itextpdf.awt.geom.Point(line.getBox().x, line.getBox().y);
				com.itextpdf.awt.geom.Dimension tDim = new com.itextpdf.awt.geom.Dimension(line.getBox().width,
						line.getBox().height);
				com.itextpdf.awt.geom.Rectangle tmp = new com.itextpdf.awt.geom.Rectangle(tPoint, tDim);
				Rectangle box = new Rectangle(tmp);

				// Calculate vertical transition (text is rendered at baseline -> descending
				// bits are below the chosen position)
				int descent = (int) font.getDescentPoint(text, fontSize);
				int ascent = (int) font.getAscentPoint(text, fontSize);
				int textHeight = Math.abs(descent) + ascent;
				int transY = descent;

				if (textHeight < box.getHeight()) {
					transY = (int) (descent - (box.getHeight() - textHeight) / 2);
				}

				// render actual text
				cb.beginText();
				cb.moveText(box.getLeft(), pageHeight - box.getBottom());
				cb.setTextMatrix(box.getLeft(), pageHeight - box.getBottom() - transY);
				cb.setFontAndSize(font, fontSize);
				cb.setColorFill(BaseColor.BLACK);
				cb.showText(text);
				cb.endText();
			}
		}
		// handle overlying image
		String imagePath = page.getPath().toString();
		Image image = Image.getInstance(imagePath);
		image.setAbsolutePosition(0f, 0f);
		cb.addImage(image);
		cb.restoreState();
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
		try {
			pages = resolvePaths();
		} catch (IOException e) {
			throw new DigitalDerivansException(e);
		}

		// get dimension of first page
		Image image = null;
		try {
			image = Image.getInstance(pages.get(0).getPath().toString());
		} catch (BadElementException | IOException e) {
			throw new DigitalDerivansException(e);
		}
		Rectangle firstPageSize = new Rectangle(0, 0, image.getWidth(), image.getHeight());
		Document document = new Document(firstPageSize, 0f, 0f, 0f, 0f);

		boolean hasPagesAdded = false;
		boolean hasOutlineAdded = false;
		Path pathToPDF = this.output.getPath();

		PdfWriter writer = null;
		try {
			FileOutputStream fos = new FileOutputStream(pathToPDF.toFile());
			if (pdfConformanceLevel != null) {
				PdfAConformanceLevel pdfa_level = PdfAConformanceLevel.valueOf(pdfConformanceLevel);
				writer = PdfAWriter.getInstance(document, fos, pdfa_level);

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

			int nPagesAdded = addPages(document, writer, pages, pdfConformanceLevel);

			// inform doc how many pages it holds
			document.setPageCount(nPagesAdded);
			hasPagesAdded = nPagesAdded == pages.size();

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

}
