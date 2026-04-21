package gov.fbi.casemgmt.dto;

import gov.fbi.casemgmt.model.CaseStatus;
import gov.fbi.casemgmt.model.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class SearchDtos {
    private SearchDtos() {}

    public record SearchRequest(
        @NotBlank @Size(max = 1024) String query,
        Set<String>       classifications,
        Set<String>       offices,
        Set<CaseStatus>   statuses,
        Set<DocumentType> documentTypes,
        Instant           fromDate,
        Instant           toDate,
        Integer           page,
        Integer           size
    ) {
        public int pageOrDefault()  { return page == null ? 0  : Math.max(page, 0); }
        public int sizeOrDefault()  { return size == null ? 25 : Math.min(Math.max(size, 1), 100); }
    }

    public record SearchHit(
        UUID         documentId,
        UUID         caseId,
        String       caseNumber,
        int          serialIndex,
        DocumentType documentType,
        String       title,
        String       snippet,
        float        score
    ) {}

    public record SearchResponse(
        long totalHits,
        int  page,
        int  size,
        long queryTimeMs,
        List<SearchHit> hits,
        List<FacetBucket> classificationFacets,
        List<FacetBucket> officeFacets,
        List<FacetBucket> typeFacets
    ) {}

    public record FacetBucket(String key, long count) {}
}
