package com.livings.common;

import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Pageables {

    private Pageables() {
    }

    /** Rejects sort properties not on the allow-list so clients cannot sort by arbitrary/internal columns. */
    public static Pageable sanitize(Pageable pageable, Set<String> allowedSorts, Sort defaultSort) {
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : defaultSort;
        for (Sort.Order order : sort) {
            if (!allowedSorts.contains(order.getProperty())) {
                throw ApiException.badRequest("Cannot sort by '" + order.getProperty() + "'. Allowed: " + allowedSorts);
            }
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
