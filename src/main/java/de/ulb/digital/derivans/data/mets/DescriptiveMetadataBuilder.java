package de.ulb.digital.derivans.data.mets;

import java.util.Optional;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.config.DefaultConfiguration;
import de.ulb.digital.derivans.data.IMetadataStore;
import de.ulb.digital.derivans.model.pdf.DescriptiveMetadata;

/**
 * 
 * Build descriptive metadata (=DMD) from METS/MODS information
 * 
 * @author u.hartwig
 *
 */
public class DescriptiveMetadataBuilder {

	private String urn = IMetadataStore.UNKNOWN;

	private String person = IMetadataStore.UNKNOWN;

	private String identifier = IMetadataStore.UNKNOWN;

	private Optional<String> optIdentXpr = Optional.empty();

	private String title = IMetadataStore.UNKNOWN;

	private String year = IMetadataStore.UNKNOWN;

	private String accessCondition = IMetadataStore.UNKNOWN;

	// private MetadataStore store;

	// private MODS primeMods;

	private METS mets;

	private static final Logger LOGGER = LogManager.getLogger(DescriptiveMetadataBuilder.class);

	public DescriptiveMetadataBuilder urn() {
		this.urn = getURN();
		return this;
	}

	public DescriptiveMetadataBuilder person() {
		this.person = getPerson();
		return this;
	}

	public DescriptiveMetadataBuilder identifier() throws DigitalDerivansException {
		this.identifier = loadIdentifier();
		return this;
	}

	public DescriptiveMetadataBuilder title() {
		this.title = getTitle();
		return this;
	}

	public DescriptiveMetadataBuilder access() {
		accessCondition = getAccessCondition();
		return this;
	}

	public DescriptiveMetadataBuilder year() {
		year = getYear();
		return this;
	}

	public DescriptiveMetadata build() {
		DescriptiveMetadata dd = new DescriptiveMetadata();
		dd.setUrn(urn);
		dd.setIdentifier(identifier);
		dd.setTitle(title);
		dd.setYearPublished(year);
		dd.setPerson(person);
		dd.setLicense(Optional.of(accessCondition));
		LOGGER.debug("build data: '{}'", dd);
		return dd;
	}

	public String getPerson() {
		if (this.mets.getPrimeMODS() != null) {
			return this.mets.getPrimeMODS().getPerson();

		}
		return IMetadataStore.UNKNOWN;
	}

	/**
	 * 
	 * Identifier to be used to label PDF-Derivates.
	 * 
	 * Handles different scenarios:
	 * <ul>
	 * <li>no metadata present, return Zero information</li>
	 * <li>configured {@link DefaultConfiguration.Key.PDF_MODS_IDENTIFIER_XPATH}
	 * present, then go for it</li>
	 * <li>former not present: use first mods:recordIdentifier</li>
	 * </ul>
	 * 
	 * If nothing present at all (since not metadata present),
	 * later stages *might* set this vital information in their
	 * own fashion (i.e., use digi root-directory as label ...)
	 * 
	 * Please note, that any included whitespaces will be replaced
	 * by the special glue-token "_"
	 * 
	 * @throws DigitalDerivansException XPath provided no matches
	 * 
	 * @return String Identifier
	 * 
	 */
	private String loadIdentifier() throws DigitalDerivansException {
		if (this.mets.getPrimeMODS() == null) {
			return null;
		}
		if (this.optIdentXpr.isPresent()) {
			String xPath = this.optIdentXpr.get();
			Element match = this.mets.evaluate(xPath).get(0);
			if (match != null) {
				String textualContent = match.getTextTrim();
				if (!textualContent.isEmpty()) {
					return textualContent.replace(' ', '_');
				} else {
					var msg = String.format("PDF Identifier XPath '%s' empty for '%s'", xPath,
					this.mets.getPath());
					throw new DigitalDerivansException(msg);
				}
			} else {
				var msg = String.format("PDF Identifier XPath '%s' no match in '%s'", xPath,
				this.mets.getPath());
				throw new DigitalDerivansException(msg);
			}
		}
		var ident = this.mets.getPrimeMODS().getIdentifier();
		if (ident.equals(IMetadataStore.UNKNOWN)) {
			throw new DigitalDerivansException("found no valid recordIdentifier");
		}
		return ident;
	}

	String getTitle() {
		if (this.mets.getPrimeMODS() != null) {
			return this.mets.getPrimeMODS().getTitle();
		}
		return IMetadataStore.UNKNOWN;
	}

	String getURN() {
		if (this.mets.getPrimeMODS() != null) {
			return this.mets.getPrimeMODS().getIdentifierURN();
		}
		return IMetadataStore.UNKNOWN;
	}

	String getAccessCondition() {
		if (this.mets.getPrimeMODS() != null) {
			return this.mets.getPrimeMODS().getAccessCondition();
		}
		return IMetadataStore.UNKNOWN;
	}

	String getYear() {
		if (this.mets.getPrimeMODS() != null) {
			return this.mets.getPrimeMODS().getYearPublication();
		}
		return IMetadataStore.UNKNOWN;
	}

	public void setMetadataStore(METS mets) {
		this.mets = mets;
	}

	public void setIdentifierExpression(String xpr) {
		this.optIdentXpr = Optional.of(xpr);
	}

}

/**
 * 
 * Predicate for filtering mods:originInfo[@eventType] elements
 * 
 * @author hartwig
 *
 */
class PredicateEventTypePublication implements Predicate<Element> {

	@Override
	public boolean test(Element el) {
		if (el.getAttribute("eventType") != null) {
			String val = el.getAttributeValue("eventType");
			return val.equalsIgnoreCase("publication");
		}
		return false;
	}

}