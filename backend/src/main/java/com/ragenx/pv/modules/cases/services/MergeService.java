package com.ragenx.pv.modules.cases.services;

import com.ragenx.pv.modules.cases.constants.Constants.FieldStatus;
import com.ragenx.pv.modules.cases.models.CaseState;
import com.ragenx.pv.modules.cases.models.ExtractedField;
import com.ragenx.pv.modules.cases.models.FollowUpRequest;
import com.ragenx.pv.modules.cases.models.MergedField;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pure merge logic — no dependencies, no mutable state, fully unit-testable without mocks. Given
 * the current case (or null for the very first version) and an incoming payload, produces the next
 * {@link CaseState}. See docs/architecture.md §4.
 */
@Service
public class MergeService {

    public CaseState merge(String caseId, CaseState current, FollowUpRequest incoming) {
        return current == null
                ? baseline(caseId, incoming)
                : diff(current, incoming);
    }

    /** First version: no diff. Every field is stored as-is with a null (untouched) status. */
    private CaseState baseline(String caseId, FollowUpRequest incoming) {
        Map<String, Map<String, MergedField>> sections = new LinkedHashMap<>();
        incoming.getSections().forEach((sectionName, fields) -> {
            Map<String, MergedField> merged = new LinkedHashMap<>();
            fields.forEach((fieldName, leaf) -> merged.put(fieldName, leafToMerged(leaf, null, null)));
            sections.put(sectionName, merged);
        });
        return CaseState.builder()
                .caseId(caseId)
                .version(1)
                .followUpNumber(incoming.getFollowUpNumber())
                .caseClassification(incoming.getCaseClassification())
                .extractedAt(incoming.getExtractedAt())
                .sourceDocument(incoming.getSourceDocument())
                .sections(sections)
                .missingFields(orEmpty(incoming.getMissingFields()))
                .build();
    }

    /** Subsequent version: carry existing fields forward (status reset), then upsert the follow-up. */
    private CaseState diff(CaseState current, FollowUpRequest incoming) {
        Map<String, Map<String, MergedField>> sections = new LinkedHashMap<>();

        // Carry every existing field forward as untouched. Status is reset each follow-up, so the
        // annotations always describe THIS follow-up, not a prior one.
        current.getSections().forEach((sectionName, fields) -> {
            Map<String, MergedField> carried = new LinkedHashMap<>();
            fields.forEach((fieldName, mf) -> carried.put(fieldName, leafToMerged(toLeaf(mf), null, null)));
            sections.put(sectionName, carried);
        });

        // Apply the follow-up: insert new fields, annotate changed/unchanged ones.
        incoming.getSections().forEach((sectionName, fields) -> {
            Map<String, MergedField> target = sections.computeIfAbsent(sectionName, k -> new LinkedHashMap<>());
            Map<String, MergedField> existingSection = current.getSections().get(sectionName);
            fields.forEach((fieldName, leaf) -> {
                MergedField existing = existingSection == null ? null : existingSection.get(fieldName);
                target.put(fieldName, annotate(existing, leaf));
            });
        });

        return CaseState.builder()
                .caseId(current.getCaseId())
                .version(current.getVersion() + 1)
                .followUpNumber(coalesce(incoming.getFollowUpNumber(), current.getFollowUpNumber()))
                .caseClassification(coalesce(incoming.getCaseClassification(), current.getCaseClassification()))
                .extractedAt(coalesce(incoming.getExtractedAt(), current.getExtractedAt()))
                .sourceDocument(coalesce(incoming.getSourceDocument(), current.getSourceDocument()))
                .sections(sections)
                .missingFields(orEmpty(incoming.getMissingFields()))
                .build();
    }

    private MergedField annotate(MergedField existing, ExtractedField leaf) {
        if (existing == null) {
            return leafToMerged(leaf, FieldStatus.NEW, null);
        }
        if (Objects.equals(existing.getValue(), leaf.getValue())) {
            return leafToMerged(leaf, FieldStatus.UNCHANGED, null);
        }
        return leafToMerged(leaf, FieldStatus.OVERRIDDEN, toLeaf(existing));
    }

    private MergedField leafToMerged(ExtractedField leaf, FieldStatus status, ExtractedField previous) {
        return MergedField.builder()
                .value(leaf.getValue())
                .confidence(leaf.getConfidence())
                .source(leaf.getSource())
                .status(status)
                .previousValue(previous)
                .build();
    }

    private ExtractedField toLeaf(MergedField mf) {
        return ExtractedField.builder()
                .value(mf.getValue())
                .confidence(mf.getConfidence())
                .source(mf.getSource())
                .build();
    }

    // Case-level metadata is updated from the follow-up when present, retained otherwise — a
    // deliberate business rule for optional fields (see context.md), not error-hiding.
    private <T> T coalesce(T preferred, T fallback) {
        return preferred != null ? preferred : fallback;
    }

    private List<String> orEmpty(List<String> list) {
        return list != null ? list : List.of();
    }
}
