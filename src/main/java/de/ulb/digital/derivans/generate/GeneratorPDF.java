package de.ulb.digital.derivans.generate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.data.mets.METS;
import de.ulb.digital.derivans.generate.pdf.ITextProcessor;
import de.ulb.digital.derivans.model.DerivansData;
import de.ulb.digital.derivans.model.DerivateStruct;
import de.ulb.digital.derivans.model.DigitalPage;
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
public class GeneratorPDF extends Generator {

	private static final Logger LOGGER = LogManager.getLogger(GeneratorPDF.class);

	private DerivateStepPDF derivateStep;

	private METS mets;

	private IPDFProcessor pdfProcessor;

	private PDFResult pdfResult;


	public GeneratorPDF(){
		super();
		this.pdfProcessor = new ITextProcessor();
	}

	/**
	 * 
	 * Create new instance on top of {@link Generator}
	 * 
	 * @param input
	 * @param output
	 * @param pages
	 * @param derivateStep
	 * @throws DigitalDerivansException
	 */
	public GeneratorPDF(DerivansData input, DerivansData output, List<DigitalPage> pages,
			DerivateStepPDF derivateStep) throws DigitalDerivansException {
		super(input, output);
		this.derivateStep = derivateStep;
		this.pdfProcessor = new ITextProcessor();
	}

	public void setMETS(METS mets) {
		this.mets = mets;
	}

	public void setStructure(DerivateStruct structure) {
		this.pdfProcessor.setStructure(structure);
	}

	public void setPDFStep(DerivateStepPDF step) {
		this.derivateStep = step;
	}

	public DerivateStepPDF getConfig() {
		return this.derivateStep;
	}

	public void setDescriptiveMD(DescriptiveMetadata dmd) {
		this.derivateStep.mergeDescriptiveData(dmd);
	}

	public void setPDFProcessor(IPDFProcessor processor) {
		this.pdfProcessor = processor;
	}

	@Override
	public int create() throws DigitalDerivansException {
		Path pathToPDF = this.derivateStep.getPathPDF();
		// if output path points to a directory, use it's name for PDF-file
		if (this.digitalPages.isEmpty()) {
			var msg = "No pages for PDF " + pathToPDF;
			throw new DigitalDerivansException(msg);
		}
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
			LOGGER.info("enrich created pdf '{}' in '{}'", pathtoPDF, this.mets.getPath());
			String filename = pathtoPDF.getFileName().toString();
			if(this.getOutputPrefix().isPresent()) {
				var optPrefix = this.getOutputPrefix().get();
				filename = optPrefix + filename;
			}
			String identifier = filename.substring(0, filename.indexOf('.'));
			this.mets.enrichPDF(identifier);
		} else {
			String msg3 = "Missing pdf " + pathtoPDF.toString() + "!";
			LOGGER.error(msg3);
			throw new DigitalDerivansException(msg3);
		}
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
