package com.ragenx.pv.modules.queries;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the queries endpoints over the real stack. The seeder provides
 * PV-2026-0451; DirtiesContext re-seeds (and clears queries) before each test for isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class QueryControllerTest {

    private static final String CASE_ID = "PV-2026-0451";

    @Autowired
    MockMvc mockMvc;

    private String body(String caseId, String fieldPath, String question) {
        return """
                { "case_id": "%s", "field_path": "%s", "question": "%s" }
                """.formatted(caseId, fieldPath, question);
    }

    @Test
    void createValidQueryReturnsItWithId() throws Exception {
        mockMvc.perform(post("/queries").contentType(MediaType.APPLICATION_JSON)
                        .content(body(CASE_ID, "adverse_event.onset_date", "Please confirm the onset date.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", not(emptyOrNullString())))
                .andExpect(jsonPath("$.data.case_id", is(CASE_ID)))
                .andExpect(jsonPath("$.data.field_path", is("adverse_event.onset_date")))
                .andExpect(jsonPath("$.data.question", is("Please confirm the onset date.")));
    }

    @Test
    void createQueryForUnknownCaseReturns404() throws Exception {
        mockMvc.perform(post("/queries").contentType(MediaType.APPLICATION_JSON)
                        .content(body("NOPE", "patient.age", "Why?")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("query.case_not_found")));
    }

    @Test
    void createQueryWithBlankQuestionReturns400() throws Exception {
        mockMvc.perform(post("/queries").contentType(MediaType.APPLICATION_JSON)
                        .content(body(CASE_ID, "patient.age", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("validation.missing_field")));
    }

    @Test
    void listReturnsCreatedQueriesInOrder() throws Exception {
        mockMvc.perform(post("/queries").contentType(MediaType.APPLICATION_JSON)
                .content(body(CASE_ID, "patient.weight_kg", "Confirm weight.")));
        mockMvc.perform(post("/queries").contentType(MediaType.APPLICATION_JSON)
                .content(body(CASE_ID, "adverse_event.outcome", "Confirm outcome.")));

        mockMvc.perform(get("/queries").param("caseId", CASE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].field_path", is("patient.weight_kg")))
                .andExpect(jsonPath("$.data[1].field_path", is("adverse_event.outcome")));
    }

    @Test
    void listForExistingCaseWithNoQueriesReturnsEmptyArray() throws Exception {
        mockMvc.perform(get("/queries").param("caseId", CASE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void listForUnknownCaseReturns404() throws Exception {
        mockMvc.perform(get("/queries").param("caseId", "NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("query.case_not_found")));
    }

    @Test
    void listWithMissingCaseIdParamReturns400() throws Exception {
        mockMvc.perform(get("/queries"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("validation.missing_field")));
    }
}
