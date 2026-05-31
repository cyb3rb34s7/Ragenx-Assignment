package com.ragenx.pv.modules.queries.repositories;

import com.ragenx.pv.modules.queries.models.Query;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure unit tests for the query store's read/write invariants. */
class QueryRepositoryTest {

    private Query query(String id, String caseId) {
        return Query.builder().id(id).caseId(caseId).fieldPath("patient.age").question("q").build();
    }

    @Test
    void findByCaseReturnsImmutableSnapshotInInsertionOrder() {
        QueryRepository repo = new QueryRepository();
        repo.save(query("1", "C"));
        repo.save(query("2", "C"));

        List<Query> result = repo.findByCase("C");

        assertThat(result).extracting(Query::getId).containsExactly("1", "2"); // insertion order
        // A regression to returning the aliased, mutable stored list would let callers corrupt state.
        assertThatThrownBy(() -> result.add(query("3", "C")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void findByCaseReturnsEmptyForUnknownCase() {
        assertThat(new QueryRepository().findByCase("NONE")).isEmpty();
    }
}
