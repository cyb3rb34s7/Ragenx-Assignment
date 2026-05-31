package com.ragenx.pv.modules.cases.services;

import com.ragenx.pv.common.error.ApiException;
import com.ragenx.pv.common.error.ErrorCode;
import com.ragenx.pv.modules.cases.constants.Constants.ValidationReason;
import com.ragenx.pv.modules.cases.models.CaseState;
import com.ragenx.pv.modules.cases.models.ExtractedField;
import com.ragenx.pv.modules.cases.models.FollowUpRequest;
import com.ragenx.pv.modules.cases.repositories.CaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrates case reads and writes: enforces not-found, validates follow-up content, and
 * delegates the actual merge to the pure {@link MergeService}. Stateless — holds only its
 * injected collaborators.
 */
@Service
@RequiredArgsConstructor
public class CaseService {

    private final CaseRepository caseRepository;
    private final MergeService mergeService;

    public CaseState get(String caseId) {
        return caseRepository.find(caseId)
                .orElseThrow(() -> new ApiException(ErrorCode.CASE_NOT_FOUND,
                        "Case '" + caseId + "' was not found."));
    }

    /** Non-throwing lookup for cross-module callers (e.g. queries) that decide their own error. */
    public Optional<CaseState> findCase(String caseId) {
        return caseRepository.find(caseId);
    }

    /** All cases — used by backup. */
    public List<CaseState> getAll() {
        return caseRepository.findAll();
    }

    /**
     * Replaces a case verbatim (import/restore). The body's case_id must match the path and the
     * case must have at least one section. Leaf <em>content</em> (value/confidence/source) is
     * trusted, not re-validated: this endpoint imports a snapshot the service itself produced via
     * GET /cases, so deep leaf validation would be redundant (documented trust boundary — see
     * README "Notes & limitations"). Strict JSON parsing still rejects malformed/unknown fields.
     */
    public CaseState replace(String caseId, CaseState caseState) {
        if (caseState.getCaseId() == null || !caseState.getCaseId().equals(caseId)) {
            throw new ApiException(ErrorCode.VALIDATION_BAD_FORMAT,
                    "Path caseId must match the body case_id.");
        }
        if (caseState.getSections() == null || caseState.getSections().isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_BAD_FORMAT,
                    "A case must have at least one section.");
        }
        return caseRepository.put(caseState);
    }

    /** Seeds the initial version (no prior case). Used at startup by the seeder. */
    public CaseState seedInitial(String caseId, FollowUpRequest initial) {
        return caseRepository.compute(caseId, current -> mergeService.merge(caseId, current, initial));
    }

    /** Applies a follow-up to an EXISTING case (404 otherwise) and returns the merged version. */
    public CaseState applyFollowUp(String caseId, FollowUpRequest followUp) {
        return caseRepository.compute(caseId, current -> {
            // 404 takes precedence over content validation: a follow-up to an unknown case is
            // not-found regardless of its body. Validation is pure (no mutation) so running it
            // inside the atomic section is safe — a throw aborts compute without storing anything.
            if (current == null) {
                throw new ApiException(ErrorCode.CASE_NOT_FOUND,
                        "Case '" + caseId + "' was not found.");
            }
            validateLeaves(followUp);
            return mergeService.merge(caseId, current, followUp);
        });
    }

    /**
     * Structural validation of follow-up leaves. Done explicitly here rather than via bean-validation
     * cascade, which is fragile across nested maps. Each leaf must carry a non-blank value and source
     * and a confidence in [0, 1].
     */
    private void validateLeaves(FollowUpRequest followUp) {
        followUp.getSections().forEach((sectionName, fields) -> {
            if (fields == null) {
                throw invalid(sectionName, ValidationReason.SECTION_MISSING);
            }
            fields.forEach((fieldName, leaf) -> {
                String path = sectionName + "." + fieldName;
                if (leaf == null) {
                    throw invalid(path, ValidationReason.FIELD_MISSING);
                }
                if (isBlank(leaf.getValue())) {
                    throw invalid(path, ValidationReason.VALUE_MISSING);
                }
                if (isBlank(leaf.getSource())) {
                    throw invalid(path, ValidationReason.SOURCE_MISSING);
                }
                Double confidence = leaf.getConfidence();
                if (confidence == null || confidence < 0.0 || confidence > 1.0) {
                    throw invalid(path, ValidationReason.CONFIDENCE_OUT_OF_RANGE);
                }
            });
        });
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ApiException invalid(String path, String reason) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("field", path);
        details.put("reason", reason);
        return new ApiException(ErrorCode.CASE_INVALID_FOLLOW_UP,
                "Follow-up field '" + path + "' is invalid.", details);
    }
}
