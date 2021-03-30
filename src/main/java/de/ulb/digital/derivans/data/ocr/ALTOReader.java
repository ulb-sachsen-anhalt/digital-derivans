package de.ulb.digital.derivans.data.ocr;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
		return new OCRData(lines);
	}

	protected List<Textline> extractTextLines() {
		List<OCRData.Textline> lines = new ArrayList<>();
		
		IteratorIterable<Element> elemIt = document.getDescendants(new ElementFilter("TextLine"));
		while(elemIt.hasNext()) {
			Element el = elemIt.next();
			List<Element> strings = el.getChildren("String", getType().toNS());
			var texts = strings.parallelStream().map(ALTOReader::toText).collect(Collectors.toList());
			if(!texts.isEmpty()) {
				OCRData.Textline line = new OCRData.Textline(texts);
				lines.add(line);
			}
		}
		
		return lines;
	}

	static OCRData.Text toText(Element word) {
		String content = word.getAttributeValue("CONTENT"); 
		int x = Integer.valueOf(word.getAttributeValue("HPOS"));
		int y = Integer.valueOf(word.getAttributeValue("VPOS"));
		int width = Integer.valueOf(word.getAttributeValue("WIDTH"));
		int height = Integer.valueOf(word.getAttributeValue("HEIGHT"));
		Rectangle rect = new Rectangle(x, y, width, height);
		OCRData.Text txt = new OCRData.Text(content, rect);
		return txt;
	}

	@Override
	public Type getType() {
		return this.type;
	}
}
