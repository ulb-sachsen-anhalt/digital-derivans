package de.ulb.digital.derivans.data.mets;

import de.ulb.digital.derivans.DigitalDerivansRuntimeException;

/**
 * 
 * Determine fixed mets:div attribute labels
 * 
 */
public enum METSContainerAttributeType {

	ID,
	DMDID,
	ADMID,
	TYPE,
	LABEL,
	ORDER,
	ORDERLABEL,
	CONTENTIDS,
	;

	public static METSContainerAttributeType get(String label) {
		for (var e : METSContainerAttributeType.values()) {
			if (e.name().equals(label)) {
				return e;
			}
		}
		throw new DigitalDerivansRuntimeException("unknown mets:div@TYPE " + label);
	}
}
