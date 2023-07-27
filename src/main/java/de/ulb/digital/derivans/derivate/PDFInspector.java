package de.ulb.digital.derivans.derivate;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.model.PDFMetaInformation;
import de.ulb.digital.derivans.model.PDFPageformation;

/**
 * 
 * Inspect PDF-Data for testing reasons
 * 
 * @author hartwig
 *
 */
public class PDFInspector {

	private Path pdfPath;

	private PdfReader pdfReader;

	private PdfReaderContentParser pdfContentParser;

	public PDFInspector(Path pdfPath) throws IOException {
		this.pdfPath = pdfPath;
		try (FileInputStream fis = new FileInputStream(pdfPath.toAbsolutePath().toString())) {
			this.pdfReader = new PdfReader(fis);
			this.pdfContentParser = new PdfReaderContentParser(pdfReader);
		}
	}

	public Path getPdfPath() {
		return Path.of(pdfPath.toString());
	}

	/**
	 * 
	 * Simple approach to check if this page contains textual
	 * data (i.e. text layer), c.f.
	 * http://stackoverflow.com/questions/33492792/how-can-i-extract-subscript-superscript-properly-from-a-pdf-using-itextsharp
	 * 
	 * @param pageNr
	 * @return
	 * @throws DigitalDerivansException
	 */
	public String getPageText(int pageNr) throws DigitalDerivansException {
		try {
			var strategy = new SimpleTextExtractionStrategy();
			var textListener = this.pdfContentParser.processContent(pageNr, strategy);
			String text = textListener.getResultantText();
			if (! text.isEmpty()) {
				return text.replace('\n', ' ');
			} else {
				return text;
			}
		} catch (IOException exc) {
			var msg = "[" + this.pdfPath.toString() + "] Failed to read contents of page " + pageNr;
			throw new DigitalDerivansException(msg);
		}
	}

	/**
	 * 
	 * Retrieve information concerning Metadata and from XMP-Data
	 * like license or pdf/a-conformance level
	 * 
	 * @return
	 * @throws IOException
	 */
	public PDFMetaInformation getPDFMetaInformation() throws IOException {
		Map<String, String> metadata = pdfReader.getInfo();
		String author = metadata.get("Author");
		String title = metadata.get("Title");

		byte[] xmpMetadataBytes = pdfReader.getMetadata();
		Document xmpMetadata = null;
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

		PDFMetaInformation pmi = new PDFMetaInformation(author, title, metadata, xmpMetadata);
		String creator = metadata.get("Creator");
		if (creator != null) {
			pmi.setCreator(Optional.of(creator));
		}

		return pmi;
	}

	/**
	 * 
	 * Retrieve particular information about the pages
	 * 
	 * @return
	 */
	public List<PDFPageformation> getPageInformation() {
		List<PDFPageformation> data = new ArrayList<>();
		int nPage = pdfReader.getNumberOfPages();
		for (int i = 1; i <= nPage; i++) {
			Rectangle dim = pdfReader.getPageSize(i);
			dim.getWidth();
			data.add(new PDFPageformation(pdfReader, i));
		}
		return data;
	}
}
