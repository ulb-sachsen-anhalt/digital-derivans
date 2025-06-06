package de.ulb.digital.derivans.model.pdf;

import java.util.ArrayList;
import java.util.List;

/**
 * @author u.hartwig
 */
public class PDFOutlineEntry {
	private String label;

	private String destiny;

	private List<PDFOutlineEntry> outlineEntries = new ArrayList<>();

	public PDFOutlineEntry(String label, String destiny) {
		this.label = label;
		this.destiny = destiny;
	}

	public String getLabel() {
		return label;
	}

	public String getDestiny() {
		return destiny;
	}

	public List<PDFOutlineEntry> getOutlineEntries() {
		return outlineEntries;
	}

}