package de.ulb.digital.derivans.derivate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jdom2.Document;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.data.XMLHandler;
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

	public String getPageTextLinebreaksReplaced(int pageNr) throws DigitalDerivansException {
		return getPageText(pageNr, "\n", " ");
	}

	/**
	 * 
	 * Simple approach to check if page contains textual data,
	 * i.e. a text layer and how it is represented
	 * 
	 * Please note:
	 * 	page copunt starts by "1", therefore is "1" the very first page.
	 * 
	 * c.f.
	 * http://stackoverflow.com/questions/33492792/how-can-i-extract-subscript-superscript-properly-from-a-pdf-using-itextsharp
	 * 
	 * @param pageNr
	 * @return
	 * @throws DigitalDerivansException
	 */
	public String getPageText(int pageNr, String replaceToken, String replacement) throws DigitalDerivansException {
		try {
			var strategy = new SimpleTextExtractionStrategy();
			var textListener = this.pdfContentParser.processContent(pageNr, strategy);
			String text = textListener.getResultantText();
			if (! text.isEmpty()) {
				if (replaceToken != null && ! replaceToken.isEmpty()) {
					return text.replace(replaceToken, replacement);
				} else {
					return text;
				}
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
		Document xmpMetadata = null;
		try {
			XMLHandler xmlHandler = new XMLHandler(pdfReader.getMetadata());
			xmpMetadata = xmlHandler.getDocument();
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
