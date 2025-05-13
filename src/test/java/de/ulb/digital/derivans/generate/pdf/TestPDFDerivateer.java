package de.ulb.digital.derivans.generate.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.DigitalDerivansRuntimeException;
import de.ulb.digital.derivans.IDerivans;
import de.ulb.digital.derivans.generate.GeneratorPDF;
import de.ulb.digital.derivans.model.DerivansData;
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
class TestPDFDerivateer {

	/**
	 * 
	 * Check: invalid pages provided => yield exception
	 * @throws DigitalDerivansException 
	 * 
	 * @throws Exception
	 */
	@Test
	void testInvalidPageArg() throws DigitalDerivansException {

		// arrange mwe
		DerivansData input = new DerivansData(Path.of("."), IDerivans.IMAGE_DIR_DEFAULT, DerivateType.JPG);
		DerivansData output = new DerivansData(Path.of("."), ".", DerivateType.PDF);
		DescriptiveMetadata dd = new DescriptiveMetadata();
		DerivateStepPDF pdfMeta = new DerivateStepPDF();
		pdfMeta.mergeDescriptiveData(dd);

		// act
		GeneratorPDF generator = new GeneratorPDF(input, output, null, pdfMeta);
		var thrown = assertThrows(DigitalDerivansRuntimeException.class, () -> {
			generator.create();
		});

		// assert
		assertEquals("Invalid pdfFilePath: null", thrown.getMessage());
	}
}
