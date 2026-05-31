package com.ragenx.pv.modules.queries.services;

import com.ragenx.pv.common.error.ApiException;
import com.ragenx.pv.common.error.ErrorCode;
import com.ragenx.pv.modules.cases.services.CaseService;
import com.ragenx.pv.modules.queries.models.CreateQueryRequest;
import com.ragenx.pv.modules.queries.models.Query;
import com.ragenx.pv.modules.queries.repositories.QueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Raises and lists reviewer queries. Reaches the cases slice only through {@link CaseService}
 * (never its repository) to enforce that a query targets an existing case. Stateless.
 */
@Service
@RequiredArgsConstructor
public class QueryService {

    private final QueryRepository queryRepository;
    private final CaseService caseService;

    public Query create(CreateQueryRequest request) {
        requireCaseExists(request.getCaseId());
        Query query = Query.builder()
                .id(UUID.randomUUID().toString())
                .caseId(request.getCaseId())
                .fieldPath(request.getFieldPath())
                .question(request.getQuestion())
                .build();
        return queryRepository.save(query);
    }

    public List<Query> list(String caseId) {
        requireCaseExists(caseId);
        return queryRepository.findByCase(caseId);
    }

    private void requireCaseExists(String caseId) {
        if (caseService.findCase(caseId).isEmpty()) {
            throw new ApiException(ErrorCode.QUERY_CASE_NOT_FOUND,
                    "Case '" + caseId + "' was not found.");
        }
    }
}
