package de.ulb.digital.derivans.derivate.image;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import de.ulb.digital.derivans.DigitalDerivansException;
import de.ulb.digital.derivans.data.io.TmpJarResource;

/**
 * 
 * Handle fonts for direct rendering
 * 
 * @author u.hartwig
 *
 */
public class FontHandler {

	public static final String DEFAULT_FONT = "DejaVuSans";

	public static final String DEFAULT_FONT_FILE = "ttf/" + DEFAULT_FONT + ".ttf";

	public Font forGraphics(String path) throws DigitalDerivansException {
		try {
			String tmpPath = Path.of(path).getFileName().toString();
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			if (!isAvailable(tmpPath, ge)) {
				TmpJarResource tmpResHandler = new TmpJarResource(path);
				String resPath = tmpResHandler.extract("derivans-tmp-font", ".ttf");
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
		Optional<String> optString = List.of(availableFonts).stream().map(s -> s.replace(" ", "")).filter(containsName)
				.findAny();
		return optString.isPresent();
	}

}
