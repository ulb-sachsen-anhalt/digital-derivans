package de.ulb.digital.derivans.model.pdf;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 
 * Gather insights of single PDF Page
 * 
 * @author hartwig
 *
 */
public class PDFPage {

	private Dimension dimension;

	private int number = -1;

	private float scale;
	
	private List<PDFElement> pdfElements = new ArrayList<>();

	/**
	 * Default empty constructor
	 */
	public PDFPage() {
	}
	
	/**
	 * Construct instance from introspection
	 * @param reader
	 * @param number
	 */
	public PDFPage(Dimension dimension, int number) {
		this.number = number;
		this.dimension = dimension;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public Dimension getDimension() {
		return new Dimension(this.dimension);
	}

	public void setDimension(int width, int heigth) {
		this.dimension = new Dimension(width, heigth);
	}

	public void add(PDFElement element) {
		this.pdfElements.add(element);
	}

	public List<PDFElement> getlines() {
		return new ArrayList<>(this.pdfElements);
	}

	public void setScale(float scale) {
		this.scale = scale;
	}

	public float getScale() {
		return this.scale;
	}

	@Override
	public String toString() {
		return "{" + " nr='" + number + "'" + ", dim='" + dimension + "'}";
	}

}
