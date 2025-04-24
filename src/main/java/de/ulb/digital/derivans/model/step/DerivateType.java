package de.ulb.digital.derivans.model.step;

/**
 * 
 * @author hartwig
 *
 */
public enum DerivateType {

	IMAGE, JPG, JPG_FOOTER, PDF, TIF, UNKNOWN;

	public static DerivateType forLabel(String label) {
		return DerivateType.valueOf(label.toUpperCase());
	}
}
