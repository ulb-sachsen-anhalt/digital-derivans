package de.ulb.digital.derivans.data.mets;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.ulb.digital.derivans.DigitalDerivansException;

/**
 * 
 * Mappings to label METS structdata types with
 * corresponding german translation in PDF outline
 * 
 * see: http://dfg-viewer.de/strukturdatenset/
 * 
 * METS places no constraints on the possible TYPE values.
 * Suggestions for controlled vocabularies for TYPE may be
 * found on the Library of Congress METS website.
 * 
 * @author u.hartwig
 * 
 */
public enum METSContainerType {

	SECTION("section", "Abschnitt"),
	FILE("file", "Akte"),
	ALBUM("album", "Album"),
	REGISTER("register", "Amtsbuch"),
	ANNOTATION("annotation", "Annotation"),
	ADDRESS("address", "Anrede"),
	ARTICLE("article", "Artikel"),
	ATLAS("atlas", "Atlas"),
	ISSUE("issue", "Ausgabe"),
	BACHELOR_THESIS("bachelor_thesis", "Bachelorarbeit"),
	// important if volume misses "LABEL"
	VOLUME("volume", "Band"),
	CONTAINED_WORK("contained_work", "Beigefügtes Werk"),
	ADDITIONAL("additional", "Beilage"),
	REPORT("report", "Bericht"),
	OFFICAL_NOTATION("official_notification", "Bescheid"),
	PROVENANCE("provenance", "Besitznachweis"),
	INVENTORY("inventory", "Bestand"),
	IMAGE("image", "Bild"),
	COLLATION("collation", "Bogensignatur"),
	ORNAMENT("ornament", "Buchschmuck"),
	LETTER("letter", "Brief"),
	COVER_FRONT("cover_front", "Vorderdeckel"),
	COVER_BACK("cover_back", "Rückdeckel"),
	DIPLOMA_THESIS("diploma_thesis", "Diplomarbeit"),
	DOCTORAL_THESIS("doctoral_thesis", "Doktorarbeit"),
	DOCUMENT("document", "Dokument"),
	PRINTERS_MARK("printers_mark", "Druckermarke"),
	PRINTED_ARCHIVES("printed_archives", "Druckerzeugnis (Archivale)"),
	BINDING("binding", "Einband"),
	ENTRY("entry", "Eintrag"),
	CORRIGENDA("corrigenda", "Errata"),
	BOOKPLATE("bookplate", "Exlibris"),
	FASCICLE("fascicle", "Faszikel"),
	LEAFLET("leaflet", "Flugblatt"),
	RESEARCH_PAPER("research_paper", "Forschungsarbeit"),
	PHOTOGRAPH("photograph", "Fotografie"),
	FRAGMENT("fragment", "Fragment"),
	LAND_REGISTER("land_register", "Grundbuch"),
	GROUND_PLAN("ground_plan", "Grundriss"),
	HABILITATION_THESIS("habilitation_thesis", "Habilitation"),
	MANUSCRIPT("manuscript", "Handschrift"),
	ILLUSTRATION("illustration", "Illustration"),
	IMPRINT("imprint", "Impressum"),
	CONTENTS("contents", "Inhaltsverzeichnis"),
	INITIAL_DECORATION("initial_decoration", "Initialschmuck"),
	YEAR("year", "Jahr"),
	CHAPTER("chapter", "Kapitel"),
	MAP("map", "Karte"),
	CARTULARY("cartulary", "Kartular"),
	COLOPHON("colophon", "Kolophon"),
	EPHEMERA("ephemera", "Konzertprogramm"),
	ENGRAVED_TITLEPAGE("engraved_titlepage", "Kupfertitel"),
	MAGISTER_THESIS("magister_thesis", "Magisterarbeit"),
	FOLDER("folder", "Mappe"),
	MASTER_THESIS("master_thesis", "Masterarbeit"),
	MULTIVOLUME_WORK("multivolume_work", "Mehrbändiges Werk"),
	MONTH("month", "Monat"),
	MONOGRAPH("monograph", "Monographie"),
	MUSICAL_NOTATION("musical_notation", "Musiknotation"),
	PERIODICAL("periodical", "Periodica"),
	POSTER("poster", "Poster"),
	PLAN("plan", "Plan"),
	PRIVILEGES("privileges", "Privilegien"),
	INDEX("index", "Register"),
	SPINE("spine", "Rücken"),
	SCHEME("scheme", "Schema"),
	EDGE("edge", "Schnitt"),
	SEAL("seal", "Siegel"),
	PASTE_DOWN("paste_down", "Spiegel"),
	STAMP("stamp", "Stempel"),
	STUDY("study", "Studie"),
	TABLE("table", "Tabelle"),
	DAY("day", "Tag"),
	PROCEEDING("proceeding", "Tagungsband"),
	TEXT("text", "Text"),
	TITLE_PAGE("title_page", "Titelblatt"),
	SUBINVENTORY("subinventory", "Unterbestannd"),
	ACT("act", "Urkunde"),
	JUDGEMENT("judgement", "Urteil"),
	VERSE("verse", "Verse"),
	NOTE("note", "Vermerk"),
	PREPRINT("preprint", "Vorabdruck"),
	DOSSIER("dossier", "Vorgang"),
	LECTURE("lecture", "Vorlesung"),
	ENDSHEET("endsheet", "Vorsatz"),
	PAPER("paper", "Vortrag"),
	PREFACE("preface", "Vorwort"),
	DEDICATION("dedication", "Widmung"),
	NEWSPAPER("newspaper", "Zeitung"),

