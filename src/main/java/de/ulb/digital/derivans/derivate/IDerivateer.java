package de.ulb.digital.derivans.derivate;

import java.util.List;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.model.DerivateType;
import de.ulb.digital.derivans.model.DigitalPage;

/**
 * 
 * 
 * @author hartwig
 *
 */
public interface IDerivateer {

	/**
	 * 
	 * Derive digitalization data.
	 * 
	 * @return
	 * @throws DigitalDerivansException
	 */
	boolean create() throws DigitalDerivansException;

//	boolean create(String conformanceLevel) throws DigitalDerivansException;

	List<DigitalPage> getDigitalPages();
	
	void setDigitalPages(List<DigitalPage> pages);
	
	DerivateType getType();
}
