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
	 * METS file group used for OAI harvesting
	 */
	String IMAGE_DIR_MAX = "MAX";

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
