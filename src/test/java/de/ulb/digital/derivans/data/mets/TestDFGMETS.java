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
		assertEquals(0, root.getFiles().size());
		assertEquals(6, root.getChildren().size());
	}

	/**
	 * 
	 * MWE of all times - if this fails, we fall.
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testMWEContainerStructure() throws DigitalDerivansException {
		METS mets737429 = new METS(TestResource.VLS_HD_Aa_737429.get(), TestHelper.ULB_MAX);
		mets737429.init();
		METSContainer root = mets737429.getLogicalRoot();
		assertEquals(METSContainerType.MONOGRAPH, root.getType());
		assertTrue(root.determineLabel().startsWith("Ode "));
		assertEquals(0, root.getFiles().size());
		assertEquals(2, root.getChildren().size());

		// inspect first child "Titelblatt"
		var lvl01Child01 = root.getChildren().get(0);
		assertEquals(METSContainerType.TITLE_PAGE, lvl01Child01.getType());
		assertEquals("Titelblatt", lvl01Child01.determineLabel());
		assertEquals(1, lvl01Child01.getChildren().size());
		var lvl01Child01Child01 = lvl01Child01.getChildren().get(0);
		assertEquals(METSContainerType.PAGE, lvl01Child01Child01.getType());
		assertEquals("[Seite 2]", lvl01Child01Child01.determineLabel());

		// inspect secnd child "Ode"
		var lvl01Child02 = root.getChildren().get(1);
		assertEquals(METSContainerType.VERSE, lvl01Child02.getType());
		assertEquals(3, lvl01Child02.getChildren().size());
		assertEquals("[Seite 3]", lvl01Child02.getChildren().get(0).determineLabel());
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
	 * 
	 * Tackle issue with linked containers, resulting in too many linked pages
	 * later in PDF outline stage.
	 * 
	 * This original METS containes 7 links between issue and page-Containers,
	 * from which 2 are linked via sub-container "Feuilleton-Beilage. Nr. 20."
	 * already for total 7 pages.
	 * 
	 * Ensure only 5 unique pages are linked from top issue container.
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
		METSContainer root = issueMets.getLogicalRoot();
		assertEquals(METSContainerType.ISSUE, root.getType());
		assertEquals("Nr. 58.", root.determineLabel());
		assertEquals(0, root.getFiles().size());
		assertEquals(6, root.getChildren().size());
		var additionalSection = root.getChildren().get(4);
		assertEquals(METSContainerType.ADDITIONAL, additionalSection.getType());
		assertEquals("Feuilleton-Beilage. Nr. 20.", additionalSection.determineLabel());
		assertEquals(2, additionalSection.getChildren().size());
	}

	/**
	 * Issue without explicite sub-structures, but linked page containers
	 * @throws DigitalDerivansException 
	 */
	@Test
	void testIssueWithoutSubstructs() throws DigitalDerivansException {
		
		// arrange
		var issueMets = new METS(TestResource.SHARE_DIGIT_ZD_1516514412012_170057.get());

		// act
		issueMets.init();

		// assert
		METSContainer issueContainer = issueMets.getLogicalRoot();
		assertFalse(issueContainer.getChildren().isEmpty());
		assertEquals(4, issueContainer.getChildren().size());
	}


	/**
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
		assertEquals(0, root.getFiles().size());
		assertEquals(12, root.getChildren().size());
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
		assertEquals(0, root.getFiles().size());
		assertEquals(4, root.getChildren().size());
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
