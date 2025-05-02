package de.ulb.digital.derivans.model;

import java.nio.file.Path;
import java.util.Optional;

import de.ulb.digital.derivans.model.ocr.OCRData;

/**
 * 
 * Representation of a single digital page with the following properties:
 * <ul>
 * <li>orderNr: order of page in a larger, compound work</li>
 * <li>image file: {@link File} path to image of this page</li>
 * <li>opt. ocr file: {@link File} path to physical OCR Data</li>
 * <li>opt. ocr data: {@link OCRData} that has been read from ocr file</li>
 * <li>opt. identifier: unique, granular identifier</li>
 * </ul>
 * 
 * @author hartwig
 *
 */
public class DigitalPage {

	private Integer orderNr = 0;

	private String pageId;

	private String pageLabel;

	/**
	 * Pointer to single physical image for given page
	 * Subject to change due subsequent derivation actions
	 */
	private DigitalPage.File file;

	/**
	 * Pointer to optional OCR Data for given page
	 */
	private Optional<File> ocrFile = Optional.empty();

	/**
	 * Container for optional OCRData used for PDF text layer
	 */
	private Optional<OCRData> ocrData = Optional.empty();

	private Optional<String> contentIds = Optional.empty();
	
	/**
	 * If Image was extended by Footer vertical, store the added amount.
	 * Optional OCR-Data needs to know if page was extended.
	 */
	private Optional<Integer> footerHeight = Optional.empty();

	
	public DigitalPage(String pageId, int orderNr, Path localFilePath) {
		this.pageId = pageId;
		this.orderNr = orderNr;
		DigitalPage.File dFile = new File(FileType.IMAGE, localFilePath);
		this.file = dFile;
	}

	public void setOcrFile(Path ocrPath) {
		this.ocrFile = Optional.of(new File(FileType.OCR, ocrPath));
	}

	public Optional<Path> getOcrFile() {
		if (this.ocrFile.isPresent()) {
			File theFile = this.ocrFile.get();
			return Optional.of(theFile.getPath());
		}
		return Optional.empty();
	}

	public String getPageId() {
		return this.pageId;
	}

	public Integer getOrderNr() {
		return orderNr;
	}

	public DigitalPage.File getFile() {
		return this.file;
	}

	public void setContentIds(String id) {
		if (id != null) {
			this.contentIds = Optional.of(id);
		}
	}

	public Optional<String> optContentIds() {
		return this.contentIds;
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

	public void setPageLabel(String pageLabel) {
		if(pageLabel != null) {
			this.pageLabel = pageLabel;
		}
	}

	public Optional<String> getPageLabel() {
		if(this.pageLabel != null) {
			return Optional.of(this.pageLabel);
		}
		return Optional.empty();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		if (orderNr != null)
			builder.append(String.format("%04d", orderNr)).append(":");
		builder.append(this.file.toString());
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
	public static class File {
		private FileType fileType;
		private Path physicalPath;

		public File(FileType fileType, Path phyiscalPath) {
			this.fileType = fileType;
			this.physicalPath = phyiscalPath;
		}

		public Path getPath() {
			return this.physicalPath;
		}

		public String getFileName() {
			return this.physicalPath.getFileName().toString();
		}

		public Path withDirname(String newDirname) {
			Path currDir = this.physicalPath.getParent();
			Path currFile = this.physicalPath.getFileName();
			return currDir.getParent().resolve(newDirname).resolve(currFile);
		}

		public String subDir() {
			return this.physicalPath.getParent().getFileName().toString();
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
