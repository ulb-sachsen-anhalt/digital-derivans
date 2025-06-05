package de.ulb.digital.derivans.data.ocr.alto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Rectangle;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.data.ocr.ALTOReader;
import de.ulb.digital.derivans.data.ocr.Type;
import de.ulb.digital.derivans.data.ocr.ValidTextPredicate;
import de.ulb.digital.derivans.model.text.Textline;

/**
 * 
 * @author u.hartwig
 *
 */
class TestALTOV4Reader {

	/**
	 * Read ALTO V4 Data produced by ocrd_formatconverter from resulting PAGE 2019
	 * by plain conversion. From VLS 2012 ID 737429.
	 */
	@Test
	void testALTOV4fromOCRD() throws Exception {
		// arrange
		Path vlInhouse737434 = Path.of("./src/test/resources/ocr/alto/737429/FULLTEXT/FULLTEXT_737434.xml");
		ALTOReader reader = new ALTOReader(Type.ALTO_V4);

		// act
		var actual = reader.get(vlInhouse737434);

		// assert
		assertNotNull(actual);
		assertEquals(29, actual.getTextlines().size());
		Textline line12 = actual.getTextlines().get(11);
		assertEquals("[1979.0x128.0]AVGVSTISSIMO AC POTENTISSIMO", line12.toString());
		assertEquals("AVGVSTISSIMO AC POTENTISSIMO", line12.getText());
		assertEquals("AVGVSTISSIMO", line12.getWords().get(0).getText());
		// geometric data
		var line15Shape = line12.getArea();
		assertEquals(new Rectangle(222, 1055, 1979, 128), line15Shape.getBounds());
	}

	@Test
	void testALTOfromZD1() throws Exception {

		// arrange
		Path input = Path.of("src/test/resources/ocr/alto/1667524704_J_0150/1667524704_J_0150_0512.xml");
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
		Path input = Path.of("src/test/resources/ocr/alto/1667524704_J_0150/1667524704_J_0150_0512.xml");
		ALTOReader reader = new ALTOReader(Type.ALTO_V3);
		var actual = reader.get(input);
		int originalPageHeigt = actual.getPageHeight();
		assertEquals(10536, originalPageHeigt);
		Textline line001 = actual.getTextlines().get(0);
		int originalHeightLine001 = (int)line001.getBox().getHeight();
		assertEquals(17, originalHeightLine001);
		Textline line635 = actual.getTextlines().get(634);
		int originalHeightLine646 = (int)line635.getBox().getHeight();
		assertEquals(57, originalHeightLine646);

		// act
		int maximal = 4678;
		float ratio = (float) maximal / (float) actual.getPageHeight();
		actual.scale(ratio);

		// assert
		assertEquals(maximal, actual.getPageHeight());
		assertNotEquals(originalPageHeigt, actual.getPageHeight());
		assertEquals(8, (int)line001.getBox().getHeight());
		assertEquals(25, (int)line635.getBox().getHeight());
	}

	@Test
	void testALTOV3fromZD1() throws Exception {

		// arrange
		Path input = Path.of("src/test/resources/ocr/alto/1667522809_J_0025_0001/1667522809_J_0025_0001.xml");
		ALTOReader reader = new ALTOReader(Type.ALTO_V3);
		var actual = reader.get(input);
		int originalPageHeigt = actual.getPageHeight();
		assertEquals(10808, originalPageHeigt);
		Textline line001 = actual.getTextlines().get(0);
		int originalHeightLine001 = (int)line001.getBox().getHeight();
		assertEquals(95, originalHeightLine001);
		Textline lastLine = actual.getTextlines().get(319);
		assertEquals(94, (int)lastLine.getBox().getHeight());

		// act
		int maximal = 4678;
		float ratio = (float) maximal / (float) actual.getPageHeight();
		actual.scale(ratio);

		// assert
		assertEquals(maximal, actual.getPageHeight());
		assertNotEquals(originalPageHeigt, actual.getPageHeight());
		assertEquals(41, (int)line001.getBox().getHeight());
		assertEquals(40, (int)lastLine.getBox().getHeight());
	}

	@Test
	void testALTOfromVLS320808() throws Exception {

		// arrange
		Path input = TestResource.VD_18_148811035_ALTO4.get();
		ALTOReader reader = new ALTOReader(Type.ALTO_V4);

		// act
		var actual = reader.get(input);

		// assert
		assertEquals(36, actual.getTextlines().size());
		Rectangle r01 = actual.getTextlines().get(0).getArea().getBounds();
		assertEquals(917, r01.x);
		assertEquals(270, r01.y);
		assertEquals(80, r01.width);
		assertEquals(47, r01.height);
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
		Path input = Path.of("src/test/resources/ocr/alto/369765/316642.xml");
		ALTOReader reader = new ALTOReader(Type.ALTO_V4);
		var predicate = new ValidTextPredicate();

		// act
		var actual = reader.get(input);

		// assert
		assertEquals(27, actual.getTextlines().size());
		var filtered = actual.getTextlines().stream()
				.map(Textline::getText)
				.filter(predicate)
				.collect(Collectors.toList());
		assertEquals(27, filtered.size());
	}

	/**
	 * Read ALTO V4 Data to ensure even right-to-left
	 * gets properly parsed - compare also first
	 * and last token of parsed line
	 */
	@Test
	void testALTOV4fromRahbar() throws Exception {
		// arrange
		Path rahbar88120 = TestResource.SHARE_IT_RAHBAR_88120_LEGACY.get();
		ALTOReader reader = new ALTOReader(Type.ALTO_V4);

		// act
		var actual = reader.get(rahbar88120);

		// assert
		assertNotNull(actual);
		assertEquals(30, actual.getTextlines().size());
		Textline line2 = actual.getTextlines().get(1);
		assertEquals("کرده اند مثلاً انوری حافظ قصائدو غزلپائی بفتند رفتند پس از آبان", line2.getText());
		assertEquals("کرده", line2.getWords().get(0).getText());
		assertEquals("آبان", line2.getWords().get(line2.getWords().size() - 1).getText());
	}

	/**
	 * Ensure empty ALTO data can be handled
	 * 
	 * DigitalDerivansException:
	 * No Page data: src/test/resources/ocr/alto/1981185920_94220/00000805.xml
	 */
	@Test
	void testALTOV4EmptyFromRahbar() {
		// arrange
		Path rahbar88120 = TestResource.SHARE_IT_RAHBAR_94220.get();
		ALTOReader reader = new ALTOReader(Type.ALTO_V4);

		// act
		var actual = assertThrows(DigitalDerivansException.class,
				() -> reader.get(rahbar88120));

		// assert
		assertNotNull(actual);
		assertEquals("No Page data: src/test/resources/ocr/alto/1981185920_94220/00000805.xml",
				actual.getMessage());
	}
}
