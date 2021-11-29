package de.ulb.digital.derivans.data.ocr;

import org.jdom2.Namespace;

/**
 * 
 * Type mappings for OCR-Data formats
 * 
 * @author u.hartwig
 *
 */
public enum Type {

	ALTO_V4("alto4", "http://www.loc.gov/standards/alto/ns-v4#"),
	
	ALTO_V3("alto3", "http://www.loc.gov/standards/alto/ns-v3#"),
	
	PAGE_2019("pc2019", "http://schema.primaresearch.org/PAGE/gts/pagecontent/2019-07-15"),
	
	UNKNOWN("","")
	;
	
	private String ns;
	
	private String uri;
	
	private Type(String ns, String uri) {
		this.ns = ns;
		this.uri = uri;
	}
	
	public static Type valueOfUri(String uri) {
		for(Type t : values()) {
			if(t.uri.equalsIgnoreCase(uri)) {
				return t;
			}
		}
		return UNKNOWN;
	}
	
	public Namespace toNS() {
		return Namespace.getNamespace(ns, uri);
	}
	
}
