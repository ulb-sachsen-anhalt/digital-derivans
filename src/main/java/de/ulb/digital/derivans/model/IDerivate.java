package de.ulb.digital.derivans.model;

import java.nio.file.Path;
import java.util.List;

import de.ulb.digital.derivans.DigitalDerivansException;

/**
 * 
 * Input, output and final destination of Derivans' efforts.
 * 
 * @author u.hartwig
 */
public interface IDerivate {

	void init(Path startPath) throws DigitalDerivansException;
	
	boolean isInited();

	DerivateStruct getStructure();

	List<DigitalPage> getAllPages();

	Path getPathRootDir();

	boolean isMetadataPresent();

	void setOcr(Path ocrPath) throws DigitalDerivansException;

	// String getImageLocalDir();

	// void setImageLocalDir(String localSubDir);

	void setStartFileExtension(String startFileExtension);

	String getStartFileExtension();
}