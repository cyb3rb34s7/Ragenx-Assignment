package com.ragenx.pv.modules.queries.models;

import lombok.Builder;
import lombok.Value;

/** A persisted reviewer query against a field of a case. */
@Value
@Builder
public class Query {

    String id;
    String caseId;
    String fieldPath;
    String question;
}
