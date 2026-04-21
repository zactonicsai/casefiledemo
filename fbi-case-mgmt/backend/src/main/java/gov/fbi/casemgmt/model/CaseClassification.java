package gov.fbi.casemgmt.model;

/**
 * A subset of FBI case classification codes.
 * The real FBI classification schedule runs to several hundred codes;
 * this is a representative subset for reference purposes.
 */
public enum CaseClassification {
    C_066("066", "Administrative Matter"),
    C_089("089", "Drug Investigation"),
    C_091("091", "Bank Robbery"),
    C_111("111", "Violent Crime - Bank Crimes"),
    C_174("174", "Bombing Matters"),
    C_196("196", "Fraud Against the Government"),
    C_245("245", "Domestic Terrorism"),
    C_281("281", "Foreign Counterintelligence"),
    C_288("288", "Cyber Intrusion"),
    C_305("305", "Public Corruption");

    private final String code;
    private final String description;

    CaseClassification(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static CaseClassification fromCode(String code) {
        for (CaseClassification c : values()) {
            if (c.code.equals(code)) return c;
        }
        throw new IllegalArgumentException("Unknown classification: " + code);
    }
}
