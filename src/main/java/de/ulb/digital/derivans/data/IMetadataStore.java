package de.ulb.digital.derivans.data;

import java.util.List;

import org.jdom2.Namespace;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.model.DescriptiveData;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.DigitalStructureTree;

/**
 * 
 * Provide Access to relevant Metadata for derivation of digitalizates
 * 
 * @author hartwig
 *
 */
public interface IMetadataStore {

	// METS file group for images with maximal resolution
	String DEFAULT_METS_FILEGROUP_MAX = "MAX";
	// METS file group for OCR-data with, most likely, MIMETYPE="application/alto+xml"
	String DEFAULT_METS_FILEGROUP_FULLTEXT = "FULLTEXT";
	// Mark unresolved information about author, title, ...
	String UNKNOWN = "n.a.";
	Namespace NS_MODS = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");
	Namespace NS_METS = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");

	/**
	 * 
	 * Provide specific subset of information from metadata section.
	 * 
	 * May contain information about official title, author and alike.
	 * 
	 * @return {@link DescriptiveData}
	 */
	DescriptiveData getDescriptiveData();

	/**
	 * 
	 * Create recursive data structure used as Outline for PDF
	 * 
	 * @return
	 * @throws DigitalDerivansException
	 */
	DigitalStructureTree getStructure() throws DigitalDerivansException;

	/**
	 * 
	 * Get the {@link DigitalPage Digital Pages} in exact the order that is defined
	 * in the physical Sequence container of the METS section, if METS/MODS is
	 * available.
	 * 
	 * If no METS/MODS is available, use filenames in corresponding directory to
	 * create page order. Please note, that this can lead to unexpected results.
	 * 
	 * @return ordered list of pages
	 * 
	 */
	List<DigitalPage> getDigitalPagesInOrder();

	/**
	 * 
	 * Enrich Metadata with
	 * <ul>
	 * <li>FileGroup USE="DOWNLOAD" that holds PDF information</li>
	 * <li>metsHdr-Agent entry for Digital Derivans Tool</li>
	 * </ul>
	 * 
	 * @param fileId
	 * @return
	 */
	boolean enrichPDF(String fileId);
}