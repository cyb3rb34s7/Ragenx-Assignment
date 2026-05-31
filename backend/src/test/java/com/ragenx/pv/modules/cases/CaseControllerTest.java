package com.ragenx.pv.modules.cases;

import com.ragenx.pv.modules.cases.constants.Constants.ValidationReason;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests over the real stack (filter + dispatcher + handler + Jackson). The seeder
 * populates PV-2026-0451 v1 at startup; DirtiesContext re-seeds before each test so the
 * mutation tests stay isolated.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CaseControllerTest {

    private static final String CASE_ID = "PV-2026-0451";

    @Autowired
    MockMvc mockMvc;

    @Test
    void getSeededCaseReturnsV1WithoutStatuses() throws Exception {
        mockMvc.perform(get("/cases/{id}", CASE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.version", is(1)))
                .andExpect(jsonPath("$.data.case_id", is(CASE_ID)))
                .andExpect(jsonPath("$.data.sections.patient.age.value", is("62")))
                .andExpect(jsonPath("$.data.sections.patient.age.status").doesNotExist())
                .andExpect(jsonPath("$.data.missing_fields").isArray());
    }

    @Test
    void getUnknownCaseReturns404() throws Exception {
        mockMvc.perform(get("/cases/{id}", "NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("case.not_found")));
    }

    @Test
    void applyFollowUpMergesAllFourOutcomesAndIncrementsVersion() throws Exception {
        // weight_kg (78 -> 80) overridden; outcome re-sent same -> unchanged;
        // recovery_date not in v1 -> new; patient.age not in payload -> untouched.
        String body = """
                {
                  "case_classification": "significant",
                  "sections": {
                    "patient": { "weight_kg": { "value": "80", "confidence": 0.90, "source": "p.3 §2" } },
                    "adverse_event": {
                      "outcome": { "value": "Recovered", "confidence": 0.83, "source": "p.5 §1" },
                      "recovery_date": { "value": "2026-04-01", "confidence": 0.88, "source": "p.5 §2" }
                    }
                  },
                  "missing_fields": ["adverse_event.causality_assessment"]
                }
                """;
        mockMvc.perform(post("/cases/{id}/follow-ups", CASE_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version", is(2)))
                .andExpect(jsonPath("$.data.sections.patient.weight_kg.status", is("overridden")))
                .andExpect(jsonPath("$.data.sections.patient.weight_kg.previous_value.value", is("78")))
                .andExpect(jsonPath("$.data.sections.adverse_event.outcome.status", is("unchanged")))
                .andExpect(jsonPath("$.data.sections.adverse_event.recovery_date.status", is("new")))
                .andExpect(jsonPath("$.data.sections.patient.age.status").doesNotExist())
                .andExpect(jsonPath("$.data.case_classification", is("significant")))
                .andExpect(jsonPath("$.data.missing_fields[0]", is("adverse_event.causality_assessment")));
    }

    @Test
    void overrideCapturesPreviousValue() throws Exception {
        String body = """
                { "sections": { "patient": { "age": { "value": "63", "confidence": 0.95, "source": "p.2 §1" } } } }
                """;
        mockMvc.perform(post("/cases/{id}/follow-ups", CASE_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sections.patient.age.status", is("overridden")))
                .andExpect(jsonPath("$.data.sections.patient.age.previous_value.value", is("62")));
    }

    @Test
    void applyFollowUpToUnknownCaseReturns404() throws Exception {
        String body = """
                { "sections": { "patient": { "age": { "value": "63", "confidence": 0.95, "source": "p.2 §1" } } } }
                """;
        mockMvc.perform(post("/cases/{id}/follow-ups", "NOPE")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("case.not_found")));
    }

    @Test
    void invalidLeafReturns400() throws Exception {
        String body = """
                { "sections": { "patient": { "age": { "value": "63", "confidence": 1.5, "source": "p2 s1" } } } }
                """;
        mockMvc.perform(post("/cases/{id}/follow-ups", CASE_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("case.invalid_follow_up")))
                .andExpect(jsonPath("$.error.details.reason", is(ValidationReason.CONFIDENCE_OUT_OF_RANGE)));
    }

    @Test
    void blankValueLeafReturns400() throws Exception {
        String body = """
                { "sections": { "patient": { "age": { "value": "", "confidence": 0.9, "source": "p2 s1" } } } }
                """;
        mockMvc.perform(post("/cases/{id}/follow-ups", CASE_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("case.invalid_follow_up")))
                .andExpect(jsonPath("$.error.details.reason", is(ValidationReason.VALUE_MISSING)));
    }

    @Test
    void nullSectionReturns400() throws Exception {
        String body = """
                { "sections": { "patient": null } }
                """;
        mockMvc.perform(post("/cases/{id}/follow-ups", CASE_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("case.invalid_follow_up")))
                .andExpect(jsonPath("$.error.details.reason", is(ValidationReason.SECTION_MISSING)));
    }

    @Test
    void invalidLeafOnUnknownCaseReturns404NotValidationError() throws Exception {
        // 404 takes precedence over content validation for a non-existent case.
        String body = """
                { "sections": { "patient": { "age": { "value": "63", "confidence": 1.5, "source": "p2 s1" } } } }
                """;
        mockMvc.perform(post("/cases/{id}/follow-ups", "NOPE")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("case.not_found")));
    }

    @Test
    void unknownTopLevelFieldIsRejected() throws Exception {
        String body = """
                {
                  "sections": { "patient": { "age": { "value": "63", "confidence": 0.95, "source": "p.2 §1" } } },
                  "unexpected_key": "boom"
                }
                """;
        mockMvc.perform(post("/cases/{id}/follow-ups", CASE_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("validation.bad_format")));
    }

    @Test
    void listCasesReturnsTheSeededCase() throws Exception {
        mockMvc.perform(get("/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].case_id", is(CASE_ID)))
                .andExpect(jsonPath("$.data[0].version", is(1)));
    }

    @Test
    void putReplacesCaseVerbatimForExactCheckpointRestore() throws Exception {
        // A backed-up checkpoint: version 7, an overridden field carrying its previous_value.
        String checkpoint = """
                {
                  "case_id": "PV-2026-0451",
                  "version": 7,
                  "case_classification": "significant",
                  "extracted_at": "2026-05-20T11:00:00Z",
                  "source_document": "restored.pdf",
                  "sections": {
                    "patient": {
                      "weight_kg": { "value": "99", "confidence": 0.7, "source": "p9",
                                     "status": "overridden",
                                     "previous_value": { "value": "78", "confidence": 0.85, "source": "p3" } }
                    }
                  },
                  "missing_fields": ["adverse_event.causality_assessment"]
                }
                """;
        mockMvc.perform(put("/cases/{id}", CASE_ID).contentType(MediaType.APPLICATION_JSON).content(checkpoint))
                .andExpect(status().isOk());

        // GET must return the EXACT checkpoint — version, status, and previous_value preserved.
        mockMvc.perform(get("/cases/{id}", CASE_ID))
                .andExpect(jsonPath("$.data.version", is(7)))
                .andExpect(jsonPath("$.data.sections.patient.weight_kg.status", is("overridden")))
                .andExpect(jsonPath("$.data.sections.patient.weight_kg.previous_value.value", is("78")))
                .andExpect(jsonPath("$.data.missing_fields[0]", is("adverse_event.causality_assessment")));
    }

    @Test
    void putIsIdempotent() throws Exception {
        String body = """
                { "case_id": "PV-2026-0451", "version": 5,
                  "sections": { "patient": { "age": { "value": "62", "confidence": 0.91, "source": "p2" } } },
                  "missing_fields": [] }
                """;
        mockMvc.perform(put("/cases/{id}", CASE_ID).contentType(MediaType.APPLICATION_JSON).content(body));
        mockMvc.perform(put("/cases/{id}", CASE_ID).contentType(MediaType.APPLICATION_JSON).content(body));

        // Two identical PUTs leave the same state — version stays 5, not incremented.
        mockMvc.perform(get("/cases/{id}", CASE_ID))
                .andExpect(jsonPath("$.data.version", is(5)));
    }

    @Test
    void putWithUnknownFieldStatusReturns400() throws Exception {
        // The @JsonCreator on FieldStatus fails loud on an unknown wire value -> clean 400, not 500.
        String body = """
                { "case_id": "PV-2026-0451", "version": 1,
                  "sections": { "patient": { "age": { "value": "62", "confidence": 0.9, "source": "p2", "status": "bogus" } } } }
                """;
        mockMvc.perform(put("/cases/{id}", CASE_ID).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("validation.bad_format")));
    }

    @Test
    void putCanInsertANewCase() throws Exception {
        String body = """
                { "case_id": "PV-9999", "version": 3,
                  "sections": { "patient": { "age": { "value": "40", "confidence": 0.9, "source": "p1" } } },
                  "missing_fields": [] }
                """;
        mockMvc.perform(put("/cases/{id}", "PV-9999").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mockMvc.perform(get("/cases/{id}", "PV-9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version", is(3)));
    }

    @Test
    void putWithMismatchedCaseIdReturns400() throws Exception {
        String body = """
                { "case_id": "PV-2026-0451", "version": 1,
                  "sections": { "patient": { "age": { "value": "62", "confidence": 0.91, "source": "p2" } } } }
                """;
        mockMvc.perform(put("/cases/{id}", "OTHER").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("validation.bad_format")));
    }

    @Test
    void emptySectionsReturns400() throws Exception {
        String body = """
                { "sections": {} }
                """;
        mockMvc.perform(post("/cases/{id}/follow-ups", CASE_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("validation.missing_field")));
    }
}
