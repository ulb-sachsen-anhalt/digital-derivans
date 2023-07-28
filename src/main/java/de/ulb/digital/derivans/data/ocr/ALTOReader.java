package de.ulb.digital.derivans.data.ocr;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jdom2.Element;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.data.XMLHandler;
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

	private XMLHandler handler;

	public static final Predicate<String> VALID_TEXT = new ValidTextPredicate();
	
	protected Type type;

	public ALTOReader(Type type) {
		this.type = type;
	}

	@Override
	public OCRData get(Path pathOcr) throws DigitalDerivansException {
		this.handler = new XMLHandler(pathOcr);
		var lines = extractTextLines();
		var dim = extractPageDimension();
		return new OCRData(lines, dim);
	}

	private Dimension extractPageDimension() {
		Element page = this.handler.extractElements("Page").get(0);
		int w = Integer.parseInt(page.getAttributeValue("WIDTH"));
		int h = Integer.parseInt(page.getAttributeValue("HEIGHT"));
		return new Dimension(w, h);
	}

	protected List<Textline> extractTextLines() {
		List<OCRData.Textline> lines = new ArrayList<>();
		List<Element> altoLines = this.handler.extractElements("TextLine");
		for (Element el : altoLines) {
			List<Element> strings = el.getChildren("String", getType().toNS());
			var texts = strings.parallelStream()
					.filter(e -> VALID_TEXT.test(e.getAttributeValue("CONTENT")))
					.map(ALTOReader::toText)
					.filter(OCRData.Text::hasPrintableChars)
					.collect(Collectors.toList());
			if (!texts.isEmpty()) { 
				OCRData.Textline line = new OCRData.Textline(texts);
				lines.add(line);
			}
		}
		return lines;
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
