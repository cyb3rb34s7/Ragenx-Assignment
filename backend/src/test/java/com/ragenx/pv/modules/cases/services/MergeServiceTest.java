package com.ragenx.pv.modules.cases.services;

import com.ragenx.pv.modules.cases.constants.Constants.FieldStatus;
import com.ragenx.pv.modules.cases.models.CaseState;
import com.ragenx.pv.modules.cases.models.ExtractedField;
import com.ragenx.pv.modules.cases.models.FollowUpRequest;
import com.ragenx.pv.modules.cases.models.MergedField;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for the merge logic — the "meaningful part". No Spring, no mocks. Each test
 * fails only when a real merge invariant breaks.
 */
class MergeServiceTest {

    private static final String CASE_ID = "PV-2026-0451";
    private final MergeService mergeService = new MergeService();

    private ExtractedField leaf(String value, double confidence, String source) {
        return ExtractedField.builder().value(value).confidence(confidence).source(source).build();
    }

    private FollowUpRequest followUp(Map<String, Map<String, ExtractedField>> sections, List<String> missing) {
        return FollowUpRequest.builder().sections(sections).missingFields(missing).build();
    }

    private CaseState seedV1() {
        FollowUpRequest initial = followUp(Map.of("patient", Map.of(
                "age", leaf("62", 0.91, "p.2 §1"),
                "sex", leaf("Male", 0.99, "p.2 §1"))), null);
        return mergeService.merge(CASE_ID, null, initial);
    }

    private MergedField field(CaseState state, String section, String name) {
        return state.getSections().get(section).get(name);
    }

    @Test
    void baselineHasVersion1NullStatusesAndEmptyMissingFields() {
        CaseState v1 = seedV1();
        assertThat(v1.getVersion()).isEqualTo(1);
        assertThat(v1.getCaseId()).isEqualTo(CASE_ID);
        assertThat(field(v1, "patient", "age").getStatus()).isNull();
        assertThat(field(v1, "patient", "age").getValue()).isEqualTo("62");
        assertThat(v1.getMissingFields()).isEmpty();
    }

    @Test
    void newFieldIsInserted() {
        CaseState v1 = seedV1();
        FollowUpRequest fu = followUp(Map.of("patient", Map.of(
                "weight_kg", leaf("78", 0.85, "p.3 §2"))), null);

        MergedField weight = field(mergeService.merge(CASE_ID, v1, fu), "patient", "weight_kg");

        assertThat(weight.getStatus()).isEqualTo(FieldStatus.NEW);
        assertThat(weight.getValue()).isEqualTo("78");
        assertThat(weight.getPreviousValue()).isNull();
    }

    @Test
    void changedValueIsOverriddenAndCapturesPreviousValue() {
        CaseState v1 = seedV1();
        FollowUpRequest fu = followUp(Map.of("patient", Map.of(
                "age", leaf("63", 0.95, "p.2 §1"))), null);

        CaseState v2 = mergeService.merge(CASE_ID, v1, fu);
        MergedField age = field(v2, "patient", "age");

        assertThat(v2.getVersion()).isEqualTo(2);
        assertThat(age.getStatus()).isEqualTo(FieldStatus.OVERRIDDEN);
        assertThat(age.getValue()).isEqualTo("63");
        assertThat(age.getPreviousValue().getValue()).isEqualTo("62");
    }

    @Test
    void sameValueIsUnchangedAndRefreshesConfidence() {
        CaseState v1 = seedV1();
        FollowUpRequest fu = followUp(Map.of("patient", Map.of(
                "age", leaf("62", 0.70, "p.2 §1"))), null);

        MergedField age = field(mergeService.merge(CASE_ID, v1, fu), "patient", "age");

        assertThat(age.getStatus()).isEqualTo(FieldStatus.UNCHANGED);
        assertThat(age.getPreviousValue()).isNull();
        assertThat(age.getConfidence()).isEqualTo(0.70); // refreshed to the latest extraction
    }

    @Test
    void fieldAbsentFromFollowUpIsUntouchedAndPreserved() {
        CaseState v1 = seedV1();
        FollowUpRequest fu = followUp(Map.of("patient", Map.of(
                "age", leaf("63", 0.95, "p.2 §1"))), null);

        MergedField sex = field(mergeService.merge(CASE_ID, v1, fu), "patient", "sex");

        assertThat(sex.getStatus()).isNull();          // untouched
        assertThat(sex.getValue()).isEqualTo("Male");  // preserved, not dropped
    }

    @Test
    void brandNewSectionIsInserted() {
        CaseState v1 = seedV1(); // only has a "patient" section
        FollowUpRequest fu = followUp(Map.of("adverse_event", Map.of(
                "outcome", leaf("Recovered", 0.81, "p.5 §1"))), null);

        CaseState v2 = mergeService.merge(CASE_ID, v1, fu);

        // existing section preserved, new section added with its field marked NEW
        assertThat(field(v2, "patient", "age").getValue()).isEqualTo("62");
        assertThat(field(v2, "adverse_event", "outcome").getStatus()).isEqualTo(FieldStatus.NEW);
        assertThat(field(v2, "adverse_event", "outcome").getValue()).isEqualTo("Recovered");
    }

    @Test
    void missingFieldsAreSurfacedFromTheFollowUp() {
        CaseState v1 = seedV1();
        FollowUpRequest fu = followUp(Map.of("patient", Map.of(
                "age", leaf("62", 0.91, "p.2 §1"))), List.of("adverse_event.causality_assessment"));

        CaseState v2 = mergeService.merge(CASE_ID, v1, fu);

        assertThat(v2.getMissingFields()).containsExactly("adverse_event.causality_assessment");
    }
}
