package de.ulb.digital.derivans.data.mets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.TestResource;

/**
 * 
 * Medium size periodical volume from Share-IT with weird mixture of pages and
 * sub-structures, this time analyzed to the bone
 * 
 * @author u.hartwig
 */
class TestDFGMETSInhouse322332 {

	static METS metsInhouse322332;

	@BeforeAll
	static void setupBeforeClass() throws DigitalDerivansException {
		metsInhouse322332 = new METS(TestResource.VLS_INHOUSE_322332.get(), TestHelper.ULB_MAX);
		metsInhouse322332.init();
	}

	/**
	 * 
	 * Ensure top-level contains 11 sub-structures.
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testTopLevel() {
		// assert
		var root = metsInhouse322332.getLogicalRoot();
		assertEquals(464, metsInhouse322332.getPages().size());
		assertEquals(METSContainerType.VOLUME, root.getType());
		assertEquals("Bdch. 1", root.determineLabel());
		assertEquals(26, root.getChildren().size());
	}

	/**
	 * Inspect real first chapter "I. Elisa's Auftritt."
	 */
	@Test
	void testChildSectionOne() {
		// arrange
		var root = metsInhouse322332.getLogicalRoot();
		var child = root.getChildren().get(6);

		// assert
		assertEquals(METSContainerType.SECTION, child.getType());
		assertEquals("I. Elisa`s Auftritt.", child.determineLabel());
		assertEquals(22, child.getChildren().size());
		assertEquals(17, child.getOrder());
	}

	/**
	 * Inspect 2nd chapter "II. Das Gericht bei Bethel."
	 */
	@Test
	void testChildSectionTwo() {
		// arrange
		var root = metsInhouse322332.getLogicalRoot();
		var child = root.getChildren().get(7);

		// assert
		assertEquals(METSContainerType.SECTION, child.getType());
		assertEquals("II. Das Gericht bei Bethel.", child.determineLabel());
		assertEquals(25, child.getChildren().size());
		assertEquals(39, child.getOrder());
	}

	/**
	 * Inspect last chapter "XVIII. Gehafi."
	 */
	@Test
	void lastSection() {
		// arrange
		var root = metsInhouse322332.getLogicalRoot();
		var thisChild = root.getChildren().get(23);

		// assert
		assertEquals(METSContainerType.SECTION, thisChild.getType());
		assertEquals("XVIII. Gehafi.", thisChild.determineLabel());
		assertEquals(23, thisChild.getChildren().size());
		assertEquals(436, thisChild.getOrder());
	}

}
