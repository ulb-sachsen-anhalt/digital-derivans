package de.ulb.digital.derivans.data.ocr.alto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Rectangle;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.data.ocr.ALTOReader;
import de.ulb.digital.derivans.data.ocr.Type;
import de.ulb.digital.derivans.data.ocr.ValidTextPredicate;
import de.ulb.digital.derivans.model.ocr.OCRData;

/**
 * 
 * @author u.hartwig
 *
 */
public class TestALTOV4Reader {

	/**
	 * Read ALTO V4 Data produced by ocrd_formatconverter from resulting PAGE 2019
	 * by plain conversion. From VLS 2012 ID 737429.
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
		assertEquals(29, actual.getTextlines().size());
		OCRData.Textline line12 = actual.getTextlines().get(11);
		assertEquals("[1979.0x128.0]AVGVSTISSIMO AC POTENTISSIMO", line12.toString());
		assertEquals("AVGVSTISSIMO AC POTENTISSIMO", line12.getText());
		assertEquals("AVGVSTISSIMO", line12.getTokens().get(0).getText());
		// geometric data
		var line15Shape = line12.getArea();
		assertEquals(new Rectangle(222, 1055, 1979, 128), line15Shape.getBounds());
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
		// now shrink again to 635 because of some invalid lines
		assertEquals(635, actual.getTextlines().size());
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
		int originalHeightLine001 = line001.getBox().height;
		assertEquals(17, originalHeightLine001);
		OCRData.Textline line635 = actual.getTextlines().get(634);
		int originalHeightLine646 = line635.getBox().height;
		assertEquals(57, originalHeightLine646);

		// act
		int maximal = 4678;
		float ratio = (float) maximal / (float) actual.getPageHeight();
		actual.scale(ratio);

		// assert
		assertEquals(maximal, actual.getPageHeight());
		assertNotEquals(originalPageHeigt, actual.getPageHeight());
		assertEquals(8, line001.getBox().height);
		assertEquals(25, line635.getBox().height);
	}
	
	@Test
	void testALTOV3fromZD1() throws Exception {

		// arrange
		Path input = Path.of("src/test/resources/alto/1667522809_J_0025_0001/1667522809_J_0025_0001.xml");
		ALTOReader reader = new ALTOReader(Type.ALTO_V3);
		var actual = reader.get(input);
		int originalPageHeigt = actual.getPageHeight();
		assertEquals(10808, originalPageHeigt);
		OCRData.Textline line001 = actual.getTextlines().get(0);
		int originalHeightLine001 = line001.getBox().height;
		assertEquals(95, originalHeightLine001);
		OCRData.Textline lastLine = actual.getTextlines().get(319);
		assertEquals(94, lastLine.getBox().height);

		// act
		int maximal = 4678;
		float ratio = (float) maximal / (float) actual.getPageHeight();
		actual.scale(ratio);

		// assert
		assertEquals(maximal, actual.getPageHeight());
		assertNotEquals(originalPageHeigt, actual.getPageHeight());
		assertEquals(41, line001.getBox().height);
		assertEquals(40, lastLine.getBox().height);
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

	/**
	 * 
	 * Confidence Testing - handle lines with bad chars only
	 * 
	 * @throws Exception
	 */
	@Test
	void testALTOfromVLS369765() throws Exception {

		// arrange
		Path input = Path.of("src/test/resources/alto/369765/316642.xml");
		ALTOReader reader = new ALTOReader(Type.ALTO_V4);
		var predicate = new ValidTextPredicate();

		// act
		var actual = reader.get(input);

		// assert
		assertEquals(27, actual.getTextlines().size());
		var filtered = actual.getTextlines().stream()
				.map(OCRData.Textline::getText)
				.filter(predicate)
				.collect(Collectors.toList());
		assertEquals(27, filtered.size());
	}
}
