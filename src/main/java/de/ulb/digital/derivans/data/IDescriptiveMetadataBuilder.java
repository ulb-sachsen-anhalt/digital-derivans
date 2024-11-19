package de.ulb.digital.derivans.data;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.data.mets.MetadataStore;
import de.ulb.digital.derivans.model.pdf.DescriptiveMetadata;

/**
 * 
 * Define functionalities to gather descriptive metadata 
 * for digital object in question - used to enrich in
 * future PDF metadata or as PDF title
 * 
 * @author hartwig
 */
public interface IDescriptiveMetadataBuilder {

	/**
	 * 
	 * Set used {@link Metadata Storage} as store
	 * to collect all required {@link DescriptiveMetadata}
	 * 
	 * @param store
	 */
	void setMetadataStore(MetadataStore store);

	IDescriptiveMetadataBuilder access();

	IDescriptiveMetadataBuilder title();

	IDescriptiveMetadataBuilder identifier() throws DigitalDerivansException;

	IDescriptiveMetadataBuilder person();

	IDescriptiveMetadataBuilder urn();

	IDescriptiveMetadataBuilder year();
	
	DescriptiveMetadata build();
}