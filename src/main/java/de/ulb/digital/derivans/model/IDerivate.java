package de.ulb.digital.derivans.model;

import java.util.List;

import de.ulb.digital.derivans.DigitalDerivansException;

public interface IDerivate {

	void init(String startSubDir) throws DigitalDerivansException;

	List<DigitalPage> getAllPages();

}