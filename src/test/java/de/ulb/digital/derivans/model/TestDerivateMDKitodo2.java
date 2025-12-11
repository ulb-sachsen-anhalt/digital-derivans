package de.ulb.digital.derivans.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.model.pdf.DescriptiveMetadata;

/**
 * 
 * Specification of {@link MetadataStore} with Kitodo.Production2 Metadata
 * 
 * @author hartwig
 *
 */
class TestDerivateMDKitodo2 {

	@Test
	void testMetadata143074601() throws DigitalDerivansException {
		DerivateMD d143074601 = new DerivateMD(TestResource.K2_Aa_143074601.get(), TestHelper.ULB_MAX_PATH.toString());
		assertTrue(d143074601.isMetadataPresent());
		DescriptiveMetadata dd143074601 = d143074601.getDescriptiveData();
		assertEquals("urn:nbn:de:gbv:3:1-1192015415-143074601-16", dd143074601.getUrn());
		assertEquals("Heister, Lorenz", dd143074601.getPerson());
		assertEquals("143074601", dd143074601.getIdentifier());
	}

	@Test
	void testDigitalPagesWithoutGranularUrn143074601() throws DigitalDerivansException {
		DerivateMD d143074601 = new DerivateMD(TestResource.K2_Aa_143074601.get());
		List<DigitalPage> pages = d143074601.allPagesSorted();
		for (DigitalPage page : pages) {
			assertTrue(page.optContentIds().isEmpty());
		}
	}

	/**
	 * 
	 * Check expected information is extracted from METS/MODS-export of
	 * kitodo.production2
	 * 
	 * imported in open data as:
	 * https://opendata.uni-halle.de/handle/1981185920/36228
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testDescriptiveDataFromKitodo2() throws DigitalDerivansException {

		DerivateMD d147573602 = new DerivateMD(TestResource.K2_Aa_147573602.get(), TestHelper.ULB_MAX_PATH.toString());
		DescriptiveMetadata dd147573602 = d147573602.getDescriptiveData();
		// mods:recodInfo/mods:recordIdentifier[@source]/text()
		assertEquals("147573602", dd147573602.getIdentifier());
		// mods:titleInfo/mods:title
		assertTrue(dd147573602.getTitle().startsWith("Tractätgen von denen Jüdischen Fabeln und Aberglauben"));
		// mods:accessCondition[type="use and reproduction"]/text()
		assertEquals("CC-BY-SA 3.0 DE", dd147573602.getLicense().get());
		// mods:identifier[@type="urn"]
		assertEquals("urn:nbn:de:gbv:3:1-1192015415-147573602-14", dd147573602.getUrn());
		// mods:role/mods:displayForm/text() IF
		// mods:name/mods:role/mods:roleTerm[@type="code"]/text() = "aut"
		assertEquals("Christian, Magnus", dd147573602.getPerson());
		// mods:originInfo/mods:dateIssued[@keyDate="yes"]/text()
		assertEquals("1718", dd147573602.getYearPublished());
	}

	/**
	 * 
	 * BUGFIX
	 * 
	 * see: https://github.com/ulb-sachsen-anhalt/digital-derivans/issues/35
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testMetadataWithSingleSectionOnlyFromKitodo2() throws DigitalDerivansException {

		// arrange
		var mds = new DerivateMD(TestResource.K2_Hau_1748529021.get(), TestHelper.ULB_MAX_PATH.toString());

		// act
		var dd = mds.getDescriptiveData();

		// assert
		assertEquals("1044", dd.getYearPublished());
	}

	/**
	 * 
	 * BUGFIX
	 * 
	 */
	@Test
	void testEnsureIdentifierPPNMatches() throws DigitalDerivansException {
		// arrange
		var mds1186819316 = new DerivateMD(TestResource.K2_Aa_1186819316.get(), TestHelper.ULB_MAX_PATH.toString());

		// act
		var dd1186819316 = mds1186819316.getDescriptiveData();
		assertEquals("1186819316", dd1186819316.getIdentifier());
	}

	/**
	 * 
	 * BUGFIX https://github.com/ulb-sachsen-anhalt/digital-derivans/issues/55
	 * 
	 */
	@Test
	void testFindRecordIdentifierPeriodicalVolume() throws DigitalDerivansException {
		// arrange
		var mds = new DerivateMD(TestResource.K2_AB_16740608619039.get(), TestHelper.ULB_MAX_PATH.toString());

		// act
		var dd = mds.getDescriptiveData();
		assertNotNull(dd);
		assertEquals("16740608619039", dd.getIdentifier());
	}
}
