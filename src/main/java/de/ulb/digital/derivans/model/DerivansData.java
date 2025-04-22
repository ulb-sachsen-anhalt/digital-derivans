package de.ulb.digital.derivans.model;

import java.nio.file.Path;

import de.ulb.digital.derivans.model.step.DerivateType;

/**
 * 
 * @author hartwig
 *
 */
public class DerivansData {

	private Path rootDir;

	private String subDir;

	private DerivateType type;

	public DerivansData(Path rootDir, String subDir, DerivateType type) {
		this.rootDir = rootDir;
		this.subDir = subDir;
		this.type = type;
	}

	public Path getRootDir() {
		return rootDir;
	}

	public String getSubDir() {
		return this.subDir;
	}

	public DerivateType getType() {
		return type;
	}

	@Override
	public String toString() {
		return type + ":" + rootDir;
	}
}
