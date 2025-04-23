package de.ulb.digital.derivans.model.pdf;

import java.util.Optional;

import de.ulb.digital.derivans.derivate.IDerivateer;

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
 * <li>Person: being with central Relation, as Author or Editor<br />
 * listed with mods:displayName, if given, otherwise
 * mods:namePart[@type="family"]</li>
 * <li>optional Creator (for PDF Metadata)</li>
 * <li>License: optional information from MODS:accessCondition@"use and
 * reproduction" or
 * configuration</li>
 * </ul>
 * 
 * 
 * @author u.hartwig
 *
 */
public class DescriptiveMetadata {

	private String urn = IDerivateer.UNKNOWN;

	private String identifier = IDerivateer.UNKNOWN;

	private String title = IDerivateer.UNKNOWN;

	private String person = IDerivateer.UNKNOWN;

	private String yearPublished = IDerivateer.UNKNOWN;

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

	public String getPerson() {
		return person;
	}

	public void setPerson(String author) {
		this.person = author;
	}

	public String getYearPublished() {
		return yearPublished;
	}

	public void setYearPublished(String yearPublished) {
		if (IDerivateer.UNKNOWN.equals(yearPublished))
			yearPublished = "0";
		this.yearPublished = yearPublished;
	}

	public Optional<String> getLicense() {
		return license;
	}

	public void setLicense(Optional<String> labelLicense) {
		if (labelLicense.isPresent()) {
			String newLicence = labelLicense.get();
			if (!IDerivateer.UNKNOWN.equals(newLicence)) {
				this.license = labelLicense;
			}
		}
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
		if (person != null)
			builder.append(person);
		if (yearPublished != null)
			builder.append(" (").append(yearPublished).append(") ");
		if (title != null)
			builder.append(" ").append(title);
		return builder.toString();
	}

}
