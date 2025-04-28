package de.ulb.digital.derivans.derivate;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.DigitalDerivansRuntimeException;
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

	protected DerivateType derivateType;

	protected List<DigitalPage> digitalPages;

	protected Optional<String> inputPrefix = Optional.empty();

	protected Optional<String> outputPrefix = Optional.empty();

	protected IDerivate derivate;

	public static final String EXT_JPG = ".jpg";

	public static final String EXT_TIF = ".tif";

	public BaseDerivateer() {
	}

	public BaseDerivateer(DerivansData input, DerivansData output) {
		this.input = input;
		this.output = output;
		this.derivateType = output.getType();
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

	public Optional<String> getInputPrefix() {
		return this.inputPrefix;
	}

	public void setInputPrefix(String prefix) {
		if (prefix != null) {
			this.inputPrefix = Optional.of(prefix);
		}
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
	}

	@Override
	public DerivansData getInput() {
		return this.input;
	}

	@Override
	public void setOutput(DerivansData output) {
		this.output = output;
		this.derivateType = output.getType();
	}

	@Override
	public DerivansData getOutput() {
		return this.output;
	}

	protected Path setInpath(DigitalPage page) {
		Path pathIn = page.getFile().withDirname(this.input.getSubDir());
		var pathFnm = pathIn.getFileName();
		var pathDir = pathIn.getParent();
		StringBuilder fName = new StringBuilder(this.setFileExtension(pathFnm, this.input.getType()));
		this.getInputPrefix().ifPresent(prefix -> fName.insert(0, prefix));
		pathIn = pathDir.resolve(fName.toString());
		return pathIn;
	}

	protected Path setOutpath(DigitalPage page) {
		Path pathOut = page.getFile().withDirname(this.output.getSubDir());
		var pathFnm = pathOut.getFileName();
		var pathDir = pathOut.getParent();
		StringBuilder fName = new StringBuilder(this.setFileExtension(pathFnm, this.output.getType()));
		this.getOutputPrefix().ifPresent(prefix -> fName.insert(0, prefix));
		pathOut = pathDir.resolve(fName.toString());
		return pathOut;
	}

	private String setFileExtension(Path fileName, DerivateType dType) {
		String fnstr = fileName.toString();
		if (fnstr.contains(".")) { // common local file
			int rightOffset = fnstr.lastIndexOf(".");
			String fileNamePrt = fnstr.substring(0, rightOffset);
			String fileNameExt = fnstr.substring(rightOffset);
			if ((dType == DerivateType.JPG || dType == DerivateType.JPG_FOOTER)
					&& !EXT_JPG.equals(fileNameExt)) {
				return fileNamePrt + EXT_JPG;
			} else if (dType == DerivateType.TIF) {
				return fileNamePrt + EXT_TIF;
			}
			return fnstr;
		} else if (dType == DerivateType.JPG || dType == DerivateType.JPG_FOOTER) { // loaded via OAI
			return fnstr + EXT_JPG;
		}
		String msg = String.format("Fail to set any ext for %s, type %s", fileName, this.derivateType);
		throw new DigitalDerivansRuntimeException(msg);
	}
}
