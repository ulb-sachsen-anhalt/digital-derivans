package de.ulb.digital.derivans.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.TestResource;

/**
 * 
 * Special case for newspaper issue with deep logical structures
 * 
 * @author hartwig
 *
 */
public class TestDigitalStructureTreeDeepIssue {

	static DerivateStruct deepIssue;

	@BeforeAll
	static void setupClazz() throws Exception {
		Path testData = TestResource.METS_ZD_ISSUE_182327845018001101.get();
		DerivateMD devmd = new DerivateMD(testData);
		devmd.checkRessources(false);
		devmd.init(testData.getParent());
		deepIssue = devmd.getStructure();
	}

	@Test
	void testStructureLevelNewspaper() throws Exception {
		assertEquals("Hallisches patriotisches Wochenblatt. 1799-1855", deepIssue.getLabel());
		assertEquals(1, deepIssue.getChildren().size());
	}

	@Test
	void testStructureLevelYear() throws Exception {
		var years = deepIssue.getChildren();
		assertEquals(1, years.size());
		assertEquals("1800", years.get(0).getLabel());
	}

	@Test
	void testStructureLevelMonth() throws Exception {
		var year = deepIssue.getChildren();
		var months = year.get(0).getChildren();
		assertEquals(1, months.size());
		assertEquals("11", months.get(0).getLabel());
	}

	@Test
	void testStructureLevelDay() throws Exception {
		var year = deepIssue.getChildren().get(0);
		var month = year.getChildren().get(0);
		var days = month.getChildren();
		assertEquals(1, days.size());
		assertEquals("01", days.get(0).getLabel());
	}

	@Test
	void testStructureLevelIssue() throws Exception {
		var year = deepIssue.getChildren().get(0);
		var month = year.getChildren().get(0);
		var day = month.getChildren().get(0);
		var issue = day.getChildren();
		assertEquals(1, issue.size());
		assertEquals("1.11.1800 (No. 5)", issue.get(0).getLabel());
	}

	@Test
	void testSubstructuresIssue() throws Exception {
		var year = deepIssue.getChildren().get(0);
		var month = year.getChildren().get(0);
		var day = month.getChildren().get(0);
		var issue = day.getChildren().get(0);
		var issueParts = issue.getChildren();
		assertEquals(6, issueParts.size());
		assertEquals("Bekanntmachungen.", issueParts.get(5).getLabel());
	}

	@Test
	void testStructureLevelChronik() throws Exception {
		var year = deepIssue.getChildren().get(0);
		var month = year.getChildren().get(0);
		var day = month.getChildren().get(0);
		var issue = day.getChildren().get(0);
		var issueParts = issue.getChildren();
		var chronikPieces = issueParts.get(4).getChildren();
		assertEquals(2, chronikPieces.size());
		assertEquals("1. Armensachen.", chronikPieces.get(0).getLabel());
	}

}
