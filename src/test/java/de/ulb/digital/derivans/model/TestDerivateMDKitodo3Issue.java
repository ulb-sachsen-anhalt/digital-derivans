package de.ulb.digital.derivans.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestResource;

/**
 * 
 * Behavior of {@link MetadataStore} with Kitodo 3 Metadata Exports
 * 
 * @author hartwig
 *
 */
class TestDerivateMDKitodo3Issue {

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
		var dZD2 = new DerivateMD(TestResource.K3_ZD2_1021634069.get());

		// 
		dZD2.setStartFileExtension(".tif");
		dZD2.checkRessources(false);
		dZD2.init(Path.of("MAX"));
		var digiPages = dZD2.allPagesSorted();

		// assert
		assertEquals(4, digiPages.size());
		for (var digiPage : digiPages) {
			assertTrue(digiPage.getFile().getPath().toString().endsWith(".tif"));
		}
	}

}
