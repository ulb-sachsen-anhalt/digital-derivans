package de.ulb.digital.derivans.generate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.DigitalDerivansRuntimeException;
import de.ulb.digital.derivans.data.mets.METS;
import de.ulb.digital.derivans.generate.pdf.ITextProcessor;
import de.ulb.digital.derivans.model.DerivateMD;
import de.ulb.digital.derivans.model.DerivateStruct;
import de.ulb.digital.derivans.model.IPDFProcessor;
import de.ulb.digital.derivans.model.pdf.DescriptiveMetadata;
import de.ulb.digital.derivans.model.pdf.PDFResult;
import de.ulb.digital.derivans.model.step.DerivateStep;
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

	private METS mets;

	private IPDFProcessor pdfProcessor;

	private Path pathPDF;

	private PDFResult pdfResult;

	public GeneratorPDF() {
		this.pdfProcessor = new ITextProcessor();
	}

	@Override
	public void setStep(DerivateStep step) {
		super.setStep(step);
		if(this.derivate == null) {
			throw new DigitalDerivansRuntimeException("No derivate set: null!");
		}
		DerivateStepPDF pdfStep = (DerivateStepPDF) step;
		try {
			this.pathPDF = this.setPDFPath(pdfStep);
		} catch (DigitalDerivansException exc) {
			throw new DigitalDerivansRuntimeException(exc);
		}
		if (this.derivate.isMetadataPresent()) {
			this.mets = ((DerivateMD) this.derivate).getMets();
		}
		this.setStructure(this.derivate.getStructure());
	}

	public void setMETS(METS mets) {
		this.mets = mets;
	}

	public void setStructure(DerivateStruct structure) {
		this.pdfProcessor.setStructure(structure);
	}

	public void setDescriptiveMD(DescriptiveMetadata dmd) {
		((DerivateStepPDF)this.step).mergeDescriptiveData(dmd);
	}

	public void setPDFProcessor(IPDFProcessor processor) {
		this.pdfProcessor = processor;
	}

	@Override
	public int create() throws DigitalDerivansException {
		if (this.digitalPages.isEmpty()) {
			var msg = "No pages for PDF " + this.pathPDF;
			throw new DigitalDerivansException(msg);
		}
		// forward pdf generation
		this.pdfProcessor.init((DerivateStepPDF)this.step, this.derivate);
		this.pdfResult = this.pdfProcessor.write(this.pathPDF.toFile());
		this.pdfResult.setPath(this.pathPDF);
		var nPagesAdded = this.pdfResult.getPdfPages().size();
		LOGGER.info("created pdf '{}' with {} pages", this.pathPDF, nPagesAdded);

		// post-action
		if ((this.mets != null) && ((DerivateStepPDF) this.step).isEnrichMetadata()) {
			this.enrichPDFInformation(this.pathPDF);
		} else {
			LOGGER.warn("pdf '{}' not enriched in METS", this.pathPDF);
		}
		return 1;
	}

	private void enrichPDFInformation(Path pathtoPDF) throws DigitalDerivansException {
		if (Files.exists(pathtoPDF)) {
			LOGGER.info("enrich created pdf '{}' in '{}'", pathtoPDF, this.mets.getPath());
			String filename = pathtoPDF.getFileName().toString();
			Optional<String> optPrefix = this.getOutputPrefix();
			if (optPrefix.isPresent()) {
				var prefix = optPrefix.get();
				filename = prefix + filename;
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

	private Path setPDFPath(DerivateStepPDF pdfStep) throws DigitalDerivansException {
		Path rootDir = this.derivate.getRootDir();
		String pdfName = rootDir.getFileName().toString() + ".pdf"; // default PDF name like workdir
		if (this.derivate instanceof DerivateMD) {
			var derivateMd = (DerivateMD) this.derivate;
			pdfStep.getModsIdentifierXPath().ifPresent(derivateMd::setIdentifierExpression);
			DescriptiveMetadata descriptiveData = derivateMd.getDescriptiveData();
			pdfName = descriptiveData.getIdentifier() + ".pdf"; // if metadata present, use as PDF name
		}
		String finalPDFName = pdfStep.getNamePDF().orElse(pdfName); // if name arg passed, use as PDF name
		if (!finalPDFName.endsWith(".pdf")) {
			finalPDFName += ".pdf";
		}
		if (Path.of(finalPDFName).isAbsolute()) {
			return Path.of(finalPDFName);
		} else {
			return rootDir.resolve(finalPDFName);
		}
	}
}
