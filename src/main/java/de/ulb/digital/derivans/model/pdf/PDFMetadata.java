package de.ulb.digital.derivans.model.pdf;

import java.util.Map;
import java.util.Optional;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;

import de.ulb.digital.derivans.data.IMetadataStore;


/**
 * 
 * Carry PDF-related Metadata for
 * introspection of created PDF-derivates
 * 
 * @author hartwig
 *
 */
public class PDFMetadata {

	private String author = IMetadataStore.UNKNOWN;
	private String title = IMetadataStore.UNKNOWN;
	private Map<String, String> metadata;
	private Document xmpMetadata;
	private Optional<String> optCreator = Optional.empty();
	public PDFMetadata() {
	}

	public PDFMetadata(String author, String title, Map<String, String> metadata, Document xmpMetadata) {
		this.author = author;
		this.title = title;
		this.metadata = metadata;
		this.xmpMetadata = xmpMetadata;
	}

	public String getAuthor() {
		return this.author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Map<String, String> getMetadata() {
		return this.metadata;
	}

	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}

	public Optional<Element> getFromXmpMetadataBy(String elementLabel) {
		var it = this.xmpMetadata.getDescendants(new ElementFilter(elementLabel));
		if (it.hasNext()) {
			return Optional.of(it.next());
		}
		return Optional.empty();
	}

	public void setXmpMetadata(Document xmpMetadata) {
		this.xmpMetadata = xmpMetadata;
	}

	@Override
	public String toString() {
		return "{ metadata='" + getMetadata()
				+ "'" + ", xmpMetadata='" + xmpMetadata + "'" + "}";
	}

	public Optional<String> getCreator() {
		return this.optCreator;
	}

	public void setCreator(Optional<String> optCreator) {
		this.optCreator = optCreator;
	}

}
