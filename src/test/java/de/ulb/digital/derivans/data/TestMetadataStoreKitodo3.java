package de.ulb.digital.derivans.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.data.mets.MetadataStore;

/**
 * 
 * Behavior of {@link MetadataStore} with Kitodo 3 Metadata Exports
 * 
 * @author hartwig
 *
 */
class TestMetadataStoreKitodo3 {

	/**
	 * 
	 * Ensure that existing file extension within mets:fileGrp/mets: is respected
	 * as-is and ".jpg" is *not* appended
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testImageExtension() throws DigitalDerivansException {
		// arrange
		IMetadataStore mdsZD2 = new MetadataStore(TestResource.K3_ZD2_1021634069.get());

		// act
		var digiPages = mdsZD2.getDigitalPagesInOrder();

		// assert
		assertEquals(4, digiPages.size());
		for (var digiPage : digiPages) {
			assertTrue(digiPage.getImageFile().endsWith(".tif"));
		}
	}

}
