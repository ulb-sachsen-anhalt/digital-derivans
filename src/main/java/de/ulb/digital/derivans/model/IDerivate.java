package de.ulb.digital.derivans.model;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.data.mets.METS;

/**
 * 
 * Input, output and final destination of Derivans' efforts.
 * 
 * @author u.hartwig
 */
public interface IDerivate {

	void init(Path derivatetPath) throws DigitalDerivansException;
	
	boolean isInited();

	DerivateStruct getStructure();

	List<DigitalPage> getAllPages();

	Path getPathInputDir();

	// Optional<METS> optMetadata();

	boolean hasMetadata();

	void setOcr(Path ocrPath) throws DigitalDerivansException;

	String getImageLocalDir();

	void setImageLocalDir(String localSubDir);
}