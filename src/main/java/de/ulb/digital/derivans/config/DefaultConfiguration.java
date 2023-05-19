package de.ulb.digital.derivans.config;

import java.nio.file.Path;

/**
 * 
 * Derivans Configuration Fallbacks
 * 
 * @author u.hartwig
 *
 */
public class DefaultConfiguration {

	public static final Integer DEFAULT_QUALITY = 80;
	
	public static final Integer DEFAULT_QUALITY_FOOTER = 95;

	// PDF_A_1A, PDF_A_1B, PDF_A_2A, PDF_A_2B 
	// PDF_A_2U, PDF_A_3A, PDF_A_3B, PDF_A_3U
	public static final String PDFA_CONFORMANCE_LEVEL = "PDF_A_1B";
	
	public static final int PDF_IMAGE_DPI = 300;

	public static final String DEFAULT_FONT = "./src/test/resources/ttf/FreeMonoBold.ttf";

	public static final String DEFAULT_INPUT_TYPE = "jpg";
	
	public static final String DEFAULT_OUTPUT_TYPE = "jpg";

	public static final Integer DEFAULT_FONT_SIZE = 8;
	
	public static final Integer DEFAULT_POOLSIZE = 2;

	public static final Integer DEFAULT_MAXIMAL = 14400; // limited by itextpdf  

	public static final String DEFAULT_INPUT_IMAGES_LABEL = "MAX";

	public static final Path DEFAULT_INPUT_IMAGES = Path.of(DEFAULT_INPUT_IMAGES_LABEL);

	public static final String DEFAULT_INPUT_FULLTEXT_LABEL = "FULLTEXT";
	
	public static final Path DEFAULT_INPUT_FULLTEXT = Path.of(DEFAULT_INPUT_FULLTEXT_LABEL);
	
	public static final Path DEFAULT_MAX_FOOTER_OUTPUT_SUB_PATH = Path.of("FOOTER_MAX");
	
	public static final Path DEFAULT_FOOTER_MIN_OUTPUT_SUB_PATH = Path.of("FOOTER_80");

	public static final String DEFAULT_MIN_OUTPUT_LABEL = "IMAGE_80";
	
	public static final Path DEFAULT_FOOTER_TEMPLATE_PATH = Path.of("").toAbsolutePath()
			.resolve("config/footer_template.png");
	
	public static final String DEFAULT_FOOTER_LABEL = "Mass Digitalization";

	public static final String DEFAULT_CONFIG_FILE_LABEL = "derivans.ini";

	public static final String DEFAULT_RENDER_LEVEL = "line";

	public static final String DEFAULT_RENDER_VISIBILTY = "invisible";
	
	private DefaultConfiguration() {
	}
}
