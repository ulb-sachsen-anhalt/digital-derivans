package de.ulb.digital.derivans.model;

import java.nio.file.Path;

import de.ulb.digital.derivans.model.step.DerivateType;

/**
 * 
 * @author hartwig
 *
 */
public class DerivansData {

	private Path pathData;

	private DerivateType type;

	public DerivansData(Path pathIn, DerivateType type) {
		this.pathData = pathIn;
		this.type = type;
	}

	public Path getPath() {
		return pathData;
	}

	public void setPath(Path path) {
		this.pathData = path;
	}

	public DerivateType getType() {
		return type;
	}

	@Override
	public String toString() {
		return type + ":" + pathData;
	}
}
