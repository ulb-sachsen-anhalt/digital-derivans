package de.ulb.digital.derivans.model.ocr;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * @author u.hartwig
 */
public class TestOCRData {

	/**
	 * Minimum LTR example
	 * Textline with single ASCII word is
	 * considered to be left-to-right
	 */
	@Test
	void testTextOrientationTextLine() {
		// arrange
		var w1 = new OCRData.Word("hello", new Rectangle(0, 0, 100, 20));
		List<OCRData.Word> words = Arrays.asList(w1);
		OCRData.Textline line = new OCRData.Textline(words);

		// act
		assertTrue(line.isLTR());
	}

	@Test
	void testTextOrientationForPersianWord() {
		// arrange
		var w1 = new OCRData.Word("چه", new Rectangle(0, 0, 100, 20));
		List<OCRData.Word> words = Arrays.asList(w1);
		OCRData.Textline line = new OCRData.Textline(words);

		// act
		assertFalse(line.isLTR());
	}

	@Test
	void testTextOrientationForHebrewWord() {
		// arrange
		var w1 = new OCRData.Word("א", new Rectangle(0, 0, 100, 20));
		List<OCRData.Word> words = Arrays.asList(w1);
		OCRData.Textline line = new OCRData.Textline(words);

		// act
		assertFalse(line.isLTR());
	}
}