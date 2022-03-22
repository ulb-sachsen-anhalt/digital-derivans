package de.ulb.digital.derivans.data;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingInt;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;

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
import org.mycore.mets.model.sections.DmdSec;
import org.mycore.mets.model.struct.SmLink;

import de.ulb.digital.derivans.Derivans;
import de.ulb.digital.derivans.DigitalDerivansException;

/**
 * 
 * Handle Metadata on Path-and DOM-Level
 * 
 * @author hartwig
 *
 */
public class MetadataHandler {

	public static final Namespace NS_METS = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");
	public static final Namespace NS_MODS = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");

	public static DateTimeFormatter MD_DT_FORMAT = new DateTimeFormatterBuilder().appendPattern("YYYY-MM-dd")
			.appendLiteral('T').appendPattern("HH:mm:SS").toFormatter();

	private Path pathFile;

	private Document document;

	private Mets mets;

	private Element primaryMods;

	public MetadataHandler(Path pathFile) throws DigitalDerivansException {
		this.pathFile = pathFile;
		try {
			this.mets = read();
			this.primaryMods = setPrimaryMods();
		} catch (JDOMException | IOException | IllegalArgumentException | DigitalDerivansException e) {
			throw new DigitalDerivansException(e);
		}
	}

	public Mets getMets() {
		return this.mets;
	}

	private Mets read() throws JDOMException, IOException, IllegalArgumentException {
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
		agentName.setText(this.getLabel());
		Element agentNote = new Element("note", NS_METS);
		String ts = LocalDateTime.now().format(MD_DT_FORMAT);
		String agentNoteText = "PDF FileGroup for " + fileId + " created at " + ts;
		agentNote.setText(agentNoteText);
		agent.addContent(List.of(agentName, agentNote));
		return agent;
	}

	/**
	 * 
	 * Read version label from properties-file if exists, otherwise use hard-coded
	 * default value
	 * 
	 * @return
	 */
	public String getLabel() {
		return readRevisionProperties().orElse(Derivans.LABEL);
	}

	
	public Element getPrimaryMods() {
		return this.primaryMods;
	}
	
	private Optional<String> readRevisionProperties() {
		String fileName = "derivans-git.properties";
		ClassLoader classLoader = getClass().getClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream(fileName);
		StringBuilder resultStringBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.strip().length() > 3) {
					resultStringBuilder.append(line);
				}
			}
			String contents = resultStringBuilder.toString();
			String[] parts = contents.split(":");
			if (parts.length > 1) {
				String version = parts[1].replace('"', ' ').strip();
				return Optional.of(Derivans.LABEL + " V" + version);
			}
		} catch (IOException e) {
			Derivans.LOGGER.warn("cannot read {}", fileName);
		}
		return Optional.empty();
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
		var elements = document.getDescendants(new MetsDivFilter());
		String id = primaryMods.getParentElement().getParentElement().getParentElement().getAttributeValue("ID");
		Element logDiv = null;
		for (Element e : elements) {
			if (id.equals(e.getAttributeValue("DMDID")))
				logDiv = e;
		}
		if (logDiv != null) {
			logDiv.addContent(asElement);
			// ATTENTION: the specific goal to re-order is inversion
			// to ensure mets:fptr is *before* any subsequent mets:divs
			if (reorder) {
				logDiv.sortChildren((el1, el2) -> Math.negateExact(el1.getName().compareToIgnoreCase(el2.getName())));
			}
		}
	}

	/**
	 * 
	 * Catch logical sub-containers with attribute DMDID -> must have a descriptive
	 * MODS section
	 * 
	 * @param typeValue
	 * @return
	 */
	public List<Element> requestLogicalSubcontainers() {
		Element logRoot = extractStructLogicalRoot();
		IteratorIterable<Element> iter = logRoot.getDescendants(new LogSubContainers());
		List<Element> subConainers = new ArrayList<>();
		for (Element el : iter) {
			subConainers.add(el);
		}
		return subConainers;
	}

	private Element extractStructLogicalRoot() {
		var elements = document.getContent(new ElementFilter());
		return elements.stream().map(Element::getChildren).flatMap(List::stream)
				.filter(el -> "LOGICAL".equals(el.getAttributeValue("TYPE"))).findFirst().get();
	}

	private Element setPrimaryMods() throws DigitalDerivansException {
		String dmdId = getDescriptiveMetadataIdentifierOfInterest();
		DmdSec primaryDMD = mets.getDmdSecById(dmdId);
		if (primaryDMD == null) {
			throw new DigitalDerivansException("can't identify primary MODS section");
		}
		Iterable<Element> iter = primaryDMD.asElement().getDescendants(new ModsFilter());
		return iter.iterator().next();
	}

	/**
	 * 
	 * Resolve identifier for descriptive section backwards. Starting from Physical
	 * Top-Level container of MAX filegroup, pick the most reasonable link and use
	 * this to identify the proper logical structure.
	 * 
	 * TODO review mets-model conversions necessary since mets-model doesn't read
	 * properly all Element's Attributes like "DMDID"
	 * 
	 * @return
	 */
	private String getDescriptiveMetadataIdentifierOfInterest() {
		// get most probably link from structMapping section
		String logId = getLogicalLinkFromStructMapping();

		// get logical divisions as raw elements (see above)
		List<Element> logSubcontainers = this.requestLogicalSubcontainers();

		for (Element e : logSubcontainers) {
			if (e.getAttributeValue("ID").equals(logId)) {
				return e.getAttributeValue("DMDID");
			}
		}
		return null;
	}

	/**
	 * 
	 * If the structMap links contain a mapping to "physroot", then this is
	 * considered to link the logical struct's corresponding to the main logical
	 * structure (VLS-like) OR count strctMap-links from logical to physical, and
	 * identify the one containing most links / as many links as there are physical
	 * containers (Kitodo2-like)
	 * 
	 * In both cases, follow the link's origin to the logical map.
	 * 
	 * @return
	 */
	private String getLogicalLinkFromStructMapping() {
		List<SmLink> smLinksToPhysRoot = mets.getStructLink().getSmLinkByTo("physroot");
		if (!smLinksToPhysRoot.isEmpty()) {
			return smLinksToPhysRoot.get(0).getFrom();
		} else {
			List<SmLink> links = mets.getStructLink().getSmLinks();
			// map SmLinks to their @from-count
			Map<String, Integer> mapToN = links.stream().collect(groupingBy(SmLink::getFrom, summingInt(n -> 1)));
			// get identifier of SmLink with MAX counts
			var optMaxLink = mapToN.entrySet().stream().max(Comparator.comparingInt(Entry::getValue));
			if (optMaxLink.isPresent()) {
				var maxMapping = optMaxLink.get();
				return maxMapping.getKey();
			}
		}
		return null;
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

	private static final long serialVersionUID = 1L;

	@Override
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
		}
		return null;
	}

}

/**
 * 
 * Get the real mods:mods elements
 * 
 * @author u.hartwig
 *
 */
class ModsFilter extends ElementFilter {

	private static final long serialVersionUID = 1L;

	@Override
	public Element filter(Object content) {
		if (content instanceof Element) {
			Element el = (Element) content;
			if ("mods".equals(el.getName())) {
				return el;
			}
		}
		return null;
	}

}

class MetsDivFilter extends ElementFilter {

	private static final long serialVersionUID = 1L;

	@Override
	public Element filter(Object content) {
		if (content instanceof Element) {
			Element el = (Element) content;
			if ("div".equals(el.getName())) {
				return el;
			}
		}
		return null;
	}

}
