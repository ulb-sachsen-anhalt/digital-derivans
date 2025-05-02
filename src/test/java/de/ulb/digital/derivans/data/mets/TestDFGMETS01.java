package de.ulb.digital.derivans.data.mets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.derivate.IDerivateer;
import de.ulb.digital.derivans.model.DerivateMD;
import de.ulb.digital.derivans.model.DerivateStruct;
import de.ulb.digital.derivans.model.DigitalPage;

/**
 * 
 * Test METS/MODS parsing for digital object urn:nbn:de:gbv:3:3-21437
 * created 2010 for ULB inhouse / Historische Drucke
 * VD18 PPN 191092622 / VD18 DB 10787747
 * 
 * @author u.hartwig
 */
class TestDFGMETS01 {

	static METS mets737429;

	@BeforeAll
	static void setupClazz() throws DigitalDerivansException {
		mets737429 = new METS(TestResource.HD_Aa_737429.get());
		mets737429.determine();
	}
	
	/**
	 * 
	 * Check expected information is extracted from OAI-record in VLS 12 format
	 * 
	 * http://digital.bibliothek.uni-halle.de/hd/oai/?verb=GetRecord&metadataPrefix=mets&mode=xml&identifier=737429
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testDescriptiveDataHDmonography() {
		// mods:recordInfo/mods:recordIdentifier[@source]/text()
		assertEquals("191092622", mets737429.getPrimeMODS().getIdentifier());
		// mods:titleInfo/mods:title
		assertTrue(mets737429.getPrimeMODS().getTitle().startsWith("Ode In Solemni Panegyri Avgvstissimo Ac Potentissimo"));
		// mods:identifier[@type="urn"]
		assertEquals("urn:nbn:de:gbv:3:3-21437", mets737429.getPrimeMODS().getIdentifierURN());
		// METS/MODS contains no license information, therefore unknown
		assertEquals(IDerivateer.UNKNOWN, mets737429.getPrimeMODS().getAccessCondition());
		// mods:originInfo/mods:dateIssued[@keyDate="yes"]/text()
		assertEquals("1731", mets737429.getPrimeMODS().getYearPublication());
		// mods:role/mods:displayForm/text()
		// OR
		// mods:namePart[@type="family"]/text()
		// WITH
		// IF NOT mods:name/mods:role/mods:roleTerm[@type="code"]/text() = "aut"
		// IF mods:name/mods:role/mods:roleTerm[@type="code"]/text() = "pbl
		assertEquals("Officina Langenhemia", mets737429.getPrimeMODS().getPerson());
	}

	@Test
	void testDigitalPagesOrderOf737429() throws DigitalDerivansException {

		// act
		DerivateMD derivateMD = new DerivateMD(mets737429.getPath());
		assertNotNull(derivateMD);
		derivateMD.checkRessources(false);
		derivateMD.init(Path.of("MAX"));
		List<DigitalPage> pages = derivateMD.getAllPages();

		// // assert
		for (DigitalPage page : pages) {
			assertTrue(page.optContentIds().isPresent());
		}

		String urn1 = "urn:nbn:de:gbv:3:3-21437-p0001-0";
		String urn2 = "urn:nbn:de:gbv:3:3-21437-p0004-6";
		assertEquals(urn1, pages.get(0).optContentIds().get());
		assertEquals(urn2, pages.get(3).optContentIds().get());
		assertTrue(pages.get(0).getFile().getPath().toString().endsWith("MAX/737434.jpg"));
		assertTrue(pages.get(1).getFile().getPath().toString().endsWith("MAX/737436.jpg"));
		assertTrue(pages.get(3).getFile().getPath().toString().endsWith("MAX/737438.jpg"));
	}

	@Test
	void testStructureOf737429() throws DigitalDerivansException {
		DerivateMD derivateMD = new DerivateMD(mets737429.getPath());
		assertNotNull(derivateMD);
		derivateMD.checkRessources(false);
		derivateMD.init(Path.of("MAX"));

		DerivateStruct struct = derivateMD.getStructure();
		assertTrue(struct.getLabel().startsWith("Ode In Solemni Panegyri"));
		assertEquals(1, struct.getOrder());

		// level 1
		List<DerivateStruct> children = struct.getChildren();
		assertEquals(2, struct.getChildren().size());
		assertEquals("Titelblatt", children.get(0).getLabel());
		assertEquals(1, children.get(0).getPages().size());
		assertEquals("[Ode]", children.get(1).getLabel());
		assertEquals(3, children.get(1).getPages().size());
	}
}
