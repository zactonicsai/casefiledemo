package gov.fbi.casemgmt.repository;

import gov.fbi.casemgmt.model.SerialDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SerialDocumentRepository extends JpaRepository<SerialDocument, UUID> {

    List<SerialDocument> findByCaseFile_IdOrderBySerialIndexAsc(UUID caseId);

    Optional<SerialDocument> findByCaseFile_IdAndSerialIndex(UUID caseId, Integer serialIndex);

    @Query("SELECT COALESCE(MAX(d.serialIndex), 0) FROM SerialDocument d WHERE d.caseFile.id = :caseId")
    Integer findMaxSerialIndex(UUID caseId);

    @Modifying
    @Query("UPDATE SerialDocument d SET d.processingStatus = :status WHERE d.id = :id")
    int updateProcessingStatus(UUID id, SerialDocument.ProcessingStatus status);
}
