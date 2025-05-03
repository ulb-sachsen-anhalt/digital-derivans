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

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

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
@Disabled
class TestMetadataStoreVLSlarge {

	static DerivateMD mds737429;

	static DescriptiveMetadata dd737429;

	static DerivateMD mds201517;

	static DescriptiveMetadata dd201517;

	static DerivateMD mds5175671;

	static DescriptiveMetadata dd5175671;

	@BeforeAll
	static void setupClazz() throws DigitalDerivansException {


		try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            DefaultHandler handler = new DefaultHandler() {
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    System.out.println("Start Element: " + qName);
                }
                public void endElement(String uri, String localName, String qName) throws SAXException {
                    System.out.println("End Element: " + qName);
                }
                public void characters(char[] ch, int start, int length) throws SAXException {
                    System.out.println("Content: " + new String(ch, start, length));
                }
            };
            saxParser.parse(TestResource.VLS_HD_Aa_201517.get().toFile(), handler);
        } catch (Exception e) {
            e.printStackTrace();
        }


		TestMetadataStoreVLSlarge.mds201517 = new DerivateMD(TestResource.VLS_HD_Aa_201517.get());
		TestMetadataStoreVLSlarge.mds201517.checkRessources(false);
		TestMetadataStoreVLSlarge.mds201517.init(Path.of(IDerivans.IMAGE_DIR_MAX));
		dd201517 = mds201517.getDescriptiveData();
	}

	@Test
	void testDescriptiveDataOf201517() {
		assertEquals("urn:nbn:de:gbv:3:3-6252", dd201517.getUrn());
		assertEquals("535610149", dd201517.getIdentifier());
		assertEquals("Micraelius, Johann", dd201517.getPerson());
		assertEquals(
				"Historia Ecclesiastica, Qua Ab Adamo Judaicae, & a Salvatore nostro Christianae Ecclesiae, ritus, persecutiones, Concilia, Doctores, Haereses & Schismata proponuntur",
				dd201517.getTitle());
		assertEquals("1699", dd201517.getYearPublished());
	}

	@Test
	void testStructureOf201517() {
		DerivateStruct dst = mds201517.getStructure();
		assertNotNull(dst);

		assertNotNull(dst.getLabel());
		assertEquals(1, dst.getPages().size());
		assertFalse(dst.getChildren().isEmpty());

		// level 1
		List<DerivateStruct> children = dst.getChildren();
		assertEquals(12, children.size());
		assertEquals("Vorderdeckel", children.get(0).getLabel());
		assertEquals(1, children.get(0).getPages().size());
		assertEquals("Kupfertitel", children.get(1).getLabel());
		assertEquals(6, children.get(1).getPages().size());

		// level 1+2
		assertEquals("Liber Primus,", children.get(5).getLabel());
		assertFalse(children.get(5).getChildren().isEmpty());
		// dropped from formerly "207"(!)
		// to just "2" because removal of duplicate
		// structure links
		assertEquals(2, children.get(5).getChildren().size());

		// level 1+2+3
		assertEquals(
				"Continuatio Historiae Ecclesiasticae Iohannis Micraelii, Secunda Hac Editione Emendata & plurimis locis aucta à Daniele Hartnaccio, Pomerano.",
				children.get(9).getLabel());
		// changed due removal of structure link duplicates
		// from "1246"(!) to just "5"
		assertEquals(5, children.get(9).getChildren().size());
		assertEquals(1, children.get(9).getChildren().get(1).getChildren().size());
	}

}
