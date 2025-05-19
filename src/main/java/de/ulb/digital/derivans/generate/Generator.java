package de.ulb.digital.derivans.generate;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.DigitalDerivansRuntimeException;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.IDerivate;
import de.ulb.digital.derivans.model.step.DerivateStep;
import de.ulb.digital.derivans.model.step.DerivateType;

/**
 * 
 * Basic Generator
 * 
 * @author u.hartwig
 *
 */
public abstract class Generator {

	protected Path rootDir;

	protected DerivateStep step;

	protected DerivateType derivateType;

	protected List<DigitalPage> digitalPages;

	protected Optional<String> inputPrefix = Optional.empty();

	protected Optional<String> outputPrefix = Optional.empty();

	protected IDerivate derivate;

	public static final String EXT_JPG = ".jpg";

	public static final String EXT_TIF = ".tif";

	public int create() throws DigitalDerivansException {
		return 0;
	}

	public List<DigitalPage> getDigitalPages() {
		return this.digitalPages;
	}

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

	public DerivateType getType() {
		return this.derivateType;
	}

	public void setDerivate(IDerivate derivate) {
		this.derivate = derivate;
		this.rootDir = this.derivate.getRootDir();
		this.digitalPages = this.derivate.getAllPages();
	}

	public IDerivate getDerivate() {
		return this.derivate;
	}

	public void setStep(DerivateStep step) throws DigitalDerivansException {
		if (step == null) {
			throw new DigitalDerivansException("Refuse invalid step = null!");
		}
		this.step = step;
		this.derivateType = step.getOutputType();
	}

	public DerivateStep getStep() {
		return this.step;
	}

	protected Path setInpath(DigitalPage page) {
		Path pathIn = page.getFile().withDirname(this.step.getInputDir());
		var pathFnm = pathIn.getFileName();
		var pathDir = pathIn.getParent();
		StringBuilder fName = new StringBuilder(this.setFileExtension(pathFnm, this.step.getInputType()));
		this.getInputPrefix().ifPresent(prefix -> fName.insert(0, prefix));
		pathIn = pathDir.resolve(fName.toString());
		return pathIn;
	}

	protected Path setOutpath(DigitalPage page) {
		Path pathOut = page.getFile().withDirname(this.step.getOutputDir());
		var pathFnm = pathOut.getFileName();
		var pathDir = pathOut.getParent();
		StringBuilder fName = new StringBuilder(this.setFileExtension(pathFnm, this.step.getOutputType()));
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
