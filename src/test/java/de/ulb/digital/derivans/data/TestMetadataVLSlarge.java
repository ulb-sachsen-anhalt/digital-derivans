package de.ulb.digital.derivans.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.TestResource;
import de.ulb.digital.derivans.TestHelper;
import de.ulb.digital.derivans.model.DerivateMD;
import de.ulb.digital.derivans.model.DerivateStruct;
import de.ulb.digital.derivans.model.pdf.DescriptiveMetadata;

/**
 * 
 * Specification of {@link DerivateMD} for large digital object with 2.300+
 * pages
 * 
 * @author u.hartwig
 *
 */
class TestMetadataVLSlarge {

	static DerivateMD derivate201517;

	static DescriptiveMetadata dmd201517;

	@BeforeAll
	static void setupClazz() throws DigitalDerivansException {

		// try {
		// SAXParserFactory factory = SAXParserFactory.newInstance();
		// SAXParser saxParser = factory.newSAXParser();
		// DefaultHandler handler = new DefaultHandler() {
		// public void startElement(String uri, String localName, String qName,
		// Attributes attributes) throws SAXException {
		// System.out.println("Start Element: " + qName);
		// }
		// public void endElement(String uri, String localName, String qName) throws
		// SAXException {
		// System.out.println("End Element: " + qName);
		// }
		// public void characters(char[] ch, int start, int length) throws SAXException
		// {
		// System.out.println("Content: " + new String(ch, start, length));
		// }
		// };
		// saxParser.parse(TestResource.VLS_HD_Aa_201517.get().toFile(), handler);
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

		TestMetadataVLSlarge.derivate201517 = new DerivateMD(TestResource.VLS_HD_Aa_201517.get());
		TestMetadataVLSlarge.derivate201517.checkRessources(false);
		TestMetadataVLSlarge.derivate201517.init(TestHelper.ULB_MAX_PATH);
		dmd201517 = derivate201517.getDescriptiveData();
	}

	@Test
	void test201517Identifier() {
		assertEquals("urn:nbn:de:gbv:3:3-6252", dmd201517.getUrn());
		assertEquals("535610149", dmd201517.getIdentifier());
	}

	@Test
	void test201517Publication() {
		assertEquals("Micraelius, Johann", dmd201517.getPerson());
		assertEquals(
				"Historia Ecclesiastica, Qua Ab Adamo Judaicae, & a Salvatore nostro Christianae Ecclesiae, ritus, persecutiones, Concilia, Doctores, Haereses & Schismata proponuntur",
				dmd201517.getTitle());
		assertEquals("1699", dmd201517.getYearPublished());
	}

	/**
	 * 
	 * Due error in way of bulding the data, at first it yielded 6.561 pages
	 * because pages were counted each time they were linked
	 * 
	 */
	@Test
	void test201517NumberOfPages() {
		assertEquals(2306, derivate201517.getAllPages().size());
	}

	@Test
	void testStructureOf201517Root() {
		DerivateStruct rootStruct = derivate201517.getStructure();
		assertNotNull(rootStruct);
		assertNotNull(rootStruct.getLabel());
		assertTrue(rootStruct.getPages().isEmpty());
		assertFalse(rootStruct.getChildren().isEmpty());

	}

	@Test
	void testStructure201517Level01() {
		List<DerivateStruct> children = derivate201517.getStructure().getChildren();
		assertEquals(12, children.size());
		assertEquals("Vorderdeckel", children.get(0).getLabel());
		assertEquals(5, children.get(0).getPages().size());
		assertEquals("Kupfertitel", children.get(1).getLabel());
		assertEquals(1, children.get(1).getPages().size());
	}

	@Test
	void testStructure201517Level0206() {
		List<DerivateStruct> children = derivate201517.getStructure().getChildren();
		DerivateStruct l0206 = children.get(5);
		// level 1+2
		assertEquals("Liber Primus,", l0206.getLabel());
		assertFalse(l0206.getChildren().isEmpty());
		// dropped from formerly "207"(!)
		// to just "2" because removal of duplicate
		// structure links
		assertEquals(2, l0206.getChildren().size());

	}

	@Test
	void testStructure201517Level0210() {
		List<DerivateStruct> children = derivate201517.getStructure().getChildren();
		DerivateStruct l0210 = children.get(9);
		// level 1+2+3
		assertEquals(
				"Continuatio Historiae Ecclesiasticae Iohannis Micraelii, Secunda Hac Editione Emendata & plurimis locis aucta à Daniele Hartnaccio, Pomerano.",
				l0210.getLabel());
		// changed due removal of structure link duplicates
		// from "1246"(!) to just "5"
		assertEquals(1242, l0210.getPages().size());
		assertEquals(4, l0210.getChildren().size());
	}
}
