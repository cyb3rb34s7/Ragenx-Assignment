package com.ragenx.pv.modules.cases.models;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * The current merged state of a case. This same shape is returned by both GET /cases/{id} and
 * POST /cases/{id}/follow-ups. Sections are ordered maps (section name -> field name -> field) so
 * arbitrary new sections/fields are accepted; see docs/architecture.md §5.2.
 */
@Value
@Builder
public class CaseState {

    String caseId;
    int version;
    String caseClassification;
    String extractedAt;
    String sourceDocument;
    Map<String, Map<String, MergedField>> sections;
    List<String> missingFields;
}
