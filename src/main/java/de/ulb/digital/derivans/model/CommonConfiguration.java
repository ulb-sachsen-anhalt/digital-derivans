package de.ulb.digital.derivans.model;

import java.util.Optional;

/**
 * 
 * @author u.hartwig
 *
 */
public class CommonConfiguration {

	private Optional<String> optCreator = Optional.empty();

	private Optional<String> optLicense = Optional.empty();

	private Optional<String> optKeywords = Optional.empty();

	public Optional<String> getCreator() {
		return this.optCreator;
	}

	public Optional<String> getKeywords() {
		return this.optKeywords;
	}

	public Optional<String> getLicense() {
		return this.optLicense;
	}

	public void setCreator(Optional<String> optCreator) {
		this.optCreator = optCreator;
	}

	public void setLicense(Optional<String> optLicence) {
		this.optLicense = optLicence;
	}

	public void setKeywords(Optional<String> optKeywords) {
		this.optKeywords = optKeywords;
	}
}
