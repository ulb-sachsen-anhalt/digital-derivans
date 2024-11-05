package de.ulb.digital.derivans.derivate.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.model.pdf.PDFMetadata;

/**
 * 
 * @author hartwig
 *
 */
class TestPDFInspector {

	/**
	 * 
	 * Inspect existing real-world PDF data
	 * 
	 * @throws Exception
	 */
	@Test
	void testReadMetaInformationFromPDF01() throws Exception {
		// act
		Path pdfPath = Paths.get("src/test/resources/pdf/169683404X.pdf");
		PDFInspector inspector = new PDFInspector(pdfPath);
		PDFMetadata pdfMetaInformation = inspector.getPDFMetaInformation();

		// assert
		assertEquals(
				"Namensteil: Boethius (Typ: family), 480-524 (Typ: date), Anicius Manlius Severinus (Typ: given), authority URI: , value URI: , Rolle: Rollenbezeichnung: aut (Norm: marcrelator, Typ: code), Anzeigeform: Boethius, Anicius Manlius Severinus, Typ: personal, Norm: gnd",
				pdfMetaInformation.getAuthor());
		assertEquals("[Halle (Saale), Universitäts- und Landesbibliothek Sachsen-Anhalt, Qu. Cod. 77, Fragment 1]",
				pdfMetaInformation.getTitle());
		var optRecordData = pdfMetaInformation.getFromXmpMetadataBy("recordIdentifier");
		assertTrue(optRecordData.isPresent());
		var recordData = optRecordData.get();
		assertEquals("169683404X", recordData.getChildren().get(0).getValue());
	}
}
