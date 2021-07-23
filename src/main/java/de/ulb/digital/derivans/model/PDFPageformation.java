package de.ulb.digital.derivans.model;

import java.awt.Dimension;
//import java.util.List;
//import java.util.Optional;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;

/**
 * 
 * Gather insights of single PDF Page
 * 
 * @author hartwig
 *
 */
public class PDFPageformation {

	private Dimension dimension;
	private int number;

	// TODO
//	private Optional<List<String>> ocrData;
	
	public PDFPageformation(PdfReader reader, int number) {
		this.number = number;
		Rectangle r = reader.getPageSize(this.number);
		this.dimension = new Dimension((int) r.getWidth(), (int) r.getHeight());
	}

	public int getNumber() {
		return number;
	}

	public Dimension getDimension() {
		return new Dimension(this.dimension);
	}

	@Override
	public String toString() {
		return "{" + " nr='" + number + "'" + ", dim='" + dimension + "'}";
	}

}
