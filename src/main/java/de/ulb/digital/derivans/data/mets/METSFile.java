package de.ulb.digital.derivans.data.mets;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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

	private Optional<String> contentIds;

	private String pageLabel;

	private Path localRootPath;

	private String location;

	private String locationType;

	private String mimeType;

	private Optional<String> checksum;

	private Optional<String> checksumType;

	private Optional<Long> size;

	private Optional<String> created;

	/*
	 * List any containers this File has been attached.
	 * With respect to DFG METS there must exist at least two linked containers.
	 * 
	 * 1 mets:div@TYPE="page" (from structMap@TYPE="PHYSICAL")
	 * 1+ mets:div (from structMap@TYPE="LOGICAL")
	 */
	private List<METSContainer> linkedContainers = new ArrayList<>();

	public METSFile(Element element, String fileGroup) {
		this.mimeType = element.getAttributeValue("MIMETYPE");
		this.fileId = element.getAttributeValue("ID");
		var fstLocat = element.getChildren("FLocat", METS.NS_METS).get(0);
		String hRef = fstLocat.getAttributeValue("href", METS.NS_XLINK);
		int offsetCtx = hRef.lastIndexOf('/');
		if (offsetCtx > 0) {
			hRef = hRef.substring(offsetCtx + 1);
		}
		int offsetExt = hRef.lastIndexOf('.');
		if (offsetExt < 0) { // no extension detected, make a guess
			if (isTypeJPG(mimeType) && !hRef.endsWith(".jpg")) {
				hRef += ".jpg";
			}
			if (isTypeXML(mimeType) && !hRef.endsWith(".xml")) {
				hRef += ".xml";
			}
		}
		this.location = hRef;
	}

	public METSFile(String id, String location) {
		this(id, location, "MAX", "image/jpeg", "URL");
	}

	public METSFile(String id, String location, String fGroup) {
		this(id, location, fGroup, "image/jpeg", "URL");
	}

	public METSFile(String id, String location, String fileGroup, String mimeType) {
		this(id, location, fileGroup, mimeType, "URL");
	}

	public METSFile(String id, String location, String fileGroup, String mimeType, String locationType) {
		this.fileId = id;
		this.location = location;
		this.fileGroup = fileGroup;
		this.mimeType = mimeType;
		this.locationType = locationType;
	}

	public void setContentIds(String cid) {
		this.contentIds = Optional.of(cid);
	}

	public Optional<String> optContentIds() {
		return this.contentIds;
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

	public Optional<String> getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = Optional.of(checksum);
	}

	public Optional<String> getChecksumType() {
		return checksumType;
	}

	public void setChecksumType(String checksumType) {
		this.checksumType = Optional.of(checksumType);
	}

	public Optional<Long> getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = Optional.of(size);
	}

	public Optional<String> getCreated() {
		return created;
	}

	public void setCreated(String created) {
		this.created = Optional.of(created);
	}

	public Path getLocalPath(boolean testExists) throws DigitalDerivansException {
		String lastPart = this.location;
		int lastSlashIndex = this.location.lastIndexOf('/');
		if (lastSlashIndex > -1) { // even if it starts with leading slash
			String[] tokens = this.location.split("/");
			lastPart = tokens[tokens.length - 1];
		}
		var theName = String.format("%s/%s", this.fileGroup, lastPart);
		Path fileLoc = this.localRootPath.resolve(theName);
		if (testExists && !Files.exists(fileLoc)) {
			String alarma = String.format("Try to set invalid local path %s", fileLoc);
			throw new DigitalDerivansException(alarma);
		}
		return fileLoc;
	}

	/**
	 * 
	 * Integrate a new mets:file Element
	 * for adding assets like PDF file
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
		if (otherFile.getChecksum() != null && this.checksum != null) {
			return otherFile.getChecksum().equals(this.checksum);
		}
		return this.fileId.equals(otherFile.getFileId());
	}

	@Override
	public int hashCode() {
		int h = 0;
		String useAttribute = this.fileId;
		if (this.checksum.isPresent()) {
			useAttribute = this.checksum.get();
		}
		for (int o = 0; o < useAttribute.length(); o++) {
			h += useAttribute.codePointAt(o);
		}
		return h;
	}

	private boolean isTypeJPG(String mimeType) {
		if (mimeType != null && mimeType.contains("image")) {
			return mimeType.contains("jpeg") || mimeType.contains("jpg");
		}
		return false;
	}

	private boolean isTypeXML(String mimeType) {
		return mimeType != null && mimeType.contains("xml");
	}
}

class PredicatePage implements Predicate<METSContainer> {

	@Override
	public boolean test(METSContainer t) {
		return t.getType() == METSContainerType.PAGE;
	}

}
