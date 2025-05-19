package de.ulb.digital.derivans.data.mets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestResource;

/**
 * @author u.hartwig
 */
class TestDFGMETS {
	
	@Test
	void parseLegacyMETS() throws DigitalDerivansException {
		var path = TestResource.VLS_VD18P_14163614.get();
		var m = new METS(path);
		assertNotNull(m);
		assertTrue(m.getPath().toString().endsWith(".xml"));
	}

	/**
	 * 
	 * Jg 1765 "Pirnaischer Chronicken und Historien Calender"
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void parseContainerStructureVD18PVLS() throws DigitalDerivansException {
		var path = TestResource.VLS_VD18P_14163614.get();
		var m = new METS(path);
		m.init();
		var containerStructure = m.getLogicalRoot();
		assertEquals(METSContainerType.VOLUME, containerStructure.getType());
		assertEquals("1765", containerStructure.determineLabel());
	}


	@Test
	void testInvalidImageFileGroup() throws DigitalDerivansException {
		METS mets737429 = new METS(TestResource.VLS_HD_Aa_737429.get());
		mets737429.setImgFileGroup("FOO");
		var result = assertThrows(DigitalDerivansException.class, () -> mets737429.init());
		assertEquals("Invalid input mets:fileGrp FOO!", result.getMessage());
	}
}
