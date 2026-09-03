package com.cyrev.nitelestate.common.search;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;

/** Small, reusable {@link Specification} builders for optional list-endpoint filters/search. */
public final class Specs {

    private Specs() {
    }

    /** Case-insensitive substring match across any of the given entity fields; null if {@code q} is blank. */
    public static <T> Specification<T> contains(String q, String... fields) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String pattern = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            Predicate[] predicates = Arrays.stream(fields)
                    .map(field -> cb.like(cb.lower(root.get(field)), pattern))
                    .toArray(Predicate[]::new);
            return cb.or(predicates);
        };
    }

    /** Exact-match filter on a single field; null if {@code value} is null. */
    public static <T> Specification<T> eq(Object value, String field) {
        if (value == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(field), value);
    }
}
