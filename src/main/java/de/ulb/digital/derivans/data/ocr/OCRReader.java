package de.ulb.digital.derivans.data.ocr;

import java.nio.file.Path;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.model.ocr.OCRData;


/**
 * 
 * Interface for reading OCR-Data from a given resource.
 * 
 * @author u.hartwig
 *
 */
public interface OCRReader {
	
	/**
	 * 
	 * Read OCR-Data from given {@link Path} and transform it into domain specific {@link OCRData}.
	 * 
	 * @param pathOcr
	 * @return ocr data
	 * @throws DigitalDerivansException
	 */
	OCRData get(Path pathOcr) throws DigitalDerivansException;

	/**
	 * 
	 * Map Reader to a specific {@link Type OCR-Data Type}
	 * 
	 * @return
	 */
	Type getType();
}
