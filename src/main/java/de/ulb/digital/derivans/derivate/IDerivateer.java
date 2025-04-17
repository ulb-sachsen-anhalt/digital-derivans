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

	/**
	 * 
	 * Get to know what {@link DerivateType} this one will create.
	 * 
	 * @return
	 */
	DerivateType getType();

	DerivansData getInput();
}
