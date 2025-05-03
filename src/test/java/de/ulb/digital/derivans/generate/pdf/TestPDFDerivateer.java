package de.ulb.digital.derivans.generate.pdf;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansException;
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
	 * 
	 * @throws Exception
	 */
	@Test
	void testInvalidPageArg() {

		// arrange mwe
		DerivansData input = new DerivansData(Path.of("."), IDerivans.IMAGE_DIR_DEFAULT, DerivateType.JPG);
		DerivansData output = new DerivansData(Path.of("."), ".", DerivateType.PDF);
		DescriptiveMetadata dd = new DescriptiveMetadata();
		DerivateStepPDF pdfMeta = new DerivateStepPDF();
		pdfMeta.mergeDescriptiveData(dd);

		// act
		var thrown = assertThrows(DigitalDerivansException.class, () -> {
			new GeneratorPDF(input, output, null, pdfMeta);
		});

		// assert
		assertTrue(thrown.getMessage().contains("Invalid pages"));
	}
}
