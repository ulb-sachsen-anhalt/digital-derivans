package de.ulb.digital.derivans.derivate;

import de.ulb.digital.derivans.DigitalDerivansException;

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

	boolean create(String conformanceLevel) throws DigitalDerivansException;

}
