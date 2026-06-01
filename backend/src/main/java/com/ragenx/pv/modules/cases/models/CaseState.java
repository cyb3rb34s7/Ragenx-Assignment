package com.ragenx.pv.modules.cases.models;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

/**
 * The current merged state of a case. Returned by GET /cases/{id}, GET /cases, and
 * POST /cases/{id}/follow-ups, and accepted verbatim by PUT /cases/{id} (import/restore).
 * Sections are ordered maps (section name -> field name -> field) so arbitrary new
 * sections/fields are accepted; see docs/architecture.md §5.2.
 */
@Value
@Builder
@Jacksonized
public class CaseState {

    String caseId;
    int version;
    Integer followUpNumber;        // the source's follow-up sequence (null on the initial case)
    String caseClassification;
    String extractedAt;
    String sourceDocument;
    Map<String, Map<String, MergedField>> sections;
    List<String> missingFields;
}
