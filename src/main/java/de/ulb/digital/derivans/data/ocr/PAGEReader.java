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
 * Transform PAGE 2019-07-15 OCR data into domain OCR data
 * 
 * @author u.hartwig
 *
 */
public class PAGEReader implements OCRReader {

	private Document document;

	public static final Predicate<String> VALID_TEXT = new ValidTextPredicate();

	protected Type type;

	public PAGEReader(Type type) {
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
		int w = Integer.parseInt(page.getAttributeValue("imageWidth"));
		int h = Integer.parseInt(page.getAttributeValue("imageHeight"));
		return new Dimension(w, h);
	}

	protected List<Textline> extractTextLines() {
		List<OCRData.Textline> ocrLines = new ArrayList<>();
		List<Element> pgLines = extractElements(new ElementFilter("TextLine"));
		for (Element el : pgLines) {
			List<Element> words = el.getChildren("Word", getType().toNS());
			if (!words.isEmpty()) {
				var texts = words.parallelStream()
						.filter(e -> VALID_TEXT.test(e.getChild("TextEquiv", getType().toNS())
								.getChild("Unicode", getType().toNS()).getTextTrim()))
						.map(this::toText).filter(OCRData.Text::hasPrintableChars).collect(Collectors.toList());
				OCRData.Textline line = new OCRData.Textline(texts);
				ocrLines.add(line);
			}
		}
		return ocrLines;
	}

	protected List<Element> extractElements(ElementFilter filter) {
		List<Element> elements = new ArrayList<>();
		IteratorIterable<Element> elemIt = document.getDescendants(filter);
		while (elemIt.hasNext()) {
			elements.add(elemIt.next());
		}
		return elements;
	}

	OCRData.Text toText(Element word) {
		String content = word
				.getChild("TextEquiv", getType().toNS())
				.getChild("Unicode", getType().toNS())
				.getTextTrim();
		String[] coordPoints = word.getChild("Coords", getType().toNS()).getAttributeValue("points").split(" ");
		String[] topLeft = coordPoints[0].split(",");
		String[] btmRight = coordPoints[2].split(",");
		int topLeftX = Integer.parseInt(topLeft[0]);
		int topLeftY = Integer.parseInt(topLeft[1]);
		int width = Integer.parseInt(btmRight[0]) - topLeftX ;
		int height = Integer.parseInt(btmRight[1]) - topLeftY;
		Rectangle rect = new Rectangle(topLeftX, topLeftY, width, height);
		return new OCRData.Text(content, rect);
	}

	@Override
	public Type getType() {
		return this.type;
	}
}
