package de.ulb.digital.derivans.model.pdf;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * @author u.hartwig
 */
class TestTextElement {
	
	@Test
	void testEuropeanTextElement() {
		assertFalse(new PDFTextElement("foo").isRTL());
	}

	@Test
	void testPersianTextElement() {
		assertTrue(new PDFTextElement("ديباجه").isRTL());
	}

	/**
	 * 
	 * Ensure Changes in text orientation reflected on line-level, too.
	 * 
	 */
	@Test
	void testOrientationWithBiscriptualChildren() {
		var w1 = new PDFTextElement("مخصوصی", new Rectangle2D.Float(523f, 1521f, 135f, 36f));
		var w2 = new PDFTextElement("Pascal", new Rectangle2D.Float(384f, 1513f, 107f, 44f));
		var w3 = new PDFTextElement("کرد", new Rectangle2D.Float(312f, 1514f, 60f, 43f));

		var l1 = new PDFTextElement();
		l1.add(w1);
		l1.add(w2);
		l1.add(w3);

		var printText = l1.forPrint();
		assertTrue(printText.contains("Pascal"));
		assertTrue(printText.contains("یصوصخم"));
		assertTrue(printText.contains("درک"));
	}

		/**
	 * Minimum LTR example
	 * Textline with single ASCII word is
	 * considered to be left-to-right
	 */
	@Test
	void testTextOrientationTextLine() {
		// arrange
		var w1 = new PDFTextElement("hello", new Rectangle2D.Double(0, 0, 100, 20));
		List<PDFTextElement> words = Arrays.asList(w1);
		PDFTextElement line = new PDFTextElement(words);

		// act
		assertFalse(line.isRTL());
	}

	@Test
	void testTextOrientationForPersianWord() {
		// arrange
		var w1 = new PDFTextElement("چه", new Rectangle2D.Double(0, 0, 100, 20));
		List<PDFTextElement> words = Arrays.asList(w1);
		var line = new PDFTextElement(words);

		// act
		assertTrue(line.isRTL());
	}

	@Test
	void testTextOrientationForHebrewWord() {
		// arrange
		var w1 = new PDFTextElement("א", new Rectangle2D.Double(0, 0, 100, 20));
		List<PDFTextElement> words = Arrays.asList(w1);
		var line = new PDFTextElement(words);

		// act
		assertTrue(line.isRTL());
	}
}
