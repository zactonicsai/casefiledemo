package gov.fbi.casemgmt.model;

/**
 * Types of serial documents that can be filed into a case.
 * Maps to the "serial" concept: numbered artifacts within a case file.
 */
public enum DocumentType {
    /** Interview report — the famous FBI FD-302 form. */
    FD_302("FD-302", "Interview Report"),
    /** Electronic communication. */
    EC("EC", "Electronic Communication"),
    /** Letterhead memorandum. */
    LHM("LHM", "Letterhead Memorandum"),
    /** Chain-of-custody log for physical evidence. */
    EVIDENCE_LOG("EVD", "Evidence Log"),
    /** Photographic or digital imagery evidence. */
    PHOTO("PHOTO", "Photograph / Image"),
    /** Investigator notes and observations. */
    NOTE("NOTE", "Investigator Notes"),
    /** Affidavit or sworn statement. */
    AFFIDAVIT("AFF", "Affidavit"),
    /** Subpoena or legal process. */
    LEGAL_PROCESS("LP", "Legal Process"),
    /** Generic attachment. */
    OTHER("OTH", "Other Attachment");

    private final String code;
    private final String label;

    DocumentType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode()  { return code; }
    public String getLabel() { return label; }
}
