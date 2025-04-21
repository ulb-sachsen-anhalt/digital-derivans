package de.ulb.digital.derivans.derivate.pdf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.data.IMetadataStore;
import de.ulb.digital.derivans.data.mets.METS;
import de.ulb.digital.derivans.derivate.BaseDerivateer;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DerivateMD;
import de.ulb.digital.derivans.model.DigitalPage;
import de.ulb.digital.derivans.model.DigitalStructureTree;
import de.ulb.digital.derivans.model.IDerivate;
import de.ulb.digital.derivans.model.IPDFProcessor;
import de.ulb.digital.derivans.model.pdf.DescriptiveMetadata;
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

	private DerivateStepPDF derivateStep;

	private METS mets;

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
			throw new DigitalDerivansException("Miss pages for PDF!");
		}
		this.digitalPages = pages;
		this.derivateStep = derivateStep;
		this.pdfProcessor = new ITextProcessor();
	}

	@Override
	public void setDerivate(IDerivate derivate) {
		this.derivate = derivate;
		if (this.derivate instanceof DerivateMD) {
			this.mets = ((DerivateMD) this.derivate).getMets();
		}
	}

	public DerivateStepPDF getConfig() {
		return this.derivateStep;
	}

	public void setDescriptiveMD(DescriptiveMetadata dmd) {
		// var store = this.optMetadataStore.get();
		// store.setFileGroupOCR(this.derivateStep.getParamOCR());
		// store.setFileGroupImages(this.derivateStep.getParamImages());
		// var descriptiveData = store.getDescriptiveData();
		this.derivateStep.mergeDescriptiveData(dmd);
	}

	public void setPDFProcessor(IPDFProcessor processor) {
		this.pdfProcessor = processor;
	}

	@Override
	public int create() throws DigitalDerivansException {
		Path pathToPDF = this.output.getPath().normalize();
		// if output path points to a directory, use it's name for PDF-file
		if (Files.isDirectory(pathToPDF)) {
			pathToPDF = pathToPDF.resolve(pathToPDF.getFileName() + ".pdf");
		}
		if (getDigitalPages().isEmpty()) {
			var msg = "No pages for PDF " + pathToPDF;
			throw new DigitalDerivansException(msg);
		}
		// this.resolver.enrichData(getDigitalPages(), this.getInput());

		// forward pdf generation
		this.pdfProcessor.init(this.derivateStep, /* digitalPages, structure, */ this.derivate);
		this.pdfResult = this.pdfProcessor.write(pathToPDF.toFile());
		this.pdfResult.setPath(pathToPDF);
		var nPagesAdded = this.pdfResult.getPdfPages().size();
		LOGGER.info("created pdf '{}' with {} pages", pathToPDF, nPagesAdded);

		// post-action
		if ((this.mets != null) && this.derivateStep.isEnrichMetadata()) {
			this.enrichPDFInformation(pathToPDF);
		} else {
			LOGGER.warn("pdf '{}' not enriched in METS", pathToPDF);
		}
		return 1;
	}

	private void enrichPDFInformation(Path pathtoPDF) throws DigitalDerivansException {
		if (Files.exists(pathtoPDF)) {
			// var store = this.optMetadataStore.get();
			// var storePath = store.usedStore();
			LOGGER.info("enrich created pdf '{}' in '{}'", pathtoPDF, this.mets.getPath());
			String filename = pathtoPDF.getFileName().toString();
			String identifier = filename.substring(0, filename.indexOf('.'));
			this.mets.enrichPDF(identifier);
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
	// public void setStructure(DigitalStructureTree tree) {
	// this.structure = tree;
	// }

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
