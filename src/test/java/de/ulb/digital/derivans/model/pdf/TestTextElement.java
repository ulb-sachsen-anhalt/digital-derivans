package de.ulb.digital.derivans.model.pdf;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

}
