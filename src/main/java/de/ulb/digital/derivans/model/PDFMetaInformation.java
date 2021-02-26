package de.ulb.digital.derivans.model;

import java.util.Map;
import java.util.Objects;

public class PDFMetaInformation {

	private String author;
	private String creator;
	private String title;
	private Map<String, String> metadata;
	private org.w3c.dom.Document xmpMetadata;

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public PDFMetaInformation(String author, String title, Map<String, String> metadata,
			org.w3c.dom.Document xmpMetadata) {
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

	public org.w3c.dom.Document getXmpMetadata() {
		return this.xmpMetadata;
	}

	public void setXmpMetadata(org.w3c.dom.Document xmpMetadata) {
		this.xmpMetadata = xmpMetadata;
	}

	public PDFMetaInformation author(String author) {
		this.author = author;
		return this;
	}

	public PDFMetaInformation title(String title) {
		this.title = title;
		return this;
	}

	public PDFMetaInformation metadata(Map<String, String> metadata) {
		this.metadata = metadata;
		return this;
	}

	public PDFMetaInformation xmpMetadata(org.w3c.dom.Document xmpMetadata) {
		this.xmpMetadata = xmpMetadata;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof PDFMetaInformation)) {
			return false;
		}
		PDFMetaInformation pDFInformation = (PDFMetaInformation) o;
		return Objects.equals(author, pDFInformation.author) && Objects.equals(title, pDFInformation.title)
				&& Objects.equals(metadata, pDFInformation.metadata)
				&& Objects.equals(xmpMetadata, pDFInformation.xmpMetadata);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(title);
	}

	@Override
	public String toString() {
		return "{" + " author='" + getAuthor() + "'" + ", title='" + getTitle() + "'" + ", metadata='" + getMetadata()
				+ "'" + ", xmpMetadata='" + getXmpMetadata() + "'" + "}";
	}

}
