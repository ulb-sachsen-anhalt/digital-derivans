package de.ulb.digital.derivans.data;

import java.util.List;
import java.util.Optional;

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

	/*
	 * Default METS file group for images with maximal resolution
	 */
	String DEFAULT_INPUT_IMAGES = "MAX";

	/*
	 * Default METS file group for OCR-data with, most likely,
	 * MIMETYPE="application/alto+xml"
	 */
	String DEFAULT_INPUT_FULLTEXT = "FULLTEXT";

	/*
	 * Mark all unresolved information about author, title, ...
	 */
	String UNKNOWN = "n.a.";

	/*
	 * XML Namespaces for METS/MODS parsing
	 */
	Namespace NS_MODS = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");
	Namespace NS_METS = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");

	/**
	 * 
	 * Provide specific subset of information from descriptive 
	 * metadata section.
	 * 
	 * May contain information about title, author, 
	 * URN of the digital object and alike.
	 * 
	 * @return {@link DescriptiveData}
	 * @throws {@link DigitalDerivansException}
	 */
	DescriptiveData getDescriptiveData() throws DigitalDerivansException;

	/**
	 * 
	 * Create recursive data structure used for rendering
	 * Outline of PDF.
	 * 
	 * @return {@link DigitalStructureTree}
	 * @throws {@link DigitalDerivansException}
	 */
	DigitalStructureTree getStructure() throws DigitalDerivansException;

	/**
	 * 
	 * Get the {@link DigitalPage Digital Pages} in exact the order that is defined
	 * in the physical Sequence container of the METS section, if METS/MODS is
	 * available.
	 * 
	 * If no METS/MODS is available, use filenames in corresponding directory to
	 * create page order. Please note, that this can lead to unexpected results,
	 * since it is assumed, that filenames for images and corresponding
	 * OCR-data match exactly by filename (without concerning file extensions).
	 * 
	 * @return ordered list of {@link DigitalPage pages}
	 * 
	 */
	List<DigitalPage> getDigitalPagesInOrder();

	/**
	 * 
	 * Enrich generated PDF file in METS metadata with
	 * <ul>
	 * <li>FileGroup USE="DOWNLOAD" that holds PDF information</li>
	 * <li>metsHdr-Agent entry for Digital Derivans Tool</li>
	 * </ul>
	 * 
	 * @param fileId
	 * @return isEnriched
	 */
	boolean enrichPDF(String fileId);


	/*
	 * Where to find the Metastore input?
	 */
	String usedStore();


	/**
	 * 
	 * Alter or re-set metadata Filegroup for images
	 * 
	 * @param imageGroup
	 */
	void setFileGroupImages(String imageGroup);


	/**
	 * 
	 * Alter or re-set metadata Filegroup for OCR-data
	 * 
	 * @param ocrGroup
	 */
	void setFileGroupOCR(String ocrGroup);


	/**
	 * 
	 * Set optional XPath-Expression to determine
	 * identifier for PDF file
	 * 
	 * @param xPath
	 */
	void setIdentifierExpression(String xPath); 


	/**
	 * 
	 * Return optional identifier expression
	 * 
	 * @return
	 */
	Optional<String> optionalIdentifierExpression();
}