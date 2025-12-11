package de.ulb.digital.derivans.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.TestResource;

/**
 *
 * Specification of {@link DerivateMD} for legacy inhouse newspaper
 * which link top most physSequence from logical structMap
 *
 * @author u.hartwig
 */
class TestDerivateMDVLS {

	/**
	 *
	 * Ensure structs are properly built even in legacy inhouse newspaper
	 * which has a complete flat
	 *
	 * @throws DigitalDerivansException
	 */
	@Test
	void testLegacyInhouseNewspaper() throws DigitalDerivansException {
		DerivateMD derivateIssue = new DerivateMD(TestResource.VLS_ZD_INHOUSE_2337658.get(),
				TestHelper.ULB_MAX_PATH.toString());
		derivateIssue.checkRessources(false);
		derivateIssue.init(TestHelper.ULB_MAX_PATH);
		var pages = derivateIssue.allPagesSorted();
		assertEquals(16, pages.size());
	}

	@Test
	void testInvalidLegacyPilotIssue() throws DigitalDerivansException {
		DerivateMD derivateIssue = new DerivateMD(TestResource.VLS_ZD_ISSUE_182327845018001101.get(),
				TestHelper.ULB_MAX_PATH.toString());
		derivateIssue.checkRessources(false);
		derivateIssue.init(TestHelper.ULB_MAX_PATH);
		var pages = derivateIssue.allPagesSorted();
		assertEquals(16, pages.size());
	}

	@Test
	void testLegacyLittleMono() throws DigitalDerivansException {
		DerivateMD derivateIssue = new DerivateMD(TestResource.VLS_HD_Aa_737429.get(),
				TestHelper.ULB_MAX_PATH.toString());
		derivateIssue.checkRessources(false);
		derivateIssue.init(TestHelper.ULB_MAX_PATH);
		var pages = derivateIssue.allPagesSorted();
		assertEquals(4, pages.size());
	}
}
