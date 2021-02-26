package de.ulb.digital.derivans.model;

import java.util.Optional;

import de.ulb.digital.derivans.data.MetadataStore;

/**
 * 
 * 
 * Subset of descriptive Metadata for PDF generation.
 * 
 * Contains:
 * <ul>
 * <li>URN<br/>
 * <li>Identifier from source catalog source (like PPN)</li> <i>Optional unique
 * URNs for physical pages (granular URNs) might exist</i></li>
 * <li>Title</li>
 * <li>Author (alternative: Editor)</li>
 * <li>optional Creator (user for PDF Metadata)</li>
 * <li>License information from MODS:accessCondition@"use and reproduction" or
 * configuration</li>
 * </ul>
 * 
 * 
 * @author hartwig
 *
 */
public class DescriptiveData {

	private String urn = MetadataStore.UNKNOWN;

	private String identifier = MetadataStore.UNKNOWN;

	private String title = MetadataStore.UNKNOWN;

	private String author = MetadataStore.UNKNOWN;

	private String yearPublished = MetadataStore.UNKNOWN;

	private Optional<String> creator = Optional.empty();

	private Optional<String> license = Optional.empty();

	private Optional<String> keywords = Optional.empty();

	public String getUrn() {
		return urn;
	}

	public void setUrn(String urn) {
		this.urn = urn;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public Optional<String> getCreator() {
		return this.creator;
	}

	public String getYearPublished() {
		return yearPublished;
	}

	public void setYearPublished(String yearPublished) {
		this.yearPublished = yearPublished;
	}

	public void setCreator(Optional<String> creator) {
		this.creator = creator;
	}

	public Optional<String> getLicense() {
		return license;
	}

	public void setLicense(Optional<String> labelLicense) {
		this.license = labelLicense;
	}

	public Optional<String> getKeywords() {
		return keywords;
	}

	public void setKeywords(Optional<String> keywords) {
		this.keywords = keywords;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (author != null)
			builder.append(author);
		if (yearPublished != null)
			builder.append('(').append(yearPublished).append(')');
		if (title != null)
			builder.append(" ").append(title);
		return builder.toString();
	}

}
