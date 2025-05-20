package de.ulb.digital.derivans.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import de.ulb.digital.derivans.IDerivans;
import de.ulb.digital.derivans.model.step.DerivateStep;
import de.ulb.digital.derivans.model.step.DerivateStepImage;
import de.ulb.digital.derivans.model.step.DerivateStepPDF;
import de.ulb.digital.derivans.model.step.DerivateType;

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

		public static final String PDF_CONFORMANCE = "pdf_conformance";

		private Key() {}
	}

	public static final Integer DEFAULT_QUALITY = 80;
	
	public static final Integer DEFAULT_QUALITY_FOOTER = 95;

	/**
	 * 
	 * Please note, that PDF/A conformance level 
	 * (cf. https://en.wikipedia.org/wiki/PDF/A) depends
	 * on the specific PDF backend library version
	 * 
	 */
	public static final String DEFAULT_CONFORMANCE_LEVEL = "PDF/A-1B";
	
	public static final int DEFAULT_IMAGE_DPI = 300;

	public static final Integer DEFAULT_FONT_SIZE = 8;
	
	public static final Integer DEFAULT_POOLSIZE = 2;

	/*
	 * Restricted by used PDF-rendering component (itextpdf5)
	 */
	public static final Integer DEFAULT_MAXIMAL = 14400; 

	public static final Path DEFAULT_MAX_FOOTER_OUTPUT_SUB_PATH = Path.of("FOOTER_MAX");
	
	public static final Path DEFAULT_FOOTER_MIN_OUTPUT_SUB_PATH = Path.of(IDerivans.IMAGE_Q80);

	public static final String DEFAULT_IMG_REDUCED = IDerivans.IMAGE_Q80;
	
	public static final Path DEFAULT_PATH_FOOTER_TEMPLATE = Path.of("").toAbsolutePath()
			.resolve("config/footer_template.png");
	
	public static final String DEFAULT_FOOTER_LABEL = "Mass Digitalization";

	public static final String DEFAULT_CONFIG_FILE_LABEL = "derivans.ini";

	public static final TypeConfiguration DEFAULT_RENDER_LEVEL = TypeConfiguration.RENDER_LEVEL_LINE;

	public static final TypeConfiguration DEFAULT_RENDER_VISIBILTY = TypeConfiguration.RENDER_MODUS_HIDE;
	
	private DefaultConfiguration() {
	}


		/**
	 * Default Steps supposed to work out-of-the-box,
	 * without any METS-data to be respected.
	 * 
	 * Take care of optional provided CLI params.
	 * 
	 */
	public static List<DerivateStep> provideDefaultSteps() {
		List<DerivateStep> steps = new ArrayList<>();
		DerivateStepImage create80sJpgs = new DerivateStepImage();
		create80sJpgs.setOutputType(DerivateType.JPG);
		var output = DefaultConfiguration.DEFAULT_IMG_REDUCED;
		var imgDir = IDerivans.IMAGE_DIR_DEFAULT;
		create80sJpgs.setInputDir(imgDir);
		create80sJpgs.setOutputDir(output);
		create80sJpgs.setQuality(DefaultConfiguration.DEFAULT_QUALITY);
		create80sJpgs.setPoolsize(DefaultConfiguration.DEFAULT_POOLSIZE);
		steps.add(create80sJpgs);
		DerivateStepPDF createPdf = new DerivateStepPDF();
		createPdf.setInputDir(output);
		createPdf.setOutputType(DerivateType.PDF);
		createPdf.setOutputDir(".");
		steps.add(createPdf);
		return steps;
	}
}
