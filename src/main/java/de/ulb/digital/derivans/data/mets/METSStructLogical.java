package de.ulb.digital.derivans.data.mets;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.mets.model.Mets;
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
public class METSStructLogical {

	private static final Logger LOGGER = LogManager.getLogger(METSStructLogical.class);

	// special case, where the complete physical section is
	// linked to the top-most logical entity
	public static final String STRUCT_PHYSICAL_ROOT = "physroot";

	// mark newspaper structures
	public static final List<String> NEWSPAPER_STRUCTS = List.of("newspaper", "year", "month", "day");

	// mark missing data
	public static final String UNSET = "n.a.";

	private Mets mets;

	private String title;

	private boolean renderPlainLeafes;

	public METSStructLogical(Mets mets, String title) {
		this(mets, title, true);
	}

	/**
	 * 
	 * Create Instance
	 * 
	 * Set renderPlainLeafes = false to disable rendering
	 * of plain page elements of a print.
	 * 
	 * @param mets
	 * @param title
	 * @param renderPlainLeafes
	 */
	public METSStructLogical(Mets mets, String title, boolean renderPlainLeafes) {
		this.mets = mets;
		this.title = title;
		this.renderPlainLeafes = renderPlainLeafes;
	}

	public DigitalStructureTree build() throws DigitalDerivansException {
		if (this.mets != null) {
			LogicalStructMap lsm = this.mets.getLogicalStructMap();
			if (lsm == null) {
				String msg = "mets is missing logical StructMap!";
				LOGGER.error(msg);
				throw new DigitalDerivansException(msg);
			}
			LogicalDiv logDiv = lsm.getDivContainer();
			DigitalStructureTree theRoot = new DigitalStructureTree();
			String label = logDiv.getLabel();
			if (label == null) {
				label = logDiv.getOrderLabel();
				if (label == null) {
					label = this.title;
				}
			}
			theRoot.setLabel(label);
			theRoot.setPage(1);
			var typedKids = logDiv.getChildren().stream().filter(div -> div.getType() != null)
					.collect(Collectors.toList());
			for (LogicalDiv logicalChild : typedKids) {
				// hack around bug: not only div children are respected
				// to avoid empty entries from TextNodes
				if (logicalChild.getType() != null) {
					DigitalStructureTree subTree = new DigitalStructureTree();
					theRoot.addSubStructure(subTree);
					subTree.setParentStructure(theRoot);
					extendStructure(subTree, logicalChild);
				}
			}

			// review invalid page links
			clearInvalidPageLinks(theRoot);

			// review redundant page links
			clearRedundantPageLinks(theRoot);

			return theRoot;
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
	 * Invalid links are pointing to a Page marked with -1.
	 * 
	 * @param tree
	 */
	private void clearInvalidPageLinks(DigitalStructureTree tree) {
		if (tree.hasSubstructures()) {
			for (DigitalStructureTree subTree : tree.getSubstructures()) {
				if (subTree.getPage() == -1) {
					boolean isRemoved = tree.removeSubStructure(subTree);
					LOGGER.warn("Droped invalid subStructure '{}':{}", subTree, isRemoved);
				}
				clearInvalidPageLinks(subTree);
			}
		}
	}

	int clearRedundantPageLinks(DigitalStructureTree root) {
		// nothing to foster, go back
		if (!root.hasSubstructures()) {
			return 0;
		}

		// collect all sub-nodes in a stack
		List<DigitalStructureTree> allNodes = this.getSubstructures(root, new ArrayList<>());

		// map structures to linked pages
		Map<String, List<DigitalStructureTree>> structureMap = new HashMap<>();
		for (var plainNode : allNodes) {
			String structStr = plainNode.toString();
			structureMap.computeIfPresent(structStr, (k, v) -> {
				v.add(plainNode);
				return v;
			});
			structureMap.computeIfAbsent(structStr, k -> {
				var l = new ArrayList<DigitalStructureTree>();
				l.add(plainNode);
				return l;
			});
		}
		// filter structures which are linked more than once
		var multiLinked = structureMap.entrySet().stream()
				.filter(e -> e.getValue().size() > 1)
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));

		if (multiLinked.size() > 0) {
			LOGGER.warn("Detected '{}' multi linked structures", multiLinked.size());
		}

		// inspect each multiple linked node
		int dropped = 0;
		for (var multiList : multiLinked.values()) {
			// determine longest path -> this is the most valid link
			int maxDepth = multiList.stream().map(e -> e.getParents().size())
					.max(Comparator.comparing(Integer::valueOf)).orElse(0);
			// assume every shorter path is a faulty linked
			// parent structure link to be removed
			for (int i = 0; i < multiList.size(); i++) {
				int currDepth = multiList.get(i).getParents().size();
				if (currDepth > 0 && currDepth < maxDepth) {
					var parent = multiList.get(i).getParentStructure();
					parent.removeSubStructure(multiList.get(i));
					dropped++;
				}
			}
		}
		return dropped;
	}

