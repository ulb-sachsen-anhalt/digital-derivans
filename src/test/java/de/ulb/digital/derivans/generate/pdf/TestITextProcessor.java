package de.ulb.digital.derivans.generate.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.model.DerivateMD;
import de.ulb.digital.derivans.model.pdf.PDFTextElement;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;

/**
 * Inspect harmonizing behavior
 * especially for corner cases
 *
 * @author u.hartwig
 */
class TestITextProcessor {

	@ParameterizedTest
	@CsvSource({ "foo bar, foo bar",
			"Kaͤhſerlichen Majeſtaͤ, Kähserlichen Majestä",
			"eines beſtaͤndigen Frie⸗, eines beständigen Frie-",
			"beſtaͤ⸗, bestä-"
	})
	void testITextHarmonize01(String textIn, String homarnizedText) throws DigitalDerivansException {
		var token = new PDFTextElement(textIn);

		var iTextProc = new ITextProcessor();
		iTextProc.loadFont("ttf/DejaVuSans.ttf");

		String result = iTextProc.harmonizeText(token);
		assertEquals(homarnizedText, result);

	}

	/**
	 * Test that valid DPI values are accepted (72-600 range)
	 */
	@ParameterizedTest
	@ValueSource(ints = { 72, 150, 300, 400, 600 })
	void testValidDpiValues(int dpi) throws Exception {
		var step = new DerivateStepPDF(".", ".");
		step.setImageDpi(dpi);
		
		var processor = new ITextProcessor();
		var derivate = new DerivateMD(java.nio.file.Path.of("src/test/resources/mets/kitodo2/319696111.xml"));
		
		// Should not throw exception for valid DPI values
		assertDoesNotThrow(() -> processor.init(step, derivate));
	}

	/**
	 * Test that invalid DPI values are rejected (outside 72-600 range)
	 * This test exposes the bug where the condition was: dpi <= 300 && dpi >= 600
	 * which could never be true.
	 */
	@ParameterizedTest
	@ValueSource(ints = { 50, 71, 601, 700, 1000 })
	void testInvalidDpiValues(int dpi) throws Exception {
		var step = new DerivateStepPDF(".", ".");
		step.setImageDpi(dpi);
		
		var processor = new ITextProcessor();
		var derivate = new DerivateMD(java.nio.file.Path.of("src/test/resources/mets/kitodo2/319696111.xml"));
		
		// Should throw DigitalDerivansException for invalid DPI values
		assertThrows(DigitalDerivansException.class, () -> processor.init(step, derivate));
	}

	/**
	 * Test boundary values at the edges of the valid range
	 */
	@Test
	void testDpiBoundaryValues() throws Exception {
		var step72 = new DerivateStepPDF(".", ".");
		step72.setImageDpi(72);
		var processor72 = new ITextProcessor();
		var derivate = new DerivateMD(java.nio.file.Path.of("src/test/resources/mets/kitodo2/319696111.xml"));
		
		// 72 should be valid (minimum)
		assertDoesNotThrow(() -> processor72.init(step72, derivate));
		
		// 71 should be invalid (below minimum)
		var step71 = new DerivateStepPDF(".", ".");
		step71.setImageDpi(71);
		var processor71 = new ITextProcessor();
		assertThrows(DigitalDerivansException.class, () -> processor71.init(step71, derivate));
		
		// 600 should be valid (maximum)
		var step600 = new DerivateStepPDF(".", ".");
		step600.setImageDpi(600);
		var processor600 = new ITextProcessor();
		assertDoesNotThrow(() -> processor600.init(step600, derivate));
		
		// 601 should be invalid (above maximum)
		var step601 = new DerivateStepPDF(".", ".");
		step601.setImageDpi(601);
		var processor601 = new ITextProcessor();
		assertThrows(DigitalDerivansException.class, () -> processor601.init(step601, derivate));
	}
}
