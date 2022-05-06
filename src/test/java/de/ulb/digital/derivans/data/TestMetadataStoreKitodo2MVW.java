package de.ulb.digital.derivans.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.model.DescriptiveData;

/**
 * 
 * Specification of {@link MetadataStore} with Kitodo.Production2 Metadata
 * 
 * @author hartwig
 *
 */
class TestMetadataStoreKitodo2MVW {

	/**
	 * 
	 * Kitodo2 volumes difer from visual library server volumes/tomes
	 * Please note:
	 * If not Label exists, map the type according to StructureMapper#map 
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testKitodo2MultivolumeMetadata() throws DigitalDerivansException {
		// arrange
		IMetadataStore mds = new MetadataStore(TestResource.K2_Af_140257772.get());
		
		// act
		DescriptiveData dd140257772 = mds.getDescriptiveData();
		
		// assert
		assertEquals("140257772", dd140257772.getIdentifier());
		assertEquals("Waldau, Georg Ernst", dd140257772.getPerson());
		assertEquals("Band", dd140257772.getTitle());
		
		// inspect structure
		var dst = mds.getStructure();
		assertEquals("Materialien zur Geschichte des Bauernkriegs in Franken, Schwaben, Thüringen [et]c. im Jahre 1525.", dst.getLabel());
		assertEquals("Band", dst.getSubstructures().get(0).getLabel());
		assertEquals("Vorderdeckel", dst.getSubstructures().get(0).getSubstructures().get(0).getLabel());
	}
	
	/**
	 * 
	 * This Record is assumed to fail at least for DDB-Validation
	 * Because one of it's logical sections, with ID "LOG_0216",
	 * is not linked to any physical structure.
	 *
	 * Obviously forgotten, but must be corrected anyway.
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testKitodo2InvalidLegacyFStage030745780() throws DigitalDerivansException {
		// arrange
		IMetadataStore mds = new MetadataStore(TestResource.K2_Af_030745780.get());
		
		// act
		DescriptiveData dd = mds.getDescriptiveData();
		
		// assert
		assertEquals("030745780", dd.getIdentifier());
		assertEquals("Schleiermacher, Friedrich", dd.getPerson());
		assertEquals("Sein Werden", dd.getTitle());
		
		// inspect structure, which a bit messy at the end
		var actualExc = assertThrows(DigitalDerivansException.class, 
			() -> mds.getStructure());

		// assert
		assertEquals("No physical struct linked from 'LOG_0216@section(207. [An Charlotte Pistorius.])'!", actualExc.getMessage());
	}
}
