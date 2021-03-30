package de.ulb.digital.derivans.model.ocr;

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 
 * Domain specific model of OCR-Data organized as lists of {@link Textline lines} 
 * which are composed of a list of {@link Text textual tokens}. 
 * 
 * @author u.hartwig
 *
 */
public class OCRData {

	private List<Textline> textlines;

	public OCRData(List<Textline> lines) {
		this.textlines = lines;
	}

	public List<Textline> getTextlines() {
		return this.textlines;
	}

	public static class Textline {
		private List<Text> tokens;
		private String text;
		private Shape polygon;

		public Textline(List<Text> texts) {
			this.tokens = texts;
			Optional<Rectangle> optRect = texts.stream().map(Text::getBox).reduce((p, c) -> {
				p.add(c);
				return p;
			});
			if (optRect.isPresent()) {
				this.polygon = optRect.get();
			}
			this.text = String.join(" ",
					texts.stream().map(Text::getText).filter(Objects::nonNull).collect(Collectors.toList()));
		}

		public String getText() {
			return this.text;
		}
		
		public Shape getShape() {
			return this.polygon;
		}
		
		public List<Text> getTokens() {
			return new ArrayList<>(this.tokens);
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			var shape = this.polygon.getBounds();
			Double w = Double.valueOf(shape.getWidth());
			Double h = Double.valueOf(shape.getHeight());
			builder.append('[').append(w).append('x').append(h).
			append(']').append(this.text);
			return builder.toString();
		}
	}

	public static class Text {
		private String text;
		private Rectangle rect;

		public Text(String text, Rectangle box) {
			this.text = text;
			this.rect = box;
		}

		public String getText() {
			return this.text;
		}

		public Rectangle getBox() {
			return this.rect;
		}

		public float getHeight() {
			return this.rect.height;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			var oldString = super.toString(); 
			var hash = oldString.substring(oldString.indexOf('@')+1);
			Double w = Double.valueOf(rect.getWidth());
			Double h = Double.valueOf(rect.getHeight());
			builder.append(hash).append('[').append(w).append('x').append(h).
			append(']').append(text);
			return builder.toString();
		}
	}
}
