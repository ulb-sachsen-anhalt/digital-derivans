package de.ulb.digital.derivans.data.mets.parse;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.filter.AbstractFilter;

/**
 * 
 * Custom Filter for METS Element Attributes
 * 
 * @author u.hartwig
 */
public class METSElementAttributeFilter extends AbstractFilter<Element> {

	private String elementName;
	private String attributeName;
	private String attributeValue;

	/**
		 * Create a filter for elements with specific name and attribute in a namespace
		 * 
		 * @param elementName   The name of the element to filter
		 * @param attributeName The name of the attribute that must be present
		 * @param namespace     The namespace of the attribute (can be null for no
		 *                      namespace)
		 */
		public METSElementAttributeFilter(String elementName, String attributeName, String value) {
			this.elementName = elementName;
			this.attributeName = attributeName;
			this.attributeValue = value;
		}

	@Override
	public Element filter(Object content) {
		if (content instanceof Element) {
			Element element = (Element) content;
			// First check element name
			if (element.getName().equals(elementName)) {
				// Then check for attribute and value
				Attribute attr = element.getAttribute(attributeName);
				if (attr != null && attr.getValue().equals(attributeValue)) {
					return element;
				}
			}
		}
		return null;
	}
}