	// this is peculiar since not listed at Strukturdatenset
	// *but* DFG METS heavy relies on it for it's physical line out
	PAGE("page", "Seite"),
	PHYSSEQUENCE("physSequence","Seiten"),
	// OCR-D related container type
	OTHER("other", "Weitere")
	;

	private METSContainerType(String label, String translation) {
		this.label = label;
		this.translation = translation;
	}

	private String label;
	private String translation;

	public static Optional<String> getTranslation(String label) {
		Optional<METSContainerType> optMatch = Arrays.stream(METSContainerType.values())
				.filter(struct -> struct.label.toUpperCase().equals(label))
				.findFirst();
		if (optMatch.isPresent()) {
			return Optional.of(optMatch.get().translation);
		}
		return Optional.empty();
	}

	public String getLabel() {
		return this.label;
	}

	public static METSContainerType forLabel(String label) throws DigitalDerivansException {
		Optional<METSContainerType> optValue = Arrays.stream(METSContainerType.values())
				.filter(struct -> struct.label.equalsIgnoreCase(label))
				.findFirst();
		if (optValue.isPresent()) {
			return optValue.get();
		}
		throw new DigitalDerivansException("No known METS container type: " + label);
	}

	static List<METSContainerType> digitalObjects = List.of(MONOGRAPH, MANUSCRIPT, VOLUME, ISSUE, ADDITIONAL);

	/**
	 * 
	 * Is given type stand-alone digital object
	 * with pages/images?
	 * 
	 * @param type
	 * @return
	 */
	public static boolean isObject(METSContainerType type) {
		return digitalObjects.contains(type);
	}

	// mark top container
	public static final List<METSContainerType> MEDIA_CONTAINER = List.of(
		METSContainerType.MONOGRAPH,
		METSContainerType.MANUSCRIPT,
		METSContainerType.VOLUME,
		METSContainerType.ISSUE,
		METSContainerType.ADDITIONAL);
	public static final List<METSContainerType> MEDIA_CONTAINER_PARENT = List.of(
		METSContainerType.MULTIVOLUME_WORK,
		METSContainerType.PERIODICAL);
	public static final List<METSContainerType> NEWSPAPER_CONTAINER = List.of(
		METSContainerType.ISSUE,
		METSContainerType.ADDITIONAL);
	public static final List<METSContainerType> NEWSPAPER_CONTAINER_PARENT = List.of(
		METSContainerType.NEWSPAPER,
		METSContainerType.YEAR, 
		METSContainerType.MONTH, 
		METSContainerType.DAY);

}
