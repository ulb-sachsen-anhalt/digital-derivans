package de.ulb.digital.derivans.derivate;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Header;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfAConformanceException;
import com.itextpdf.text.pdf.PdfAConformanceLevel;
import com.itextpdf.text.pdf.PdfAWriter;
import com.itextpdf.text.pdf.PdfAction;
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
			DescriptiveData descriptiveData, List<DigitalPage> pages) {
		super(basic.getInput(), basic.getOutput());
		this.getOutput().setPath(outputPath);
		this.structure = tree;
		this.description = descriptiveData;
		this.pages = pages;
	}

	private List<String> resolvePages() throws IOException {
		List<String> images = new ArrayList<>();

		Path pathInput = input.getPath();
		if (!Files.exists(pathInput)) {
			throw new IllegalArgumentException("Invalid inputDir '" + pathInput + "'");
		}

		if (pages != null && !pages.isEmpty()) {
			for (DigitalPage page : pages) {
				images.add(pathInput.resolve(page.getFilePointer()).toString());
			}
		} else {
			// try to get jpg file paths directly from imageDir
			try (Stream<Path> filesList = Files.list(pathInput)) {
				return filesList.map(pathInput::resolve).map(Path::toString).filter(p -> p.endsWith(".jpg")).sorted()
						.collect(Collectors.toList());
			}
		}

		if (images.isEmpty()) {
			throw new IllegalArgumentException("No Images in '" + pathInput + "'");
		}

		return images;
	}

	static int addImagePages(Document document, List<String> imagePaths) throws IOException, DocumentException {
		int pages = 0;
		for (String imagePath : imagePaths) {
			Image image = Image.getInstance(imagePath);
			Rectangle rect = new Rectangle(0, 0, image.getWidth(), image.getHeight());
			document.setPageSize(rect);
			document.newPage();
			document.add(image);
			pages++;
		}
		document.setPageCount(pages);
		return pages;
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
		List<String> images = new ArrayList<>();
		try {
			images = resolvePages();
		} catch (IOException e) {
			throw new DigitalDerivansException(e);
		}

		// get dimension of first page
		Image image = null;
		try {
			image = Image.getInstance(images.get(0));
		} catch (BadElementException | IOException e) {
			throw new DigitalDerivansException(e);
		}
		Rectangle firstPageSize = new Rectangle(0, 0, image.getWidth(), image.getHeight());
		Document document = new Document(firstPageSize, 0f, 0f, 0f, 0f);

		boolean hasImagesAdded = false;
		boolean hasOutlineAdded = false;
		Path pathToPDF = this.output.getPath();

		try {
			PdfWriter pdfWriter = PdfWriter.getInstance(document, new FileOutputStream(pathToPDF.toFile()));

			// metadata must be added afterwards creation of pdfWriter
			document.addTitle(this.description.getTitle());
			document.addAuthor(this.description.getAuthor());
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

			pdfWriter.createXmpMetadata();
			document.open();

			int nPagesAdded = addImagePages(document, images);
			hasImagesAdded = nPagesAdded == images.size();
			if (structure != null) {
				hasOutlineAdded = buildOutline(pdfWriter, nPagesAdded, structure);
			}
			document.close();
			pdfWriter.close();
		} catch (DocumentException | IOException exc) {
			LOGGER.error(exc);
			throw new DigitalDerivansException(exc);
		}

		boolean result = hasImagesAdded && hasOutlineAdded;
		LOGGER.info("created pdf '{}' with {} pages (outline:{})", pathToPDF, document.getPageNumber(),
				hasOutlineAdded);
		return result;
	}

	@Override
	public boolean create(String conformanceLevel) throws DigitalDerivansException, PdfAConformanceException {
		List<String> images = new ArrayList<>();
		try {
			images = resolvePages();
		} catch (IOException e) {
			throw new DigitalDerivansException(e);
		}
		// get dimension of first page
		Image image = null;
		try {
			image = Image.getInstance(images.get(0));
		} catch (BadElementException | IOException e) {
			throw new DigitalDerivansException(e);
		}
		Rectangle firstPageSize = new Rectangle(0, 0, image.getWidth(), image.getHeight());
		Document document = new Document(firstPageSize, 0f, 0f, 0f, 0f);

		boolean hasImagesAdded = false;
		boolean hasOutlineAdded = false;
		String defaultFont = DefaultConfiguration.PDFA_CONFORMANCE_LEVEL;
		Integer defaultFontSize = DefaultConfiguration.DEFAULT_FONT_SIZE;
		Path pathToPDF = this.output.getPath();
		PdfAConformanceLevel pdfa_level = PdfAConformanceLevel.valueOf(conformanceLevel);
		Font font = FontFactory.getFont(defaultFont, BaseFont.WINANSI, BaseFont.EMBEDDED, defaultFontSize);

		try {
			PdfAWriter pdfWriter = PdfAWriter.getInstance(
				document, new FileOutputStream(pathToPDF.toFile()), pdfa_level);

				// metadata must be added afterwards creation of pdfWriter
			document.addTitle(this.description.getTitle());
			document.addAuthor(this.description.getAuthor());
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

			pdfWriter.createXmpMetadata();
			document.open();

			int nPagesAdded = addImagePages(document, images);
			hasImagesAdded = nPagesAdded == images.size();
			if (structure != null) {
				hasOutlineAdded = buildOutline(pdfWriter, nPagesAdded, structure);
			}
			document.close();
			pdfWriter.close();
		} catch (DocumentException | IOException exc) {
			LOGGER.error(exc);
			throw new DigitalDerivansException(exc);
		}

		boolean result = hasImagesAdded && hasOutlineAdded;
		LOGGER.info("created pdf-a '{}' with level '{}'' and pages (outline:{})", 
					pathToPDF, pdfa_level, document.getPageNumber(), hasOutlineAdded);
		return result;
	}



}
