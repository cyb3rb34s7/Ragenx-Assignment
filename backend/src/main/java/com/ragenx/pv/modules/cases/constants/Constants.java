package com.ragenx.pv.modules.cases.constants;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Cases-module constants. The per-field diff status is an enum (no magic strings in critical
 * comparisons). A field absent from a follow-up has a {@code null} status (untouched) — there is
 * deliberately no enum constant for it. See docs/conventions.md §6 and docs/architecture.md §4.
 */
public final class Constants {

    private Constants() {
    }

    /** Classpath resource seeded as the initial case version on startup. */
    public static final String SEED_RESOURCE = "case_v1.json";

    /** Wire-visible reasons attached to a {@code case.invalid_follow_up} error's details. */
    public static final class ValidationReason {

        private ValidationReason() {
        }

        public static final String SECTION_MISSING = "section_missing";
        public static final String FIELD_MISSING = "field_missing";
        public static final String VALUE_MISSING = "value_missing";
        public static final String SOURCE_MISSING = "source_missing";
        public static final String CONFIDENCE_OUT_OF_RANGE = "confidence_out_of_range";
    }

    public enum FieldStatus {
        NEW("new"),                 // field inserted by the follow-up
        OVERRIDDEN("overridden"),   // value changed; carries previous_value
        UNCHANGED("unchanged");     // field re-sent with the same value

        private final String wire;

        FieldStatus(String wire) {
            this.wire = wire;
        }

        @JsonValue
        public String wire() {
            return wire;
        }
    }
}
