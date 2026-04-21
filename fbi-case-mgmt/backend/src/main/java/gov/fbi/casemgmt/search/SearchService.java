package gov.fbi.casemgmt.search;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import gov.fbi.casemgmt.dto.SearchDtos;
import gov.fbi.casemgmt.model.DocumentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Full-text search over indexed documents with facets + highlighted snippets.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final ElasticsearchTemplate template;
    private final IndexedDocumentRepository indexRepo;

    @Value("${app.search.index-name}")
    private String indexName;

    public void index(IndexedDocument doc) {
        indexRepo.save(doc);
    }

    public void delete(String id) {
        indexRepo.deleteById(id);
    }

    public SearchDtos.SearchResponse search(SearchDtos.SearchRequest req) {
        long start = System.currentTimeMillis();

        BoolQuery.Builder bool = new BoolQuery.Builder();
        bool.must(m -> m.multiMatch(mm -> mm
            .query(req.query())
            .fields("title^3", "description^2", "body")
            .type(TextQueryType.BestFields)
            .fuzziness("AUTO")));

        addTerms(bool, "classificationCode", req.classifications());
        addTerms(bool, "originatingOffice",  req.offices());
        addTerms(bool, "caseStatus",
            req.statuses() == null ? null :
            req.statuses().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()));
        addTerms(bool, "documentType",
            req.documentTypes() == null ? null :
            req.documentTypes().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()));

        if (req.fromDate() != null || req.toDate() != null) {
            bool.filter(f -> f.range(r -> {
                r.field("uploadedAt");
                if (req.fromDate() != null) r.gte(JsonData.of(req.fromDate().toString()));
                if (req.toDate()   != null) r.lte(JsonData.of(req.toDate().toString()));
                return r;
            }));
        }

        int page = req.pageOrDefault();
        int size = req.sizeOrDefault();

        SearchRequest esReq = SearchRequest.of(s -> s
            .index(indexName)
            .query(q -> q.bool(bool.build()))
            .highlight(Highlight.of(h -> h
                .fields("body",  HighlightField.of(f -> f.numberOfFragments(2).fragmentSize(200)))
                .fields("title", HighlightField.of(f -> f.numberOfFragments(1).fragmentSize(120)))
                .preTags("<mark>").postTags("</mark>")))
            .aggregations("classifications", a -> a.terms(t -> t.field("classificationCode").size(20)))
            .aggregations("offices",         a -> a.terms(t -> t.field("originatingOffice").size(20)))
            .aggregations("documentTypes",   a -> a.terms(t -> t.field("documentType").size(20)))
            .sort(so -> so.score(v -> v.order(SortOrder.Desc)))
            .from(page * size)
            .size(size)
            .trackTotalHits(th -> th.enabled(true))
        );

        try {
            SearchResponse<IndexedDocument> resp =
                template.getElasticsearchClient().search(esReq, IndexedDocument.class);

            List<SearchDtos.SearchHit> hits = resp.hits().hits().stream()
                .map(this::toHit)
                .toList();

            long total = resp.hits().total() != null ? resp.hits().total().value() : 0L;

            return new SearchDtos.SearchResponse(
                total, page, size, System.currentTimeMillis() - start,
                hits,
                extractFacet(resp, "classifications"),
                extractFacet(resp, "offices"),
                extractFacet(resp, "documentTypes")
            );
        } catch (IOException e) {
            throw new IllegalStateException("Search failed", e);
        }
    }

    private void addTerms(BoolQuery.Builder bool, String field, Set<String> values) {
        if (values == null || values.isEmpty()) return;
        bool.filter(f -> f.terms(t -> t
            .field(field)
            .terms(tv -> tv.value(values.stream()
                .map(co.elastic.clients.elasticsearch._types.FieldValue::of).toList()))));
    }

    private SearchDtos.SearchHit toHit(Hit<IndexedDocument> h) {
        IndexedDocument d = h.source();
        if (d == null) return null;
        String snippet = null;
        if (h.highlight() != null) {
            List<String> body = h.highlight().get("body");
            if (body != null && !body.isEmpty()) snippet = String.join(" ... ", body);
        }
        if (snippet == null && d.getBody() != null) {
            snippet = d.getBody().length() > 240 ? d.getBody().substring(0, 240) + "..." : d.getBody();
        }
        return new SearchDtos.SearchHit(
            UUID.fromString(d.getId()),
            UUID.fromString(d.getCaseId()),
            d.getCaseNumber(),
            d.getSerialIndex() == null ? 0 : d.getSerialIndex(),
            d.getDocumentType() == null ? DocumentType.OTHER : d.getDocumentType(),
            d.getTitle(),
            snippet,
            h.score() == null ? 0f : h.score().floatValue()
        );
    }

    private List<SearchDtos.FacetBucket> extractFacet(SearchResponse<?> resp, String name) {
        var agg = resp.aggregations().get(name);
        if (agg == null || !agg.isSterms()) return List.of();
        return agg.sterms().buckets().array().stream()
            .map(b -> new SearchDtos.FacetBucket(b.key().stringValue(), b.docCount()))
            .toList();
    }
}
