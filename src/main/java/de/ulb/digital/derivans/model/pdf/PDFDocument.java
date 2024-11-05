package de.ulb.digital.derivans.model.pdf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hartwig
 */
public class PDFDocument {

	private LinkedHashMap<Integer, PDFPage> outline = new LinkedHashMap<>(); 
	
	private List<PDFPage> pdfPages = new ArrayList<>();
	
	private PDFMetadata metadata;
	
	public PDFMetadata getMetadata() {
		return metadata;
	}

	public void setMetadata(PDFMetadata metadata){
		this.metadata = metadata;
	}

	public List<PDFPage> getPdfPages() {
		return pdfPages;
	}

	public Map<Integer, PDFPage> getOutline() {
		return outline;
	}
	
	public void addPages(List<PDFPage> pages) {
		this.pdfPages.addAll(pages);
	}
}
