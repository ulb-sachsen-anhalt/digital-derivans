package de.ulb.digital.derivans.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jdom2.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.data.mets.MetadataStore;
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
class TestMetadataStoreVLSMultivolumes {

	static IMetadataStore mds19788;

	static DescriptiveMetadata dd19788;

	static IMetadataStore mds11250807;

	static DescriptiveMetadata dd11250807;

	static IMetadataStore mds9427337;

	static DescriptiveMetadata dd9427337;

	@BeforeAll
	static void setupClazz() throws DigitalDerivansException {
		mds19788 = new MetadataStore(TestResource.VD17_Af_19788.get());
		dd19788 = mds19788.getDescriptiveData();
		mds11250807 = new MetadataStore(TestResource.VD17_AF_11250807.get());
		dd11250807 = mds11250807.getDescriptiveData();
		mds9427337 = new MetadataStore(TestResource.VD18_Af_9427337.get());
		dd9427337 = mds9427337.getDescriptiveData();
	}

	@Test
	void testDescriptiveDataVD17Volume() throws DigitalDerivansException {
		// mods:recodInfo/mods:recordIdentifier[@source]/text()
		assertEquals("005209242", dd19788.getIdentifier());
		// mods:titleInfo/mods:title
		assertTrue(dd19788.getTitle().startsWith("Disputatio Ethica Prima De Summo"));
		// mods:identifier[@type="urn"]
		assertEquals("urn:nbn:de:gbv:3:1-2085", dd19788.getUrn());
		// METS/MODS contains no license information
		assertFalse(dd19788.getLicense().isEmpty());
		// mods:originInfo/mods:dateIssued[@keyDate="yes"]/text()
		assertEquals("1654", dd19788.getYearPublished());
		// mods:role/mods:displayForm/text()
		// OR
		// mods:namePart[@type="family"]/text()
		// WITH
		// IF NOT mods:name/mods:role/mods:roleTerm[@type="code"]/text() = "aut"
		// IF mods:name/mods:role/mods:roleTerm[@type="code"]/text() = "pbl
		assertEquals("Schwertner, David", dd19788.getPerson());
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
		IMetadataStore mds = new MetadataStore(TestResource.MENA_Af_1237560.get());

		// act
		DescriptiveMetadata dd = mds.getDescriptiveData();

		// assert
		assertEquals("385228910", dd.getIdentifier());
		assertEquals("Band", dd.getTitle());
		assertEquals("urn:nbn:de:gbv:3:5-8691", dd.getUrn());
		assertTrue(dd.getLicense().isPresent());
		assertEquals("1906", dd.getYearPublished());
		// this is from mods:displayForm of mods:role/mods:roleTerm/text() = "aut"
		assertEquals("Subkī, Taqī-ad-Dīn ʿAlī Ibn-ʿAbd-al-Kāfī as-", dd.getPerson());
	}

	@Test
	void testDigitalPagesOrderOf737429() throws DigitalDerivansException {

		// act
		List<DigitalPage> pages = mds19788.getDigitalPagesInOrder();

		// assert
		for (DigitalPage page : pages) {
			assertTrue(page.optIdentifier().isPresent());
		}

		String urn1 = "urn:nbn:de:gbv:3:1-2085-p0001-3";
		String urn2 = "urn:nbn:de:gbv:3:1-2085-p0004-9";
		assertEquals(urn1, pages.get(0).optIdentifier().get());
		assertEquals(urn2, pages.get(3).optIdentifier().get());
		assertEquals("MAX/61196.jpg", pages.get(0).getImageFile());
		assertEquals("MAX/61201.jpg", pages.get(3).getImageFile());
	}

	@Test
	void testStructureOf19788() throws DigitalDerivansException {
		DigitalStructureTree dst = mds19788.getStructure();
		assertNotNull(dst);

		// level 1 = C-Stage
		assertTrue(dst.getLabel().startsWith("... Idea Summi"));
		assertEquals(1, dst.getPage());
		assertTrue(dst.hasSubstructures());

		// level 2 = F-Stage
		List<DigitalStructureTree> children = dst.getSubstructures();
		assertEquals(1, children.size());
		assertEquals("Disputatio Ethica Prima De Summo Bono Practico Quod Sit Et Quid Sit", children.get(0).getLabel());
		assertEquals(1, children.get(0).getPage());

		// level 3 = F-Stage struct
		List<DigitalStructureTree> grandchilds = children.get(0).getSubstructures();
		assertEquals("Titelblatt", grandchilds.get(0).getLabel());
		assertEquals(1, grandchilds.get(0).getPage());
		assertEquals("Widmung", grandchilds.get(1).getLabel());
		assertEquals(2, grandchilds.get(1).getPage());
	}

	@Test
	void testDescriptiveData11250807() throws DigitalDerivansException {
		assertEquals("urn:nbn:de:gbv:3:1-699854", dd11250807.getUrn());
		assertEquals("005836395", dd11250807.getIdentifier());
		assertEquals("Martini, Jakob", dd11250807.getPerson());
		assertEquals("De Compositione Syllogismi", dd11250807.getTitle());
		assertEquals("1616", dd11250807.getYearPublished());
	}

