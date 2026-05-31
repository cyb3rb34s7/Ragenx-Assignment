package com.ragenx.pv.modules.queries.controllers;

import com.ragenx.pv.common.response.ApiResponse;
import com.ragenx.pv.common.response.ResponseFactory;
import com.ragenx.pv.modules.queries.models.CreateQueryRequest;
import com.ragenx.pv.modules.queries.models.Query;
import com.ragenx.pv.modules.queries.services.QueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/queries")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    @PostMapping
    public ApiResponse<Query> createQuery(@Valid @RequestBody CreateQueryRequest request) {
        return ResponseFactory.ok(queryService.create(request));
    }

    @GetMapping
    public ApiResponse<List<Query>> listQueries(@RequestParam("caseId") String caseId) {
        return ResponseFactory.ok(queryService.list(caseId));
    }
}
