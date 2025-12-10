package de.ulb.digital.derivans.model.pdf;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Simple representation of PDF outline entry with label, page number and
 * possible child entries.
 * 
 * @author u.hartwig
 */
public class PDFOutlineEntry {
	private String label;

	private int pageNumber;

	private List<PDFOutlineEntry> outlineEntries = new ArrayList<>();

	public PDFOutlineEntry(String label, int pageNumber) {
		this.label = label;
		this.pageNumber = pageNumber;
	}

	public String getLabel() {
		return label;
	}

	public int getPageNumber() {
		return pageNumber;
	}

	public List<PDFOutlineEntry> getOutlineEntries() {
		return outlineEntries;
	}

	@Override
	public String toString() {
		String s = super.toString();
		return s + " [label=" + label + ", pageNumber=" + pageNumber + ", "+outlineEntries.size() + " entries]";
	}

}