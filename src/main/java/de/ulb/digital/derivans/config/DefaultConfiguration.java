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
	
	public static final String DEFAULT_INPUT_TYPE = "jpg";
	
	public static final String DEFAULT_OUTPUT_TYPE = "jpg";
	
	public static final Integer DEFAULT_POOLSIZE = 2;
	
	public static final Path DEFAULT_FOOTER_INPUT_SUB_PATH = Path.of("MAX");
	
	public static final Path DEFAULT_FOOTER_OUTPUT_SUB_PATH = Path.of("FOOTER_MAX");
	
	public static final Path DEFAULT_MIN_OUTPUT_SUB_PATH = Path.of("FOOTER_80");
	
	public static final Path DEFAULT_FOOTER_TEMPLATE_PATH = Path.of("").toAbsolutePath()
			.resolve("config/footer_template.png");
	
	public static final String DEFAULT_FOOTER_LABEL = "Mass Digitalization";

	public static final String DEFAULT_CONFIG_FILE = "derivans.ini";
	
	private DefaultConfiguration() {
	}
}
