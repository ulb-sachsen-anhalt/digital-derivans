package de.ulb.digital.derivans.data.mets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

		var file = new METSFile(identifier, identifier + ".pdf", useGroup, mimeType);

		assertNull(file.getContentIds());
		assertEquals(identifier, file.getFileId());
		assertEquals("DOWNLOAD", file.getFileGroup());
		assertEquals(identifier + ".pdf", file.getLocation());

		var el = file.asElement().getAttribute("MIMETYPE");
		assertEquals(mimeType, el.getValue());
	}

	@ParameterizedTest
	@CsvSource({ "058141367,058141367.pdf,DOWNLOAD,058141367,058141367.pdf,DOWNLOAD",
			"00000001,00000001.tif,MAX,00000001,00000001.tif,MAX",
			"00000001,00000001.jpg,DEFAULT,00000001,00000001.jpg,DEFAULT",
			"0001,0001,THUMBS,0001,0001,THUMBS"
	})
	void testFilesEquals(String id1, String loc1, String fileGroup1,
			String id2, String loc2, String fileGroup2) {
		METSFile f1 = new METSFile(id1, loc1, fileGroup1);
		METSFile f2 = new METSFile(id2, loc2, fileGroup2);
		assertEquals(f1, f2);
	}

}
