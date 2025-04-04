package de.ulb.digital.derivans.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.data.mets.METS;

/**
 * 
 * API describe Derivate to create
 * 
 * @author u.hartwig
 */
public interface IDerivate {

	void init(Path derivatetPath) throws DigitalDerivansException;
	
	boolean isInited();

	List<DigitalPage> getAllPages();

	Path getPathInputDir();

	Optional<METS> optMetadata();

	void setOcr(Path ocrPath) throws DigitalDerivansException;
}