package de.ulb.digital.derivans.model.ocr;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.model.text.Textline;
import de.ulb.digital.derivans.model.text.Word;

/**
 * @author u.hartwig
 */
class TestOCRData {

	/**
	 * Minimum LTR example
	 * Textline with single ASCII word is
	 * considered to be left-to-right
	 */
	@Test
	void testTextOrientationTextLine() {
		// arrange
		var w1 = new Word("hello", new Rectangle(0, 0, 100, 20));
		List<Word> words = Arrays.asList(w1);
		Textline line = new Textline(words);

		// act
		assertFalse(line.isRTL());
	}

	@Test
	void testTextOrientationForPersianWord() {
		// arrange
		var w1 = new Word("چه", new Rectangle(0, 0, 100, 20));
		List<Word> words = Arrays.asList(w1);
		Textline line = new Textline(words);

		// act
		assertTrue(line.isRTL());
	}

	@Test
	void testTextOrientationForHebrewWord() {
		// arrange
		var w1 = new Word("א", new Rectangle(0, 0, 100, 20));
		List<Word> words = Arrays.asList(w1);
		Textline line = new Textline(words);

		// act
		assertTrue(line.isRTL());
	}
}