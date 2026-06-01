package com.ragenx.pv.modules.cases.models;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

/**
 * Inbound follow-up payload. Same shape as the initial case, plus an optional top-level
 * {@code missing_fields}. {@code caseId}/{@code version} are accepted (so strict parsing passes)
 * but ignored — the path is authoritative for the id and the version is server-managed. Unknown
 * top-level fields are rejected globally (spring.jackson.deserialization.fail-on-unknown-properties).
 */
@Value
@Builder
@Jacksonized
public class FollowUpRequest {

    String caseId;
    Integer version;
    Integer followUpNumber;        // optional — the source's follow-up sequence (follow_up_number)
    String caseClassification;
    String extractedAt;
    String sourceDocument;

    @NotEmpty(message = "sections must not be empty")
    Map<String, Map<String, ExtractedField>> sections;

    List<String> missingFields;
}
