package de.ulb.digital.derivans.model.ocr;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 
 * Domain specific model of OCR-Data organized as lists of {@link Textline lines} 
 * which are itself composed each of a list of {@link Text textual tokens}. 
 * 
 * @author u.hartwig
 *
 */
public class OCRData {
	
	private Dimension dimension;

	private List<Textline> textlines;

	public OCRData(List<Textline> lines, Dimension dim) {
		this.textlines = lines;
		this.dimension = dim;
	}

	public List<Textline> getTextlines() {
		return this.textlines;
	}
	
	/**
	 * 
	 * The Height of the original image which passed actual OCR-creation.
	 * Need to remember, since of derivans scaling abilities the actual 
	 * image from which the PDF later on is being created <b>will be different</b>.
	 * 
	 * @return
	 */
	public int getPageHeight() {
		return dimension.height;
	}

	public void scale(float ratio) {
		for(Textline line : textlines) {
			for(Text txt : line.getTokens()) {
				txt.rect.x = Math.round(txt.rect.x * ratio);
				txt.rect.y = Math.round(txt.rect.y * ratio);
				txt.rect.width = Math.round(txt.rect.width * ratio);
				txt.rect.height = Math.round(txt.rect.height * ratio);
			}
			line.calculateArea();
		}
		int widht = Math.round(dimension.width * ratio);
		int height = Math.round(dimension.height * ratio);
		this.dimension = new Dimension(widht, height);
	}
	
	/**
	 * 
	 * Represents a single line of textual tokens that form a conceptual unit.
	 * 
	 * @author u.hartwig
	 *
	 */
	public static class Textline {
		private List<Text> texts;
		private String actualText;
		private Area area;

		public Textline(List<Text> texts) {
			this.texts = texts;
			this.calculateArea();
			this.actualText = String.join(" ",
					texts.stream().map(Text::getText).filter(Objects::nonNull).collect(Collectors.toList()));
		}

		private void calculateArea() {
			List<Rectangle> boxes = texts.stream().map(Text::getBox).collect(Collectors.toList());
			this.area = new Area();
			for(Rectangle r : boxes) {
				this.area.add(new Area(r));
			}
		}
		
		public String getText() {
			return this.actualText;
		}
		
		public Area getArea() {
			return this.area;
		}
		
		public Rectangle getBounds() {
			return this.area.getBounds();
		}
		
		public List<Text> getTokens() {
			return new ArrayList<>(this.texts);
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			var shape = this.area.getBounds();
			Double w = Double.valueOf(shape.getWidth());
			Double h = Double.valueOf(shape.getHeight());
			builder.append('[').append(w).append('x').append(h).
			append(']').append(this.actualText);
			return builder.toString();
		}
	}

	/**
	 * 
	 * Single textual token, i.e. word, abbreviation or alike.
	 * 
	 * @author u.hartwig
	 *
	 */
	public static class Text {
		private String actualText;
		private Rectangle rect;

		public Text(String actualText, Rectangle box) {
			this.actualText = actualText;
			this.rect = box;
		}

		public String getText() {
			return this.actualText;
		}

		public Rectangle getBox() {
			return this.rect.getBounds();
		}

		public float getHeight() {
			return this.rect.height;
		}
		
		public boolean hasPrintableChars() {
			return (this.actualText != null) && (!this.actualText.isBlank());
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			var oldString = super.toString(); 
			var hash = oldString.substring(oldString.indexOf('@')+1);
			Double w = Double.valueOf(rect.getWidth());
			Double h = Double.valueOf(rect.getHeight());
			builder.append(hash).append('[').append(w).append('x').append(h).
			append(']').append(actualText);
			return builder.toString();
		}
	}
}
