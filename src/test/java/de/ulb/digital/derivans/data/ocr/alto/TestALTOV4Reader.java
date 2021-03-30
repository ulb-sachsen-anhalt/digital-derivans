package de.ulb.digital.derivans.data.ocr.alto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Rectangle;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.data.ocr.ALTOReader;
import de.ulb.digital.derivans.data.ocr.Type;
import de.ulb.digital.derivans.model.ocr.OCRData;

/**
 * 
 * @author u.hartwig
 *
 */
public class TestALTOV4Reader {

	/**
	 * Read ALTO V4 Data produced by ocrd_formatconverter from resulting PAGE 2019 by plain conversion.
	 * From VLS 2012 ID 737429.
	 */
	@Test
	void testALTOV4fromOCRD() throws Exception {
		// arrange
		Path vlInhouse737434 = Path.of("./src/test/resources/alto/737429/FULLTEXT/FULLTEXT_737434.xml");
		ALTOReader reader = new ALTOReader(Type.ALTO_V4);
		
		// act
		var actual = reader.get(vlInhouse737434);
		
		// assert
		assertNotNull(actual);
		assertEquals(33, actual.getTextlines().size());
		OCRData.Textline line15 = actual.getTextlines().get(14);
		assertEquals("[1979.0x128.0]AVGVSTISSIMO AC POTENTISSIMO", line15.toString());
		assertEquals("AVGVSTISSIMO AC POTENTISSIMO", line15.getText());
		assertEquals("AVGVSTISSIMO", line15.getTokens().get(0).getText());
		// geometric data
		var line15Shape = line15.getShape();
		assertEquals(new Rectangle(222,1055,1979,128), line15Shape.getBounds());
	}
	
	@Test
	void testALTOfromZD1() throws Exception {
		
		// arrange
		Path input = Path.of("src/test/resources/alto/1667524704_J_0150/1667524704_J_0150_0512.xml");
		ALTOReader reader = new ALTOReader(Type.ALTO_V3);
		
		// act
		var actual = reader.get(input);
		
		// assert
		assertEquals(673, actual.getTextlines().size());
	}
}
