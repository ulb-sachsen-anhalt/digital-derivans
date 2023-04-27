package de.ulb.digital.derivans.data.ocr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.data.ocr.OCRReader;
import de.ulb.digital.derivans.data.ocr.OCRReaderFactory;
import de.ulb.digital.derivans.data.ocr.Type;
import de.ulb.digital.derivans.model.ocr.OCRData;

/**
 * 
 * @author u.hartwig
 *
 */
public class TestOCRReaderFactory {

	@Test
	void testDetectUnmappedType() throws Exception {
		// arrange
		Path vlInhouse737434 = Path.of("./src/test/resources/metadata/kitodo2/meta.xml");
		
		// act
		assertThrows(DigitalDerivansException.class, () -> OCRReaderFactory.from(vlInhouse737434)); 
	}
	
	@Test
	void testResolveALTOV4() throws Exception {
		// arrange
		Path vlInhouse737434 = Path.of("./src/test/resources/alto/737429/FULLTEXT/FULLTEXT_737434.xml");
		
		// act
		OCRReader reader = OCRReaderFactory.from(vlInhouse737434);
		OCRData data = reader.get(vlInhouse737434);
		
		// assert
		var actualType = reader.getType();
		assertEquals(Type.ALTO_V4, actualType);
		assertEquals(29, data.getTextlines().size());
	}
	
	@Test
	void testResolveALTOfromZD1() throws Exception {
		
		// arrange
		Path input = Path.of("src/test/resources/alto/1667524704_J_0150/1667524704_J_0150_0512.xml");
		
		// act
		OCRReader reader = OCRReaderFactory.from(input);
		
		// assert
		assertEquals(Type.ALTO_V3, reader.getType());
	}
	
	@Test
	void testResolvePAGE2019FromODEM() throws Exception {
		// arrange
		Path input = Path.of("src/test/resources/page/16258167.xml");
		
		// act
		OCRReader reader = OCRReaderFactory.from(input);
		
		// assert
		assertEquals(Type.PAGE_2019, reader.getType());
	}
}
