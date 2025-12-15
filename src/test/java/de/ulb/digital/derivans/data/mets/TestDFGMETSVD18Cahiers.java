package de.ulb.digital.derivans.data.mets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
class TestDFGMETSVD18Cahiers {

	static METS metsCahiers;

	@BeforeAll
	static void setupBeforeClass() throws DigitalDerivansException {
		metsCahiers = new METS(TestResource.SHARE_IT_VD18_1981185920_35126.get(), TestHelper.ULB_MAX);
		metsCahiers.init();
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
		var root = metsCahiers.getLogicalRoot();
		assertEquals(415, metsCahiers.getPages().size());
		assertEquals(METSContainerType.VOLUME, root.getType());
		assertTrue(root.determineLabel().startsWith("Nouveaux cahiers de lecture"));
		assertEquals(11, root.getChildren().size());
	}

	/**
	 * Inspect real first chapter "Janvier"
	 */
	@Test
	void testChildJanvier() {
		// arrange
		var root = metsCahiers.getLogicalRoot();
		var childJanvier = root.getChildren().get(3);

		// assert
		assertEquals(METSContainerType.ISSUE, childJanvier.getType());
		assertEquals("Janvier.", childJanvier.determineLabel());
		assertEquals(96, childJanvier.getChildren().size());
		assertEquals(11, childJanvier.getOrder());
	}

	/**
	 * Inspect ORDER of sub-structure "Janvier"
	 */
	@Test
	void testChildJanvierSuborder() {
		// arrange
		var root = metsCahiers.getLogicalRoot();
		var childJanvier = root.getChildren().get(3);

		// assert
		var firstSubChild = childJanvier.getChildren().get(0);
		assertEquals(METSContainerType.PAGE, firstSubChild.getType());
		assertEquals(" - ", firstSubChild.determineLabel());
		var lastSubChild = childJanvier.getChildren().get(95);
		assertEquals(METSContainerType.CONTENTS, lastSubChild.getType());
		assertEquals("Tables des matieres.", lastSubChild.determineLabel());
		assertEquals(106, lastSubChild.getOrder());
	}

	@Test
	void testChildJanvierIllustrationes() {
		// arrange
		var root = metsCahiers.getLogicalRoot();
		var thisChild = root.getChildren().get(4);

		// assert
		assertEquals(METSContainerType.ILLUSTRATION, thisChild.getType());
		assertEquals("Illustration", thisChild.determineLabel());
		assertEquals(2, thisChild.getChildren().size());
		assertEquals(109, thisChild.getOrder());
	}

	@Test
	void testChildAvril() {
		// arrange
		var root = metsCahiers.getLogicalRoot();
		var thisChild = root.getChildren().get(9);

		// assert
		assertEquals(METSContainerType.ISSUE, thisChild.getType());
		assertEquals("Avril.", thisChild.determineLabel());
		assertEquals(96, thisChild.getChildren().size());
		assertEquals(315, thisChild.getOrder());
	}

}
