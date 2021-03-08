package de.ulb.digital.derivans.data;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.mycore.mets.model.Mets;
import org.mycore.mets.model.files.FLocat;
import org.mycore.mets.model.files.File;
import org.mycore.mets.model.files.FileGrp;
import org.mycore.mets.model.struct.Fptr;
import org.mycore.mets.model.struct.LOCTYPE;
import org.mycore.mets.model.struct.PhysicalStructMap;
import org.mycore.mets.model.struct.PhysicalSubDiv;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.model.DescriptiveData;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.DigitalStructureTree;

/**
 * 
 * Implementation of {@link IMetadataStore} Interface
 * 
 * @author hartwig
 *
 */
public class MetadataStore implements IMetadataStore {

	private static final Logger LOGGER = LogManager.getLogger(MetadataStore.class);

	// Mark unresolved information about author, title, ...
	public static final String UNKNOWN = "n.a.";

	static final Namespace NS_MODS = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");

	private DescriptiveData descriptiveData;

	private MetadataHandler handler;

	private Mets mets;

	/**
	 * Constructor if no METS/MODS available
	 */
	public MetadataStore() {
	}

	/**
	 * 
	 * Default constructor requires Path to valid Metadata file
	 * 
	 * @param filePath
	 * @throws DigitalDerivansException
	 */
	public MetadataStore(Path filePath) throws DigitalDerivansException {
		this.handler = new MetadataHandler(filePath);
		this.load();
		LOGGER.info("created new metadatastore from '{}'", filePath);
	}

	private void load() throws DigitalDerivansException {
		try {
			mets = handler.read();
		} catch (IOException | JDOMException e) {
			LOGGER.error(e);
			throw new DigitalDerivansException(e);
		}
	}

	@Override
	public DigitalStructureTree getStructure() throws DigitalDerivansException {
		StructureMapper creator = new StructureMapper(mets, getDescriptiveData().getTitle());
		return creator.build();
	}

	@Override
	public List<DigitalPage> getDigitalPagesInOrder() {
		List<DigitalPage> pages = new ArrayList<>();
		int n = 1;
		if (this.mets != null) {
			PhysicalStructMap physStruct = mets.getPhysicalStructMap();
			if (physStruct != null) {
				for (PhysicalSubDiv physSubDiv : physStruct.getDivContainer().getChildren()) {
					String fptrsFirstId = requestFilePointerID(physSubDiv);
					String fileRefSegment = requestFileReference(fptrsFirstId);
					if (!fileRefSegment.isBlank()) {
						// sanitize file endings that are missing in METS links for valid local access
						if (!fileRefSegment.endsWith(".jpg")) {
							fileRefSegment += ".jpg";
						}
						DigitalPage page = new DigitalPage(n, fileRefSegment);
						String contentIds = physSubDiv.getContentIds();
						if (contentIds != null) {
							LOGGER.debug("[{}] contentids '{}'", physSubDiv.getId(), contentIds);
							page.setIdentifier(contentIds);
						}
						pages.add(page);
					}
					n++;
				}
			}
		}
		return pages;
	}

	private String requestFilePointerID(PhysicalSubDiv physSubDiv) {
		var filePointers = physSubDiv.getChildren();
		if (filePointers.isEmpty()) {
			LOGGER.warn("missing filePointers Entry!");
			return UNKNOWN;
		}
		if (filePointers.size() != 1) {
			LOGGER.warn("ambigious filePointers Entry: {}, use first one anyway", filePointers.size());
		}
		return filePointers.get(0).getFileId();
	}

	private String requestFileReference(String fptrId) {
		// inspect ALL FileGrps, not only "MAX"
		for (FileGrp fileGrp : mets.getFileSec().getFileGroups()) {
			for (File fileGrpfile : fileGrp.getFileList()) {
				if (fileGrpfile.getId().equals(fptrId)) {
					String link = fileGrpfile.getFLocat().getHref();
					if (!link.isBlank()) {
						String[] linkTokens = link.split("/");
						return linkTokens[linkTokens.length - 1];
					}
				}
			}
		}
		return "";
	}

	@Override
	public boolean enrichPDF(String identifier) {
		if(MetadataStore.UNKNOWN.equals(identifier)) {
			LOGGER.warn("no mets available to enrich created PDF");
			return false;
		}
		if (this.handler != null) {
			String mimeType = "application/pdf";
			String fileGroup = "DOWNLOAD";
			LOGGER.info("enrich derivate with href '{}' as '{}' in '{}'", identifier, mimeType, fileGroup);
			String fileId = integrateFileGroup(fileGroup, identifier, mimeType);
			String note = this.handler.enrichAgent(fileId);
			integrateFprt("PDF_" + identifier);
			this.handler.write();
			LOGGER.info("integrated pdf fileId '{}' in '{}'", fileId, this.handler.getPath());
			LOGGER.info("integrated mets:agent {}", note);
			return true;
		}
		return false;
	}

	private String integrateFileGroup(String fileGroupUse, String identifier, String mimeType) {
		FileGrp fileGrp = new FileGrp(fileGroupUse);
		String fileId = "PDF_" + identifier;
		File f = new File(fileId, mimeType);
		FLocat fLocat = new FLocat(LOCTYPE.URL, identifier + ".pdf");
		f.setFLocat(fLocat);
		fileGrp.addFile(f);
		this.handler.addFileGroup(fileGrp.asElement());
		return fileId;
	}

	private void integrateFprt(String fileHref) {
		Fptr fpts = new Fptr(fileHref);
		this.handler.addTo(fpts.asElement(), "LOGICAL", true);
	}

	@Override
	public DescriptiveData getDescriptiveData() {
		if (descriptiveData == null) {
			DescriptiveDataBuilder builder = new DescriptiveDataBuilder(this.mets);
			try {
				descriptiveData = builder.author().access().identifier().title().urn().year().build();
			} catch (DigitalDerivansException e) {
				LOGGER.error(e);
			}
		}
		return descriptiveData;
	}
}


