package de.ulb.digital.derivans.data.mets;

/**
 * 
 * Map MODS MARC relator codes
 * 
 * @author u.hartwig
 *
 */
enum MODSRelator {

	AUTHOR("aut"),
	ASSIGNED_NAME("asn"),
	CONTRIBUTOR("ctb"),
	EDITOR("edt"),
	OTHER("oth"),
	PUBLISHER("pbl"),
	PRINTER("prt"),
	UNKNOWN("n.a.");

	private String code;

	private MODSRelator(String code) {
		this.code = code;
	}

	public static MODSRelator forCode(String code) {
		for (MODSRelator e : values()) {
			if (e.code.equals(code)) {
				return e;
			}
		}
		return UNKNOWN;
	}
}