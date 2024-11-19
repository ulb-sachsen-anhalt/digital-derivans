package de.ulb.digital.derivans.model.text;

import java.awt.geom.Rectangle2D;
import java.util.List;

import de.ulb.digital.derivans.model.IVisualElement;
import de.ulb.digital.derivans.model.ITextElement;
import de.ulb.digital.derivans.model.pdf.PDFTextElementType;

/**
 * 
 * Single visual textual token, i.e.:
 * word, number, abbreviation ...
 * 
 * @author u.hartwig
 *
 */
public class Word implements ITextElement, IVisualElement {

	protected PDFTextElementType type = PDFTextElementType.TOKEN;
	
	protected ITextElement parent;

	protected List<ITextElement> children;
	
	protected Rectangle2D rect;
	
	protected String actualText = "";

	public Word(String actualText) {
		this(actualText, new Rectangle2D.Float());
	}

	public Word(String actualText, Rectangle2D box) {
		this.actualText = actualText;
		this.rect = box;
	}

	public Word(String actualText, Rectangle2D.Float box) {
		this.actualText = actualText;
		this.rect = box;
	}

	public String getText() {
		return this.actualText;
	}

	public Rectangle2D getBox() {
		return this.rect;
	}

	public float getHeight() {
		return (float)this.rect.getHeight();
	}

	public boolean hasPrintableChars() {
		return (this.actualText != null) && (!this.actualText.isBlank());
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

	public void setParent(Textline parent) {
		this.parent = parent;
	}

	@Override
	public void setBox(Rectangle2D rectangle) {
		this.rect = rectangle;
	}

}