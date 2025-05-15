package de.ulb.digital.derivans.generate.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansRuntimeException;
import de.ulb.digital.derivans.generate.GeneratorPDF;
import de.ulb.digital.derivans.model.pdf.DescriptiveMetadata;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;
import de.ulb.digital.derivans.model.step.DerivateType;

/**
 * 
 * Basic specification for triggering PDF derivates
 * 
 * @author hartwig
 *
 */
class TestPDFGenerator {

	/**
	 * 
	 * Check: missing derivate yielded
	 * 
	 * @throws Exception
	 */
	@Test
	void testInvalidPageArg() {

		DescriptiveMetadata dd = new DescriptiveMetadata();
		DerivateStepPDF pdfStep = new DerivateStepPDF();
		pdfStep.setInputDir(".");
		pdfStep.setOutputDir(".");
		pdfStep.setInputType(DerivateType.JPG);
		pdfStep.setOutputType(DerivateType.PDF);
		pdfStep.mergeDescriptiveData(dd);

		// act
		GeneratorPDF generator = new GeneratorPDF();
		var thrown = assertThrows(DigitalDerivansRuntimeException.class, () -> generator.setStep(pdfStep));

		// assert
		assertEquals("No derivate set: null!", thrown.getMessage());
	}
}
