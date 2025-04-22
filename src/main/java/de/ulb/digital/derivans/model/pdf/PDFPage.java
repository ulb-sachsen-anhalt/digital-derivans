package de.ulb.digital.derivans.model.pdf;

import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.IVisualElement;
import de.ulb.digital.derivans.model.ocr.OCRData;

/**
 * 
 * Represents a page ready to print
 * with a retro-print image spanning
 * the complete page.
 * 
 * The major difference is the transition
 * of Y-axis from top to bottom, hence
 * all heights are counted from below
 * and increase upwards.
 * 
 * Assumes a footer, if any exits, is attach
 * _below_ at the bottom of the page.
 * Therefore, if image and corresponding ocr
 * differ _only_ in height the difference
 * is taken as additional bottom footer.
 * 
 * 
 * @author hartwig
 *
 */
public class PDFPage implements IVisualElement {

	private int number = -1;	// unset marked "-1"

	private float scale;

	private DigitalPage digitalPage;
	
	private Optional<List<PDFTextElement>> txtElements = Optional.empty();

	private Dimension imageDimension = new Dimension();

	private Dimension imageDimensionOriginal;

	private Dimension ocrDimensionOriginal;

	private Dimension ocrDimensionScaled;

	/**
	 * Default empty constructor
	 */
	public PDFPage() {
	}
	
	/**
	 * Construct instance from foreground image
	 * 
	 * @param reader
	 * @param number
	 */
	public PDFPage(Dimension dimension, int number) {
		this.imageDimension = dimension;
		this.number = number;
	}

	public boolean containsText() {
		return this.txtElements.isPresent() && !this.txtElements.get().isEmpty();
	}

	public void setImageDimensionOriginal(int orgWidth, int orgHeight) {
		this.imageDimensionOriginal = new Dimension(orgWidth, orgHeight);
	}

	/**
	 * 
	 * Pass/forward optional OCR-data for
	 * given image page.
	 * 
	 * Encapsulates:
	 *  * scaling of shapes
	 *  * transformation of coordinates / translate Y-origin
	 *  * pre-calculation of fontsize
	 * 
	 * @param ocrData
	 */
	public void passOCR(DigitalPage digitalPage) {
		this.digitalPage = digitalPage;
		if(!digitalPage.getOcrData().isPresent()) {
			return;
		}
		OCRData ocrData = digitalPage.getOcrData().get();
		this.ocrDimensionOriginal = new Dimension(ocrData.getPageWidth(), ocrData.getPageHeight());
		int ocrPageHeight = ocrData.getPageHeight();
		int footerHeight = 0;
		Optional<Integer> optFooterHeight = this.digitalPage.getFooterHeight();
		if (optFooterHeight.isPresent()) {
			footerHeight = optFooterHeight.get();
			ocrPageHeight += footerHeight;
		} else {
			if (this.imageDimensionOriginal.height != this.ocrDimensionOriginal.height) {
				var delta = this.imageDimensionOriginal.height - this.ocrDimensionOriginal.height;
			}
		}
		// need to scale
		// page height corresponds to original image height
		float currentImageHeight = (float) this.imageDimension.getHeight();
		float ratio = currentImageHeight / ocrPageHeight;
		if (Math.abs(1.0 - ratio) > 0.01) {
			ocrData.scale(ratio);
			ocrPageHeight = ocrData.getPageHeight(); // respect new height
			this.ocrDimensionScaled = new Dimension(ocrData.getPageWidth(), ocrData.getPageHeight());
			this.setScale(ratio);
		}
		var txtElems = new ArrayList<PDFTextElement>();
		for (var line : ocrData.getTextlines()) {
			var theText = line.getText();
			Rectangle2D rect = line.getBox();
			PDFTextElement lineElement = new PDFTextElement(theText, rect, "line");
			lineElement.invert(ocrPageHeight);
			for (var token : line.getWords()) {
				PDFTextElement wordToken = new PDFTextElement(token.getText(), token.getBox(), "word");
				wordToken.invert(ocrPageHeight);
				lineElement.add(wordToken);
				wordToken.setParent(lineElement);
			}
			txtElems.add(lineElement);
		}
		this.txtElements = Optional.of(txtElems);
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public String getImagePath() {
		return this.digitalPage.getFile().getPath().toString();
	}

	public Dimension getDimension() {
		return new Dimension(this.imageDimension);
	}

	public void add(PDFTextElement element) {
		if (!this.txtElements.isPresent()) {
			this.txtElements = Optional.of(new ArrayList<>());
		}
		this.txtElements.get().add(element);
	}

	public Optional<List<PDFTextElement>> getTextcontent() {
		return this.txtElements;
	}

	public void setScale(float scale) {
		this.scale = scale;
	}

	public float getScale() {
		return this.scale;
	}

	public Dimension getOcrPageOriginalDimension() {
		return this.ocrDimensionOriginal;
	}

	public Dimension getOcrPageScaledDimension() {
		return this.ocrDimensionScaled;
	}

	@Override
	public String toString() {
		return "{" + " nr='" + number + "'" + ", image='" + imageDimension + "'}";
	}

	/**
	 * 
	 * Get complete image cavas used for the page
	 * 
	 */
	@Override
	public Rectangle2D getBox() {
		return new Rectangle2D.Float(0, 0 , (float)this.imageDimension.getWidth(), 
			(float)this.imageDimension.getHeight());
	}
	
	@Override
	public void setBox(Rectangle2D rectangle) {
		throw new UnsupportedOperationException("Unimplemented method 'setBox'");
	}

}
