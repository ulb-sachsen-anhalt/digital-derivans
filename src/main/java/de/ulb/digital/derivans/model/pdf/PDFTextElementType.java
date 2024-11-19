package de.ulb.digital.derivans.model.pdf;

import de.ulb.digital.derivans.DigitalDerivansException;

/**
 * 
 * Define PDF Elements carrying textual content.
 * 
 * @author u.hartwig
 * 
 */
public enum PDFTextElementType {

	REGION(new String[]{"region"}),

	IMAGE(new String[]{"image"}),

	LINE(new String[]{"line"}),

	TOKEN(new String[] {"token", "word"}),
	
	GLYPH(new String[] {"glyph", "character", "char"});

	private String[] labels;

	private PDFTextElementType(String[] labels) {
		this.labels = labels;
	}
	
	public static PDFTextElementType label(String label) throws DigitalDerivansException {
		for(var elem : values()) {
			for(var theLabel : elem.labels) {
				if (theLabel.equalsIgnoreCase(label)) {
					return elem;
				}
			}
		}
		throw new DigitalDerivansException("Unknown PDFElementType : " + label);
	}
}
