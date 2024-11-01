package de.ulb.digital.derivans.derivate.pdf;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Font;
import java.net.URL;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.itextpdf.text.pdf.BaseFont;


/**
 * 
 * Font Handling Specification
 * 
 * @author u.hartwig
 *
 */
public class TestFontHandler {

	@Test
	void testLoadFontDejaVuSansForGraphics() throws Exception {
		String path = "src/main/resources/ttf/DejaVuSans.ttf";
		Font f = new FontHandler().forGraphics(path);
		assertNotNull(f);
	}
	
	@Test
	void testLoadFontDejaVuSansForGraphicsFromClassPath() throws Exception {
		String path = "ttf/DejaVuSans.ttf";
		URL url = this.getClass().getClassLoader().getResource(path);
		assertNotNull(url);
		Path p = Path.of(url.toURI());
		Font f = new FontHandler().forGraphics(p.toString());
		assertNotNull(f);
	}
	
	@Test
	void testLoadFontDejaVuSansForPDF() throws Exception {
		String path = "src/main/resources/ttf/DejaVuSans.ttf";
		BaseFont baseFont = new FontHandler().forPDF(path);
		assertNotNull(baseFont);
	}
}
