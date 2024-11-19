package de.ulb.digital.derivans.config;

import de.ulb.digital.derivans.DigitalDerivansException;

/**
 * 
 * Define configuration option types how to render
 * textual PDF content for internal usage.
 * 
 * @author u.hartwig
 * 
 */
public enum TypeConfiguration {

	RENDER_MODUS_DBUG("debug"),

	RENDER_MODUS_HIDE("invisible"),
	
	RENDER_LEVEL_LINE("line"),

	RENDER_LEVEL_WORD("word"),

	;

	private String label;

	private TypeConfiguration(String label) {
		this.label = label;
	}

	public static TypeConfiguration get(String label) throws DigitalDerivansException {
		for(var elem : values()) {
			if (elem.label.equalsIgnoreCase(label)) {
				return elem;
			}
		}
		throw new DigitalDerivansException("Unknown Type: " + label);
	}
}
