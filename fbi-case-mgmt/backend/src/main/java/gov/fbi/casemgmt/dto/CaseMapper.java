package gov.fbi.casemgmt.dto;

import gov.fbi.casemgmt.model.Case;
import gov.fbi.casemgmt.model.SerialDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CaseMapper {

    @Mapping(target = "documentCount", expression = "java(c.getDocuments() == null ? 0 : c.getDocuments().size())")
    CaseDtos.CaseSummary toSummary(Case c);

    @Mapping(target = "documents", qualifiedByName = "toDocSummaries")
    CaseDtos.CaseDetail toDetail(Case c);

    DocumentDtos.DocumentSummary toDocSummary(SerialDocument d);

    @Mapping(target = "caseId",     expression = "java(d.getCaseFile().getId())")
    @Mapping(target = "caseNumber", expression = "java(d.getCaseFile().getCaseNumber())")
    DocumentDtos.DocumentDetail toDocDetail(SerialDocument d);

    @Named("toDocSummaries")
    default List<DocumentDtos.DocumentSummary> toDocSummaries(List<SerialDocument> docs) {
        return docs == null ? List.of() : docs.stream().map(this::toDocSummary).toList();
    }
}
