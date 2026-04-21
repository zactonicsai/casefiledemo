package gov.fbi.casemgmt.dto;

import gov.fbi.casemgmt.model.CaseStatus;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.*;

public final class CaseDtos {
    private CaseDtos() {}

    public record CreateCaseRequest(
        @NotBlank @Pattern(regexp = "^[0-9]{3}$",  message = "classification must be 3 digits")
        String classificationCode,

        @NotBlank @Pattern(regexp = "^[A-Z]{2,3}$", message = "office must be 2-3 upper case letters")
        String originatingOffice,

        @NotBlank @Size(min = 3, max = 255)
        String title,

        @Size(max = 10_000)
        String synopsis,

        Set<String> tags,

        Map<String, Object> metadata
    ) {}

    public record UpdateCaseRequest(
        @Size(min = 3, max = 255) String title,
        @Size(max = 10_000)        String synopsis,
        Set<String>                tags,
        Map<String, Object>        metadata,
        UUID                       assignedAgentId,
        UUID                       supervisorId
    ) {}

    public record CaseSummary(
        UUID        id,
        String      caseNumber,
        String      classificationCode,
        String      originatingOffice,
        Long        serialNumber,
        String      title,
        CaseStatus  status,
        UUID        assignedAgentId,
        Instant     createdAt,
        Instant     openedAt,
        int         documentCount
    ) {}

    public record CaseDetail(
        UUID        id,
        String      caseNumber,
        String      classificationCode,
        String      originatingOffice,
        Long        serialNumber,
        String      title,
        String      synopsis,
        CaseStatus  status,
        UUID        assignedAgentId,
        UUID        supervisorId,
        Set<String> tags,
        Map<String, Object> metadata,
        String      workflowId,
        Instant     createdAt,
        String      createdBy,
        Instant     updatedAt,
        String      updatedBy,
        Instant     openedAt,
        Instant     closedAt,
        String      closureReason,
        List<DocumentDtos.DocumentSummary> documents
    ) {}

    public record TransitionRequest(
        @NotBlank String comments
    ) {}

    public record CloseCaseRequest(
        @NotBlank @Size(max = 500) String closureReason
    ) {}
}
