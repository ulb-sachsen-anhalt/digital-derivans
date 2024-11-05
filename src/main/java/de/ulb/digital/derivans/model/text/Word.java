package de.ulb.digital.derivans.model.text;

import java.awt.Rectangle;

import de.ulb.digital.derivans.model.ICharacterToken;

/**
 * 
 * Single textual token, i.e. word, abbreviation or alike
 * spanning a rectangular area
 * 
 * @author u.hartwig
 *
 */
public class Word implements ICharacterToken {
	private String actualText;
	Rectangle rect;

	public Word(String actualText) {
		this(actualText, new Rectangle());
	}

	public Word(String actualText, Rectangle box) {
		this.actualText = actualText;
		this.rect = box;
	}

	public String getText() {
		return this.actualText;
	}

	public Rectangle getBox() {
		return this.rect.getBounds();
	}

	public void setBox(Rectangle rectangle) {
		this.rect = rectangle;
	}

	public float getHeight() {
		return this.rect.height;
	}

	public boolean hasPrintableChars() {
		return (this.actualText != null) && (!this.actualText.isBlank());
	}
	
	@Override
	public void scale(float ratio) {
		this.rect.x = Math.round(rect.x * ratio);
		this.rect.y = Math.round(rect.y * ratio);
		this.rect.width = Math.round(rect.width * ratio);
		this.rect.height = Math.round(rect.height * ratio);
	}

	public String getLabel() {
		return "Word";
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		var oldString = super.toString();
		var hash = oldString.substring(oldString.indexOf('@') + 1);
		double w = rect.getWidth();
		double h = rect.getHeight();
		builder.append(hash).append('[').append(w).append('x').append(h).append(']').append(actualText);
		return builder.toString();
	}
}