package com.ragenx.pv.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdGeneratorTest {

    @Test
    void generatesTenCharIdsWithinTheCharset() {
        // Fails if the id length or charset regresses (e.g. an ambiguous char sneaks in).
        for (int i = 0; i < 1000; i++) {
            String id = TraceIdGenerator.generate();
            assertThat(id).hasSize(10);
            assertThat(TraceIdGenerator.isValidTraceId(id)).isTrue();
        }
    }

    @Test
    void rejectsInvalidTraceIds() {
        assertThat(TraceIdGenerator.isValidTraceId(null)).isFalse();
        assertThat(TraceIdGenerator.isValidTraceId("tooShort")).isFalse();    // length 8
        assertThat(TraceIdGenerator.isValidTraceId("ABCDEFGHIJ")).isFalse();  // 'I' is excluded from the charset
        assertThat(TraceIdGenerator.isValidTraceId("ABCDE-2345")).isFalse();  // '-' is not in the charset
    }
}
