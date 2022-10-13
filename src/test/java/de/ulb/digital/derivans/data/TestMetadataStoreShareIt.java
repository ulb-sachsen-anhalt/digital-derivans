package de.ulb.digital.derivans.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
		IMetadataStore mds = new MetadataStore(TestResource.SHARE_IT_VD18_43053.get());

        // act
		var actualExc = assertThrows(DigitalDerivansException.class, () -> mds.getStructure());

		// assert
		assertEquals("No physical struct linked from 'log1646693@section(Abschnitt)'!", actualExc.getMessage());
    }
}
