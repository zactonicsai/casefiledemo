package gov.fbi.casemgmt.controller;

import gov.fbi.casemgmt.dto.SearchDtos;
import gov.fbi.casemgmt.search.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Full-text + faceted search over documents")
public class SearchController {

    private final SearchService search;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Execute a full-text search with facets & highlights")
    public SearchDtos.SearchResponse query(@Valid @RequestBody SearchDtos.SearchRequest req) {
        return search.search(req);
    }
}
