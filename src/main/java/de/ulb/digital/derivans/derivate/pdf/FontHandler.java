package de.ulb.digital.derivans.derivate.pdf;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.BaseFont;

import de.ulb.digital.derivans.DigitalDerivansException;

/**
 * 
 * Handle Fonts
 * 
 * @author u.hartwig
 *
 */
public class FontHandler {
	
	public static final String DEFAULT_FONT = "DejaVuSans";
	
	public static final String DEFAULT_FONT_FILE = "ttf/"+DEFAULT_FONT+".ttf";

	public Font forGraphics(String path) throws DigitalDerivansException {
		try {
			String tmpPath = Path.of(path).getFileName().toString();
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			if (!isAvailable(tmpPath, ge)) {
				String resPath = this.storeAsTempfile(path);
				Font font = Font.createFont(Font.TRUETYPE_FONT, new File(resPath));
				if (!ge.registerFont(font)) {
					throw new DigitalDerivansException("failed to register font from " + path);
				}
				return font;
			} else {
				return Font.decode(DEFAULT_FONT);
			}
		} catch (FontFormatException | IOException e) {
			throw new DigitalDerivansException(e);
		}
	}

	/**
	 * 
	 * Inspect if desired font is already registered within system
	 * 
	 * @param resPath
	 * @param ge
	 * @return
	 */
	private boolean isAvailable(String resPath, GraphicsEnvironment ge) {
		String[] availableFonts = ge.getAvailableFontFamilyNames();
		String fileName = resPath.substring(0, resPath.lastIndexOf('.')).toLowerCase();
		Predicate<String> containsName = s -> s.toLowerCase().startsWith(fileName);
		Optional<String> optString = List.of(availableFonts).stream().map(s -> s.replace(" ", "")).filter(containsName).findAny();
		return optString.isPresent();
	}

	public BaseFont forPDF(String path) throws DigitalDerivansException {
		try {
			String resPath = this.storeAsTempfile(path);
			BaseFont baseFont = BaseFont.createFont(resPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
			com.itextpdf.text.Font f = new com.itextpdf.text.Font(baseFont, 12.0f);
			return f.getBaseFont();
		} catch (DocumentException | IOException e) {
			throw new DigitalDerivansException(e);
		}
	}

	/**
	 * 
	 * Create temporary File if Font within JAR
	 * 
	 * @param resPath
	 * @return
	 * @throws DigitalDerivansException
	 */
	private String storeAsTempfile(String resPath) throws DigitalDerivansException {
		ClassLoader cl = this.getClass().getClassLoader();
		if (cl.getResource(resPath) != null && cl.getResource(resPath).getProtocol().equals("jar")) {
			try (InputStream input = cl.getResourceAsStream(resPath)) {
				File file = File.createTempFile("derivans-tmp-font-", ".ttf");
				
				try(OutputStream out = new FileOutputStream(file)) {
					int read;
					byte[] bytes = new byte[1024];
					while ((read = input.read(bytes)) != -1) {
						out.write(bytes, 0, read);
					}
				}

				file.deleteOnExit();
				return file.toString();
			} catch (IOException e) {
				throw new DigitalDerivansException(e);
			}
		} else {
			return resPath;
		}
	}
}
