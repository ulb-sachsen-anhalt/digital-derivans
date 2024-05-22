package de.ulb.digital.derivans.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.ElementFilter;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.util.IteratorIterable;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathFactory;

import de.ulb.digital.derivans.DigitalDerivansException;

/**
 * 
 * Basic XML handling for any kind of
 * Metadata processing
 * 
 * @author u.hartwig
 * 
 */
public class XMLHandler {

	protected Path pathFile;
	protected final Document document;

	public XMLHandler(byte[] bytes) throws DigitalDerivansException {
		this.document = this.parseBytes(bytes);
	}

	public XMLHandler(Path pathFile) throws DigitalDerivansException {
		this.pathFile = pathFile;
		try {
			this.document = this.parseBytes(Files.readAllBytes(pathFile));
		} catch (IOException e) {
			throw new DigitalDerivansException(e);
		}
	}

	private Document parseBytes(byte[] bytes) throws DigitalDerivansException {
		SAXBuilder builder = new SAXBuilder();
		// please sonarqube "Disable access to external entities in XML parsing"
		builder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		builder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		try {
			return builder.build(new ByteArrayInputStream(bytes));
		} catch (JDOMException | IOException e) {
			throw new DigitalDerivansException(e);
		}
	}

	public Document getDocument() {
		return this.document;
	}

	public Path getFilePath() {
		return this.pathFile;
	}

	public List<Element> extractElements(String elementName) {
		ElementFilter filter = new ElementFilter(elementName);
		List<Element> elements = new ArrayList<>();
		IteratorIterable<Element> elemIt = document.getDescendants(filter);
		while (elemIt.hasNext()) {
			elements.add(elemIt.next());
		}
		return elements;
	}

	/**
	 * Evaluate XPath expressions on underlying {@link Document document instance}
	 * 
	 * @param xpathStr
	 * @return {@link Element}
	 * @throws DigitalDerivansException If any internal Exceptions occour
	 */
	public Element evaluateFirst(String xpathStr, List<Namespace> namespaces) throws DigitalDerivansException {
		try {
			XPathBuilder<Element> builder = new XPathBuilder<>(xpathStr, Filters.element());
			for (Namespace ns : namespaces) {
				builder.setNamespace(ns);
			}
			var xpr = builder.compileWith(XPathFactory.instance());
			return xpr.evaluateFirst(this.document);
		} catch (Exception exc) {
			throw new DigitalDerivansException(exc.getMessage());
		}
	}

	public boolean write(Path pathFile) {
		try (OutputStream outStream = Files.newOutputStream(pathFile)) {
			XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
			xout.output(this.document, outStream);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
}
