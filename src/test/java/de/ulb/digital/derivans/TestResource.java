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
	 * contains titleInfo but child node title is missing 
	 */
	HD_Aa_5175671(Path.of("src/test/resources/mets/vls/5175671.mets.xml")),

	/**
	 * contains logical links to non-existing physical structures 
	 */
	HD_Aa_226134857_LEGACY(Path.of("src/test/resources/mets/vls/hd-Aa-226134857.prep.xml")),
	
	MENA_Af_1237560(Path.of("src/test/resources/mets/vls/mena-Af-1237560.ulb.xml")),
	
	VD17_Af_19788(Path.of("src/test/resources/mets/vls/vd17-Af-19788.ulb.xml")),
	VD17_AF_11250807(Path.of("src/test/resources/mets/vls/vd17-AF-11250807.ulb.xml")),
	
	VD18_Af_9427337(Path.of("src/test/resources/mets/vls/vd18-Af-9427337.ulb.xml")),
	VD18_Aa_9989442(Path.of("src/test/resources/mets/vls/vd18-9989442.ulb.xml")),
			
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
	
	/**
	 * Kitodo 3
	 */
	K3_ZD2_1021634069(Path.of("src/test/resources/mets/kitodo3/zd2-1021634069-18680621/1021634069-18680621.xml")),

	/**
	 * Image Processing
	 */
	IMG_JPG_148811035_MAX_1(Path.of("src/test/resources/alto/148811035/MAX/00000001.jpg")),
	IMG_TIF_MENA_1(Path.of("src/test/resources/images/3900_00000010x128.tif")),
	IMG_TIF_ZD1_GREY(Path.of("src/test/resources/images/1681877805_J_0125_0001x128.tif")),
	IMG_TIF_ZD1_RGB(Path.of("src/test/resources/images/1681875195_J_0001_0008x128.tif")),
	IMG_JPG_ZD2_GREY(Path.of("src/test/resources/images/1667522809_J_0025_0001x128.jpg")),

	/**
	 * opendata 
	 * both METS belong together, they describe the same digital object
	 */
	SHARE_IT_VD18_MIG(Path.of("src/test/resources/mets/share_it/1981185920_79009.xml")),
	VD18_Aa_VD18_MIG(Path.of("src/test/resources/mets/vls/vd18-Aa-16372279.mets.xml")),

	;
	
	private Path path;

	private TestResource(Path path) {
		this.path = path;
	}
	
	public Path get() {
		return this.path;
	}
}
