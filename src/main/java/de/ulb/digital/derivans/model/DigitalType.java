package de.ulb.digital.derivans.model;

/**
 *
 * @author hartwig
 *
 */
public enum DigitalType {

	IMAGE, JPG, JPG_FOOTER, OCR, PDF, TIF, UNKNOWN;

	public static DigitalType forLabel(String label) {
		return DigitalType.valueOf(label.toUpperCase());
	}
}
