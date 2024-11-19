package de.ulb.digital.derivans.model.ocr;

import java.awt.Dimension;
import java.util.List;

import de.ulb.digital.derivans.model.IVisualElement;
import de.ulb.digital.derivans.model.text.Textline;
import de.ulb.digital.derivans.model.text.Word;

/**
 * 
 * Domain specific model of OCR-Data organized as lists of {@link Textline
 * lines}
 * which are itself composed each of a list of {@link Word textual tokens}.
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

	public int getPageWidth() {
		return dimension.width;
	}

	public void scale(float ratio) {
		for (Textline line : textlines) {
			for (IVisualElement txt : line.getWords()) {
				txt.scale(ratio);
			}
			line.calculateArea();
		}
		int widht = Math.round(dimension.width * ratio);
		int height = Math.round(dimension.height * ratio);
		this.dimension = new Dimension(widht, height);
	}

}
