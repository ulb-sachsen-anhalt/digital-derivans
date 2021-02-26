package de.ulb.digital.derivans.model;

import java.nio.file.Path;
import java.util.Optional;

/**
 * 
 * Representation of a single digital page with the following properties:
 * <ul>
 * <li>orderNr: where the page must be sorted in a work that it is part of</li>
 * <li>filePointer: valid link to physical representation (via
 * METS-FileGroup)</li>
 * <li>path: Path of physical image representation as local image file</li>
 * <li>optional: sort of unique, granular identifier for each page</li>
 * </ul>
 * 
 * @author hartwig
 *
 */
public class DigitalPage {

	private Integer orderNr;

	private String filePointer;

	private Path physicalPath;

	private Optional<String> uniqueIdentifier = Optional.empty();

	public DigitalPage(int orderNr, String filePointer) {
		this.orderNr = orderNr;
		this.filePointer = filePointer;
		this.uniqueIdentifier = Optional.empty();
	}

	public Integer getOrderNr() {
		return orderNr;
	}

	public String getFilePointer() {
		return this.filePointer;
	}

	public void setIdentifier(String id) {
		if (id != null) {
			this.uniqueIdentifier = Optional.of(id);
		}
	}

	public Optional<String> getIdentifier() {
		return this.uniqueIdentifier;
	}

	public void setPath(Path path) {
		this.physicalPath = path;
	}

	public Path getPath() {
		return this.physicalPath;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		if (orderNr != null)
			builder.append(String.format("%04d", orderNr)).append(":");
		if (filePointer != null)
			builder.append(filePointer);
		builder.append("]");
		return builder.toString();
	}

}
