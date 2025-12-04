package de.ulb.digital.derivans.data.mets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestResource;

/**
 * @author u.hartwig
 */
class TestDFGMETS {

	/**
	 * 
	 * Jg 1765 "Pirnaischer Chronicken und Historien Calender"
	 * containes 38 pages
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void parseContainerStructureVD18PVLS() throws DigitalDerivansException {
		// given
		var path = TestResource.VLS_VD18P_14163614.get();
		var m = new METS(path);
		assertFalse(m.isInited());

		// when
		m.init();

		// then
		assertTrue(m.isInited());
		METSContainer root = m.getLogicalRoot();
		assertEquals(METSContainerType.VOLUME, root.getType());
		assertEquals("1765", root.determineLabel());
		assertEquals(38, m.getPages().size());
	}

	/**
	 * 
	 * This one does not contain files in fileGrp "FOO"
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testInvalidImageFileGroup() throws DigitalDerivansException {
		METS mets737429 = new METS(TestResource.VLS_HD_Aa_737429.get());
		mets737429.addFileGroup("FOO");
		var result = assertThrows(DigitalDerivansException.class, mets737429::init);
		assertEquals("Missing required mets:fileGrp FOO!", result.getMessage());
	}

	/**
	 * Only accept *real* physical pages, i.e. container with @TYPE="page"
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testKitodo3Issue() throws DigitalDerivansException {
		// arrange
		var metsZD2Issue = new METS(TestResource.K3_ZD2_253780594.get());

		// act
		metsZD2Issue.init();

		// assert
		var root = metsZD2Issue.getLogicalRoot();
		assertEquals(7, metsZD2Issue.getPages().size());

		assertEquals(METSContainerType.ISSUE, root.getType());
		assertEquals("Nr. 58.", root.determineLabel());
	}

	/**
	 * 
	 * Large monograph from Inhouse historical printworks
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testLargeMonograph() throws DigitalDerivansException {
		// arrange
		var metsZD2Issue = new METS(TestResource.VLS_HD_Aa_201517.get());

		// act
		metsZD2Issue.init();

		// assert
		var root = metsZD2Issue.getLogicalRoot();
		assertEquals(2306, metsZD2Issue.getPages().size());
		assertEquals(METSContainerType.MONOGRAPH, root.getType());
		assertTrue(root.determineLabel().startsWith("Johannis Micraelii, Pomerani Historia Ecclesiastica"));
	}
}
