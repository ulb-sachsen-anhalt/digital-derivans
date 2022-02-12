package de.ulb.digital.derivans.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.model.DescriptiveData;
import de.ulb.digital.derivans.model.DigitalPage;

/**
 * 
 * Specification of {@link MetadataStore} with Kitodo.Production2 Metadata
 * 
 * @author hartwig
 *
 */
class TestMetadataStoreKitodo2 {

	static IMetadataStore mds143074601;

	static DescriptiveData dd143074601;

	static IMetadataStore mds147573602;
	
	static DescriptiveData dd147573602;

	@BeforeAll
	static void setup() throws Exception {
		mds143074601 = new MetadataStore(TestResource.K2_Aa_143074601.get());
		dd143074601 = mds143074601.getDescriptiveData();
		mds147573602 = new MetadataStore(TestResource.K2_Aa_147573602.get());
		dd147573602 = mds147573602.getDescriptiveData();
	}

	@Test
	void testUrn143074601() throws DigitalDerivansException {
		assertEquals("urn:nbn:de:gbv:3:1-1192015415-143074601-16", dd143074601.getUrn());
	}
	
	@Test
	void testPerson143074601() throws DigitalDerivansException {
		assertEquals("Heister, Lorenz", dd143074601.getPerson());
	}

	@Test
	void testIdentifier143074601() throws DigitalDerivansException {
		assertEquals("143074601", dd143074601.getIdentifier());
	}

	@Test
	void testDigitalPagesWithoutGranularUrn143074601() throws DigitalDerivansException {
		List<DigitalPage> pages = mds143074601.getDigitalPagesInOrder();
		for (DigitalPage page : pages) {
			assertTrue(page.getIdentifier().isEmpty());
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
	
}
