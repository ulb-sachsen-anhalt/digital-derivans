package de.ulb.digital.derivans.data.mets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestResource;

/**
 * @author u.hartwig
 */
class TestDFGMETS {
	
	@Test
	void parseLegacyMETS() throws DigitalDerivansException {
		var path = TestResource.VD18P_14163614.get();
		var m = new METS(path);
		assertNotNull(m);
		assertTrue(m.getPath().toString().endsWith(".xml"));
	}

	/**
	 * 
	 * Test data shall be structured into a root container
	 * and total 32 child containers
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void parseContainerStructureVD18PVLS() throws DigitalDerivansException {
		var path = TestResource.VD18P_14163614.get();
		var m = new METS(path);
		m.setStructure();
		var containerStructure = m.getContainer();
		assertEquals(METSContainerType.PERIODICAL, containerStructure.getType());
		assertEquals("Pirnaischer Chronicken und Historien Calender", containerStructure.determineLabel());
		assertEquals(32, m.getLogContainers().size());
	}

	/**
	 * 
	 * This METS shall contain 38 Files in group "MAX"
	 *   38 * (MAX, THUMB, MIN, DEFAULT)  = 152 Files
	 *    8 * DOWNLOAD                   += 160
	 *    1 * TEASER                     += 161
	 * 
	 *  => 6 fileGroups 
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void parseFileStructureVD18PVLS() throws DigitalDerivansException {
		var path = TestResource.VD18P_14163614.get();
		var m = new METS(path);
		m.setStructure();
		m.setFiles(null);
		Map<String, List<METSFile>> allFiles = m.getFiles();
		assertEquals(6, allFiles.keySet().size());
		assertEquals(38, m.getFiles("MAX").size());
		List<METSFile> flatFiles = allFiles.values().stream().collect(ArrayList::new, List::addAll, List::addAll);
		assertEquals(161, flatFiles.size());
	}
}
