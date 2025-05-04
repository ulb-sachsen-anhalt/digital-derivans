package de.ulb.digital.derivans.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.IDerivans;
import de.ulb.digital.derivans.model.DerivateMD;
import de.ulb.digital.derivans.model.DerivateStruct;
import de.ulb.digital.derivans.model.pdf.DescriptiveMetadata;

/**
 * 
 * Specification of {@link MetadataStore}
 * 
 * @author u.hartwig
 *
 */
class TestMetadataVLSmedium {

	static DerivateMD mds5175671;

	static DescriptiveMetadata dd5175671;

	@BeforeAll
	static void setupClazz() throws DigitalDerivansException {
		TestMetadataVLSmedium.mds5175671 = new DerivateMD(TestResource.VLS_HD_Aa_5175671.get());
		TestMetadataVLSmedium.mds5175671.checkRessources(false);
		TestMetadataVLSmedium.mds5175671.init(Path.of(IDerivans.IMAGE_DIR_MAX));
		dd5175671 = mds5175671.getDescriptiveData();
	}

	@Test
	void testStructureOf5175671() {
		String title = dd5175671.getTitle();
		assertEquals("n.a.", title);
	}

	/**
	 * 
	 * Example of invalid legacy METS Data -
	 * It contains links to non-existing physical sructures
	 * and therefore cannot generate proper Page Mappings.
	 * 
	 * DATA LIKE THIS MUST BE CORRECT FOREHAND!
	 * 
	 * @throws Exception
	 */
	@Test
	void testInvalidStructure226134857() throws Exception {

		// arrange
		var mds = new DerivateMD(TestResource.VLS_HD_Aa_226134857.get());
		mds.checkRessources(false);

		// act
		var actualExc = assertThrows(DigitalDerivansException.class,
				() -> mds.init(TestHelper.ULB_MAX_PATH));

		// was: LogId 'log2404939' : Invalid physical struct 'phys2404942'!
		assertEquals("No files link div log2404930/Scene VIII. in @USE=MAX!", actualExc.getMessage());
	}

	/**
	 * 
	 * Ensure: invalid XML which confuses the Parser yields
	 * proper Exception for Diagnostics
	 * @throws DigitalDerivansException 
	 * 
	 */
	@Test
	@Disabled
	void testInvalidXMLFromMigration() throws DigitalDerivansException {

		// arrange
		DerivateMD derivate = new DerivateMD(TestResource.VLS_VD18_Aa_9989442.get());
		derivate.checkRessources(false);

		// act
		Exception exc = assertThrows(DigitalDerivansException.class, () -> {
			derivate.init(TestHelper.ULB_MAX_PATH);
		});

		// assert
		assertEquals("java.lang.IllegalArgumentException: The given document is not a valid mets document",
				exc.getMessage());
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
		assertEquals(69, devMD.getAllPages().size());
		assertEquals(5, lvlOneStructs.size());
		var lvlOneStructOne = lvlOneStructs.get(0);
		assertEquals("Vorderdeckel", lvlOneStructOne.getLabel());

		// according to legacy PDF ouline, expect 4 (four)
		// pages actually belonging to logical section "cover_front":
		// leaf 1 : "[Seite 3]"
		// leaf 2 : "[Seite 4]"
		// leaf 3 : "[Leerseite]"
		// leaf 4 : "[Leerseite]"
		assertEquals(4, lvlOneStructOne.getPages().size());
		assertEquals("[Seite 3]", lvlOneStructOne.getPages().get(0).getPageLabel().get());
		assertEquals(1, lvlOneStructOne.getPages().get(0).getOrderNr());
		assertEquals("[Seite 4]", lvlOneStructOne.getPages().get(1).getPageLabel().get());
		assertEquals(2, lvlOneStructOne.getPages().get(1).getOrderNr());
		assertEquals("[Leerseite]", lvlOneStructOne.getPages().get(2).getPageLabel().get());
		assertEquals(3, lvlOneStructOne.getPages().get(2).getOrderNr());
		assertEquals("[Leerseite]", lvlOneStructOne.getPages().get(3).getPageLabel().get());
		assertEquals(4, lvlOneStructOne.getPages().get(3).getOrderNr());
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
		assertEquals(38, mds.getAllPages().size());

		// original problem as following
		// section with LABEL=[Calender] and ORDER=4 is linked
		// as log14163656 about 25x, but it's children are also linked
		// therefore the linked pages appear several times
		// in the structure tree, and finally in the PDF
		var structCalendar = lvlOneStructOne.getChildren().get(3);
		assertEquals("[Calender]", structCalendar.getLabel());
		assertEquals(6, structCalendar.getPages().size());
		// here was wrong of old with 37 links
		// 12 *real* children structure,
		// plus 25 direct links to physical containers
		// WHICH ACTUALLY ALREADY LINK THESE IMAGES
		assertNotEquals(37, structCalendar.getChildren().size());
		assertEquals(12, structCalendar.getChildren().size());
	}
}
