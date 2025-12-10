package de.ulb.digital.derivans.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.model.DerivateMD;
import de.ulb.digital.derivans.model.DerivateStruct;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.pdf.DescriptiveMetadata;

/**
 * 
 * MWE for VL METS
 * 
 * @author u.hartwig
 *
 */
class TestMetadataVLSminimum {

	static DerivateMD der737429;

	static DescriptiveMetadata dmd737429;

	@BeforeAll
	static void setupClazz() throws DigitalDerivansException {
		TestMetadataVLSminimum.der737429 = new DerivateMD(TestResource.VLS_HD_Aa_737429.get());
		TestMetadataVLSminimum.der737429.checkRessources(false);
		TestMetadataVLSminimum.der737429.init(TestHelper.ULB_MAX_PATH);
		dmd737429 = der737429.getDescriptiveData();
	}


	@Test
	void testDescriptiveDataHDmonography() {
		// mods:recodInfo/mods:recordIdentifier[@source]/text()
		assertEquals("191092622", dmd737429.getIdentifier());
		// mods:titleInfo/mods:title
		assertTrue(dmd737429.getTitle().startsWith("Ode In Solemni Panegyri Avgvstissimo Ac Potentissimo"));
		// mods:identifier[@type="urn"]
		assertEquals("urn:nbn:de:gbv:3:3-21437", dmd737429.getUrn());
		// METS/MODS contains no license information
		assertTrue(dmd737429.getLicense().isEmpty());
		// mods:originInfo/mods:dateIssued[@keyDate="yes"]/text()
		assertEquals("1731", dmd737429.getYearPublished());
		// mods:role/mods:displayForm/text()
		// OR
		// mods:namePart[@type="family"]/text()
		// WITH
		// IF NOT mods:name/mods:role/mods:roleTerm[@type="code"]/text() = "aut"
		// IF mods:name/mods:role/mods:roleTerm[@type="code"]/text() = "pbl
		assertEquals("Officina Langenhemia", dmd737429.getPerson());
	}

	@Test
	void testDigitalPagesOrderOf737429() {

		// act
		List<DigitalPage> pages = TestMetadataVLSminimum.der737429.allPagesSorted();

		// assert
		assertEquals(4, pages.size());
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
	void testStructureOf737429() {
		var dst = der737429.getStructure();
		assertNotNull(dst);

		assertTrue(dst.getLabel().startsWith("Ode In Solemni Panegyri"));
		assertTrue(dst.getPages().isEmpty());
		assertFalse(dst.getChildren().isEmpty());

		// level 1
		List<DerivateStruct> children = dst.getChildren();
		assertEquals(2, children.size());
		assertEquals("Titelblatt", children.get(0).getLabel());
		assertEquals(1, children.get(0).getChildren().size());
		assertEquals("[Ode]", children.get(1).getLabel());
		assertEquals(3, children.get(1).getChildren().size());
	}

}
