package de.ulb.digital.derivans.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jdom2.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.model.pdf.DescriptiveMetadata;

/**
 * 
 * @author u.hartwig
 *
 */
class TestDerivateMDVLSMultivolumes {

	static DerivateMD derivate19788;

	static DescriptiveMetadata dmd19788;

	static DerivateMD derivate11250807;

	static DescriptiveMetadata dmd11250807;

	static DerivateMD derivate9427337;

	static DescriptiveMetadata dmd9427337;

	@BeforeAll
	static void setupClazz() throws DigitalDerivansException {
		derivate19788 = new DerivateMD(TestResource.VLS_VD17_Af_19788.get());
		derivate19788.checkRessources(false);
		derivate19788.init(TestHelper.ULB_MAX_PATH);
		dmd19788 = derivate19788.getDescriptiveData();
		derivate11250807 = new DerivateMD(TestResource.VLS_VD17_AF_11250807.get());
		derivate11250807.checkRessources(false);
		derivate11250807.init(TestHelper.ULB_MAX_PATH);
		dmd11250807 = derivate11250807.getDescriptiveData();
		derivate9427337 = new DerivateMD(TestResource.VLS_VD18_Af_9427337.get());
		derivate9427337.checkRessources(false);
		derivate9427337.init(TestHelper.ULB_MAX_PATH);
		dmd9427337 = derivate9427337.getDescriptiveData();
	}

