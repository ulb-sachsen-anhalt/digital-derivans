package de.ulb.digital.derivans.model.pdf;

import java.awt.Rectangle;

import de.ulb.digital.derivans.model.text.Word;

/**
 * 
 * Represents rendering unit, which can a single word or a
 * complete line, depending on used render level 
 * 
 * Please note, that render origin is located at (0,MAX)
 * (lower left) instead of screen origin (0,0) (upper left)
 * 
 * @author hartwig
 */
public class PDFElement extends Word {

	private float fontSize;

	private float leading;

	private float lowerLeftX;

	private float lowerLeftY;

	private float upperRightX;

	private float upperRightY;

	private boolean isPrinted;
	
	public PDFElement(String actualText) {
		this(actualText, new Rectangle());
	}

	public PDFElement(String actualText, Rectangle box) {
		super(actualText, box);
	}

	public float getFontSize() {
		return fontSize;
	}

	public void setFontSize(float fontSize) {
		this.fontSize = fontSize;
	}

	public float getLeading() {
		return this.leading;
	}

	public void setLeading(float leading) {
		this.leading = leading;
	}

	public boolean isPrinted() {
		return this.isPrinted;
	}

	public void setPrinted(boolean state) {
		this.isPrinted = state;
	}
}
