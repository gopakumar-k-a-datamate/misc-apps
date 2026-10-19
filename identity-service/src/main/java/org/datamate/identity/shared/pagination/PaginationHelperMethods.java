package org.datamate.identity.shared.pagination;

import com.datamate.bedrock.framework.common.pagination.Paged;
import com.datamate.bedrock.framework.common.pagination.PageQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PaginationHelperMethods {

    /**
     * Converts a pure domain PageQuery into a Spring Data Pageable object.
     * To be used ONLY inside adapter.out.persistence classes.
     *
     * @param query The pure Java pagination query
     * @return Pageable for Spring Data Repositories
     */
    public static Pageable toPageable(PageQuery query) {
        return PageRequest.of(query.page() - 1, query.size());
    }

    /**
     * Converts a pure domain PageQuery into a Spring Data Pageable object with sorting.
     *
     * @param query The pure Java pagination query
     * @param sort The sorting criteria
     * @return Pageable for Spring Data Repositories
     */
    public static Pageable toPageable(PageQuery query, Sort sort) {
        return PageRequest.of(query.page() - 1, query.size(), sort);
    }

    /**
     * Converts a Spring Data Page object back into a pure domain Paged record.
     * To be used ONLY inside adapter.out.persistence classes to return to the Use Case.
     *
     * @param page The Spring Data page returned from the repository
     * @param <T> The type of the domain object or entity
     * @return Paged record for the application layer
     */
    public static <T> Paged<T> toPaged(Page<T> page) {
        return new Paged<>(
                page.getContent(),
                page.getNumber() + 1, // Convert from 0-indexed Spring to 1-indexed API
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
