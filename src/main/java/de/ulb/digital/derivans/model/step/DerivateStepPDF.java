package de.ulb.digital.derivans.model.step;

import java.nio.file.Path;
import java.util.Optional;

import de.ulb.digital.derivans.config.DefaultConfiguration;
import de.ulb.digital.derivans.config.TypeConfiguration;
import de.ulb.digital.derivans.DigitalDerivansRuntimeException;
import de.ulb.digital.derivans.IDerivans;
import de.ulb.digital.derivans.model.pdf.DescriptiveMetadata;

/**
 * Specific PDF Derivate Generation Step
 * 
 * @author u.hartwig
 * 
 */
public class DerivateStepPDF extends DerivateStep {

	protected boolean enrichMetadata = true;
	private int imageDpi = DefaultConfiguration.DEFAULT_IMAGE_DPI;
	private Boolean debugRender = Boolean.FALSE;
	private TypeConfiguration renderLevel = DefaultConfiguration.DEFAULT_RENDER_LEVEL;
	private TypeConfiguration renderModus = DefaultConfiguration.DEFAULT_RENDER_VISIBILTY;
	private Optional<String> modsIdentifierXPath = Optional.empty();
	private String conformanceLevel = DefaultConfiguration.DEFAULT_CONFORMANCE_LEVEL;
	private String author = IDerivans.UNKNOWN;
	private String title = IDerivans.UNKNOWN;
	private String publicationYear = IDerivans.UNKNOWN;
	private Optional<String> optCreator = Optional.empty();
	private Optional<String> optLicense = Optional.empty();
	private Optional<String> optKeywords = Optional.empty();
	private Optional<String> optNamePDF = Optional.empty();
	private Path pdfFilePath;

	public DerivateStepPDF() {
		super();
		this.setOutputType(DerivateType.PDF);
	}

	public boolean isEnrichMetadata() {
		return enrichMetadata;
	}

	public void setEnrichMetadata(boolean enrichMetadata) {
		this.enrichMetadata = enrichMetadata;
	}

	public int getImageDpi() {
		return imageDpi;
	}

	public void setImageDpi(int imageDpi) {
		this.imageDpi = imageDpi;
	}

	public Boolean getDebugRender() {
		return debugRender;
	}

	public void setDebugRender(Boolean debugRender) {
		this.debugRender = debugRender;
	}

	public TypeConfiguration getRenderLevel() {
		return this.renderLevel;
	}

	public void setRenderLevel(TypeConfiguration level) {
		this.renderLevel = level;
	}

	public TypeConfiguration getRenderModus() {
		return this.renderModus;
	}

	public void setRenderModus(TypeConfiguration modus) {
		this.renderModus = modus;
	}

	public Optional<String> getModsIdentifierXPath() {
		return modsIdentifierXPath;
	}

	public void setModsIdentifierXPath(String modsIdentifierXPath) {
		this.modsIdentifierXPath = Optional.of(modsIdentifierXPath);
	}

	public String getConformanceLevel() {
		return conformanceLevel;
	}

	public void setConformanceLevel(String conformanceLevel) {
		this.conformanceLevel = conformanceLevel;
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

	public Optional<String> getNamePDF() {
		return this.optNamePDF;
	}

	public void setNamePDF(String optNamePDF) {
		this.optNamePDF = Optional.of(optNamePDF);
	}

	/**
	 * 
	 * Enrich information from {@link DescriptiveMetadata descriptive metadata
	 * (dmd)}.
	 * 
	 * _Attention_:
	 * Overrides license from configuration, if present.
	 * 
	 * @param dd
	 */
	public void mergeDescriptiveData(DescriptiveMetadata dd) {
		Optional<String> optMetaLicense = dd.getLicense();
		if (optMetaLicense.isPresent()) {
			this.setLicense(optMetaLicense);
		}
		this.setAuthor(dd.getPerson());
		this.setTitle(dd.getTitle());
		this.setPublicationYear(dd.getYearPublished());
	}

	public void setPathPDF(Path pdfFilePath) {
		this.pdfFilePath = pdfFilePath;
	}

	public Path getPathPDF() {
		if (this.pdfFilePath == null) {
			throw new DigitalDerivansRuntimeException("Invalid pdfFilePath: null");
		}
		return this.pdfFilePath.normalize();
	}

}
