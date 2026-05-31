package com.ragenx.pv.modules.cases;

import com.ragenx.pv.common.error.ApiException;
import com.ragenx.pv.common.error.ErrorCode;
import com.ragenx.pv.common.util.JsonLoader;
import com.ragenx.pv.modules.cases.constants.Constants;
import com.ragenx.pv.modules.cases.models.CaseState;
import com.ragenx.pv.modules.cases.models.FollowUpRequest;
import com.ragenx.pv.modules.cases.services.CaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * On startup, loads {@code case_v1.json} into the same FollowUpRequest model and runs it through
 * the same pipeline (CaseService -> MergeService) as a real follow-up, with no prior version, so it
 * is stored as the baseline v1. This exercises the full parse -> merge -> store path on every boot.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CaseSeeder implements CommandLineRunner {

    private final JsonLoader jsonLoader;
    private final CaseService caseService;

    @Override
    public void run(String... args) {
        FollowUpRequest initial = jsonLoader.load(Constants.SEED_RESOURCE, FollowUpRequest.class);
        String caseId = initial.getCaseId();
        if (caseId == null || caseId.isBlank()) {
            // Fail loud with a named cause rather than a bare NPE from the map key later.
            throw new ApiException(ErrorCode.SYSTEM_UNEXPECTED,
                    "Seed resource " + Constants.SEED_RESOURCE + " is missing case_id.");
        }
        CaseState seeded = caseService.seedInitial(caseId, initial);
        log.info("cases.seed.loaded caseId={} version={}", seeded.getCaseId(), seeded.getVersion());
    }
}
