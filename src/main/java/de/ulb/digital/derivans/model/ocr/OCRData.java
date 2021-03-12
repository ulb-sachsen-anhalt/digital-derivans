package de.ulb.digital.derivans.model.ocr;

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 
 * @author u.hartwig
 *
 */
public class OCRData {

	private List<Textline> textlines;
	private String identifier;

	public OCRData(String identifier, List<Textline> lines) {
		this.identifier = identifier;
		this.textlines = lines;
	}

	public OCRData(List<Textline> lines) {
		this("n.a.", lines);
	}

	public String getIdentifier() {
		return this.identifier;
	}

	public List<Textline> getTextlines() {
		return this.textlines;
	}

	public static class Textline {
		private String identifier;
		private List<Text> texts;
		private Shape polygon;

		public Textline(String identifier, List<Text> texts) {
			this.identifier = identifier;
			this.texts = texts;
			Optional<Rectangle> optRect = texts.stream().map(Text::getBox).reduce((p, c) -> {
				p.add(c);
				return p;
			});
			if (optRect.isPresent()) {
				this.polygon = optRect.get();
			}
		}

		public Textline(List<Text> texts) {
			this(null, texts);
		}

		public String getIdentifier() {
			return this.identifier;
		}

		public Text getLine() {
			String text = String.join(" ",
					texts.stream().map(Text::getText).filter(Objects::nonNull).collect(Collectors.toList()));
			return new Text(text, this.polygon.getBounds());
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
	}
}
