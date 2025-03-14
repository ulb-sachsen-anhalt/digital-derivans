package de.ulb.digital.derivans.data.mets;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.config.DefaultConfiguration;
import de.ulb.digital.derivans.data.IDescriptiveMetadataBuilder;
import de.ulb.digital.derivans.data.IMetadataStore;
import de.ulb.digital.derivans.model.pdf.DescriptiveMetadata;

/**
 * 
 * Implementation of {@link IDescriptiveMetadataBuilder Descriptive metadata
 * builder}
 * from METS/MODS information
 * 
 * @author u.hartwig
 *
 */
public class DescriptiveMetadataBuilder implements IDescriptiveMetadataBuilder {

	private String urn = IMetadataStore.UNKNOWN;

	private String person = IMetadataStore.UNKNOWN;

	private String identifier = IMetadataStore.UNKNOWN;

	private String title = IMetadataStore.UNKNOWN;

	private String year = IMetadataStore.UNKNOWN;

	private String accessCondition = IMetadataStore.UNKNOWN;

	private MetadataStore store;

	private MODS primeMods;

	private static final Logger LOGGER = LogManager.getLogger(DescriptiveMetadataBuilder.class);

	@Override
	public IDescriptiveMetadataBuilder urn() {
		this.urn = getURN();
		return this;
	}

	@Override
	public IDescriptiveMetadataBuilder person() {
		this.person = getPerson();
		return this;
	}

	@Override
	public IDescriptiveMetadataBuilder identifier() throws DigitalDerivansException {
		this.identifier = loadIdentifier();
		return this;
	}

	@Override
	public IDescriptiveMetadataBuilder title() {
		this.title = getTitle();
		return this;
	}

	@Override
	public IDescriptiveMetadataBuilder access() {
		accessCondition = getAccessCondition();
		return this;
	}

	@Override
	public IDescriptiveMetadataBuilder year() {
		year = getYear();
		return this;
	}

	@Override
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
		if (this.primeMods != null) {
			return this.primeMods.getPerson();

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
	 * Please note, that any including whitespaces will be replaced
	 * by the special glue-token "_"
	 * 
	 * @throws DigitalDerivansException
	 *                                  If Xpath provided but nothing matches
	 * 
	 * @return String Identifier
	 * 
	 */
	private String loadIdentifier() throws DigitalDerivansException {
		if (this.primeMods == null) {
			return null;
		}
		if (this.store.optionalIdentifierExpression().isPresent()) {
			String xPath = this.store.optionalIdentifierExpression().get();
			Element match = this.store.getMetadataHandler().evaluateFirst(xPath);
			if (match != null) {
				String textualContent = match.getTextTrim();
				if (!textualContent.isEmpty()) {
					return textualContent.replace(' ', '_');
				} else {
					var msg = String.format("PDF Identifier XPath '%s' empty for '%s'", xPath,
							this.store.getMetadataHandler().getPath());
					throw new DigitalDerivansException(msg);
				}
			} else {
				var msg = String.format("PDF Identifier XPath '%s' no match in '%s'", xPath,
						this.store.getMetadataHandler().getPath());
				throw new DigitalDerivansException(msg);
			}
		}
		var ident = this.primeMods.getIdentifier();
		if (ident.equals(IMetadataStore.UNKNOWN)) {
			throw new DigitalDerivansException("found no valid recordIdentifier");
		}
		return ident;
	}

	String getTitle() {
		if (this.primeMods != null) {
			return this.primeMods.getTitle();
		}
		return IMetadataStore.UNKNOWN;
	}

	String getURN() {
		if (this.primeMods != null) {
			return this.primeMods.getIdentifierURN();
		}
		return IMetadataStore.UNKNOWN;
	}

	String getAccessCondition() {
		if (this.primeMods != null) {
			return this.primeMods.getAccessCondition();
		}
		return IMetadataStore.UNKNOWN;
	}

	String getYear() {
		if (this.primeMods != null) {
			return this.primeMods.getYearPublication();
		}
		return IMetadataStore.UNKNOWN;
	}

	@Override
	public void setMetadataStore(MetadataStore store) {
		this.store = store;
		this.primeMods = store.getMetadataHandler().getPrimeMODS();
	}

}
