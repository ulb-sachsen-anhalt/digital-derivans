package de.ulb.digital.derivans.model.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import de.ulb.digital.derivans.model.ocr.OCRData;
import de.ulb.digital.derivans.model.pdf.PDFTextElement;

/**
 * 
 * Behavor scaling and (especially!) inversion of top-left vs. bottom-left
 * 
 * @author hartwig
 *
 */
class TestWords {

	private int textMarginLeft = 50;

	private int textMarginTop = 200;

	/**
	 * 
	 * Gather insights 
	 * 
	 */
	@Test
	void inspectScalingInversion() {
		var originalPageHeight = 800;
		var originalLineHeight = 30;

		List<Word> texts1 = new ArrayList<>();
		texts1.add(new Word("BELLA", new Rectangle(textMarginLeft, textMarginTop, 70, originalLineHeight)));
		texts1.add(new Word("CHIAO", new Rectangle(130, textMarginTop, 70, originalLineHeight)));
		texts1.add(new Word("(DELLE", new Rectangle(210, textMarginTop, 80, originalLineHeight)));
		texts1.add(new Word("MODINE)", new Rectangle(300, textMarginTop, 100, originalLineHeight)));
		List<Textline> lines = List.of(new Textline(texts1));

		OCRData ocrData = new OCRData(lines, new Dimension(575, originalPageHeight));
		var originalMax = 400;
		var originalY1 = textMarginTop;
		var scaleRatio = 0.5f;
		var boxOriginal = ocrData.getTextlines().get(0).getBox();
		assertEquals(originalMax, boxOriginal.getMaxX());
		assertEquals(originalY1, boxOriginal.getMinY());

		// act
		ocrData.scale(scaleRatio);

		// scaling only halfes
		var boxScaled = ocrData.getTextlines().get(0).getBox();
		assertEquals(originalMax * scaleRatio, boxScaled.getMaxX());
		assertEquals(originalY1 * scaleRatio, boxScaled.getMinY());
		var lineIn = ocrData.getTextlines().get(0);
		PDFTextElement textElem = new PDFTextElement(lineIn.getText(), lineIn.getBox());
		var scaledTop = textElem.getBox().getMinY();
		var scaledBtm = textElem.getBox().getMaxY();
		assertEquals(originalMax * scaleRatio, textElem.getBox().getMaxX());
		assertEquals(originalY1 * scaleRatio, scaledTop);
		assertEquals(originalY1 * scaleRatio + originalLineHeight * scaleRatio, scaledBtm);

		// re-act
		var newHeight = originalPageHeight * scaleRatio;
		textElem.invert(newHeight);
		assertEquals(originalMax * scaleRatio, textElem.getBox().getMaxX());
		var newTop = textElem.getBox().getMaxY();
		var newBtm = textElem.getBox().getMinY();
		assertEquals(315 - textElem.getBox().getHeight(), newTop);
		assertEquals(300 - textElem.getBox().getHeight(), newBtm);
		assertEquals(288.75, textElem.getBaseline().getY1());
	}
}
