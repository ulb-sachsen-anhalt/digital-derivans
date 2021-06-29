package de.ulb.digital.derivans.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.model.DescriptiveData;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.DigitalStructureTree;

/**
 * 
 * Specification of {@link MetadataStore}
 * 
 * @author hartwig
 *
 */
class TestMetadataStoreVLS {

	static Path vlInhouse737429 = Path.of("./src/test/resources/metadata/vls/737429.xml");

	static Path vlInhouse201517 = Path.of("./src/test/resources/metadata/vls/201517.xml");
	
	static Path vlMVWVD189427337 = Path.of("./src/test/resources/metadata/vls/9427337.ulb.xml");

	static IMetadataStore mds737429;
	
	static DescriptiveData dd737429;
	
	static IMetadataStore mds201517;
	
	static DescriptiveData dd201517;
	
	static IMetadataStore mds9427337;
	
	static DescriptiveData dd9427337;

	@BeforeAll
	static void setupClazz() throws DigitalDerivansException {
		mds737429 = new MetadataStore(vlInhouse737429);
		dd737429 = mds737429.getDescriptiveData();
		mds201517 = new MetadataStore(vlInhouse201517);
		dd201517 = mds201517.getDescriptiveData();
		mds9427337 = new MetadataStore(vlMVWVD189427337);
		dd9427337 = mds9427337.getDescriptiveData();
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
	void testDescriptiveDataFromVL12InhouseOAI() throws DigitalDerivansException {
		// PDF creator from configuration, not from METS/MODS
		assertTrue(dd737429.getCreator().isEmpty());
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
		assertEquals("Langenheim", dd737429.getPerson());
	}

	/**
	 * 
	 * Some MVW F-Stage from menadoc with logical structure "section" / "section" 
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testDescriptiveDataFromVL12MenalibOAI() throws DigitalDerivansException {
		// arrange
		Path vlMena1237560 = Path.of("./src/test/resources/metadata/vls/1237560.xml");
		IMetadataStore mds = new MetadataStore(vlMena1237560);

		// act
		DescriptiveData dd = mds.getDescriptiveData();

		// assert
		assertTrue(dd.getCreator().isEmpty());
		assertEquals("385228910", dd.getIdentifier());
		assertEquals("n.a.", dd.getTitle());
		assertEquals("urn:nbn:de:gbv:3:5-8691", dd.getUrn());
		assertTrue(dd.getLicense().isPresent());
		assertEquals("1906", dd.getYearPublished());
		// this is from mods:displayForm of mods:role/mods:roleTerm/text() = "aut"
		assertEquals("Subkī, Taqī-ad-Dīn ʿAlī Ibn-ʿAbd-al-Kāfī as-", dd.getPerson());
	}

	@Test
	void testDigitalPagesOrderOf737429() throws DigitalDerivansException {

		// act
		List<DigitalPage> pages = mds737429.getDigitalPagesInOrder();

		// assert
		for (DigitalPage page : pages) {
			assertTrue(page.getIdentifier().isPresent());
		}

		String urn1 = "urn:nbn:de:gbv:3:3-21437-p0001-0";
		String urn2 = "urn:nbn:de:gbv:3:3-21437-p0004-6";
		assertEquals(urn1, pages.get(0).getIdentifier().get());
		assertEquals(urn2, pages.get(3).getIdentifier().get());
		assertEquals("737434.jpg", pages.get(0).getImageFile());
		assertEquals("737436.jpg", pages.get(1).getImageFile());
		assertEquals("737437.jpg", pages.get(2).getImageFile());
		assertEquals("737438.jpg", pages.get(3).getImageFile());
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
		assertEquals(Optional.empty(), dd201517.getCreator());
		assertEquals("Micraelius, Johann", dd201517.getPerson());
		assertEquals("Historia Ecclesiastica, Qua Ab Adamo Judaicae, & a Salvatore nostro Christianae Ecclesiae, ritus, persecutiones, Concilia, Doctores, Haereses & Schismata proponuntur", dd201517.getTitle());
		assertEquals("1699", dd201517.getYearPublished());
	}
	
	/**
	 * 
	 * Check descriptive data for common VD18 MVW F-Stage
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testDescriptiveDataOf9427337() throws DigitalDerivansException {
		assertEquals("urn:nbn:de:gbv:3:1-635986", dd9427337.getUrn());
		assertEquals(Optional.empty(), dd9427337.getCreator());
		assertEquals("Steuart, James", dd9427337.getPerson());
		assertEquals("1771", dd9427337.getYearPublished());
		assertEquals("Untersuchung der Grund-Säze Der Staats-Wirthschaft als ein Versuch über die Wissenschaft von der Innerlichen Politik bey freyen Nationen", dd9427337.getTitle());
		assertEquals("211999628", dd9427337.getIdentifier());
	}
	
	/**
	 * 
	 * Check structure for common VD18 MVW F-Stage
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test @Disabled
	void testStructureOf9427337() throws DigitalDerivansException {
		DigitalStructureTree dst = mds9427337.getStructure();
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
		assertEquals(2, children.get(5).getSubstructures().size());

		// level 1+2+3
		assertEquals(
				"Continuatio Historiae Ecclesiasticae Iohannis Micraelii, Secunda Hac Editione Emendata & plurimis locis aucta à Daniele Hartnaccio, Pomerano.",
				children.get(9).getLabel());
		assertEquals(4, children.get(9).getSubstructures().size());
		assertEquals(11, children.get(9).getSubstructures().get(1).getSubstructures().size());
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
		assertEquals(2, children.get(5).getSubstructures().size());

		// level 1+2+3
		assertEquals(
				"Continuatio Historiae Ecclesiasticae Iohannis Micraelii, Secunda Hac Editione Emendata & plurimis locis aucta à Daniele Hartnaccio, Pomerano.",
				children.get(9).getLabel());
		assertEquals(4, children.get(9).getSubstructures().size());
		assertEquals(11, children.get(9).getSubstructures().get(1).getSubstructures().size());
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
		Path sourcePathFile = Path.of("src/test/resources/metadata/vls/737429.xml");
		Path targetPathFile = tempDir.resolve("737429.xml");
		if (Files.exists(targetPathFile)) {
			Files.delete(targetPathFile);
		}
		Files.copy(sourcePathFile, targetPathFile);
		IMetadataStore mds = new MetadataStore(targetPathFile);

		// act
		String identifier = mds.getDescriptiveData().getIdentifier();
		boolean renderPDFOutcome = mds.enrichPDF(identifier);

		// assert
		assertTrue(renderPDFOutcome);
	}

	/**
	 * 
	 * Example of invalid Test Data - contains links to non-existing physical
	 * structures and therefore cannot generate proper Mappings for this part of
	 * Document Structure
	 * 
	 * @throws Exception
	 */
	@Test
	void testInvalidStructure226134857() throws Exception {

		// arrange
		Path path226134857 = Path.of("./src/test/resources/metadata/vls/226134857.prep.xml");
		IMetadataStore mds = new MetadataStore(path226134857);

		// act
		DigitalStructureTree tree = mds.getStructure();
		DescriptiveData dd = mds.getDescriptiveData();

		// assert
		for (DigitalStructureTree subTree : tree.getSubstructures()) {
			if (subTree.hasSubstructures()) {
				for (DigitalStructureTree subSubTree : subTree.getSubstructures()) {
					assertFalse(subSubTree.hasSubstructures());
					assertTrue(subSubTree.getPage() > 0);
				}
			}
			assertTrue(subTree.getPage() > 0);
		}
		
		// assert even some more
		assertEquals("1740", dd.getYearPublished());
		assertEquals("Prault, Pierre", dd.getPerson());
	}

	@Test
	void testStructurOf133573613() throws Exception {

		// arrange
		Path path133573613 = Path.of("./src/test/resources/metadata/vls/133573613.prep.xml");
		IMetadataStore store = new MetadataStore(path133573613);

		// act
		DigitalStructureTree tree = store.getStructure();
		DescriptiveData dd = store.getDescriptiveData();

		// assert
		assertEquals(5, tree.getSubstructures().size());
		assertEquals("1768", dd.getYearPublished());
		assertEquals("Voltaire", dd.getPerson());
	}

}
