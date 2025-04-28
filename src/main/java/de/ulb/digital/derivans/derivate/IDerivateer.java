package de.ulb.digital.derivans.derivate;

import java.util.List;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.IDerivate;
import de.ulb.digital.derivans.model.step.DerivateType;

/**
 * 
 * Derivateer API
 * 
 * @author hartwig
 *
 */
public interface IDerivateer {


	/*
	 * Default METS file group required by DDB / DFG viewer
	 */
	String IMAGE_DIR_DEFAULT = "DEFAULT";

	/*
	 * METS file group used for ULB OAI harvesting and OCR-ing
	 */
	String IMAGE_DIR_MAX = "MAX";

	/**
	 * ULB specific Workflow directories
	 * IMAGE_FOOTER		: images with footer appended and quality set t
	 * IMAGE_80			: jpg images with quality set to 80% and max dimension depending on media type
	 * 					  (input for PDF generation)
	 * (DSpace specific image bundles)
	 * IMAGE_PREVIEW	: jpg images with max dimension forced to 1000px
	 * IMAGE_THUMBNAIL	: jpg images with max dimension forced to 128px
	 * 
	*/
	String IMAGE_FOOTER = "IMAGE_FOOTER";
	String IMAGE_Q80 = "IMAGE_80";
	String IMAGE_PREVIEW = "BUNDLE_BRANDED_PREVIEW__";
	String IMAGE_THUMBNAIL = "BUNDLE_THUMBNAIL__";

	/*
	 * METS file group used by Kitodo.Presentation
	 */
	String IMAGE_DIR_ORIGINAL = "ORIGINAL";

	/*
	 * Default METS file group for OCR-data with, most likely,
	 * MIMETYPE="application/alto+xml"
	 */
	String FULLTEXT_DIR = "FULLTEXT";

	/*
	 * Mark all unresolved information about author, title, ...
	 */
	String UNKNOWN = "n.a.";

	/**
	 * 
	 * Derive digitalization data.
	 * 
	 * Return number of created derivates, which will be 1 in case of a PDF file
	 * and can be many with regular images.
	 * 
	 * @return
	 * @throws DigitalDerivansException
	 */
	int create() throws DigitalDerivansException;

	List<DigitalPage> getDigitalPages();

	void setDigitalPages(List<DigitalPage> pages);

	void setDerivate(IDerivate derivate);

	IDerivate getDerivate();

	/**
	 * 
	 * Get to know what {@link DerivateType} this one will create.
	 * 
	 * @return
	 */
	DerivateType getType();

	DerivansData getInput();

	void setInput(DerivansData input);

	DerivansData getOutput();

	void setOutput(DerivansData output);
}
