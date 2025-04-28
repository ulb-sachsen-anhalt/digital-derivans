package de.ulb.digital.derivans.data.mets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;


/**
 * @author u.hartwig
 */
class TestMETSFile {

	@Test
	void testPDFDownloadFile() {
		// '058141367' as 'application/pdf' in 'DOWNLOAD'
		String useGroup = "DOWNLOAD";
		String identifier = "058141367";
		String mimeType = "application/pdf";

		var file = new METSFile(identifier, identifier + ".pdf", useGroup ,mimeType);

		assertNull(file.getContentIds());
		assertEquals(identifier, file.getFileId());
		assertEquals("DOWNLOAD", file.getFileGroup());
		assertEquals(identifier + ".pdf", file.getLocation());

		var el = file.asElement().getAttribute("MIMETYPE");
		assertEquals(mimeType, el.getValue());
	}

}
