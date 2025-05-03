package de.ulb.digital.derivans;

/*
 * 
 * Common API
 * 
 * @autho u.hartwig
 * 
 */
public interface IDerivans {

    /*
	 * Default METS file group required by DDB / DFG viewer
	 */
	String IMAGE_DIR_DEFAULT = "DEFAULT";

	/*
	 * METS file group used for ULB OAI harvesting and OCR-ing
	 */
	String IMAGE_DIR_MAX = "MAX";

	/**
	 * ULB specific Workflow directories
	 * IMAGE_FOOTER		: images with footer appended and quality set t
	 * IMAGE_80			: jpg images with quality set to 80% and max dimension depending on media type
	 * 					  (input for PDF generation)
	 * (DSpace specific image bundles)
	 * IMAGE_PREVIEW	: jpg images with max dimension forced to 1000px
	 * IMAGE_THUMBNAIL	: jpg images with max dimension forced to 128px
	 * 
	*/
	String IMAGE_FOOTER = "IMAGE_FOOTER";
	String IMAGE_Q80 = "IMAGE_80";
	String IMAGE_PREVIEW = "BUNDLE_BRANDED_PREVIEW__";
	String IMAGE_THUMBNAIL = "BUNDLE_THUMBNAIL__";

	/*
	 * METS file group used by Kitodo.Presentation
	 */
	String IMAGE_DIR_ORIGINAL = "ORIGINAL";

	/*
	 * Default METS file group for OCR-data with, most likely,
	 * MIMETYPE="application/alto+xml"
	 */
	String FULLTEXT_DIR = "FULLTEXT";

	/*
	 * Mark all unresolved information about author, title, ...
	 */
	String UNKNOWN = "n.a.";
    
}
