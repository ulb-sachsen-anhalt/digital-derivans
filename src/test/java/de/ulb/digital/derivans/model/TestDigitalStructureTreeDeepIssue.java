package de.ulb.digital.derivans.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.data.mets.MetadataStore;

/**
 * 
 * Special case for newspaper issue with deep logical structures
 * 
 * @author hartwig
 *
 */
public class TestDigitalStructureTreeDeepIssue {

	static DigitalStructureTree deepIssue;

	@BeforeAll
	static void setupClazz() throws Exception {
		deepIssue = new MetadataStore(TestResource.METS_ZD_ISSUE_182327845018001101.get()).getStructure();
	}

	@Test
	void testStructureLevelNewspaper() throws Exception {
		assertEquals("Hallisches patriotisches Wochenblatt. 1799-1855", deepIssue.getLabel());
		assertEquals(1, deepIssue.getSubstructures().size());
	}

	@Test
	void testStructureLevelYear() throws Exception {
		var years = deepIssue.getSubstructures();
		assertEquals(1, years.size());
		assertEquals("1800", years.get(0).getLabel());
	}

	@Test
	void testStructureLevelMonth() throws Exception {
		var year = deepIssue.getSubstructures();
		var months = year.get(0).getSubstructures();
		assertEquals(1, months.size());
		assertEquals("11", months.get(0).getLabel());
	}

	@Test
	void testStructureLevelDay() throws Exception {
		var year = deepIssue.getSubstructures().get(0);
		var month = year.getSubstructures().get(0);
		var days = month.getSubstructures();
		assertEquals(1, days.size());
		assertEquals("01", days.get(0).getLabel());
	}

	@Test
	void testStructureLevelIssue() throws Exception {
		var year = deepIssue.getSubstructures().get(0);
		var month = year.getSubstructures().get(0);
		var day = month.getSubstructures().get(0);
		var issue = day.getSubstructures();
		assertEquals(1, issue.size());
		assertEquals("1.11.1800 (No. 5)", issue.get(0).getLabel());
	}

	@Test
	void testSubstructuresIssue() throws Exception {
		var year = deepIssue.getSubstructures().get(0);
		var month = year.getSubstructures().get(0);
		var day = month.getSubstructures().get(0);
		var issue = day.getSubstructures().get(0);
		var issueParts = issue.getSubstructures();
		assertEquals(6, issueParts.size());
		assertEquals("Bekanntmachungen.", issueParts.get(5).getLabel());
	}

	@Test
	void testStructureLevelChronik() throws Exception {
		var year = deepIssue.getSubstructures().get(0);
		var month = year.getSubstructures().get(0);
		var day = month.getSubstructures().get(0);
		var issue = day.getSubstructures().get(0);
		var issueParts = issue.getSubstructures();
		var chronikPieces = issueParts.get(4).getSubstructures();
		assertEquals(2, chronikPieces.size());
		assertEquals("1. Armensachen.", chronikPieces.get(0).getLabel());
	}

}
