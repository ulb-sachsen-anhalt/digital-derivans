package de.ulb.digital.derivans.data.mets;

import java.util.Optional;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.config.DefaultConfiguration;
import de.ulb.digital.derivans.IDerivans;
import de.ulb.digital.derivans.model.pdf.DescriptiveMetadata;

/**
 * 
 * Build descriptive metadata (=DMD) from METS/MODS information
 * 
 * @author u.hartwig
 *
 */
public class DescriptiveMetadataBuilder {

	private String urn = IDerivans.UNKNOWN;

	private String person = IDerivans.UNKNOWN;

	private String identifier = IDerivans.UNKNOWN;

	private Optional<String> optIdentXpr = Optional.empty();

	private String title = IDerivans.UNKNOWN;

	private String year = IDerivans.UNKNOWN;

	private String accessCondition = IDerivans.UNKNOWN;

	private METS mets;

	private static final Logger LOGGER = LogManager.getLogger(DescriptiveMetadataBuilder.class);

	public DescriptiveMetadataBuilder urn() {
		this.urn = this.getURN();
		return this;
	}

	public DescriptiveMetadataBuilder person() {
		this.person = this.getPerson();
		return this;
	}

	public DescriptiveMetadataBuilder identifier() throws DigitalDerivansException {
		this.identifier = this.loadIdentifier();
		return this;
	}

	public DescriptiveMetadataBuilder title() {
		this.title = this.getTitle();
		return this;
	}

	public DescriptiveMetadataBuilder access() {
		this.accessCondition = this.getAccessCondition();
		return this;
	}

	public DescriptiveMetadataBuilder year() {
		this.year = this.getYear();
		return this;
	}

	public DescriptiveMetadata build() {
		DescriptiveMetadata dd = new DescriptiveMetadata();
		dd.setUrn(this.urn);
		dd.setIdentifier(this.identifier);
		dd.setTitle(this.title);
		dd.setYearPublished(this.year);
		dd.setPerson(this.person);
		dd.setLicense(Optional.of(this.accessCondition));
		LOGGER.debug("build data: '{}'", dd);
		return dd;
	}

	public String getPerson() {
		if (this.mets.getPrimeMODS() != null) {
			return this.mets.getPrimeMODS().getPerson();

		}
		return IDerivans.UNKNOWN;
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
		if (ident.equals(IDerivans.UNKNOWN)) {
			throw new DigitalDerivansException("found no valid recordIdentifier");
		}
		return ident;
	}

	String getTitle() {
		if (this.mets.getPrimeMODS() != null) {
			return this.mets.getPrimeMODS().getTitle();
		}
		return IDerivans.UNKNOWN;
	}

	String getURN() {
		if (this.mets.getPrimeMODS() != null) {
			return this.mets.getPrimeMODS().getIdentifierURN();
		}
		return IDerivans.UNKNOWN;
	}

	String getAccessCondition() {
		if (this.mets.getPrimeMODS() != null) {
			return this.mets.getPrimeMODS().getAccessCondition();
		}
		return IDerivans.UNKNOWN;
	}

	String getYear() {
		if (this.mets.getPrimeMODS() != null) {
			return this.mets.getPrimeMODS().getYearPublication();
		}
		return IDerivans.UNKNOWN;
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