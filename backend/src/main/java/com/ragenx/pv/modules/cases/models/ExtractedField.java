package com.ragenx.pv.modules.cases.models;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Raw extracted leaf: the AI's value plus its confidence and source. Used for inbound follow-up
 * fields and as the {@code previous_value} snapshot. {@code confidence} is boxed so a missing
 * value is detectable (null) during validation rather than silently defaulting to 0.
 */
@Value
@Builder
@Jacksonized
public class ExtractedField {

    String value;
    Double confidence;
    String source;
}
