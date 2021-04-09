package de.ulb.digital.derivans.data;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;

import javax.xml.XMLConstants;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.mets.model.Mets;

import de.ulb.digital.derivans.Derivans;

/**
 * 
 * Handle Metadata on Path-and DOM-Level
 * 
 * @author hartwig
 *
 */
public class MetadataHandler {

	private static final Namespace NS_METS = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");

	private DateTimeFormatter dtFormatter = new DateTimeFormatterBuilder().appendPattern("YYYY-MM-DD")
			.appendLiteral('T').appendPattern("HH:mm:SS").toFormatter();

	private Path pathFile;

	private Document document;

	public MetadataHandler(Path pathFile) {
		this.pathFile = pathFile;
	}

	public Mets read() throws JDOMException, IOException {
		File f = new File(this.pathFile.toString());
		SAXBuilder builder = new SAXBuilder();
		// please sonarqube "Disable XML external entity (XXE) processing"
		builder.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		document = builder.build(f);
		return new Mets(document);
	}

	public boolean write() {
		try (OutputStream metsOut = Files.newOutputStream(this.pathFile)) {
			XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
			xout.output(document, metsOut);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public String enrichAgent(String fileId) {
		Element agent = createAgentSection(fileId);
		Element hdrSection = getMetsHdr();
		hdrSection.addContent(agent);
		return agent.getChildText("note", NS_METS);
	}

	private Element createAgentSection(String fileId) {
		Element agent = new Element("agent", NS_METS);
		agent.setAttribute("TYPE", "OTHER");
		agent.setAttribute("ROLE", "OTHER");
		agent.setAttribute("OTHERTYPE", "SOFTWARE");
		Element agentName = new Element("name", NS_METS);
		agentName.setText(Derivans.LABEL);
		Element agentNote = new Element("note", NS_METS);
		String ts = LocalDateTime.now().format(dtFormatter);
		String agentNoteText = "PDF FileGroup for " + fileId + " created at " + ts;
		agentNote.setText(agentNoteText);
		agent.addContent(List.of(agentName, agentNote));
		return agent;
	}

	public Path getPath() {
		return pathFile;
	}

	/**
	 * 
	 * Get metsHdr for given Metadata Document or create new one, if not existing.
	 * 
	 * Since mets-model library wipes existing metsHdr-Information, this information
	 * must be kept otherwise
	 * 
	 * @return
	 */
	private Element getMetsHdr() {
		List<Content> cs = document.getContent();
		Element r = (Element) cs.get(0);
		List<Element> es = r.getChildren();
		for (Element e : es) {
			if (e.getName().equalsIgnoreCase("metsHdr")) {
				return e;
			}
		}

		Element hdrSection = new Element("metsHdr", NS_METS);
		hdrSection.setAttribute("CREATEDATE", Instant.now().toString());
		this.document.addContent(hdrSection);

		return hdrSection;
	}

	public void addFileGroup(Element asElement) {
		var cs = document.getContent();
		Element r = (Element) cs.get(0);
		r.getChildren("fileSec", NS_METS).get(0).addContent(asElement);
	}

	/**
	 * 
	 * Add single element to topLevel METS-element identified by given valueType
	 * 
	 * @param asElement
	 * @param typeValue
	 * @param first
	 */
	public void addTo(Element asElement, String typeValue, boolean reorder) {
		var elements = document.getContent(new ElementFilter());
		var optElement = elements.stream()
				.map(Element::getChildren)
				.flatMap(List::stream)
				.filter(el -> el.getAttribute("TYPE") != null)
				.filter(el -> el.getAttribute("TYPE").getValue().equals(typeValue))
				.findFirst();
		
		if(optElement.isPresent()) {
			Element element = optElement.get();
			Element container = element.getChild("div", NS_METS);
			
			// create new list with element as first entry
			container.addContent(asElement);
			
			// ATTENTION: the specific goal to reorder is inversion 
			// to ensure mets:fptr is *before* any subsequent mets:divs 
			if(reorder) {
				container.sortChildren((el1, el2) -> Math.negateExact(el1.getName().compareToIgnoreCase(el2.getName())));
			}
		}
	}
	
	/**
	 * 
	 * Rather hacky way to catch first top-level volume DMDID mapping
	 * 
	 * @param typeValue
	 * @return
	 */
	public String requestDMDSubDivIDs() {
		var elements = document.getContent(new ElementFilter());
		var optDMDID = elements.stream()
				.map(Element::getChildren)
				.flatMap(List::stream)
				.filter(el -> "LOGICAL".equals(el.getAttributeValue("TYPE")))
				.map(Element::getChildren)
				.flatMap(List::stream)
				.map(Element::getChildren)
				.flatMap(List::stream)
				.filter(el -> "volume".equals(el.getAttributeValue("TYPE")))
				.map(el -> el.getAttributeValue("DMDID"))
				.findFirst();
		
		if(optDMDID.isPresent()) {
			return optDMDID.get();
		}
		return null;
	}
}
