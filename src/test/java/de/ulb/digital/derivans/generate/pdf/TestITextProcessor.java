package de.ulb.digital.derivans.generate.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.model.pdf.PDFTextElement;

/**
 *
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
}
