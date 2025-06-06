package de.ulb.digital.derivans.model.pdf;

import java.util.Map;
import java.util.Optional;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;

/**
 *
 * Carry PDF-related Metadata for
 * introspection of created PDF-derivates
 *
 * @author hartwig
 *
 */
public class PDFMetadata {

	private Map<MetadataType, String> metadata;
	private Document xmpMetadata;

	public PDFMetadata() {
	}

	public PDFMetadata(Map<MetadataType, String> metadata) {
		this.metadata = metadata;
	}

	public String getAuthor() {
		return this.metadata.get(MetadataType.AUTHOR);
	}

	public String getTitle() {
		return this.metadata.get(MetadataType.TITLE);
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
		return "{ metadata='" + metadata
				+ "'" + ", xmpMetadata='" + xmpMetadata + "'" + "}";
	}

	public String getCreator() {
		return this.metadata.get(MetadataType.CREATOR);
	}

}
