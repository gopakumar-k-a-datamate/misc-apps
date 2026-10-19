package org.datamate.collaboration.chat.application.dto;

import com.datamate.bedrock.framework.common.pagination.PageQuery;
import com.datamate.bedrock.framework.common.pagination.PaginationHelper;

/**
 * Inbound Query for fetching paginated messages within a thread.
 * Strictly adheres to CQRS naming conventions (Query for read-only data fetch).
 * Leverages Bedrock's {@link PaginationHelper} for framework-consistent validation and bounds clamping.
 */
public record GetMessagesQuery(int page, int size) {

    public GetMessagesQuery {
        page = PaginationHelper.validatePageNumber(page);
        size = PaginationHelper.validateLimit(size);
    }

    public GetMessagesQuery() {
        this(1, 10);
    }

    /**
     * Converts this validated CQRS query into Bedrock's domain {@link PageQuery}.
     */
    public PageQuery toPageQuery() {
        return new PageQuery(page, size);
    }
}