package de.ulb.digital.derivans.model.pdf;

import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import de.ulb.digital.derivans.model.IVisualElement;
import de.ulb.digital.derivans.model.ITextElement;

/**
 * 
 * Represents a rendering unit, which may stand
 * for a single word/token or complete line.
 * 
 * Please note, that render origin is located at (0,MAX)
 * (lower left) instead of screen origin (0,0) (upper left).
 * 
 * Depending on it's position may be related upwards to
 * parent Element or may contain sub-Elements (children).
 * 
 * @author hartwig
 */
public class PDFTextElement implements ITextElement, IVisualElement {

	// relative distance between baseline and descent line
	// within the spanning rectangular area
	public static final float DESCENT_RATIO = .25f;

	// Important to mark end of actual token for heavy ligated
	// script fonts like arabic or farsi
	// cf. https://en.wikipedia.org/wiki/Zero-width_space
	public static final char ZERO_WIDTH = '\u200b';

	private Rectangle2D box;

	private PDFTextElement.Baseline baseline;

	private String text;

	private PDFTextElement parent;

	private List<PDFTextElement> children = new ArrayList<>();

	private boolean isPrinted;

	public PDFTextElement() {
	}

	public PDFTextElement(List<PDFTextElement> children) {
		children.forEach(this::add);
	}

	public PDFTextElement(String actualText) {
		this(actualText, new Rectangle2D.Float());
	}

	public PDFTextElement(String actualText, Rectangle2D box) {
		this.text = actualText;
		this.box = box;
	}

	private float descent() {
		return (float) this.box.getHeight() * DESCENT_RATIO;
	}

	/**
	 * 
	 * Regular Fontsize must fit into box height minus descent
	 * 
	 * @return
	 */
	public float getFontSize() {
		return (float) this.box.getHeight() - descent();
	}

	/**
	 * 
	 * Calculate straight linear baseline with respect to descent
	 * 
	 * @return
	 */
	public PDFTextElement.Baseline getBaseline() {
		if (this.baseline == null) {
			var baselineY = (float) this.box.getMinY() + descent();
			var toTheLeft = (float) (this.box.getX() + this.box.getWidth());
			this.baseline = new PDFTextElement.Baseline((float) this.box.getX(), baselineY, toTheLeft, baselineY);
		}
		return this.baseline;
	}

	/**
	 * 
	 * Invert Y-axis with respect to provided page height = y.MAX
	 * to fit the printing coordinate system using origin (0, MAX)
	 * left-bottom instead of usual on-screen (0,0)
	 * 
	 * @param actualPageHeight
	 */
	public void invert(float actualPageHeight) {
		var oldBox = this.getBox();
		var newY = actualPageHeight - oldBox.getY() - oldBox.getHeight();
		this.setBox(new Rectangle2D.Float((float) oldBox.getX(), (float) newY,
				(float) oldBox.getWidth(), (float) oldBox.getHeight()));
	}

	/**
	 * 
	 * Whether this object has been printed or any of it's descendants
	 * 
	 * @return
	 */
	public boolean isPrinted() {
		if (!this.children.isEmpty()) {
			return this.children.stream().anyMatch(PDFTextElement::isPrinted);
		}
		return this.isPrinted;
	}

	public void setPrinted(boolean state) {
		this.isPrinted = state;
	}

	@Override
	public String getText() {
		if (!this.getChildren().isEmpty()) {
			var builder = new StringBuilder();
			for (var kid : this.children) {
				builder.append(kid.getText()).append(" ");
			}
			return builder.toString().trim();
		}
		return this.text;
	}

	@Override
	public String forPrint() {
		if (!this.children.isEmpty()) {
			var builder = new StringBuilder();
			for (var kid : this.children) {
				var txtPrint = kid.forPrint();
				builder.append(txtPrint);
				if (kid.isRTL()) {
					builder.append(ZERO_WIDTH);
				}
				builder.append(" ");
			}
			return builder.toString().trim();
		}
		if (this.isRTL()) {
			return new StringBuilder(this.text).reverse().toString();
		}
		return this.text;
	}

	public PDFTextElement getParent() {
		return this.parent;
	}

	/**
	 * 
	 * Main rationale: align child baseline to parent baseline
	 * to prevent each word from jumpin' around tha line.
	 * 
	 * Eache kid keeps it's individual width but inherits the
	 * horizontal
	 * 
	 * @param parent
	 */
	public void setParent(PDFTextElement parent) {
		this.parent = parent;
		var newBottom = (float) this.parent.box.getMaxY();
		var newTop = (float) this.parent.box.getMinY();
		var alignedHeight = newBottom - newTop;
		this.box = new Rectangle2D.Float((float) this.box.getMinX(), (float) this.parent.box.getMinY(),
				(float) this.box.getWidth(), alignedHeight);
	}

	public List<PDFTextElement> getChildren() {
		return this.children;
	}

	public void add(PDFTextElement child) {
		this.children.add(child);
	}

	@Override
	public Rectangle2D getBox() {
		return this.box;
	}

	@Override
	public void setBox(Rectangle2D rectangle) {
		this.box = rectangle;
	}

	public static class Baseline {

		private Line2D line;

		public Baseline(float x1, float y1, float x2, float y2) {
			this.line = new Line2D.Float(x1, y1, x2, y2);
		}

		public float getX1() {
			return (float) this.line.getX1();
		}

		public float getY1() {
			return (float) this.line.getY1();
		}

		public float getX2() {
			return (float) this.line.getX2();
		}

		public float getY2() {
			return (float) this.line.getY2();
		}

		public float length() {
			return (float) this.line.getP1().distance(this.line.getP2());
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Baseline)) {
				return false;
			}
			var otherBaseline = (Baseline) other;
			if (otherBaseline.line == null) {
				return false;
			}
			var otherX1 = otherBaseline.line.getX1();
			if (otherX1 != this.line.getX1()) {
				return false;
			}
			var otherY1 = otherBaseline.line.getY1();
			if (otherY1 != this.line.getY1()) {
				return false;
			}
			var otherX2 = otherBaseline.line.getX2();
			if (otherX2 != this.line.getX2()) {
				return false;
			}
			var otherY2 = otherBaseline.line.getY2();
			return otherY2 == this.line.getY2();
		}

		@Override
		public int hashCode() {
			return (int) (line.getX1() + line.getY1() + line.getX2() + line.getY2());
		}
	}
}
