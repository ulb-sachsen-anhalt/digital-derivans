package de.ulb.digital.derivans.data.mets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestHelper;
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
		METS mets737429 = new METS(TestResource.VLS_HD_Aa_737429.get(), "FOO");
		var result = assertThrows(DigitalDerivansException.class, mets737429::init);
		assertEquals("Invalid input mets:fileGrp FOO!", result.getMessage());
	}

	/**
	 * Only accept *real* physical pages, i.e. container with @TYPE="page"
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testKitodo3Issue() throws DigitalDerivansException {
		// arrange
		var issueMets = new METS(TestResource.K3_ZD2_253780594.get());

		// act
		issueMets.init();

		// assert
		var root = issueMets.getLogicalRoot();
		assertEquals(7, issueMets.getPages().size());
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
		var issueMets = new METS(TestResource.VLS_HD_Aa_201517.get());

		// act
		issueMets.init();

		// assert
		var root = issueMets.getLogicalRoot();
		assertEquals(2306, issueMets.getPages().size());
		assertEquals(METSContainerType.MONOGRAPH, root.getType());
		assertTrue(root.determineLabel().startsWith("Johannis Micraelii, Pomerani Historia Ecclesiastica"));
	}

	/**
	 * 
	 * Small monograph from Share-IT with missing page No 05
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testShareItPPN148811035() throws DigitalDerivansException {
		// arrange
		var mets = new METS(TestResource.SHARE_IT_VD18_148811035.get(), TestHelper.ULB_MAX);

		// act
		mets.init();

		// assert
		var root = mets.getLogicalRoot();
		assertEquals(16, mets.getPages().size());
		assertEquals(METSContainerType.MONOGRAPH, root.getType());
		assertTrue(root.determineLabel().startsWith("Neue Friedens-Vorschläge"));
	}

	/**
	 * 
	 * Medium size periodical volume from Share-IT with mixture of pages and
	 * sub-structures
	 * Lead to very strange gaps in final PDF, like from page image 13 to 109 or
	 * 112 to 224 only having blank white pages without any content.
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testShareItPPN168566600011796() throws DigitalDerivansException {
		// arrange
		var mets = new METS(TestResource.SHARE_IT_VD18_1981185920_35126.get(), TestHelper.ULB_MAX);

		// act
		mets.init();

		// assert
		var root = mets.getLogicalRoot();
		assertEquals(415, mets.getPages().size());
		assertEquals(METSContainerType.VOLUME, root.getType());
		assertTrue(root.determineLabel().startsWith("Nouveaux cahiers de lecture"));
	}

	/**
	 * 
	 * Decide how to handle invalid logical links
	 * => Throw Exception
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testStructureMissingLinkFromLogicalSection() throws DigitalDerivansException {
		// arrange
		var m = new METS(TestResource.SHARE_IT_VD18_43053.get(), TestHelper.ULB_MAX);

		// act
		var actualExc = assertThrows(DigitalDerivansException.class, m::validate);

		// assert
		assertEquals("No files link div log1646693 (LABEL: Abschnitt)", actualExc.getMessage());
	}

	/**
	 * 
	 * This Record is assumed to fail at least for DDB-Validation
	 * Because one of it's logical sections, with ID "LOG_0216",
	 * is not linked to any physical structure.
	 *
	 * Obviously forgotten, but must be corrected anyway.
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testKitodo2InvalidLegacyFStage030745780() throws DigitalDerivansException {
		// arrange
		var m = new METS(TestResource.K2_Af_030745780.get(), TestHelper.ULB_MAX);

		// act
		var actualExc = assertThrows(DigitalDerivansException.class, m::validate);

		// assert - as of 2025
		assertEquals("No files link div LOG_0216 (LABEL: 207. [An Charlotte Pistorius.])", actualExc.getMessage());
	}
}
