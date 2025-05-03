package de.ulb.digital.derivans.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.TestResource;

/**
 * 
 * Special case for newspaper issue with deep logical structures
 * 
 * @author hartwig
 *
 */
class TestDerivateStructDeepIssue {

	static DerivateStruct newspaper;

	static DerivateStruct issue;

	@BeforeAll
	static void setupClazz() throws Exception {
		Path testData = TestResource.VLS_ZD_ISSUE_182327845018001101.get();
		DerivateMD devmd = new DerivateMD(testData);
		devmd.checkRessources(false);
		devmd.init(TestHelper.ULB_MAX_PATH);
		TestDerivateStructDeepIssue.newspaper = devmd.getStructure();
		TestDerivateStructDeepIssue.issue = newspaper.getChildren().get(0);
	}

	@Test
	void testStructureLevelNewspaper() throws Exception {
		assertEquals("Hallisches patriotisches Wochenblatt. 1799-1855",
				TestDerivateStructDeepIssue.newspaper.getLabel());
		assertEquals(1, newspaper.getOrder());
		assertEquals(1, TestDerivateStructDeepIssue.newspaper.getChildren().size());
	}

	@Test
	void testStructureLevelIssue() throws Exception {
		assertEquals("1.11.1800 (No. 5)", issue.getLabel());
		assertEquals(1, issue.getOrder());
	}
	
	@Test
	void testIssueNumberSubstructs() {
		assertEquals(6, issue.getChildren().size());
	}

	@Test
	void testSubstructuresSection01() throws Exception {
		var issueParts = issue.getChildren();
		assertEquals("Bekanntmachungen.", issueParts.get(5).getLabel());
	}

	@Test
	void testSubstructuresSection06() throws Exception {
		var issueParts = issue.getChildren();
		assertEquals(6, issueParts.size());
		assertEquals("Bekanntmachungen.", issueParts.get(5).getLabel());
	}

	@Test
	void testStructureLevelChronik() throws Exception {
		var issueParts = issue.getChildren();
		var chronikPieces = issueParts.get(4).getChildren();
		assertEquals(2, chronikPieces.size());
		assertEquals("1. Armensachen.", chronikPieces.get(0).getLabel());
	}

}
