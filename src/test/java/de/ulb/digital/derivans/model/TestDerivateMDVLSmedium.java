package de.ulb.digital.derivans.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.IDerivans;
import de.ulb.digital.derivans.model.pdf.DescriptiveMetadata;

/**
 * 
 * Specification of {@link MetadataStore}
 * 
 * @author u.hartwig
 *
 */
class TestDerivateMDVLSmedium {

	@Test
	void testStructureOf5175671() throws DigitalDerivansException {
		DerivateMD mds5175671 = new DerivateMD(TestResource.VLS_HD_Aa_5175671.get());
		mds5175671.checkRessources(false);
		mds5175671.init(Path.of(IDerivans.IMAGE_DIR_MAX));
		DescriptiveMetadata dd5175671 = mds5175671.getDescriptiveData();
		String title = dd5175671.getTitle();
		assertEquals("n.a.", title);
	}

	/**
	 * 
	 * Example of invalid legacy METS Data -
	 * It contains links to non-existing physical structures
	 * and therefore cannot generate proper outline.
	 * 
	 * DATA LIKE THIS MUST BE CORRECTED FOREHAND!
	 * 
	 * @throws Exception
	 */
	@Test
	void testInvalidStructure226134857() throws Exception {

		// arrange
		var derivate = new DerivateMD(TestResource.VLS_HD_Aa_226134857.get());
		derivate.checkRessources(false);

		// act
		var actualExc = assertThrows(DigitalDerivansException.class,
				() -> derivate.init(TestHelper.ULB_MAX_PATH));

		// was: LogId 'log2404939' : Invalid physical struct 'phys2404942'!
		var msg = actualExc.getMessage();
		assertTrue(msg.contains("Linked div phys2404942 from log2404939 missing!"));
		assertTrue(msg.contains("Linked div phys2404932 from log637740 missing!"));
		assertTrue(msg.contains("Linked div phys2404936 from log637740 missing!"));
		assertTrue(msg.contains("Linked div phys2404932 from log2404930 missing!"));
		assertTrue(msg.contains("Linked div phys2404936 from log2404934 missing!"));
	}

	@Test
	void testStructurePageLabels() throws Exception {

		// arrange
		var devMD = new DerivateMD(TestResource.VLS_VD18_Aa_16372279.get());
		devMD.checkRessources(false);
		devMD.init(TestHelper.ULB_MAX_PATH);

		// act
		DerivateStruct tree = devMD.getStructure();

		// assert
		assertEquals("Dissertatio Inavgvralis Ivridica De Avxiliatoribvs Fvrvm Oder: Von Diebs-Helffern",
				tree.getLabel());
		var lvlOneStructs = tree.getChildren();
		// of old there was 69 which was false
		// 42 pages are contained in total
		// struct log16460310, "[Dissertatio Inauguralis ..." contains 30 linked pages
		// of which are 27 are linked from it's children but 3 only from this container
		// if we decide that a container has either children or pages these 3 pages are lost
		assertEquals(42, devMD.allPagesSorted().size());
		assertEquals(5, lvlOneStructs.size());
		var lvlOneStructOne = lvlOneStructs.get(0);
		assertEquals("Vorderdeckel", lvlOneStructOne.getLabel());

		// according to legacy PDF ouline, expect 4 (four)
		// pages actually belonging to logical section "cover_front":
		// leaf 1 : "[Seite 3]"
		// leaf 2 : "[Seite 4]"
		// leaf 3 : "[Leerseite]"
		// leaf 4 : "[Leerseite]"
		assertEquals(4, lvlOneStructOne.getChildren().size());
		assertEquals("[Seite 3]", lvlOneStructOne.getChildren().get(0).getLabel());
		assertEquals(1, lvlOneStructOne.getChildren().get(0).getOrder());
		assertEquals("[Seite 4]", lvlOneStructOne.getChildren().get(1).getLabel());
		assertEquals(2, lvlOneStructOne.getChildren().get(1).getOrder());
		assertEquals("[Leerseite]", lvlOneStructOne.getChildren().get(2).getLabel());
		assertEquals(3, lvlOneStructOne.getChildren().get(2).getOrder());
		assertEquals("[Leerseite]", lvlOneStructOne.getChildren().get(3).getLabel());
		assertEquals(4, lvlOneStructOne.getChildren().get(3).getOrder());
	}

	// vd18p-14163614.mets.xml
	/**
	 * 
	 * Handle duplicated structures
	 * 
	 * In our testdata, there are multiple duplicate links,
	 * i.e. phys14163497 is linked from log14163656 (top-section, wrong)
	 * and also from log14163496 (sub-ssection, okay)
	 * 
	 * @throws Exception
	 */
	@Test
	void testStructureDuplicatesDropped() throws Exception {

		// arrange
		var mds = new DerivateMD(TestResource.VLS_VD18P_14163614.get());
		mds.checkRessources(false);
		mds.init(TestHelper.ULB_MAX_PATH);

		// act
		DerivateStruct tree = mds.getStructure();

		// assert
		assertEquals("Pirnaischer Chronicken und Historien Calender", tree.getLabel());
		var volOne = tree.getChildren();
		assertEquals(1, volOne.size());
		var lvlOneStructOne = volOne.get(0);
		assertEquals("1765", lvlOneStructOne.getLabel());
		assertEquals(38, mds.allPagesSorted().size());

		// original problem:
		// section with LABEL=[Calender] and ORDER=4 is linked
		// as log14163656 about 25x, but it's children are also linked
		// therefore the linked pages appear several times
		// in the structure tree, and finally in the PDF
		var structCalendar = lvlOneStructOne.getChildren().get(3);
		assertEquals("[Calender]", structCalendar.getLabel());
		// here was wrong of old with 37 links
		// 12 *real* children structure,
		// plus 25 direct links to physical containers
		// WHICH ACTUALLY ALREADY LINK THESE IMAGES
		assertNotEquals(37, structCalendar.getChildren().size());
		assertEquals(12, structCalendar.getChildren().size());
	}
}
