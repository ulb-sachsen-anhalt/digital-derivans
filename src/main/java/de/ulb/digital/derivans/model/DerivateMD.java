package de.ulb.digital.derivans.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.IDerivans;
import de.ulb.digital.derivans.data.mets.DescriptiveMetadataBuilder;
import de.ulb.digital.derivans.data.mets.METS;
import de.ulb.digital.derivans.data.mets.METSContainer;
import de.ulb.digital.derivans.data.mets.METSFile;
import de.ulb.digital.derivans.model.pdf.DescriptiveMetadata;

/**
 * 
 * Input, output and final destination of Derivans' efforts.
 * 
 * This kind of derivate describes a local file system
 * layout, with the start directory being set as label
 * for the final derivate.
 * 
 * All contained images files are assumed
 * to be JPG files in a sub directory named
 * alike the corresponding mets:fileGroup
 * like "MAX" or "DEFAULT".
 * 
 * Each image file must be linked to at least one
 * logical section in the logical mets:structMap.
 * As for DFG-METS flavor, this includes an
 * additional physical mets:structMap representing
 * TYPE=page
 * 
 * @author u.hartwig
 */
public class DerivateMD implements IDerivate {

	private String startFileExtension = ".jpg";

	private String imageGroup = IDerivans.IMAGE_DIR_DEFAULT;

	private Path rootDir;

	private boolean inited;

	private boolean checkRessources = true;

	private final AtomicInteger mdPageOrder = new AtomicInteger(1);

	private METS mets;

	private DerivateStruct struct;

	private final Map<String, DigitalPage> pageIndex = new LinkedHashMap<>();

	private String identifierExpression;

	public DerivateMD(Path pathInput) throws DigitalDerivansException {
		this.mets = new METS(pathInput, this.imageGroup);
		this.rootDir = pathInput.getParent();
	}

	public DerivateMD(Path pathInput, String imageGroup) throws DigitalDerivansException {
		this.mets = new METS(pathInput, imageGroup);
		this.rootDir = pathInput.getParent();
	}

	/**
	 * Include minimal resolving of image sub dirs: => MAX, DEFAULT, if exist => use
	 * same directory to search for images
	 */
	@Override
	public void init(Path startPath) throws DigitalDerivansException {
		if (startPath == null) {
			startPath = Path.of(IDerivans.IMAGE_DIR_DEFAULT);
			this.mets.setImgFileGroup(startPath.toString());
		}
		if (startPath.isAbsolute()) {
			this.imageGroup = startPath.getFileName().toString();
		} else {
			this.imageGroup = startPath.toString();
			this.mets.setImgFileGroup(startPath.toString());
		}
		this.mets.init(); // critical
		var orderNr = this.mdPageOrder.get();
		METSContainer containerRoot = this.mets.getLogicalRoot();
		String logicalLabel = containerRoot.determineLabel();
		// look for higher structs
		var optParent = this.mets.optParentStruct();
		if (optParent.isPresent()) {
			METSContainer parentContainer = optParent.get();
			String parentLabel = parentContainer.determineLabel();
			DerivateStruct structParent = new DerivateStruct(orderNr, parentLabel);
			DerivateStruct structRoot = new DerivateStruct(orderNr, logicalLabel);
			structParent.getChildren().add(structRoot);
			this.struct = structParent;
			this.populateStruct(structRoot, containerRoot, this.startFileExtension);
		} else {
			this.struct = new DerivateStruct(orderNr, logicalLabel);
			this.populateStruct(this.struct, containerRoot, this.startFileExtension);
		}
		this.inited = true;
	}

	private void populateStruct(DerivateStruct parent, METSContainer container, String fileExt)
			throws DigitalDerivansException {
		// handle all directly linked pages. i.e. flat structures
		if (container.getChildren().isEmpty()) {
			this.handlePages(parent, container);
		} else {
			// handle objects with sub-structs
			for (var subContainer : container.getChildren()) {
				this.traverseStruct(parent, subContainer, fileExt);
			}
		}
	}

