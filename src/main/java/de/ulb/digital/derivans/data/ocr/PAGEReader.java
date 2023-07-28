package de.ulb.digital.derivans.data.ocr;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Polygon;
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
 * Transform PAGE 2019-07-15 OCR data into domain OCR data
 * 
 * @author u.hartwig
 *
 */
public class PAGEReader implements OCRReader {

	private static final String PAGE_UNICODE = "Unicode";

	private static final String PAGE_TEXT_EQUIV = "TextEquiv";

	private XMLHandler handler;

	public static final Predicate<String> VALID_TEXT = new ValidTextPredicate();

	protected Type type;

	public PAGEReader(Type type) {
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
		int w = Integer.parseInt(page.getAttributeValue("imageWidth"));
		int h = Integer.parseInt(page.getAttributeValue("imageHeight"));
		return new Dimension(w, h);
	}

	protected List<Textline> extractTextLines() {
		List<OCRData.Textline> ocrLines = new ArrayList<>();
		List<Element> pgLines = this.handler.extractElements("TextLine");
		for (Element el : pgLines) {
			List<Element> words = el.getChildren("Word", getType().toNS());
			if (!words.isEmpty()) {
				var texts = words.parallelStream()
						.filter(e -> VALID_TEXT.test(e.getChild(PAGE_TEXT_EQUIV, getType().toNS())
								.getChild(PAGE_UNICODE, getType().toNS()).getTextTrim()))
						.map(this::toText).filter(OCRData.Text::hasPrintableChars).collect(Collectors.toList());
				OCRData.Textline line = new OCRData.Textline(texts);
				ocrLines.add(line);
			} else {
				String transcription = el.getChild(PAGE_TEXT_EQUIV, getType().toNS()).getChild(PAGE_UNICODE, getType().toNS()).getTextTrim();
				if (VALID_TEXT.test(transcription)) {
					var ocrData = this.toText(el);
					if (ocrData.hasPrintableChars()) {
						ocrLines.add(new OCRData.Textline(List.of(ocrData)));
					}
				}
			}
		}
		return ocrLines;
	}

	OCRData.Text toText(Element textElement) {
		String content = textElement
				.getChild(PAGE_TEXT_EQUIV, getType().toNS())
				.getChild(PAGE_UNICODE, getType().toNS())
				.getTextTrim();
		String[] coordPoints = textElement.getChild("Coords", getType().toNS()).getAttributeValue("points").split(" ");
		Polygon polygon = new Polygon();
		for (String coordPair : coordPoints) {
			String[] tokens = coordPair.split(",");
			Integer x = Integer.valueOf(tokens[0]);
			Integer y = Integer.valueOf(tokens[1]);
			polygon.addPoint(x, y);
		}
		Rectangle rect = polygon.getBounds();
		return new OCRData.Text(content, rect);
	}

	@Override
	public Type getType() {
		return this.type;
	}
}
