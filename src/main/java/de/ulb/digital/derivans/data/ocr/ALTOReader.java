package de.ulb.digital.derivans.data.ocr;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.util.IteratorIterable;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.model.ocr.OCRData;
import de.ulb.digital.derivans.model.ocr.OCRData.Textline;

/**
 * 
 * Transform ALTO OCR data into domain OCR data
 * 
 * @author u.hartwig
 *
 */
public class ALTOReader implements OCRReader {

	private Document document;

	public static final Predicate<String> CONTAINS_CHARS = s -> (s != null) && (!s.isBlank());
	
	protected Type type;

	public ALTOReader(Type type) {
		this.type = type;
	}

	@Override
	public OCRData get(Path pathOcr) throws DigitalDerivansException {
		File f = new File(pathOcr.toString());
		SAXBuilder builder = new SAXBuilder();
		builder.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		try {
			document = builder.build(f);
		} catch (JDOMException | IOException e) {
			throw new DigitalDerivansException(e);
		}
		var lines = extractTextLines();
		var dim = extractPageDimension();
		return new OCRData(lines, dim);
	}

	private Dimension extractPageDimension() {
		Element page = extractElements(new ElementFilter("Page")).get(0);
		int w = Integer.parseInt(page.getAttributeValue("WIDTH"));
		int h = Integer.parseInt(page.getAttributeValue("HEIGHT"));
		return new Dimension(w, h);
	}

	protected List<Textline> extractTextLines() {
		List<OCRData.Textline> lines = new ArrayList<>();
		List<Element> altoLines = extractElements(new ElementFilter("TextLine"));
		for (Element el : altoLines) {
			List<Element> strings = el.getChildren("String", getType().toNS());
			var texts = strings.parallelStream().map(ALTOReader::toText)
					.filter(OCRData.Text::hasPrintableChars)
					.collect(Collectors.toList());
			if (!texts.isEmpty()) { 
				OCRData.Textline line = new OCRData.Textline(texts);
				lines.add(line);
			}
		}
		return lines;
	}

	protected List<Element> extractElements(ElementFilter filter) {
		List<Element> elements = new ArrayList<>();
		IteratorIterable<Element> elemIt = document.getDescendants(filter);
		while (elemIt.hasNext()) {
			elements.add(elemIt.next());
		}
		return elements;
	}

	static OCRData.Text toText(Element word) {
		String content = word.getAttributeValue("CONTENT");
		int x = Integer.parseInt(word.getAttributeValue("HPOS"));
		int y = Integer.parseInt(word.getAttributeValue("VPOS"));
		int width = Integer.parseInt(word.getAttributeValue("WIDTH"));
		int height = Integer.parseInt(word.getAttributeValue("HEIGHT"));
		Rectangle rect = new Rectangle(x, y, width, height);
		return new OCRData.Text(content, rect);
	}

	@Override
	public Type getType() {
		return this.type;
	}
}