	@Test
	void testStructure11250807() throws DigitalDerivansException {
		DigitalStructureTree dst = mds11250807.getStructure();

		// level 1 = C-Stage
		assertEquals("Cursus Philosophici Disputatio ...", dst.getLabel());
		assertEquals(1, dst.getPage());

		// level 2 = F-Stage
		List<DigitalStructureTree> children = dst.getSubstructures();
		assertEquals(1, children.size());
		assertEquals("De Compositione Syllogismi", children.get(0).getLabel());
		assertEquals(1, children.get(0).getPage());

		// level 3 = F-Stage struct
		List<DigitalStructureTree> grandchilds = children.get(0).getSubstructures();
		// 4 structs: "cover_front", "title_page", "section", "cover_back"
		assertEquals(4, grandchilds.size());
	}

	/**
	 * 
	 * Check PDF identifier enriched in Metadata *after* PDF generation
	 * 
	 * @param tempDir
	 * @throws Exception
	 */
	@Test
	void testMetadataIsUpdatedVD17Af19788(@TempDir Path tempDir) throws Exception {
		// arrange
		Path targetPathFile = tempDir.resolve("19788.xml");
		if (Files.exists(targetPathFile)) {
			Files.delete(targetPathFile);
		}
		Files.copy(TestResource.VD17_Af_19788.get(), targetPathFile);
		IMetadataStore mds = new MetadataStore(targetPathFile);

		// act
		String identifier = mds.getDescriptiveData().getIdentifier();
		boolean renderPDFOutcome = mds.enrichPDF(identifier);

		// assert
		assertTrue(renderPDFOutcome);
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
		assertEquals("Steuart, James", dd9427337.getPerson());
		assertEquals("1771", dd9427337.getYearPublished());
		assertEquals(
				"Untersuchung der Grund-Säze Der Staats-Wirthschaft als ein Versuch über die Wissenschaft von der Innerlichen Politik bey freyen Nationen",
				dd9427337.getTitle());
		assertEquals("211999628", dd9427337.getIdentifier());
	}

	/**
	 * 
	 * Check structure for common VD18 MVW F-Stage
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testStructureOf9427337() throws DigitalDerivansException {
		DigitalStructureTree dst = mds9427337.getStructure();
		assertNotNull(dst);

		assertEquals(
				"Sir James Stewarts, Baronets, Untersuchung der Grund-Säze von der Staats-Wirthschaft als ein Versuch über die Wissenschaft von der Innerlichen Politik bey freyen Nationen",
				dst.getLabel());
		assertEquals(1, dst.getPage());
		assertTrue(dst.hasSubstructures());

		// level 1 - F-Stage
		List<DigitalStructureTree> children = dst.getSubstructures();
		assertEquals(1, children.size());
		assertEquals(
				"Untersuchung der Grund-Säze Der Staats-Wirthschaft als ein Versuch über die Wissenschaft von der Innerlichen Politik bey freyen Nationen",
				children.get(0).getLabel());
		assertEquals(1, children.get(0).getPage());

		// level 1+2 - F-Stage
		assertTrue(children.get(0).hasSubstructures());
		assertEquals(6, children.get(0).getSubstructures().size());
		var grandChildren = children.get(0).getSubstructures();
		assertEquals("Exlibris", grandChildren.get(1).getLabel());
		assertEquals(2, grandChildren.get(1).getPage());
		assertEquals("Untersuchung der Grundsäze der Staats-Wirthschaft Drittes Buch von Geld und Münze.",
				grandChildren.get(4).getLabel());
		assertEquals(7, grandChildren.get(4).getPage());
	}

	@Test
	void testIntermediateVD17State() throws DigitalDerivansException {
		var mds = new MetadataStore(TestResource.VD17_AF_11250807.get());
		var dd = mds.getDescriptiveData();
		var strct = mds.getStructure();

		// assert
		assertEquals("De Compositione Syllogismi", dd.getTitle());
		assertEquals("Cursus Philosophici Disputatio ...", strct.getLabel());
		assertEquals("De Compositione Syllogismi", strct.getSubstructures().get(0).getLabel());
		assertEquals("Vorderdeckel", strct.getSubstructures().get(0).getSubstructures().get(0).getLabel());
	}

	/**
	 * 
	 * Since this test alters the provided METS/MODS, it is required to be executed
	 * on a temp copy
	 * 
	 * @param tempDir
	 * @throws DigitalDerivansException
	 * @throws IOException
	 */
	@Test
	void testIntermediateVD17PDFInsertion(@TempDir Path tempDir) throws DigitalDerivansException, IOException {
		// arrange
		Path sourceMETS = TestResource.VD17_AF_11250807.get();
		Path targetMETS = tempDir.resolve("11250807.xml");
		Files.copy(sourceMETS, targetMETS);

		var mds = new MetadataStore(targetMETS);

		// arrange
		Element primMods = mds.getMetadataHandler().getPrimaryMods();
		assertEquals(20, primMods.getChildren().size());

		// act + assert
		assertTrue(mds.enrichPDF("PDF_11250807"));
	}
}
