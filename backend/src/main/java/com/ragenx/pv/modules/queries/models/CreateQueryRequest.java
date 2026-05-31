package com.ragenx.pv.modules.queries.models;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Inbound payload to raise a reviewer query against a specific field of a case.
 * {@code fieldPath} is the path of a field the UI rendered (e.g. "adverse_event.onset_date");
 * it is required but not validated against the case structure (see docs/architecture.md §6).
 */
@Value
@Builder
@Jacksonized
public class CreateQueryRequest {

    @NotBlank(message = "caseId must not be blank")
    String caseId;

    @NotBlank(message = "fieldPath must not be blank")
    String fieldPath;

    @NotBlank(message = "question must not be blank")
    String question;
}