	private List<DigitalStructureTree> getSubstructures(DigitalStructureTree current,
			List<DigitalStructureTree> target) {
		if (current.hasSubstructures()) {
			for (var subStruct : current.getSubstructures()) {
				this.getSubstructures(subStruct, target);
			}
		} else {
			target.add(current);
		}
		return target;
	}

	void extendStructure(DigitalStructureTree currentNode, LogicalDiv currentLogicalDiv)
			throws DigitalDerivansException {

		// set required data for current node
		currentNode.setLabel(getLabel(currentLogicalDiv));
		MapLeafs mapedLeafs = mapLogicalDivToPhysicalSequence(currentLogicalDiv);
		currentNode.setPage(mapedLeafs.order);

		// handle current leafs (= just pages)
		// if any exists and if this is required
		if (this.renderPlainLeafes) {
			for (var leaf : mapedLeafs.leafs) {
				String leafLabel = METSStructLogical.getLabel(leaf);
				var leafStruct = new DigitalStructureTree(leaf.getOrder(), leafLabel);
				leafStruct.setParentStructure(currentNode);
				currentNode.addSubStructure(leafStruct);
			}
		}

		// iterate further down the structure tree
		if (currentLogicalDiv.getChildren() != null) {
			for (LogicalDiv child : currentLogicalDiv.getChildren()) {
				DigitalStructureTree subTree = new DigitalStructureTree();
				currentNode.addSubStructure(subTree);
				subTree.setParentStructure(currentNode);
				extendStructure(subTree, child);
			}
		}
	}

	/**
	 * 
	 * Guess name/label from logical container
	 * 
	 * @param logical
	 * @return
	 */
	private static String getLabel(LogicalDiv logical) {
		String label = logical.getLabel();
		if (label != null && !label.isBlank()) {
			return label;
		}
		String orderLabel = logical.getOrderLabel();
		if (orderLabel != null && !orderLabel.isBlank()) {
			return orderLabel;
		}
		String logicalStructType = logical.getType();
		return mapLogicalType(logicalStructType);
	}

	/**
	 * 
	 * Guess name/label for single page from
	 * physical section
	 * 
	 * @param logical
	 * @return
	 */
	private static String getLabel(PhysicalSubDiv physical) throws DigitalDerivansException {
		String label = physical.getLabel();
		if (label != null && !label.isBlank()) {
			return label;
		}
		String orderLabel = physical.getOrderLabel();
		if (orderLabel != null && !orderLabel.isBlank()) {
			return orderLabel;
		}
		throw new DigitalDerivansException("No valid labelling for page '" + physical.getId() + "'");
	}

