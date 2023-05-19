package de.ulb.digital.derivans.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.ulb.digital.derivans.DerivansParameter;
import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.config.DerivansConfiguration;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.DigitalStructureTree;

/**
 * 
 * Specification of {@link MetadataStore}
 * 
 * Check expected information from SLUB-record at
 * http://digital.slub-dresden.de/oai?verb=GetRecord&metadataPrefix=mets&mode=xml&identifier=oai:de:slub-dresden:db:id-321094271
 * 
 * @author u.hartwig
 *
 */
class TestMetadataStoreSLUB {

	/**
	 * 
	 * Using no configuration at all
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testDescriptiveDataSLUBMonographyDefaultConfig() throws DigitalDerivansException {
		// arrange
		var slub321094271 = new MetadataStore(TestResource.K2_PRES_SLUB_321094271.get());
		var dd321094271 = slub321094271.getDescriptiveData();

		// mods:recodInfo/mods:recordIdentifier[@source]/text()
		assertEquals("oai:de:slub-dresden:db:id-321094271", dd321094271.getIdentifier());
		// mods:titleInfo/mods:title
		assertEquals("Der 103te Psalm", dd321094271.getTitle());
		// mods:identifier[@type="urn"]
		assertEquals("urn:nbn:de:bsz:14-db-id3210942710", dd321094271.getUrn());
		// METS/MODS contains no license information
		assertEquals("Public Domain Mark 1.0", dd321094271.getLicense().get());
		// mods:originInfo/mods:dateIssued[@keyDate="yes"]/text()
		assertEquals("1822", dd321094271.getYearPublished());
		// mods:role/mods:displayForm/text()
		// OR
		// mods:namePart[@type="family"]/text()
		// WITH
		// IF NOT mods:name/mods:role/mods:roleTerm[@type="code"]/text() = "aut"
		// IF mods:name/mods:role/mods:roleTerm[@type="code"]/text() = "pbl
		assertEquals("Fesca, Friedrich Ernst", dd321094271.getPerson());
	}

	/**
	 * 
	 * ATTENZIONE!
	 * Requires custom configuration for desired fileGroup!
	 * Default 'MAX' is not present!
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testDigitalPagesSLUBMonography() throws DigitalDerivansException {

		// arrange
		DerivansParameter dp = new DerivansParameter();
		dp.setPathInput(TestResource.K2_PRES_SLUB_321094271.get());
		var dcMock = Mockito.mock(DerivansConfiguration.class);
		// we only need the last Path segment, so stay tuned
		when(dcMock.getInitialImageDir()).thenReturn(Path.of("foo/bar/ORIGINAL"));
		var slub321094271 = new MetadataStore(TestResource.K2_PRES_SLUB_321094271.get(), dcMock);

		// act
		List<DigitalPage> pages = slub321094271.getDigitalPagesInOrder();

		// assert
		for (DigitalPage page : pages) {
			// no granular URN present
			assertFalse(page.getIdentifier().isPresent());
		}
		assertEquals(188, pages.size());
		assertEquals("ORIGINAL/00000001.tif.original.jpg", pages.get(0).getImageFile());
		assertEquals("ORIGINAL/00000188.tif.original.jpg", pages.get(187).getImageFile());
	}

	/**
	 * 
	 * Rather flat logical structure - no monograph childs present
	 * 
	 * ATTENZIONE!
	 * Requires custom configuration for desired fileGroup!
	 * Default 'MAX' is not present!
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testStructureSLUBMonography() throws DigitalDerivansException {

		// arrange
		DerivansParameter dp = new DerivansParameter();
		dp.setPathInput(TestResource.K2_PRES_SLUB_321094271.get());
		var dcMock = Mockito.mock(DerivansConfiguration.class);
		// we only need the last Path segment, so stay tuned
		when(dcMock.getInitialImageDir()).thenReturn(Path.of("foo/bar/ORIGINAL"));
		var slub321094271 = new MetadataStore(TestResource.K2_PRES_SLUB_321094271.get(), dcMock);

		DigitalStructureTree dst = slub321094271.getStructure();
		assertNotNull(dst);
		assertEquals("Der 103te Psalm", dst.getLabel());
		assertEquals(1, dst.getPage());
		assertFalse(dst.hasSubstructures());
	}

}
