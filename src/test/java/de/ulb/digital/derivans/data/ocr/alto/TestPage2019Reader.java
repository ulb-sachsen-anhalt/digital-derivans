package de.ulb.digital.derivans.data.ocr.alto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Rectangle;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.data.ocr.PAGEReader;
import de.ulb.digital.derivans.data.ocr.Type;
import de.ulb.digital.derivans.model.ocr.OCRData;

/**
 * 
 * @author u.hartwig
 *
 */
public class TestPage2019Reader {

	/**
	 * Read ALTO V4 Data produced by ocrd_formatconverter from resulting PAGE 2019
	 * by plain conversion. From VLS 2012 ID 737429.
	 */
	@Test
	void testPAGE2019fromOCRD() throws Exception {
		// arrange
		Path page2019 = Path.of("./src/test/resources/page/16258167.xml");
		PAGEReader reader = new PAGEReader(Type.PAGE_2019);

		// act
		var actual = reader.get(page2019);

		// assert
		assertNotNull(actual);
		assertEquals(10, actual.getTextlines().size());
		OCRData.Textline loi = actual.getTextlines().get(3);
		assertEquals("[1126.0x63.0]So Guth als Blut für Ihn zu geben!", loi.toString());
		assertEquals("So Guth als Blut für Ihn zu geben!", loi.getText());
		assertEquals("Guth", loi.getTokens().get(1).getText());
		// geometric data
		var line15Shape = loi.getArea();
		assertEquals(new Rectangle(362, 1764, 1126, 63), line15Shape.getBounds());
	}


	/**
	 * 
	 * Ensure handling of PAGE with OCR data just on Textline Level
	 * 
	 * Please note, although it works, results differ from granular data
	 * and might contain some more rubbish output which would be stripped
	 * otherwise
	 * 
	 * @throws Exception
	 */
	@Test
	void testPAGEWithOnlyTextlines() throws Exception {
		// arrange
		Path pagewihoutWords = Path.of("./src/test/resources/page/16258167_no_words.xml");
		PAGEReader reader = new PAGEReader(Type.PAGE_2019);

		// act
		var actual = reader.get(pagewihoutWords);

		// assert
		assertEquals(10, actual.getTextlines().size());
		OCRData.Textline loi = actual.getTextlines().get(3);
		assertEquals("[1337.0x81.0]So Guth als Blut für Ihn zu geben! ===", loi.toString());
		assertEquals("So Guth als Blut für Ihn zu geben! ===", loi.getText());
		// geometric data
		var line15Shape = loi.getArea();
		assertEquals(new Rectangle(342, 1751, 1337, 81), line15Shape.getBounds());
	}
}
