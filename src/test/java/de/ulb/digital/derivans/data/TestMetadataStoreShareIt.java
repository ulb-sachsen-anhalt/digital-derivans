package de.ulb.digital.derivans.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestResource;

/**
 * @author Uwe Hartwig
 */
public class TestMetadataStoreShareIt {
   
    /**
	 * 
	 * Ensure: Share_it METS can be processed
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testStructureContainsLabelForEachPage() throws DigitalDerivansException {
		// arrange
		IMetadataStore mdsZD2 = new MetadataStore(TestResource.SHARE_IT_VD18_MIG.get());

        // act
        var strucTree = mdsZD2.getStructure();

        // assert
        assertEquals("Dissertatio Inavgvralis Ivridica De Avxiliatoribvs Fvrvm Oder: Von Diebs-Helffern", strucTree.getLabel());
        assertEquals(1, strucTree.getPage());
        
        // level 1
        var level1structs = strucTree.getSubstructures();
		assertEquals(5, level1structs.size());

    }
}
