package de.ulb.digital.derivans.data.mets;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.jdom2.Element;

import de.ulb.digital.derivans.DigitalDerivansException;

/**
 * 
 * Encapsulate file information (inclusive reference location)
 * Please note:
 * 
 * 1) Although METS schema permitts unbounded file location elements
 * (=FLocat) for sake of simplicity we assume always one
 * 
 * 2) A File representing a physical page is linked to more than
 * one container since it may include a physical page as well
 * as several logical sections.
 * 
 * @author u.hartwig
 */
public class METSFile {

	private String fileGroup;

	private String fileId;

	private String contentIds;

	private String pageLabel;

	private String mimeType;

	private String location;

	private Path localRootPath;

	private String locationType;

	/*
	 * List any containers this File has been attached.
	 * With respect to DFG METS there must exist at least two linked containers.
	 * 
	 * 1 mets:div@TYPE="page" (from structMap@TYPE="PHYSICAL")
	 * 1+ mets:div (from structMap@TYPE="LOGICAL")
	 */
	private List<METSContainer> linkedContainers = new ArrayList<>();

	public METSFile(String id, String location) {
		this("MAX", id, "image/jpeg", location, "URL");
	}

	public METSFile(String fileGroup, String id, String mimeType, String location) {
		this(fileGroup, id, mimeType, location, "URL");
	}

	public METSFile(String fileGroup, String id, String mimeType, String location, String locationType) {
		this.fileGroup = fileGroup;
		this.fileId = id;
		this.mimeType = mimeType;
		this.location = location;
		this.locationType = locationType;
	}

	public void addLinkedContainers(METSContainer container) {
		this.linkedContainers.add(container);
	}

	/**
	 * 
	 * Get @CONTAINERIDS attribute from corresponding physical page
	 * if present, otherwise use current set value
	 * 
	 * @return
	 */
	public String getContentIds() {
		if (!this.linkedContainers.isEmpty()) {
			PredicatePage predPage = new PredicatePage();
			Optional<String> optCid = this.linkedContainers.stream()
					.filter(predPage)
					.map(c -> c.getAttribute("CONTENTIDS"))
					.filter(Objects::nonNull)
					.findFirst();
			if (optCid.isPresent()) {
				this.contentIds = optCid.get();
			}
		}
		return this.contentIds;
	}

	public void setContentIds(String cid) {
		this.contentIds = cid;
	}

	public String getPageLabel() {
		return pageLabel;
	}

	public void setPageLabel(String pageLabel) {
		this.pageLabel = pageLabel;
	}

	public List<METSContainer> getLinkedContainers() {
		return this.linkedContainers;
	}

	public String getFileId() {
		return this.fileId;
	}

	public String getLocation() {
		return this.location;
	}

	public void setLocalRoot(Path root) {
		this.localRootPath = root;
	}

	public Path getLocalPath() throws DigitalDerivansException {
		int lastSlashIndex = this.location.lastIndexOf('/');
		if (lastSlashIndex > -1) { // even if it starts with leading slash
			String[] tokens = this.location.split("/");
			String lastPart = tokens[tokens.length - 1];
			int lastDotIndex = lastPart.lastIndexOf('.');
			if (lastDotIndex == -1) { // filename extension missing
				if (this.mimeType.toLowerCase().contains("tif")) {
					lastPart = lastPart + ".tiff";
				} else {
					lastPart = lastPart + ".jpg";
				}
			}
			var theName = String.format("%s/%s", this.fileGroup, lastPart);
			Path fileLoc = this.localRootPath.resolve(theName);
			if (!Files.exists(fileLoc)) {
				String alarma = String.format("Try to set invalid local path %s", fileLoc);
				throw new DigitalDerivansException(alarma);
			}
			return fileLoc;
		}
		return Path.of(this.location);
	}

	/**
	 * 
	 * Integrate a new mets:file Element
	 * for adding assets (like PDF file)
	 * 
	 * @return
	 */
	public Element asElement() {
		var fileEl = new Element("file", METS.NS_METS);
		fileEl.setAttribute("ID", this.fileId);
		fileEl.setAttribute("MIMETYPE", mimeType);
		var fLocat = new Element("FLocat", METS.NS_METS);
		fLocat.setAttribute("LOCTYPE", this.locationType);
		fLocat.setAttribute("href", this.location, METS.NS_XLINK);
		fileEl.addContent(fLocat);
		return fileEl;
	}

	public String getFileGroup() {
		return this.fileGroup;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof METSFile)) {
			return false;
		}
		METSFile otherFile = (METSFile) other;
		return this.fileId.equals(otherFile.getFileId());
	}

	@Override
	public int hashCode() {
		int h = 0;
		for (int o = 0; o < this.fileId.length(); o++) {
			h += this.fileId.codePointAt(o);
		}
		return h;
	}
}

class PredicatePage implements Predicate<METSContainer> {

	@Override
	public boolean test(METSContainer t) {
		return t.getType() == METSContainerType.PAGE;
	}

}