	private void handlePages(DerivateStruct parent, METSContainer container) throws DigitalDerivansException {
		List<METSContainer> pages = this.mets.getPages(container);
		for (METSContainer digiFile : pages) {
			String pageId = digiFile.getId();
			if (this.pageIndex.containsKey(pageId)) {
				DigitalPage existingPage = this.pageIndex.get(pageId);
				parent.getPages().add(existingPage);
			} else {
				METS.METSFilePack pack = this.mets.getPageFiles(digiFile);
				var imgFile = pack.imageFile;
				Path filePath = this.rootDir.resolve(imgFile.getLocalPath(this.checkRessources));
				int currOrder = this.mdPageOrder.getAndIncrement();
				DigitalPage page = new DigitalPage(imgFile.getFileId(), currOrder, filePath);
				page.setPageLabel(digiFile.determineLabel());
				digiFile.getAttribute("CONTENTIDS").ifPresent(page::setContentIds);
				if (pack.ocrFile.isPresent()) {
					METSFile ocrFile = pack.ocrFile.get();
					page.setOcrFile(ocrFile.getLocalPath(this.checkRessources));
				}
				parent.getPages().add(page);
				this.pageIndex.put(pageId, page); // also remember in self dictionary
			}
		}
	}

	/**
	 * Traverse depth-first
	 */
	private void traverseStruct(DerivateStruct parentStruct, METSContainer currentCnt,
			String fileExt) throws DigitalDerivansException {
		String logicalLabel = currentCnt.determineLabel();
		Integer structOrder = -1;
		Optional<String> optOrder = currentCnt.getAttribute("ORDER");
		if (optOrder.isPresent()) {
			structOrder = Integer.valueOf(optOrder.get());
		}
		DerivateStruct currentStruct = new DerivateStruct(structOrder, logicalLabel);
		parentStruct.getChildren().add(currentStruct);
		if (!currentCnt.getChildren().isEmpty()) {
			for (var subContainer : currentCnt.getChildren()) {
				this.traverseStruct(currentStruct, subContainer, fileExt);
			}
		}
		// assume containers may link pages on *every* level
		// even if these pages also linked by it's children
		this.handlePages(currentStruct, currentCnt);
	}

	@Override
	public DerivateStruct getStructure() {
		return this.struct;
	}

	@Override
	public List<DigitalPage> getAllPages() {
		return new ArrayList<>(this.pageIndex.values());
	}

	public Path getRootDir() {
		return this.rootDir;
	}

	@Override
	public boolean isInited() {
		return this.inited;
	}

	public void setIdentifierExpression(String xpressiob) {
		this.identifierExpression = xpressiob;
	}

	public DescriptiveMetadata getDescriptiveData() throws DigitalDerivansException {
		DescriptiveMetadataBuilder builder = new DescriptiveMetadataBuilder();
		if (!this.mets.isInited()) { // for testing purposes; shall not happen in productive flows
			this.mets.init();
		}
		builder.setMetadataStore(this.mets);
		if (this.identifierExpression != null) { // identifier might have changed for PDF labelling
			builder.setIdentifierExpression(this.identifierExpression);
		}
		return builder.person().access().identifier().title().urn().year().build();
	}

	@Override
	public boolean isMetadataPresent() {
		return true;
	}

	@Override
	public String getStartFileExtension() {
		return startFileExtension;
	}

	@Override
	public void setStartFileExtension(String startFileExtension) {
		this.startFileExtension = startFileExtension;
	}

	public String getIdentifierURN() {
		return this.mets.getPrimeMODS().getIdentifierURN();
	}

	public METS getMets() {
		return this.mets;
	}

	/**
	 * Disable local file resolving to test metadata separate
	 * 
	 * @param check
	 */
	public void checkRessources(boolean check) {
		this.checkRessources = check;
	}

	public boolean isGranularIdentifierPresent() {
		return this.getAllPages().stream()
				.filter(page -> page.optContentIds().isPresent())
				.map(page -> page.optContentIds().get())
				.findAny().isPresent();
	}

}
