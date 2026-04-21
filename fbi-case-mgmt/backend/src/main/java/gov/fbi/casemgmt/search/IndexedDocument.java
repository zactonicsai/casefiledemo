package gov.fbi.casemgmt.search;

import gov.fbi.casemgmt.model.CaseStatus;
import gov.fbi.casemgmt.model.DocumentType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;
import java.util.List;

/**
 * Denormalized document+case representation stored in Elasticsearch for
 * full-text search. Each serial document is one ES document; the parent case's
 * facets are duplicated here to make filtering cheap.
 */
@Document(indexName = "#{@environment.getProperty('app.search.index-name')}")
@Setting(settingPath = "/elasticsearch/settings.json")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class IndexedDocument {

    @Id
    private String id;                     // = SerialDocument UUID

    @Field(type = FieldType.Keyword)
    private String caseId;

    @Field(type = FieldType.Keyword)
    private String caseNumber;

    @Field(type = FieldType.Integer)
    private Integer serialIndex;

    @Field(type = FieldType.Keyword)
    private String classificationCode;

    @Field(type = FieldType.Keyword)
    private String originatingOffice;

    @Field(type = FieldType.Keyword)
    private CaseStatus caseStatus;

    @Field(type = FieldType.Keyword)
    private DocumentType documentType;

    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "english_case_analyzer"),
        otherFields = { @InnerField(suffix = "raw", type = FieldType.Keyword) }
    )
    private String title;

    @Field(type = FieldType.Text, analyzer = "english_case_analyzer")
    private String description;

    /** OCR'd / extracted body text. */
    @Field(type = FieldType.Text, analyzer = "english_case_analyzer", termVector = TermVector.with_positions_offsets)
    private String body;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Date)
    private Instant uploadedAt;

    @Field(type = FieldType.Keyword)
    private String uploadedBy;
}
