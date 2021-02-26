package de.ulb.digital.derivans.data;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.mets.model.Mets;
import org.mycore.mets.model.files.File;
import org.mycore.mets.model.files.FileGrp;
import org.mycore.mets.model.struct.Fptr;
import org.mycore.mets.model.struct.LogicalDiv;
import org.mycore.mets.model.struct.LogicalStructMap;
import org.mycore.mets.model.struct.PhysicalSubDiv;
import org.mycore.mets.model.struct.SmLink;
import org.mycore.mets.model.struct.StructLink;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.model.DigitalStructureTree;

/**
 * 
 * 
 * Create Tree-like structure for PDF-Outline
 * 
 * @author hartwig
 *
 */
class StructureMapper {

	private static final Logger LOGGER = LogManager.getLogger(StructureMapper.class);

	private Mets mets;

	private String title;

	// remember latest working link in case of dummy physical-references
	private String latestWorkingphysId;

	public StructureMapper(Mets mets, String title) {
		this.mets = mets;
		this.title = title;
	}

	public DigitalStructureTree build() throws DigitalDerivansException {
		if (this.mets != null) {
			LogicalStructMap lsm = this.mets.getLogicalStructMap();
			if (lsm == null) {
				String msg = "mets is missing logical StructMap!";
				LOGGER.error(msg);
				throw new DigitalDerivansException(msg);
			}
			LogicalDiv logicalRoot = lsm.getDivContainer();
			if (logicalRoot.getType().equalsIgnoreCase("monograph")) {
				DigitalStructureTree structureRoot = new DigitalStructureTree();
				String label = logicalRoot.getLabel();
				if (label == null) {
					label = logicalRoot.getOrderLabel();
					if (label == null) {
						label = this.title;
					}
				}
				structureRoot.setLabel(label);
				structureRoot.setPage(1);
				for (LogicalDiv logicalChild : logicalRoot.getChildren()) {
					// hack around bug: not only div children are respected
					if (logicalChild.getType() != null) {
						DigitalStructureTree subTree = new DigitalStructureTree();
						structureRoot.addSubStructure(subTree);
						extendStructure(subTree, logicalChild);
					}
				}

				// review
				clearStructure(structureRoot);

				return structureRoot;
			}
		} else {
			LOGGER.warn("no mets avaiable");
		}
		return null;
	}

	/**
	 * 
	 * Handle possible invalid links from logicalContainers to physicalSequences for
	 * all descendant structure nodes recursively
	 * 
	 * @param tree
	 */
	private void clearStructure(DigitalStructureTree tree) {
		if (tree.hasSubstructures()) {
			for (DigitalStructureTree subTree : tree.getSubstructures()) {
				if (subTree.getPage() == -1) {
					boolean isRemoved = tree.removeSubStructure(subTree);
					LOGGER.warn("Droped invalid subStructure '{}':{}", subTree, isRemoved);
				}
				clearStructure(subTree);
			}
		}
	}

	void extendStructure(DigitalStructureTree currentNode, LogicalDiv currentLogicalDiv) {
		currentNode.setLabel(getLabel(currentLogicalDiv));
		int pageInSequence = mapLogicalDivToPhysicalSequence(currentLogicalDiv);
		currentNode.setPage(pageInSequence);
		if (currentLogicalDiv.getChildren() != null) {
			for (LogicalDiv child : currentLogicalDiv.getChildren()) {
				DigitalStructureTree subTree = new DigitalStructureTree();
				currentNode.addSubStructure(subTree);
				extendStructure(subTree, child);
			}
		}
	}

	private String getLabel(LogicalDiv logical) {
		String label = logical.getLabel();
		if (label == null || "".equals(label.strip())) {
			String logicalType = logical.getType();
			return mapLogicalType(logicalType);
		}
		return label;
	}

	/**
	 * 
	 * Try to map a logical structure to the order of the corresponding physical
	 * structure. This way the start page of a logical structure
	 * 
	 * @param ld
	 * @return pageNumber (default: 1)
	 */
	private int mapLogicalDivToPhysicalSequence(LogicalDiv ld) {
		String logId = ld.getId();
		String label = getLabel(ld);
		StructLink structLink = mets.getStructLink();
		List<SmLink> smLinksTo = structLink.getSmLinkByFrom(logId);
		if (smLinksTo != null && !smLinksTo.isEmpty()) {
			String physId = smLinksTo.get(0).getTo();
			PhysicalSubDiv physDiv = mapLatestValidContainer(physId, label);
			if (physDiv != null) {
				List<Fptr> fptrs = physDiv.getChildren();
				String fileId = fptrs.get(0).getFileId();
				// inspect ALL FileGrps, not only "MAX"
				for (FileGrp fileGrp : mets.getFileSec().getFileGroups()) {
					for (int i = 0; i < fileGrp.getFileList().size(); i++) {
						File current = fileGrp.getFileList().get(i);
						if (current.getId().equals(fileId)) {
							int index = i + 1;
							LOGGER.debug("mapped '{}' => logId '{}' ({})", index, logId, label);
							return index;
						}
					}
				}
			}
		}

		LOGGER.warn("No phys struct maps logical struct '{}' - default to '1'!", logId);
		return 1;
	}

	/**
	 * 
	 * Try to map a valid physical structure that is linked from some logical
	 * structure. Handle problems with processes that allow multiple logical
	 * elements on a single physical page. For these cases, we keep track of the
	 * latest valid link that will be re-used this time.
	 * 
	 * @param physId
	 * @param label
	 * @return physicalSubDiv or null
	 */
	private PhysicalSubDiv mapLatestValidContainer(String physId, String label) {
		PhysicalSubDiv physDiv = mets.getPhysicalStructMap().getDivContainer().get(physId);
		if (physDiv == null) {
			LOGGER.warn("Invalid physical Target '{}' for '{}'", physId, label);
			if (latestWorkingphysId != null) {
				LOGGER.warn("re-use latestWorkingPhysId {} for label '{}'", latestWorkingphysId, label);
				physDiv = mets.getPhysicalStructMap().getDivContainer().get(latestWorkingphysId);
				if (physDiv == null) {
					LOGGER.error("no way to handle '{}' - skip structure!", label);
					return null;
				}
			}
		} else {
			latestWorkingphysId = physId;
		}
		return physDiv;
	}

	/**
	 * 
	 * see: http://dfg-viewer.de/strukturdatenset/
	 * 
	 * @param logicalType
	 * @return
	 */
	private String mapLogicalType(String logicalType) {
		switch (logicalType) {
		case "cover_front":
			return "Vorderdeckel";
		case "cover_back":
			return "Rückdeckel";
		case "title_page":
			return "Titelblatt";
		case "preface":
			return "Vorwort";
		case "dedication":
			return "Widmung";
		case "illustration":
			return "Illustration";
		case "image":
			return "Bild";
		case "table":
			return "Tabelle";
		case "contents":
			return "Inhaltsverzeichnis";
		case "engraved_titlepage":
			return "Kupfertitel";
		case "map":
			return "Karte";
		case "imprint":
			return "Impressum";
		case "corrigenda":
			return "Errata";
		case "section":
			return "Abschnitt";
		case "provenance":
			return "Besitznachweis";
		case "bookplate":
			return "Exlibris";
		case "entry":
			return "Eintrag";
		case "printers_mark":
			return "Druckermarke";
		case "chapter":
			return "Kapitel";
		case "index":
			return "Register";
		default:
			LOGGER.warn("no mapping for logical type: '{}'", logicalType);
			return "n.a.";
		}
	}
}
