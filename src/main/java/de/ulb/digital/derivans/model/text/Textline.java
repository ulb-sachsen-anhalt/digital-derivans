package de.ulb.digital.derivans.model.text;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import de.ulb.digital.derivans.model.ITextElement;
import de.ulb.digital.derivans.model.IVisualElement;

/**
 * 
 * Represents a single line of textual tokens that form a visual unit.
 * 
 * @author u.hartwig
 *
 */
public class Textline implements IVisualElement, ITextElement {
	
	private List<Word> textTokens = new ArrayList<>();

	private String actualText = "";
	
	private Area area;

	private Rectangle2D rect;

	public Textline() {
	}
	
	public Textline(List<Word> texts) {
		for (Word w : texts) {
			w.setParent(this);
		}
		this.textTokens.addAll(texts);
		this.calculateArea();
		this.actualText = String.join(" ",
				texts.stream().map(ITextElement::getText).filter(Objects::nonNull).collect(Collectors.toList()));
	}

	public void calculateArea() {
		List<Rectangle2D> wordBoxes = textTokens.stream().map(IVisualElement::getBox).collect(Collectors.toList());
		this.area = new Area();
		for (Rectangle2D r : wordBoxes) {
			this.area.add(new Area(r));
		}
	}

	public String getText() {
		return this.actualText;
	}

	public Area getArea() {
		return this.area;
	}

	@Override
	public Rectangle2D getBox() {
		if (this.rect != null) {
			return this.rect;
		}
		return this.area.getBounds2D();
	}

	@Override
	public void setBox(Rectangle2D rectangle) {
		this.rect = rectangle;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		var shape = this.area.getBounds();
		double w = shape.getWidth();
		double h = shape.getHeight();
		builder.append('[').append(w).append('x').append(h).append(']').append(this.actualText);
		return builder.toString();
	}

	public void add(Word textElement) {
		this.textTokens.add(textElement);
	}

	public List<Word> getWords() {
		return this.textTokens;
	}

}