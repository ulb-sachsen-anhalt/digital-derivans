package de.ulb.digital.derivans.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * Representation of Tree-like Document Outline
 * 
 * @author hartwig
 *
 */
public class DigitalStructureTree {

	private int page = 0;
	private String label;
	private List<DigitalStructureTree> subs = new LinkedList<>();
	private DigitalStructureTree parent;

	public DigitalStructureTree() {
	}

	public DigitalStructureTree(int page, String label, List<DigitalStructureTree> subStructures) {
		this.page = page;
		this.label = label;
		this.subs.addAll(subStructures);
	}

	public DigitalStructureTree(int value, String label) {
		this(value, label, new LinkedList<>());
	}

	/**
	 * Number of page.
	 * 
	 * Please note: Valid order starts from "1"
	 * 
	 * @return pageNumber
	 */
	public int getPage() {
		return page;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public void setParentStructure(DigitalStructureTree parent) {
		this.parent = parent;
	}

	public DigitalStructureTree getParentStructure() {
		return this.parent;
	}

	public boolean hasParentStructure() {
		return this.parent != null;
	}

	/**
	 * 
	 * Traverse up-tree and store a reference
	 * to every parent found along the side
	 * 
	 * Handy for iterate in case of investigating
	 * duplicate links from parent structures
	 * 
	 * @return
	 */
	public List<DigitalStructureTree> getParents() {
		List<DigitalStructureTree> parents = new ArrayList<>();
		if(this.hasParentStructure()) {
			var currParent = this.getParentStructure();
			while(currParent != null) {
				parents.add(currParent);
				currParent = currParent.getParentStructure();
			}
		}
		return parents;
	}

	public boolean hasSubstructures() {
		return !subs.isEmpty();
	}

	public void addSubStructure(DigitalStructureTree structure) {
		subs.add(structure);
	}

	public boolean removeSubStructure(DigitalStructureTree subStructure) {
		if (subs.contains(subStructure)) {
			return subs.remove(subStructure);
		}
		return false;
	}

	public List<DigitalStructureTree> getSubstructures() {
		return new LinkedList<>(this.subs);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DigitalStructureTree [page=").append(page).append(", ");
		if (label != null)
			builder.append("label=").append(label).append(", ");
		if (subs != null)
			builder.append("subs=").append(subs);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + page;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DigitalStructureTree other = (DigitalStructureTree) obj;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label)) {
			return false;
		} else {
			return page == other.page;
		}

		return true;
	}

}