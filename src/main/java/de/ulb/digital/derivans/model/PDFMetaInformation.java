package de.ulb.digital.derivans.model;

import java.util.Map;
import java.util.Optional;

import org.w3c.dom.Document;

import de.ulb.digital.derivans.config.DefaultConfiguration;
import de.ulb.digital.derivans.data.IMetadataStore;

/**
 * 
 * Carry any PDF-related Metadata
 * 
 * @author hartwig
 *
 */
public class PDFMetaInformation {

	private String author = IMetadataStore.UNKNOWN;
	private String title = IMetadataStore.UNKNOWN;
	private String publicationYear = IMetadataStore.UNKNOWN;
	private Map<String, String> metadata;
	private Document xmpMetadata;
	private Optional<String> optCreator = Optional.empty();
	private Optional<String> optLicense = Optional.empty();
	private Optional<String> optKeywords = Optional.empty();
	private String conformanceLevel = DefaultConfiguration.PDFA_CONFORMANCE_LEVEL;
	private int imageDpi = DefaultConfiguration.PDF_IMAGE_DPI;
	private Boolean debugRender = Boolean.FALSE;
	private String renderLevel = DefaultConfiguration.DEFAULT_RENDER_LEVEL;
	private String renderModus = DefaultConfiguration.DEFAULT_RENDER_VISIBILTY;
	private boolean enrichMetadata = true;

	public String getConformanceLevel() {
		return conformanceLevel;
	}

	public void setConformanceLevel(String conformanceLevel) {
		this.conformanceLevel = conformanceLevel;
	}

	public int getImageDpi() {
		return imageDpi;
	}

	public void setImageDpi(int imageDpi) {
		this.imageDpi = imageDpi;
	}

	public PDFMetaInformation() {
	}

	public PDFMetaInformation(String author, String title, Map<String, String> metadata, Document xmpMetadata) {
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

	public PDFMetaInformation xmpMetadata(Document xmpMetadata) {
		this.xmpMetadata = xmpMetadata;
		return this;
	}

	@Override
	public String toString() {
		return "{" + " author='" + getAuthor() + "'" + ", title='" + getTitle() + "'" + ", metadata='" + getMetadata()
				+ "'" + ", xmpMetadata='" + getXmpMetadata() + "'" + "}";
	}

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

	public String getPublicationYear() {
		return publicationYear;
	}

	public void setPublicationYear(String publicationYear) {
		this.publicationYear = publicationYear;
	}

	public Boolean getDebugRender() {
		return debugRender;
	}

	public void setDebugRender(Boolean debugRender) {
		this.debugRender = debugRender;
	}

	public String getRenderLevel() {
		return this.renderLevel;
	}

	public void setRenderLevel(String level) {
		this.renderLevel = level;
	}

	public String getRenderModus() {
		return this.renderModus;
	}

	public void setRenderModus(String modus) {
		this.renderModus = modus;
	}

	public boolean isEnrichMetadata() {
		return enrichMetadata;
	}

	public void setEnrichMetadata(boolean enrichMetadata) {
		this.enrichMetadata = enrichMetadata;
	}

	/**
	 * 
	 * Enrich information from configuration in workflow.
	 * 
	 * Attention: overrides license from Metadata (if any present)
	 * 
	 * @param dd
	 */
	public void mergeDescriptiveData(DescriptiveData dd) {
		Optional<String> optMetaLicense = dd.getLicense();
		if (optMetaLicense.isPresent() && this.getLicense().isEmpty()) {
			this.setLicense(optMetaLicense);
		}
		this.setAuthor(dd.getPerson());
		this.setTitle(dd.getTitle());
		this.setPublicationYear(dd.getYearPublished());
	}
}
