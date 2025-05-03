package de.ulb.digital.derivans.data.mets;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jdom2.Element;

import de.ulb.digital.derivans.DigitalDerivansRuntimeException;
import de.ulb.digital.derivans.IDerivans;

/**
 * Encapsulate mets:div, i.e. all container elements in logical or physical
 * structs linked to physical file asset, i.e. an Image page or PDF file
 * 
 * @author u.hartwig
 */
public class METSContainer {

	private String id;

	private Map<METSContainerAttributeType, String> attributes = new EnumMap<>(METSContainerAttributeType.class);

	private String label = IDerivans.UNKNOWN;

	// associated parent container
	private METSContainer parent;

	// associated child container
	public METSContainer getParent() {
		return parent;
	}

	public void setParent(METSContainer parent) {
		this.parent = parent;
	}

	private List<METSContainer> children = new ArrayList<>();

	private METSContainerType type;

	private Element element;

	public METSContainer(String id, Element element) {
		this.id = id;
		this.element = element;
		this.type = METSContainerType.valueOf(this.element.getAttributeValue("TYPE").toUpperCase());
		this.determineAttributes();
		this.determineLabel();
		this.determineHierarchy();
	}

	
	public METSContainer(Element element) {
		this(element.getAttributeValue("ID"), element);
	}
	
	public String getId() {
		return this.id;
	}
	
	public METSContainerType getType() {
		return this.type;
	}
	
	public List<METSContainer> getChildren() {
		return this.children;
	}
	
	public void setChildren(List<METSContainer> childs) {
		this.children = childs;
	}
	
	private void determineAttributes() {
		if (this.element != null) {
			for (var elat : this.element.getAttributes()) {
				String attrName = elat.getName();
				var cntAttrType = METSContainerAttributeType.valueOf(attrName);
				if (cntAttrType != null) {
					var elatVal = elat.getValue();
					this.attributes.put(cntAttrType, elatVal);
				}
			}
		}
	}
	
	public String determineLabel() {
		String structType = null;
		if (this.attributes.containsKey(METSContainerAttributeType.LABEL)) {
			String attrLabel = attributes.get(METSContainerAttributeType.LABEL);
			if (attrLabel != null && !attrLabel.isBlank()) {
				this.label = attrLabel;
			}
		} else if (this.attributes.containsKey(METSContainerAttributeType.ORDERLABEL)) {
			String orderLabel = attributes.get(METSContainerAttributeType.ORDERLABEL); // ORDERLABEL
			if (orderLabel != null && !orderLabel.isBlank()) {
				this.label = orderLabel;
			}
		} else {
			structType = this.type.name();
			Optional<String> optMapping = METSContainerType.getTranslation(structType);
			if (optMapping.isPresent()) {
				this.label = optMapping.get();
			}
		}
		return this.label;
	}

	/**
	 * Form nested logical structures for mets:structMap@TYPE="LOGICAL"/mets:div
	 */
	private void determineHierarchy() {
		List<Element> kids = this.element.getChildren("div", METS.NS_METS);
		if(! kids.isEmpty()) {
			traverse(this);
		}
	}

	private void traverse(METSContainer parent) {
		List<Element> kids = parent.get().getChildren("div", METS.NS_METS);
		if(! kids.isEmpty()) {
			for (var kid : kids) {
				METSContainer curr = new METSContainer(kid);
				parent.addChild(curr);
				traverse(curr);
			}
		}

	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public boolean isMediaContainer() {
		return METSContainerType.MEDIA_CONTAINER.stream().anyMatch(p -> p.equals(this.type));
	}
	
	public boolean isNewspaperStruct() {
		return METSContainerType.NEWSPAPER_CONTAINER_PARENT.stream().anyMatch(p -> p.equals(this.type));
	}
	
	public Element get() {
		return this.element;
	}

	public void setAttributes(Map<METSContainerAttributeType, String> attributes) {
		this.attributes = new EnumMap<>(attributes);
	}

	public void addAttribute(METSContainerAttributeType type, String value) {
		this.attributes.put(type, value);
	}

	public String getAttribute(String label) throws DigitalDerivansRuntimeException {
		var t = METSContainerAttributeType.get(label);
		if (this.attributes.containsKey(t)) {
			return this.attributes.get(t);
		}
		return null;
	}

	@Override
	public String toString() {
		var s = new StringBuilder();
		s.append("ID@" + this.id);
		s.append(",TYPE@" + this.type);
		s.append(",childs:" + this.children.size());
		return s.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof METSContainer)) {
			return false;
		}
		METSContainer otherFile = (METSContainer) other;
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

	public void addChild(METSContainer child) {
		if (!this.children.contains(child)) {
			this.children.add(child);
		}
	}

}
