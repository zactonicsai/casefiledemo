package gov.fbi.casemgmt.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Atomically issues the next serial number for a given (classification, office)
 * pair by UPSERTing into {@code case_serial_sequence} and RETURNING the bumped
 * value. Safe under concurrent creates because Postgres serializes the row
 * update.
 */
@Repository
@RequiredArgsConstructor
public class CaseSerialSequenceRepository {

    private final JdbcTemplate jdbc;

    private static final String SQL = """
        INSERT INTO case_serial_sequence (classification_code, originating_office, next_value)
        VALUES (?, ?, 2)
        ON CONFLICT (classification_code, originating_office) DO UPDATE
          SET next_value = case_serial_sequence.next_value + 1
        RETURNING (next_value - 1)
        """;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long nextSerial(String classificationCode, String office) {
        Long v = jdbc.queryForObject(SQL, Long.class, classificationCode, office);
        if (v == null) {
            throw new IllegalStateException("Failed to allocate serial number");
        }
        return v;
    }
}
