package com.ragenx.pv.modules.queries.repositories;

import com.ragenx.pv.modules.queries.models.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory query store keyed by case id. Appends go through {@code compute}, which is atomic per
 * key, so concurrent query creations on the same case cannot lose each other. Insertion order is
 * preserved. The single mutable field is a final reference to a thread-safe map.
 */
@Repository
public class QueryRepository {

    private final ConcurrentHashMap<String, List<Query>> store = new ConcurrentHashMap<>();

    public Query save(Query query) {
        store.compute(query.getCaseId(), (key, existing) -> {
            List<Query> updated = (existing == null) ? new ArrayList<>() : new ArrayList<>(existing);
            updated.add(query);
            return updated;
        });
        return query;
    }

    /** Queries for a case in creation order; empty if none. Returns an immutable snapshot. */
    public List<Query> findByCase(String caseId) {
        List<Query> queries = store.get(caseId);
        return queries == null ? List.of() : List.copyOf(queries);
    }
}
