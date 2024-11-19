package de.ulb.digital.derivans.derivate.pdf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.data.IMetadataStore;
import de.ulb.digital.derivans.derivate.BaseDerivateer;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.DigitalStructureTree;
import de.ulb.digital.derivans.model.IPDFProcessor;
import de.ulb.digital.derivans.model.pdf.PDFResult;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;

/**
 * 
 * Create PDF derivate from
 * <ul>
 * <li>JPG image data</li>
 * <li>XML ocr-data (ALTO, PAGE)</li>
 * </ul>
 * 
 * @author hartwig
 *
 */
public class PDFDerivateer extends BaseDerivateer {

	private static final Logger LOGGER = LogManager.getLogger(PDFDerivateer.class);

	private DigitalStructureTree structure = new DigitalStructureTree();

	private DerivateStepPDF derivateStep;

	private Optional<IMetadataStore> optMetadataStore = Optional.empty();
	
	private IPDFProcessor pdfProcessor;

	private PDFResult pdfResult;

	/**
	 * 
	 * Create new instance on top of {@link BaseDerivateer}
	 * 
	 * @param input
	 * @param output
	 * @param pages
	 * @param derivateStep
	 * @throws DigitalDerivansException
	 */
	public PDFDerivateer(DerivansData input, DerivansData output, List<DigitalPage> pages,
			DerivateStepPDF derivateStep) throws DigitalDerivansException {
		super(input, output);
		if (pages == null) {
			throw new DigitalDerivansException("Invalid pages for PDF!");
		}
		this.digitalPages = pages;
		this.derivateStep = derivateStep;
		this.pdfProcessor = new ITextProcessor();
	}

	public DerivateStepPDF getConfig() {
		return this.derivateStep;
	}

	public void setMetadataStore(Optional<IMetadataStore> metadataStore) throws DigitalDerivansException {
		this.optMetadataStore = metadataStore;
		if (this.optMetadataStore.isPresent()) {
			var store = this.optMetadataStore.get();
			store.setFileGroupOCR(this.derivateStep.getParamOCR());
			store.setFileGroupImages(this.derivateStep.getParamImages());
			var descriptiveData = store.getDescriptiveData();
			this.derivateStep.mergeDescriptiveData(descriptiveData);
			this.structure = store.getStructure();
			this.digitalPages = store.getDigitalPagesInOrder();
		}
	}

	public void setPDFProcessor(IPDFProcessor processor) {
		this.pdfProcessor = processor;
	}

	@Override
	public int create() throws DigitalDerivansException {
		Path pathToPDF = this.output.getPath();
		// if output path points to a directory, use it's name for PDF-file
		if (Files.isDirectory(pathToPDF)) {
			pathToPDF = pathToPDF.resolve(pathToPDF.getFileName() + ".pdf");
		}
		if (getDigitalPages().isEmpty()) {
			var msg = "No pages for PDF "+pathToPDF;
			throw new DigitalDerivansException(msg);
		}
		this.resolver.enrichData(getDigitalPages(), this.getInput());

		// forward pdf generation
		this.pdfProcessor.init(this.derivateStep, digitalPages, structure);
		this.pdfResult = this.pdfProcessor.write(pathToPDF.toFile());
		this.pdfResult.setPath(pathToPDF);
		var nPagesAdded = this.pdfResult.getPdfPages().size();
		var hasOutlineAdded = this.pdfResult.getOutline().size() > 0;
		LOGGER.info("created pdf '{}' with {} pages (outline:{})", pathToPDF, nPagesAdded,
				hasOutlineAdded);

		// post-action
		if (this.optMetadataStore.isPresent() && this.derivateStep.isEnrichMetadata()) {
			this.enrichPDFInformation(pathToPDF);
		} else {
			LOGGER.warn("pdf '{}' not enriched in METS", pathToPDF);
		}
		return 1;
	}

	private void enrichPDFInformation(Path pathtoPDF) throws DigitalDerivansException {
		if (Files.exists(pathtoPDF)) {
			var store = this.optMetadataStore.get();
			var storePath = store.usedStore();
			LOGGER.info("enrich created pdf '{}' in '{}'", pathtoPDF, storePath);
			String filename = pathtoPDF.getFileName().toString();
			String identifier = filename.substring(0, filename.indexOf('.'));
			store.enrichPDF(identifier);
		} else {
			String msg3 = "Missing pdf " + pathtoPDF.toString() + "!";
			LOGGER.error(msg3);
			throw new DigitalDerivansException(msg3);
		}
	}

	/*
	 * Set structure data for testing purposes
	 * when no *real* metadata present
	 */
	public void setStructure(DigitalStructureTree tree) {
		this.structure = tree;
	}

	/**
	 * 
	 * Gather insights how processing was done
	 * usefull for testing purposes
	 * 
	 * @return
	 */
	public PDFResult getPDFResult() {
		return this.pdfResult;
	}
}
