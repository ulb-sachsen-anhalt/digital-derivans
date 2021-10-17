package de.ulb.digital.derivans;

import java.nio.file.Path;

/**
 * 
 * Central organization of any related test resources
 * 
 * @author u.hartwig
 *
 */
public enum TestResource {

	
	/**
	 * rather large digital object (+2.000 pages, complex structure)
	 */
	HD_Aa_201517(Path.of("src/test/resources/mets/vls/hd-Aa-201517.xml")),
	
	/**
	 * rather small digital object (4 pages only)
	 */
	HD_Aa_737429(Path.of("src/test/resources/mets/vls/hd-Aa-737429.mets.xml")),
	
	/**
	 * contains logical links to non-existing physical structures 
	 */
	HD_Aa_226134857(Path.of("src/test/resources/mets/vls/hd-Aa-226134857.prep.xml")),
	
	MENA_Af_1237560(Path.of("src/test/resources/mets/vls/mena-Af-1237560.ulb.xml")),
	
	VD17_Af_19788(Path.of("src/test/resources/mets/vls/vd17-Af-19788.ulb.xml")),
	VD17_AF_11250807(Path.of("src/test/resources/mets/vls/vd17-AF-11250807.ulb.xml")),
	
	VD18_Af_9427337(Path.of("src/test/resources/mets/vls/vd18-Af-9427337.ulb.xml")),
			
	/**
	 * Kitodo 2
	 */
	K2_Aa_143074601(Path.of("./src/test/resources/mets/kitodo2/143074601.xml")),
	K2_Aa_147573602(Path.of("./src/test/resources/mets/kitodo2/147573602.xml")),
	K2_Aa_319696111(Path.of("./src/test/resources/mets/kitodo2/319696111.xml")),
	K2_Af_140257772(Path.of("./src/test/resources/mets/kitodo2/140257772/140257772.xml")),
	K2_Af_030745780(Path.of("./src/test/resources/mets/kitodo2/030745772/030745780.xml")),
	K2_Af_029024749(Path.of("./src/test/resources/mets/kitodo2/029024684/029024749.xml")),
	K2_Ac_029024684(Path.of("./src/test/resources/mets/kitodo2/029024684/029024684.xml")),
	;
	
	private Path path;

	private TestResource(Path path) {
		this.path = path;
	}
	
	public Path get() {
		return this.path;
	}
}
