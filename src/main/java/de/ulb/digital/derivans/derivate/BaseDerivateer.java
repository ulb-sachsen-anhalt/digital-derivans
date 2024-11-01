package de.ulb.digital.derivans.derivate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.data.DerivansPathResolver;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.step.DerivateType;

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
	
	protected DerivateType derivateType;
	
	protected List<DigitalPage> digitalPages;
	
	protected DerivansPathResolver resolver;

	protected Optional<String> outputPrefix;

	public BaseDerivateer(DerivansData input, DerivansData output) {
		this.input = input;
		this.output = output;
		this.resolver = new DerivansPathResolver();
		this.derivateType = output.getType();
		this.outputPrefix = Optional.empty();
	}
	
	public void setResolver(DerivansPathResolver resolver) {
		this.resolver = resolver;
	}
	
	public DerivansPathResolver getResolver() {
		return this.resolver;
	}
	
	@Override
	public DerivansData getInput() {
		return this.input;
	}

	public DerivansData getOutput() {
		return this.output;
	}

	/**
	 * Dummy implementation of Interface
	 */
	@Override
	public int create() throws DigitalDerivansException {
		return 0;
	}

	@Override
	public List<DigitalPage> getDigitalPages() {
		return this.digitalPages;
	}

	@Override
	public void setDigitalPages(List<DigitalPage> pages) {
		this.digitalPages = new ArrayList<>(pages);
	}

	public Optional<String> getOutputPrefix() {
		return outputPrefix;
	}

	public void setOutputPrefix(String prefix) {
		if (prefix != null) {
			this.outputPrefix = Optional.of(prefix);
		}
	}
	
	@Override
	public DerivateType getType() {
		return this.derivateType;
	}
	
}
