package de.ulb.digital.derivans.model.pdf;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * Encapsulate PDF processing results
 * 
 * @author hartwig
 */
public class PDFResult {

	private LinkedHashMap<Integer, PDFPage> outline = new LinkedHashMap<>(); 
	
	private List<PDFPage> pdfPages = new ArrayList<>();

	private Path pathDocument;
	
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

	public void setPath(Path path) {
		this.pathDocument = path;
	}

	public Path getPath() {
		return this.pathDocument;
	}
}
