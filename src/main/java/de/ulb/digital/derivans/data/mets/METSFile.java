package de.ulb.digital.derivans.data.mets;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.jdom2.Element;

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

	private String id;

	private String mimeType;

	private String location;

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
		this.id = id;
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
	 * 
	 * @return
	 */
	public Optional<String> getContentIds() {
		PredicatePage predPage = new PredicatePage();
		return this.linkedContainers.stream()
				.filter(predPage)
				.map(c -> c.getAttribute("CONTENTIDS"))
				.filter(Objects::nonNull)
				.findFirst();
	}

	public List<METSContainer> getLinkedContainers() {
		return this.linkedContainers;
	}

	public String getId() {
		return this.id;
	}

	public String getLocation() {
		return this.location;
	}

	/**
	 * 
	 * Integrate a new mets:file Element
	 * for adding assets (like PDF file)
	 * 
	 * @return
	 */
	public Element asElement() {
		var fileEl = new Element("file", METSHandler.NS_METS);
		fileEl.setAttribute("ID", this.id);
		fileEl.setAttribute("MIMETYPE", mimeType);
		var fLocat = new Element("FLocat", METSHandler.NS_METS);
		fLocat.setAttribute("LOCTYPE", this.locationType);
		fLocat.setAttribute("href", this.location, METSHandler.NS_XLINK);
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
		return this.id.equals(otherFile.getId());
	}

	@Override
	public int hashCode() {
		int h = 0;
		for (int o = 0; o < this.id.length(); o++) {
			h += this.id.codePointAt(o);
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
