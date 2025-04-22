package de.ulb.digital.derivans.derivate;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.data.io.DerivansPathResolver;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.IDerivate;
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

	// protected Path inputDir;

	// protected Path outputRootDir;

	protected DerivateType derivateType;

	protected List<DigitalPage> digitalPages;

	protected Optional<String> outputPrefix;

	protected IDerivate derivate;


	public BaseDerivateer() {
		this.outputPrefix = Optional.empty();
	}

	public BaseDerivateer(DerivansData input, DerivansData output) {
		this.input = input;
		this.output = output;
		this.derivateType = output.getType();
		this.outputPrefix = Optional.empty();
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

	@Override
	public void setDerivate(IDerivate derivate) {
		this.derivate = derivate;
		this.digitalPages = this.derivate.getAllPages();
	}

	@Override
	public IDerivate getDerivate() {
		return this.derivate;
	}

	@Override
	public void setInput(DerivansData input) {
		this.input = input;
		// this.inputDir = input.getRootDir();
	}

	@Override
	public DerivansData getInput() {
		return this.input;
	}

	@Override
	public void setOutput(DerivansData output) {
		this.output = output;
		// this.outputRootDir = output.getRootDir();
	}

	@Override
	public DerivansData getOutput() {
		return this.output;
	}

	protected Path setOutpath(DigitalPage page) {
		Path pathOut = page.getFile().withDirname(this.output.getSubDir());
		if(this.getOutputPrefix().isPresent()) {
			var pathDir = pathOut.getParent();
			var pathFnm = pathOut.getFileName();
			var optPrefix = this.getOutputPrefix().get();
			var prefixedFile = Path.of(optPrefix + pathFnm.toString());
			pathOut = pathDir.resolve(prefixedFile);
		}
		return pathOut;
	}
}
