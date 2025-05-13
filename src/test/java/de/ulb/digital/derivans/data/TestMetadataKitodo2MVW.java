package de.ulb.digital.derivans.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.IDerivans;
import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.model.DerivateMD;
import de.ulb.digital.derivans.model.pdf.DescriptiveMetadata;

/**
 * 
 * Specification of {@link MetadataStore} with Kitodo.Production2 Metadata
 * 
 * @author hartwig
 *
 */
class TestMetadataKitodo2MVW {

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
		var derivate = new DerivateMD(TestResource.K2_Af_030745780.get());
		derivate.checkRessources(false);

		// act
		var actualExc = assertThrows(DigitalDerivansException.class, 
			() -> derivate.init(Path.of("MAX")));
		
		// assert - as of 2025
		assertEquals("No files link div LOG_0216/207. [An Charlotte Pistorius.] in @USE=MAX!", actualExc.getMessage());
	}
}
