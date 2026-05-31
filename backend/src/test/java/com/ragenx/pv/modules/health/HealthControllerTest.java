package com.ragenx.pv.modules.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the common layer end-to-end through real endpoints: the success envelope,
 * trace-id generation + propagation, and the error envelope for unknown routes.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void healthReturnsSuccessEnvelopeWithTrace() throws Exception {
        // Fails if the success envelope shape or trace propagation breaks.
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("UP")))
                .andExpect(jsonPath("$.trace_id", not(emptyOrNullString())));
    }

    @Test
    void honorsInboundTraceId() throws Exception {
        // Fails if an upstream-provided trace id is not honored end-to-end.
        String traceId = "ABCDE23456";
        mockMvc.perform(get("/health").header("X-Trace-Id", traceId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", traceId))
                .andExpect(jsonPath("$.trace_id", is(traceId)));
    }

    @Test
    void unknownRouteReturnsErrorEnvelope() throws Exception {
        // Fails if the global error envelope/handler breaks.
        mockMvc.perform(get("/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("resource.not_found")))
                .andExpect(jsonPath("$.trace_id", not(emptyOrNullString())));
    }
}
