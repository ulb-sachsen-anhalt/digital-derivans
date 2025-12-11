package de.ulb.digital.derivans.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.IDerivans;
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
class TestDerivateMDKitodo2MVW {

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
		var devMD = new DerivateMD(TestResource.K2_Af_140257772.get(), TestHelper.ULB_MAX_PATH.toString());
		
		// act
		DescriptiveMetadata dd140257772 = devMD.getDescriptiveData();
		
		// assert
		assertEquals("140257772", dd140257772.getIdentifier());
		assertEquals("Waldau, Georg Ernst", dd140257772.getPerson());
		assertEquals("Band", dd140257772.getTitle());
		
		// inspect structure
		devMD.checkRessources(false);
		devMD.init(Path.of(IDerivans.IMAGE_DIR_MAX));
		var dst = devMD.getStructure();
		assertEquals("Materialien zur Geschichte des Bauernkriegs in Franken, Schwaben, Thüringen [et]c. im Jahre 1525.", dst.getLabel());
		// of old before 2025: "Vorderdeckel"
		assertEquals("Band", dst.getChildren().get(0).getLabel());
	}

}
