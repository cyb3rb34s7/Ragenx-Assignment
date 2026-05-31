package com.ragenx.pv.modules.cases.controllers;

import com.ragenx.pv.common.response.ApiResponse;
import com.ragenx.pv.common.response.ResponseFactory;
import com.ragenx.pv.modules.cases.models.CaseState;
import com.ragenx.pv.modules.cases.models.FollowUpRequest;
import com.ragenx.pv.modules.cases.services.CaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    @GetMapping
    public ApiResponse<List<CaseState>> listCases() {
        return ResponseFactory.ok(caseService.getAll());
    }

    @GetMapping("/{caseId}")
    public ApiResponse<CaseState> getCase(@PathVariable String caseId) {
        return ResponseFactory.ok(caseService.get(caseId));
    }

    /** Import/restore: replace the stored case with the exact CaseState provided (idempotent). */
    @PutMapping("/{caseId}")
    public ApiResponse<CaseState> replaceCase(@PathVariable String caseId,
                                              @RequestBody CaseState caseState) {
        return ResponseFactory.ok(caseService.replace(caseId, caseState));
    }

    @PostMapping("/{caseId}/follow-ups")
    public ApiResponse<CaseState> applyFollowUp(@PathVariable String caseId,
                                                @Valid @RequestBody FollowUpRequest followUp) {
        return ResponseFactory.ok(caseService.applyFollowUp(caseId, followUp));
    }
}
