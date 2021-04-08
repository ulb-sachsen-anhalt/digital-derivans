package de.ulb.digital.derivans.data.ocr;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import de.ulb.digital.derivans.DigitalDerivansException;

/**
 * 
 * Create {@link OCRReader} depending on provided data
 * 
 * @author u.hartwig
 *
 */
public class OCRReaderFactory {

	private static List<OCRReader> readers = new ArrayList<>();

	static {
		readers.add(new ALTOReader(Type.ALTO_V4));
		readers.add(new ALTOReader(Type.ALTO_V3));
	}
	
	private OCRReaderFactory() {}
	
	public static OCRReader from(Path path) throws DigitalDerivansException {
		
		String prelude = readStart(path);
		Type type = mapToType(prelude);
		if(type != Type.UNKNOWN) {
			for (OCRReader r : readers) {
				if(r.getType() == type) {
					return r;
				}
			}
		}
		
		throw new DigitalDerivansException("Unknown OCR-Data format detected in "+path);
	}

	private static Type mapToType(String prelude) {
		if(prelude.indexOf("<alto ") > -1) {
			if(prelude.indexOf("alto/ns-v4") > -1) {
				return Type.ALTO_V4;
			} else if (prelude.indexOf("alto/ns-v3") > -1) {
				return Type.ALTO_V3;
			}
		}
		return Type.UNKNOWN;
	}

	private static String readStart(Path path) throws DigitalDerivansException {
		try (FileReader reader = new FileReader(path.toString());
		     BufferedReader bufferedReader = new BufferedReader(reader, 256)) {
		  char[] cBuff = new char[256];
		  bufferedReader.read(cBuff);
		  return String.valueOf(cBuff);
		} catch (IOException e) {
			throw new DigitalDerivansException(e);
		}
	}
}
