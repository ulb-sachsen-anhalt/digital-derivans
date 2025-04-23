package de.ulb.digital.derivans.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.DigitalStructureTree;
import de.ulb.digital.derivans.model.pdf.DescriptiveMetadata;

/**
 * 
 * Specification of {@link MetadataStore}
 * 
 * @author u.hartwig
 *
 */
class TestMetadataStoreVLS {

	static IMetadataStore mds737429;

	static DescriptiveMetadata dd737429;

	static IMetadataStore mds201517;

	static DescriptiveMetadata dd201517;

	static IMetadataStore mds5175671;

	static DescriptiveMetadata dd5175671;

	@BeforeAll
	static void setupClazz() throws DigitalDerivansException {
		mds737429 = new MetadataStore(TestResource.HD_Aa_737429.get());
		dd737429 = mds737429.getDescriptiveData();
		mds201517 = new MetadataStore(TestResource.HD_Aa_201517.get());
		dd201517 = mds201517.getDescriptiveData();
		mds5175671 = new MetadataStore(TestResource.HD_Aa_5175671.get());
		dd5175671 = mds5175671.getDescriptiveData();
	}

	/**
	 * 
	 * Check expected information is extracted from OAI-record in old VLS 12 format
	 * 
	 * http://digital.bibliothek.uni-halle.de/hd/oai/?verb=GetRecord&metadataPrefix=mets&mode=xml&identifier=737429
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testDescriptiveDataHDmonography() throws DigitalDerivansException {
		// mods:recodInfo/mods:recordIdentifier[@source]/text()
		assertEquals("191092622", dd737429.getIdentifier());
		// mods:titleInfo/mods:title
		assertTrue(dd737429.getTitle().startsWith("Ode In Solemni Panegyri Avgvstissimo Ac Potentissimo"));
		// mods:identifier[@type="urn"]
		assertEquals("urn:nbn:de:gbv:3:3-21437", dd737429.getUrn());
		// METS/MODS contains no license information
		assertTrue(dd737429.getLicense().isEmpty());
		// mods:originInfo/mods:dateIssued[@keyDate="yes"]/text()
		assertEquals("1731", dd737429.getYearPublished());
		// mods:role/mods:displayForm/text()
		// OR
		// mods:namePart[@type="family"]/text()
		// WITH
		// IF NOT mods:name/mods:role/mods:roleTerm[@type="code"]/text() = "aut"
		// IF mods:name/mods:role/mods:roleTerm[@type="code"]/text() = "pbl
		assertEquals("Officina Langenhemia", dd737429.getPerson());
	}

	@Test
	void testDigitalPagesOrderOf737429() throws DigitalDerivansException {

		// act
		List<DigitalPage> pages = mds737429.getDigitalPagesInOrder();

		// assert
		for (DigitalPage page : pages) {
			assertTrue(page.optIdentifier().isPresent());
		}

		String urn1 = "urn:nbn:de:gbv:3:3-21437-p0001-0";
		String urn2 = "urn:nbn:de:gbv:3:3-21437-p0004-6";
		assertEquals(urn1, pages.get(0).optIdentifier().get());
		assertEquals(urn2, pages.get(3).optIdentifier().get());
		assertEquals("MAX/737434.jpg", pages.get(0).getImageFile());
		assertEquals("MAX/737436.jpg", pages.get(1).getImageFile());
		assertEquals("MAX/737437.jpg", pages.get(2).getImageFile());
		assertEquals("MAX/737438.jpg", pages.get(3).getImageFile());
	}

	@Test
	void testStructureOf737429() throws DigitalDerivansException {
		DigitalStructureTree dst = mds737429.getStructure();
		assertNotNull(dst);

		assertTrue(dst.getLabel().startsWith("Ode In Solemni Panegyri"));
		assertEquals(1, dst.getPage());
		assertTrue(dst.hasSubstructures());

		// level 1
		List<DigitalStructureTree> children = dst.getSubstructures();
		assertEquals(2, children.size());
		assertEquals("Titelblatt", children.get(0).getLabel());
		assertEquals(1, children.get(0).getPage());
		assertEquals("[Ode]", children.get(1).getLabel());
		assertEquals(2, children.get(1).getPage());
	}

	@Test
	void testDescriptiveDataOf201517() throws DigitalDerivansException {
		assertEquals("urn:nbn:de:gbv:3:3-6252", dd201517.getUrn());
		assertEquals("535610149", dd201517.getIdentifier());
		assertEquals("Micraelius, Johann", dd201517.getPerson());
		assertEquals(
				"Historia Ecclesiastica, Qua Ab Adamo Judaicae, & a Salvatore nostro Christianae Ecclesiae, ritus, persecutiones, Concilia, Doctores, Haereses & Schismata proponuntur",
				dd201517.getTitle());
		assertEquals("1699", dd201517.getYearPublished());
	}

	@Test
	void testStructureOf5175671() throws DigitalDerivansException {
		String title = dd5175671.getTitle();
		assertEquals("n.a.", title);
	}

	@Test
	void testStructureOf201517() throws DigitalDerivansException {
		DigitalStructureTree dst = mds201517.getStructure();
		assertNotNull(dst);

		assertNotNull(dst.getLabel());
		assertEquals(1, dst.getPage());
		assertTrue(dst.hasSubstructures());

		// level 1
		List<DigitalStructureTree> children = dst.getSubstructures();
		assertEquals(12, children.size());
		assertEquals("Vorderdeckel", children.get(0).getLabel());
		assertEquals(1, children.get(0).getPage());
		assertEquals("Kupfertitel", children.get(1).getLabel());
		assertEquals(6, children.get(1).getPage());

		// level 1+2
		assertEquals("Liber Primus,", children.get(5).getLabel());
		assertTrue(children.get(5).hasSubstructures());
		// dropped from formerly "207"(!)
		// to just "2" because removal of duplicate
		// structure links
		assertEquals(2, children.get(5).getSubstructures().size());

		// level 1+2+3
		assertEquals(
				"Continuatio Historiae Ecclesiasticae Iohannis Micraelii, Secunda Hac Editione Emendata & plurimis locis aucta à Daniele Hartnaccio, Pomerano.",
				children.get(9).getLabel());
		// changed due removal of structure link duplicates
		// from "1246"(!) to just "5"
		assertEquals(5, children.get(9).getSubstructures().size());
		assertEquals(1, children.get(9).getSubstructures().get(1).getSubstructures().size());
	}

	/**
	 * 
	 * Check PDF identifier enriched in Metadata *after* PDF generation
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testMetadataIsUpdated737429(@TempDir Path tempDir) throws Exception {
		// arrange
		Path targetPathFile = tempDir.resolve("737429.xml");
		if (Files.exists(targetPathFile)) {
			Files.delete(targetPathFile);
		}
		Files.copy(TestResource.HD_Aa_737429.get(), targetPathFile);
		IMetadataStore mds = new MetadataStore(targetPathFile);

		// act
		String identifier = mds.getDescriptiveData().getIdentifier();
		boolean renderPDFOutcome = mds.enrichPDF(identifier);

		// assert
		assertTrue(renderPDFOutcome);
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
		IMetadataStore mds = new MetadataStore(TestResource.HD_Aa_226134857_LEGACY.get());

		// act
		var actualExc = assertThrows(DigitalDerivansException.class, 
			() -> mds.getStructure());
		
		//
		assertEquals("LogId 'log2404939' : Invalid physical struct 'phys2404942'!", actualExc.getMessage());
	}

	/**
	 * 
	 * Ensure: invalid XML which confuses the Parser yields 
	 * proper Exception for Diagnostics 
	 * 
	 * @throws Exception
	 */
	@Test
	void testInvalidXMLFromMigration() throws Exception {
		
		// act
		Exception exc = assertThrows(DigitalDerivansException.class, () -> {
			new MetadataStore(TestResource.VD18_Aa_9989442.get());
		});
		
		// assert
		assertEquals("java.lang.IllegalArgumentException: The given document is not a valid mets document", exc.getMessage());
	}

