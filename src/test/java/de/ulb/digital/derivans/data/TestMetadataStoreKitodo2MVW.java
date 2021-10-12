package de.ulb.digital.derivans.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
	 * Kitodo2 volumes differ from visual library server volumes/tomes 
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testKitodo2MultivolumeMetadata() throws DigitalDerivansException {
		// arrange
		IMetadataStore mds140257772 = new MetadataStore(TestResource.K2_Af_140257772.get());
		
		// act
		DescriptiveData dd140257772 = mds140257772.getDescriptiveData();
		
		// assert
		assertEquals("140257772", dd140257772.getIdentifier());
		assertEquals("Waldau, Georg Ernst", dd140257772.getPerson());
		assertEquals("n.a.", dd140257772.getTitle());
	}
}
