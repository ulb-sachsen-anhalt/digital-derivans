package de.ulb.digital.derivans.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import de.ulb.digital.derivans.IDerivans;
import de.ulb.digital.derivans.model.DerivateMD;
import de.ulb.digital.derivans.model.DerivateStruct;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.pdf.DescriptiveMetadata;

/**
 * 
 * Specification of {@link MetadataStore}
 * 
 * @author u.hartwig
 *
 */
class TestMetadataStoreVLSminimum {

	static DerivateMD mds737429;

	static DescriptiveMetadata dd737429;

	@BeforeAll
	static void setupClazz() throws DigitalDerivansException {
		TestMetadataStoreVLSminimum.mds737429 = new DerivateMD(TestResource.HD_Aa_737429.get());
		TestMetadataStoreVLSminimum.mds737429.checkRessources(false);
		TestMetadataStoreVLSminimum.mds737429.init(Path.of(IDerivans.IMAGE_DIR_MAX));
		dd737429 = mds737429.getDescriptiveData();
	}

	/**
	 * 
	 * Check expected information is extracted from OAI-record in old VLS 12 format
	 * 
	 * http://digital.bibliothek.uni-halle.de/hd/oai/?verb=GetRecord&metadataPrefix=mets&mode=xml&identifier=737429
	 * 
	 */
	@Test
	void testDescriptiveDataHDmonography() {
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
	void testDigitalPagesOrderOf737429() {

		// act
		List<DigitalPage> pages = TestMetadataStoreVLSminimum.mds737429.getAllPages();

		// assert
		assertEquals(4, pages.size());
		for (DigitalPage page : pages) {
			assertTrue(page.optContentIds().isPresent());
		}

		String urn1 = "urn:nbn:de:gbv:3:3-21437-p0001-0";
		String urn2 = "urn:nbn:de:gbv:3:3-21437-p0004-6";
		assertEquals(urn1, pages.get(0).optContentIds().get());
		assertEquals(urn2, pages.get(3).optContentIds().get());
		assertTrue(pages.get(0).getFile().getPath().toString().endsWith("MAX/737434.jpg"));
		assertTrue(pages.get(3).getFile().getPath().toString().endsWith("MAX/737438.jpg"));
	}

	@Test
	void testStructureOf737429() {
		var dst = mds737429.getStructure();
		assertNotNull(dst);

		assertTrue(dst.getLabel().startsWith("Ode In Solemni Panegyri"));
		assertEquals(1, dst.getPages());
		assertFalse(dst.getChildren().isEmpty());

		// level 1
		List<DerivateStruct> children = dst.getChildren();
		assertEquals(2, children.size());
		assertEquals("Titelblatt", children.get(0).getLabel());
		assertEquals(1, children.get(0).getPages().size());
		assertEquals("[Ode]", children.get(1).getLabel());
		assertEquals(2, children.get(1).getPages().size());
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
		var mds = new DerivateMD(targetPathFile);

		// act
		String identifier = mds.getDescriptiveData().getIdentifier();
		var renderOutcome = mds.getMets().enrichPDF(identifier);

		// assert
		assertTrue(renderOutcome.startsWith("PDF FileGroup for PDF_191092622 created "));
	}

}