	@Test
	void testDescriptiveDataVD17Volume() {
		// mods:recodInfo/mods:recordIdentifier[@source]/text()
		assertEquals("005209242", dmd19788.getIdentifier());
		// mods:titleInfo/mods:title
		assertTrue(dmd19788.getTitle().startsWith("Disputatio Ethica Prima De Summo"));
		// mods:identifier[@type="urn"]
		assertEquals("urn:nbn:de:gbv:3:1-2085", dmd19788.getUrn());
		// METS/MODS contains no license information
		assertFalse(dmd19788.getLicense().isEmpty());
		// mods:originInfo/mods:dateIssued[@keyDate="yes"]/text()
		assertEquals("1654", dmd19788.getYearPublished());
		// mods:role/mods:displayForm/text()
		// OR
		// mods:namePart[@type="family"]/text()
		// WITH
		// IF NOT mods:name/mods:role/mods:roleTerm[@type="code"]/text() = "aut"
		// IF mods:name/mods:role/mods:roleTerm[@type="code"]/text() = "pbl
		assertEquals("Schwertner, David", dmd19788.getPerson());
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
		var deriv = new DerivateMD(TestResource.VLS_MENA_Af_1237560.get());
		deriv.checkRessources(false);
		deriv.init(TestHelper.ULB_MAX_PATH);

		// act
		DescriptiveMetadata dd = deriv.getDescriptiveData();

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
	void testDigitalPagesContentIds19788() {

		// act
		List<DigitalPage> pages = derivate19788.allPagesSorted();

		// assert
		assertEquals(22, pages.size());
		for (DigitalPage page : pages) {
			assertTrue(page.optContentIds().isPresent());
		}

		String urn1 = "urn:nbn:de:gbv:3:1-2085-p0001-3";
		String urn2 = "urn:nbn:de:gbv:3:1-2085-p0004-9";
		assertEquals(urn1, pages.get(0).optContentIds().get());
		assertEquals(urn2, pages.get(3).optContentIds().get());
		assertTrue(pages.get(0).getFile().getPath().endsWith("MAX/61196.jpg"));
		assertTrue(pages.get(3).getFile().getPath().endsWith("MAX/61201.jpg"));
	}

	@Test
	void testStructureOf19788() {
		DerivateStruct dst = derivate19788.getStructure();
		assertNotNull(dst);

		// level 1 = C-Stage
		assertTrue(dst.getLabel().startsWith("... Idea Summi"));
		assertFalse(dst.getChildren().isEmpty());

		// level 2 = F-Stage
		List<DerivateStruct> children = dst.getChildren();
		assertEquals(1, children.size());
		assertEquals("Disputatio Ethica Prima De Summo Bono Practico Quod Sit Et Quid Sit", children.get(0).getLabel());

		// level 3 = F-Stage struct
		List<DerivateStruct> grandchilds = children.get(0).getChildren();
		assertEquals("Titelblatt", grandchilds.get(0).getLabel());
		assertEquals(1, grandchilds.get(0).getChildren().size());
		assertEquals("Widmung", grandchilds.get(1).getLabel());
		assertEquals(1, grandchilds.get(1).getChildren().size());
		assertEquals("Titelblatt", grandchilds.get(2).getLabel());
		assertEquals(2, grandchilds.get(2).getChildren().size());
		assertEquals("Abschnitt", grandchilds.get(3).getLabel());
		assertEquals(18, grandchilds.get(3).getChildren().size());
	}

	@Test
	void testDescriptiveData11250807() {
		assertEquals("urn:nbn:de:gbv:3:1-699854", dmd11250807.getUrn());
		assertEquals("005836395", dmd11250807.getIdentifier());
		assertEquals("Martini, Jakob", dmd11250807.getPerson());
		assertEquals("De Compositione Syllogismi", dmd11250807.getTitle());
		assertEquals("1616", dmd11250807.getYearPublished());
	}

	@Test
	void testStructure11250807() {
		DerivateStruct dst = derivate11250807.getStructure();

		// level 1 = C-Stage
		assertEquals("Cursus Philosophici Disputatio ...", dst.getLabel());
		assertEquals(0, dst.getPages().size()); // C-dtage must not have pages

		// level 2 = F-Stage
		List<DerivateStruct> children = dst.getChildren();
		assertEquals(1, children.size());
		assertEquals("De Compositione Syllogismi", children.get(0).getLabel());

		// level 3 = F-Stage struct
		List<DerivateStruct> grandchilds = children.get(0).getChildren();
		// 4 structs: "cover_front", "title_page", "section", "cover_back"
		assertEquals(4, grandchilds.size());
	}

	/**
	 * 
	 * Check descriptive data for common VD18 MVW F-Stage
	 * 
	 */
	@Test
	void testDescriptiveDataOf9427337() {
		assertEquals("urn:nbn:de:gbv:3:1-635986", dmd9427337.getUrn());
		assertEquals("Steuart, James", dmd9427337.getPerson());
		assertEquals("1771", dmd9427337.getYearPublished());
		assertEquals(
				"Untersuchung der Grund-Säze Der Staats-Wirthschaft als ein Versuch über die Wissenschaft von der Innerlichen Politik bey freyen Nationen",
				dmd9427337.getTitle());
		assertEquals("211999628", dmd9427337.getIdentifier());
	}

	/**
	 * 
	 * Check structure for common VD18 MVW F-Stage
	 * 
	 * @throws DigitalDerivansException
	 */
	@Test
	void testStructureOf9427337() {
		DerivateStruct dst = derivate9427337.getStructure();
		assertNotNull(dst);

		assertEquals(
				"Sir James Stewarts, Baronets, Untersuchung der Grund-Säze von der Staats-Wirthschaft als ein Versuch über die Wissenschaft von der Innerlichen Politik bey freyen Nationen",
				dst.getLabel());
		assertEquals(0, dst.getPages().size());
		assertFalse(dst.getChildren().isEmpty());

		// level 1 - F-Stage
		List<DerivateStruct> children = dst.getChildren();
		assertEquals(1, children.size());
		assertEquals(
				"Untersuchung der Grund-Säze Der Staats-Wirthschaft als ein Versuch über die Wissenschaft von der Innerlichen Politik bey freyen Nationen",
				children.get(0).getLabel());
		assertEquals(0, children.get(0).getPages().size());

		// level 1+2 - F-Stage
		assertFalse(children.get(0).getChildren().isEmpty());
		assertEquals(6, children.get(0).getChildren().size());
		var grandChildren = children.get(0).getChildren();
		assertEquals("Exlibris", grandChildren.get(1).getLabel());
		assertEquals(1, grandChildren.get(1).getChildren().size());
		assertEquals("Untersuchung der Grundsäze der Staats-Wirthschaft Drittes Buch von Geld und Münze.",
				grandChildren.get(4).getLabel());
		assertEquals(1, grandChildren.get(4).getChildren().size());
	}

	@Test
	void testIntermediateVD17State() throws DigitalDerivansException {
		var mds = new DerivateMD(TestResource.VLS_VD17_AF_11250807.get());
		mds.checkRessources(false);
		mds.init(TestHelper.ULB_MAX_PATH);
		var dd = mds.getDescriptiveData();
		var strct = mds.getStructure();

		// assert
		assertEquals("De Compositione Syllogismi", dd.getTitle());
		assertEquals("Cursus Philosophici Disputatio ...", strct.getLabel());
		assertEquals("De Compositione Syllogismi", strct.getChildren().get(0).getLabel());
		assertEquals("Vorderdeckel", strct.getChildren().get(0).getChildren().get(0).getLabel());
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
	void testIntermediateVD17PDFInsertion(@TempDir Path tempDir) throws Exception {
		// arrange
		Path sourceMETS = TestResource.VLS_VD17_AF_11250807.get();
		Path targetMETS = tempDir.resolve("11250807.xml");
		Files.copy(sourceMETS, targetMETS);

		// arrange
		var deriv = new DerivateMD(targetMETS);
		deriv.checkRessources(false);
		deriv.init(TestHelper.ULB_MAX_PATH);

		// act + assert
		var result = deriv.getMets().enrichPDF("005836050");
		assertTrue(result.contains("mets:file@ID=PDF_005836050 created"));
		Document doc =TestHelper.readXMLDocument(targetMETS);
		var mets = doc.getRootElement();
		var fileSec = mets.getChild("fileSec", mets.getNamespace());
		assertNotNull(fileSec);
		var fileGrps = fileSec.getChildren("fileGrp", mets.getNamespace());
		assertEquals(2, fileGrps.size());
		var pdfGrp = fileGrps.stream().filter(g -> g.getAttributeValue("USE").equals("DOWNLOAD")).findFirst();
		assertTrue(pdfGrp.isPresent());
		var pdfFiles = pdfGrp.get().getChildren("file", mets.getNamespace());
		assertEquals(1, pdfFiles.size());
		var pdfFile = pdfFiles.get(0);
		assertEquals("PDF_005836050", pdfFile.getAttributeValue("ID"));
		var pdfLocs = pdfFile.getChildren("FLocat", mets.getNamespace());
		assertEquals(1, pdfLocs.size());
		var pdfLoc = pdfLocs.get(0);
		assertEquals("005836050.pdf",
			pdfLoc.getAttributeValue("href", mets.getNamespace("xlink")));
	}
}
