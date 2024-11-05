package de.ulb.digital.derivans.model.text;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import de.ulb.digital.derivans.model.ICharacterToken;

/**
 * 
 * Represents a single line of textual tokens that form a visual unit.
 * 
 * @author u.hartwig
 *
 */
public class Textline implements ICharacterToken {
	
	private List<Word> textTokens = new ArrayList<>();
	
	private String actualText = "";
	
	private Area area;

	public Textline() {
	}
	
	public Textline(List<Word> texts) {
		this.textTokens = texts;
		this.calculateArea();
		this.actualText = String.join(" ",
				texts.stream().map(ICharacterToken::getText).filter(Objects::nonNull).collect(Collectors.toList()));
	}

	public void calculateArea() {
		List<Rectangle> wordBoxes = textTokens.stream().map(ICharacterToken::getBox).collect(Collectors.toList());
		this.area = new Area();
		for (Rectangle r : wordBoxes) {
			this.area.add(new Area(r));
		}
	}

	public String getText() {
		return this.actualText;
	}

	public Area getArea() {
		return this.area;
	}

	public Rectangle getBox() {
		return this.area.getBounds();
	}

	public List<ICharacterToken> getTokens() {
		return new ArrayList<>(this.textTokens);
	}

	public String getLabel() {
		return "TextLine";
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

	@Override
	public void scale(float ratio) {
		// TODO Auto-generated method stub
		
	}
}