	@Test
	void testStructureForPageLabels() throws Exception {

		// arrange
		IMetadataStore mds = new MetadataStore(TestResource.VD18_Aa_VD18_MIG.get());

		// act
		DigitalStructureTree tree = mds.getStructure();

		// assert
		assertEquals("Dissertatio Inavgvralis Ivridica De Avxiliatoribvs Fvrvm Oder: Von Diebs-Helffern", tree.getLabel());
		var lvlOneStructs = tree.getSubstructures();
		assertEquals(5, lvlOneStructs.size());
		var lvlOneStructOne = lvlOneStructs.get(0);
		assertEquals("Vorderdeckel", lvlOneStructOne.getLabel());

		// according to legacy PDF ouline, expect 4 (four) 
		// pages actually belonging to logical section "cover_front":
		// leaf 1 : "[Seite 3]"
		// leaf 2 : "[Seite 4]"
		// leaf 3 : "[Leerseite]"
		// leaf 4 : "[Leerseite]"
		assertEquals(4, lvlOneStructOne.getSubstructures().size());
		assertEquals("[Seite 3]", lvlOneStructOne.getSubstructures().get(0).getLabel());
		assertEquals(1, lvlOneStructOne.getSubstructures().get(0).getPage());
		assertEquals("[Seite 4]", lvlOneStructOne.getSubstructures().get(1).getLabel());
		assertEquals(2, lvlOneStructOne.getSubstructures().get(1).getPage());
		assertEquals("[Leerseite]", lvlOneStructOne.getSubstructures().get(2).getLabel());
		assertEquals(3, lvlOneStructOne.getSubstructures().get(2).getPage());
		assertEquals("[Leerseite]", lvlOneStructOne.getSubstructures().get(3).getLabel());
		assertEquals(4, lvlOneStructOne.getSubstructures().get(3).getPage());
	}


