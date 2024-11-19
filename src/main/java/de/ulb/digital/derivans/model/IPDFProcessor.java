package de.ulb.digital.derivans.model;

import java.io.File;
import java.util.List;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.model.pdf.PDFResult;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;

/**
 * 
 * Base interface all actual PDF-Writers must comply
 * 
 * @author hartwig
 */
public interface IPDFProcessor {
	
	/*
	 * Minimal font size in points(?)
	 */
	float MIN_CHAR_SIZE = .75f;

	/**
	 * 
	 * Ensure Processor setup with required
	 * information from {@link DerivateStepPDF},
	 * knows all {@link DigitalPage digital pages}
	 * and the {@link DigitalStructureTree object structure},
	 * if present
	 * 
	 * @param pdfStep
	 * @param pages
	 * @param structure
	 * @throws DigitalDerivansException
	 */
	void init(DerivateStepPDF pdfStep, List<DigitalPage> pages, DigitalStructureTree structure) throws DigitalDerivansException;
	

	/**
	 * Enforce integration of PDF Metadata (author, title, etc.)
	 */
	void addMetadata();
	
	/**
	 * 
	 * Write PDF file to given file descriptor
	 * Return {@link PDFResult}-instance for
	 * introspection
	 * 
	 * @param fileDescriptor
	 * @return
	 * @throws DigitalDerivansException
	 */
	PDFResult write(File fileDescriptor) throws DigitalDerivansException;
	
}
