package de.ulb.digital.derivans.derivate;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.model.DerivansData;

/**
 * 
 * Basic {@link IDerivateer derivans type} to setting input and output.
 * Use only it's sub types, please
 * 
 * @author M3ssman
 *
 */
public class BaseDerivateer implements IDerivateer {

	protected DerivansData input;

	protected DerivansData output;

	public BaseDerivateer(DerivansData input, DerivansData output) {
		this.input = input;
		this.output = output;
	}

	protected DerivansData getInput() {
		return this.input;
	}

	protected DerivansData getOutput() {
		return this.output;
	}

	/**
	 * Dummy implementation of Interface
	 */
	@Override
	public boolean create() throws DigitalDerivansException {
		return false;
	}
}