	// vd18p-14163614.mets.xml
	/**
	 * 
	 * Handle duplicated structures
	 * 
	 * In our testdata, there are multiple duplicate links,
	 * i.e. phys14163497 is linked from log14163656 (top-section, wrong)
	 *		and also from log14163496 (sub-ssection, okay) 
	 * 
	 * @throws Exception
	 */
	@Test
	void testStructureDuplicatesDropped() throws Exception {

		// arrange
		IMetadataStore mds = new MetadataStore(TestResource.VD18P_14163614.get());

		// act
		DigitalStructureTree tree = mds.getStructure();

		// assert
		assertEquals("Pirnaischer Chronicken und Historien Calender", tree.getLabel());
		var volOne = tree.getSubstructures();
		assertEquals(1, volOne.size());
		var lvlOneStructOne = volOne.get(0);
		assertEquals("1765", lvlOneStructOne.getLabel());

		// original problem as following
		// section with LABEL=[Calender] and ORDER=4 is linked 
		// as log14163656 about 25x, but it's children are also linked
		// therefore the linked pages appear several times
		// in the structure tree, and finally in the PDF
		var structCalendar = lvlOneStructOne.getSubstructures().get(3);
		assertEquals("[Calender]", structCalendar.getLabel());
		assertEquals(6, structCalendar.getPage());
		// here was wrong of old with 37 links 
		// 12 *real* children structure,
		// plus 25 direct links to physical containers 
		// WHICH ACTUALLY ALREADY LINK THESE IMAGES
		assertNotEquals(37, structCalendar.getSubstructures().size());
		assertEquals(12, structCalendar.getSubstructures().size());
	}
}
