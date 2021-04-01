package de.ulb.digital.derivans.data.ocr.alto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
		var line15Shape = line15.getArea();
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
		// original 673, but now 647 since there are some empty textlines
		assertEquals(647, actual.getTextlines().size());
	}
	
	/**
	 * 
	 * Scaling of OCR-Data according to scaled images
	 * 
	 * @throws Exception
	 */
	@Test
	void testALTOfromZD1Scale() throws Exception {
		
		// arrange
		Path input = Path.of("src/test/resources/alto/1667524704_J_0150/1667524704_J_0150_0512.xml");
		ALTOReader reader = new ALTOReader(Type.ALTO_V3);
		var actual = reader.get(input);
		int originalPageHeigt = actual.getPageHeight(); 
		assertEquals(10536, originalPageHeigt);
		OCRData.Textline line001 = actual.getTextlines().get(0);
		int originalHeightLine001 = line001.getBounds().height;
		assertEquals(17, originalHeightLine001);
		OCRData.Textline line646 = actual.getTextlines().get(646);
		int originalHeightLine646 = line646.getBounds().height;
		assertEquals(57,  originalHeightLine646);
		
		// act
		int maximal = 4678;
		float ratio = (float)maximal / (float)actual.getPageHeight();
		actual.scale(ratio);
		
		// assert
		assertEquals(maximal, actual.getPageHeight());
		assertNotEquals(originalPageHeigt, actual.getPageHeight());
		assertEquals(8, line001.getBounds().height);
		assertEquals(25, line646.getBounds().height);
	}
	
	@Test
	void testALTOfromVLS320808() throws Exception {
		
		// arrange
		Path input = Path.of("src/test/resources/alto/148811035/FULLTEXT/320808.xml");
		ALTOReader reader = new ALTOReader(Type.ALTO_V4);
		
		// act
		var actual = reader.get(input);
		
		// assert
		assertEquals(25, actual.getTextlines().size());
		Rectangle r01 = actual.getTextlines().get(0).getArea().getBounds();
		assertEquals(600, r01.x);
		assertEquals(751, r01.y);
		assertEquals(1125, r01.width);
		assertEquals(88, r01.height);
	}
}
