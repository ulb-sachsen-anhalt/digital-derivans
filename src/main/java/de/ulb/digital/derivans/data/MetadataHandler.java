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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
import org.jdom2.util.IteratorIterable;
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

	public static final Namespace NS_METS = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");

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
			
			// ATTENTION: the specific goal to re-order is inversion 
			// to ensure mets:fptr is *before* any subsequent mets:divs 
			if(reorder) {
				container.sortChildren((el1, el2) -> Math.negateExact(el1.getName().compareToIgnoreCase(el2.getName())));
			}
		}
	}
	
	/**
	 * 
	 * Rather hacky way to catch first sub-level volume DMDID mapping
	 * 
	 * @param typeValue
	 * @return
	 */
	public String requestDMDSubDivIDs() {
		var elements = document.getContent(new ElementFilter());
		List<Element> dmdElements = elements.stream()
				.map(Element::getChildren)
				.flatMap(List::stream)
				.filter(el -> "LOGICAL".equals(el.getAttributeValue("TYPE")))
				.collect(Collectors.toList());
		if(dmdElements.isEmpty()) {
			return null;
		} else {
			Element logRoot = dmdElements.get(0);
			IteratorIterable<Element> iter = logRoot.getDescendants(new LogSubContainers());
			List<Element> dmdIds = new ArrayList<>();
			for (Element el : iter) {
				dmdIds.add(el);
			}
			// easy Kitodo 2 MVW
			if (dmdIds.get(0).getAttributeValue("DMDID").startsWith("DMDLOG_")) {
				return "DMDLOG_0001";
			}
			
			// rather tricky semantics mappings
			List<Element> children = new ArrayList<>();
			for (Element el : dmdIds) {
				Element elParent = el.getParentElement();
				String dmdIDParent = elParent.getAttributeValue("DMDID");
				// if we have the upper most, skip it
				if (dmdIDParent == null) {
					continue;
				}
				for (Element el2 : dmdIds) {
					String dmdIDChild = el2.getAttributeValue("DMDID");
					// dont pick same element
					if(dmdIDParent.equals(dmdIDChild)) {
						continue;
					}
					children.add(el2);
				}
			}
			return children.get(0).getAttributeValue("DMDID");
		}
	}
}

/**
 * 
 * Get all Descendants with attribute "DMDID"
 * 
 * @author u.hartwig
 *
 */
class LogSubContainers extends ElementFilter {
	
	String name = "div";
	Namespace namespace = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public Element filter(Object content) {
		if (content instanceof Element) {
			Element el = (Element) content;
			if (!name.equals(el.getName())) {
				return null;
			}
			if (!namespace.equals(el.getNamespace())) {
				return null;
			}
			boolean hasDMDID = el.getAttribute("DMDID") != null;
			if (hasDMDID) {
				return el;
			}
//			if (!hasDMDID) {
//				// but maybe has ID starting Kitodo-like?
//				String attrValID = el.getAttributeValue("ID");		
//				return attrValID.startsWith("LOG_") ? el : null;
//			}
		}
		return null;
	}
	
}
