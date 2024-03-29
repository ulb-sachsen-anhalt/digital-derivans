package de.ulb.digital.derivans.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.kohsuke.args4j.CmdLineException;

import de.ulb.digital.derivans.DigitalDerivansException;

/**
 * 
 * Specification for {@link DigitalStructureTree}
 * 
 * @author hartwig
 *
 */
public class TestDigitalStructureTree {

	@Test
	void testCommonInstancesEquals() throws CmdLineException, DigitalDerivansException {

		// arrange
		var s1 = new DigitalStructureTree(6, "[Seite 8]");
		var s2 = new DigitalStructureTree(6, "[Seite 8]");

		// act
		assertEquals(s1, s2);
	}

	@Test
	void testCommonInstancesDifferValue() throws CmdLineException {

		// arrange
		var s1 = new DigitalStructureTree(6, "[Seite 8]");
		var s2 = new DigitalStructureTree(7, "[Seite 8]");

		// act
		assertNotEquals(s1, s2);
	}

	@Test
	void testCommonInstancesDifferLabel() throws CmdLineException {

		// arrange
		var s1 = new DigitalStructureTree(6, "[Seite 8]");
		var s2 = new DigitalStructureTree(6, "[Tabelle]");

		// act
		assertNotEquals(s1, s2);
	}

	@Test
	void testCommonInstancesDifferBoth() throws CmdLineException {

		// arrange
		var s1 = new DigitalStructureTree(6, "[Seite 8]");
		var s2 = new DigitalStructureTree(7, "[Tabelle]");

		// act
		assertNotEquals(s1, s2);
	}
}