	/**
	 * 
	 * Map logical structure to order of 
	 * corresponding physical structure
	 * to determine the start page of a 
	 * logical structure.
	 * 
	 * @param ld
	 * @return pageNumber (default: 1)
	 */
	private MapLeafs mapLogicalDivToPhysicalSequence(LogicalDiv ld) throws DigitalDerivansException {
		String logId = ld.getId();
		StructLink structLink = mets.getStructLink();
		List<SmLink> smLinksTo = structLink.getSmLinkByFrom(logId);
		if (!smLinksTo.isEmpty()) {

			// according to latest (2022-05-05) requirements
			// iterate over *_all_* physical containers linked
			// from this logical container
			try {

				// try to get the link to the target physical section
				String physId = smLinksTo.get(0).getTo();

				// handle the special semantics root container
				// this is mapped immediately to page "1"
				if (physId.equalsIgnoreCase(STRUCT_PHYSICAL_ROOT)) {
					var rootLeaf = new MapLeafs();
					rootLeaf.order = 1;
					return rootLeaf;
				}

				// request valid link from logical to physical container
				PhysicalSubDiv physDiv = mets.getPhysicalStructMap().getDivContainer().get(physId);
				if (physDiv == null) {
					throw new DigitalDerivansException("Invalid physical struct '" + physId + "'!");
				}
				Integer order = physDiv.getOrder();
				if (order == null) {
					throw new DigitalDerivansException("no order for " + logId);
				}
				var mapLeafs = new MapLeafs();
				mapLeafs.order = order;

				// collect links for every page otherwise
				// but *ONLY* if this is not one of the top-most containers!
				// this is Kitodo2 related, where there is no such thing
				// as a simple "physRoot" linking but each physical page is also linked
				// to the monograph/F-stage, too
				if (!isTopLogicalContainer(ld)) {
					mapLeafs.leafs = smLinksTo.stream()
							.map(smLink -> mets.getPhysicalStructMap().getDivContainer().get(smLink.getTo()))
							.collect(Collectors.toList());
				}

				return mapLeafs;
			} catch (DigitalDerivansException e) {
				throw new DigitalDerivansException("LogId '" + logId + "' : " + e.getMessage());
			}
		}
		LOGGER.warn("No phys struct maps logical struct '{}'!", logId);
		// maybe type is newspaper related
		if (isNewspaperStruct(ld)) {
			LOGGER.info("Deal with type '{}', therefore map '{}' to page '1'.",  ld.getType(), ld.getLabel());
			var rootLeaf = new MapLeafs();
			rootLeaf.order = 1;
			return rootLeaf;
		}
		String logStr = String.format("%s@%s(%s)", logId, ld.getType(), ld.getLabel());
		throw new DigitalDerivansException("No physical struct linked from '" + logStr + "'!");
	}

	private static boolean isTopLogicalContainer(LogicalDiv logDiv) {
		String theType = logDiv.getType();
		var tops = List.of("volume", "monograph");
		return tops.stream().anyMatch(t -> t.equals(theType));
	}

	private static boolean isNewspaperStruct(LogicalDiv logicalDiv) {
		String logicalStructType = logicalDiv.getType();
		return NEWSPAPER_STRUCTS.stream().anyMatch(t -> t.equalsIgnoreCase(logicalStructType));
	}

	/**
	 * 
	 * Get german Translation for PDF outline
	 * 
	 * @param logicalStructType
	 * @return
	 */
	private static String mapLogicalType(String logicalStructType) {
		Optional<String> optMapping = METSContainerType.getTranslation(logicalStructType);
		if (optMapping.isPresent()) {
			return optMapping.get();
		} else {
			LOGGER.warn("no mapping for logical struct_type: '{}'", logicalStructType);
			return UNSET;
		}
	}
}

/**
 * Internal Data Container Clazz
 */
class MapLeafs {
	Integer order = 1;
	List<PhysicalSubDiv> leafs = new ArrayList<>();

	@Override
	public String toString() {
		return String.format("p%04d (%d leafs)", order, leafs.size());
	}
}
