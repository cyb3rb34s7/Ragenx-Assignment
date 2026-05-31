package com.ragenx.pv.modules.cases.repositories;

import com.ragenx.pv.modules.cases.models.CaseState;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/**
 * In-memory case store and the ONLY stateful component in the cases slice. Seed and follow-up
 * read-modify-write goes through {@link #compute}, which is atomic per key, so concurrent
 * follow-ups on the same case cannot clobber each other. The single mutable field is a final
 * reference to a thread-safe map.
 */
@Repository
public class CaseRepository {

    private final ConcurrentHashMap<String, CaseState> store = new ConcurrentHashMap<>();

    public Optional<CaseState> find(String caseId) {
        return Optional.ofNullable(store.get(caseId));
    }

    /**
     * Atomically applies {@code remap} to the current state for {@code caseId} (null if absent)
     * and stores the result. If {@code remap} throws, the stored value is left unchanged.
     */
    public CaseState compute(String caseId, UnaryOperator<CaseState> remap) {
        return store.compute(caseId, (key, current) -> remap.apply(current));
    }
}
