package com.ragenx.pv.modules.cases.models;

import com.ragenx.pv.modules.cases.constants.Constants.FieldStatus;
import lombok.Builder;
import lombok.Value;

/**
 * A reviewed field: the current leaf plus its diff annotation for the latest follow-up.
 * {@code status} is null when the field was untouched by the latest follow-up (omitted from JSON
 * under global non_null inclusion). {@code previousValue} is present only when overridden.
 */
@Value
@Builder
public class MergedField {

    String value;
    Double confidence;
    String source;
    FieldStatus status;
    ExtractedField previousValue;
}
