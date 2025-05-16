package de.ulb.digital.derivans.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.model.DerivateMD;

/**
 * @author Uwe Hartwig
 */
class TestMetadataShareIt {
   
    /**
	 * 
	 * Ensure: Share_it METS can be processed
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testStructureContainsLabelForEachPage() throws DigitalDerivansException {
		// arrange
		var derivate = new DerivateMD(TestResource.SHARE_IT_VD18_MIG.get());
		derivate.checkRessources(false);
		derivate.init(TestHelper.ULB_MAX_PATH);

        // act
        var struct = derivate.getStructure();

        // assert
        assertEquals("Dissertatio Inavgvralis Ivridica De Avxiliatoribvs Fvrvm Oder: Von Diebs-Helffern", struct.getLabel());
        assertEquals(0, struct.getPages().size());
        
        // level 1
        var level1structs = struct.getChildren();
		assertEquals(5, level1structs.size());
    }


	/**
	 * 
	 * Decide how to handle invalid logical links
	 * => Throw Exception
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testStructureMissingLinkFromLogicalSection() throws DigitalDerivansException {
		// arrange
		var mds = new DerivateMD(TestResource.SHARE_IT_VD18_43053.get());
		mds.checkRessources(false);

        // act
		var actualExc = assertThrows(DigitalDerivansException.class, () -> mds.init(TestHelper.ULB_MAX_PATH));

		// assert
		assertEquals("No files link div log1646693/Abschnitt in @USE=MAX!", actualExc.getMessage());
    }

}
