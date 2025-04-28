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

	static class Key {

		public static final String PDF_ENRICH_METADATA = "enrich_pdf_metadata";

		public static final String PDF_MODS_IDENTIFIER_XPATH = "mods_identifier_xpath";

		public static final String PDF_METS_FILEGROUP_FULLTEXT = "mets_filegroup_fulltext";

		public static final String PDF_METS_FILEGROUP_IMAGES = "mets_filegroup_images";

		private Key() {}
	}

	public static final Integer DEFAULT_QUALITY = 80;
	
	public static final Integer DEFAULT_QUALITY_FOOTER = 95;

	/**
	 * 
	 * Different PDF_A conformance levels
	 * PDF_A_1A, PDF_A_1B, PDF_A_2A, PDF_A_2B 
	 * PDF_A_2U, PDF_A_3A, PDF_A_3B, PDF_A_3U
	 * 
	 * cf. https://en.wikipedia.org/wiki/PDF/A
	 * 
	 */
	public static final String PDFA_CONFORMANCE_LEVEL = "PDF_A_1B";
	
	public static final int DEFAULT_IMAGE_DPI = 300;

	// public static final String DEFAULT_OUTPUT_TYPE = DerivateType.JPG.toString().toLowerCase();

	public static final Integer DEFAULT_FONT_SIZE = 8;
	
	public static final Integer DEFAULT_POOLSIZE = 2;

	/*
	 * Restricted by used PDF-rendering component (itextpdf5)
	 */
	public static final Integer DEFAULT_MAXIMAL = 14400; 

	public static final Path DEFAULT_MAX_FOOTER_OUTPUT_SUB_PATH = Path.of("FOOTER_MAX");
	
	public static final Path DEFAULT_FOOTER_MIN_OUTPUT_SUB_PATH = Path.of("FOOTER_80");

	public static final String DEFAULT_MIN_OUTPUT_LABEL = "IMAGE_80";
	
	public static final Path DEFAULT_PATH_FOOTER_TEMPLATE = Path.of("").toAbsolutePath()
			.resolve("config/footer_template.png");
	
	public static final String DEFAULT_FOOTER_LABEL = "Mass Digitalization";

	public static final String DEFAULT_CONFIG_FILE_LABEL = "derivans.ini";

	public static final TypeConfiguration DEFAULT_RENDER_LEVEL = TypeConfiguration.RENDER_LEVEL_LINE;

	public static final TypeConfiguration DEFAULT_RENDER_VISIBILTY = TypeConfiguration.RENDER_MODUS_HIDE;
	
	private DefaultConfiguration() {
	}
}
