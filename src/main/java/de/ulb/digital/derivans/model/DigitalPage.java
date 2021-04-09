package de.ulb.digital.derivans.model;

import java.nio.file.Path;
import java.util.Optional;

import de.ulb.digital.derivans.model.ocr.OCRData;

/**
 * 
 * Representation of a single digital page with the following properties:
 * <ul>
 * <li>orderNr: order of page in a larger, compound work</li>
 * <li>image file: {@link File} path to physical image representation (via
 * METS-FileGroup)</li>
 * <li>opt. ocr file: {@link File} path to physical OCR Data</li>
 * <li>opt. ocr data: {@link OCRData} that has been read from ocr file</li>
 * <li>opt. identifier: unique, granular identifier</li>
 * </ul>
 * 
 * @author hartwig
 *
 */
public class DigitalPage {

	private Integer orderNr;

	/**
	 * Pointer to single physical image for given page
	 * Subject to change due subsequent derivation actions
	 */
	private File imgFile;

	/**
	 * Pointer to optional OCR Data for given page
	 */
	private Optional<File> ocrFile = Optional.empty();

	/**
	 * Container for optional OCRData used for PDF text layer
	 */
	private Optional<OCRData> ocrData = Optional.empty();

	private Optional<String> uniqueIdentifier = Optional.empty();
	
	/**
	 * If Image was extended by Footer vertical, store the added amount.
	 * Optional OCR-Data needs to know if page was extended.
	 */
	private Optional<Integer> footerHeight = Optional.empty();

	
	public DigitalPage(int orderNr) {
		this.orderNr = orderNr;
	}
	
	public DigitalPage(int orderNr, String filePointer) {
		this.orderNr = orderNr;
		this.imgFile = new File(FileType.IMAGE, Path.of(filePointer));
	}

	public DigitalPage(Path physicalPath) {
		this.imgFile = new File(FileType.IMAGE, physicalPath);
	}

	public void setOcrFile(Path ocrPath) {
		this.ocrFile = Optional.of(new File(FileType.OCR, ocrPath));
	}

	public Optional<Path> getOcrFile() {
		if (this.ocrFile.isPresent()) {
			File theFile = this.ocrFile.get();
			return Optional.of(theFile.getPysicalPath());
		}
		return Optional.empty();
	}

	public Integer getOrderNr() {
		return orderNr;
	}

	public String getImageFile() {
		return this.imgFile.physicalPath.toString();
	}

	public void setIdentifier(String id) {
		if (id != null) {
			this.uniqueIdentifier = Optional.of(id);
		}
	}

	public Optional<String> getIdentifier() {
		return this.uniqueIdentifier;
	}

	public Path getImagePath() {
		return this.imgFile.physicalPath;
	}

	public void setImagePath(Path path) {
		this.imgFile = new File(FileType.IMAGE, path);
	}

	public Optional<OCRData> getOcrData() {
		return ocrData;
	}

	public void setOcrData(OCRData ocrData) {
		this.ocrData = Optional.of(ocrData);
	}

	public Optional<Integer> getFooterHeight() {
		return footerHeight;
	}

	public void setFooterHeight(int footerHeight) {
		this.footerHeight = Optional.of(footerHeight);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		if (orderNr != null)
			builder.append(String.format("%04d", orderNr)).append(":");
		else if (this.imgFile != null && this.imgFile.physicalPath != null)
			builder.append(this.imgFile.physicalPath);
		builder.append("]");
		return builder.toString();
	}

	/**
	 * 
	 * Store data about associated physical resources
	 * 
	 * @author u.hartwig
	 *
	 */
	static class File {
		private FileType fileType;
		private Path physicalPath;

		public File(FileType fileType, Path phyiscalPath) {
			this.fileType = fileType;
			this.physicalPath = phyiscalPath;
		}

		public Path getPysicalPath() {
			return physicalPath;
		}

		public FileType getFileType() {
			return fileType;
		}

		@Override
		public String toString() {
			return fileType.name() + ":" + physicalPath;
		}

	}

	enum FileType {
		IMAGE, OCR
	}
}
