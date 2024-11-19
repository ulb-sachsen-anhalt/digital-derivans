package de.ulb.digital.derivans.model.pdf;

import de.ulb.digital.derivans.DigitalDerivansException;

/**
 * 
 * Define PDF Elements carrying textual content.
 * 
 * @author u.hartwig
 * 
 */
public enum MetadataType {

	AUTHOR("Author"),

	CREATOR("Creator"),
	
	TITLE("Title"),

	DESCRIPTION("Description"),

	KEYWORDS("Keywords"),
	;

	private String label;

	private MetadataType(String label) {
		this.label = label;
	}
	
	public static MetadataType forLabel(String label) throws DigitalDerivansException {
		for(var elem : MetadataType.values()) {
			if (elem.label.equalsIgnoreCase(label)) {
				return elem;
			}
		}
		throw new DigitalDerivansException("Unknown PDF MetadataType : " + label);
	}
}
