package org.datamate.collaboration.chat.application.dto;

import com.datamate.bedrock.framework.common.pagination.PageQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GetMessagesQuery Unit Tests")
class GetMessagesQueryTest {

    @Test
    @DisplayName("should normalize invalid or non-positive page to 1 via Bedrock PaginationHelper")
    void shouldNormalizePageNumber_WhenInvalidOrNonPositive() {
        GetMessagesQuery queryZero = new GetMessagesQuery(0, 20);
        assertThat(queryZero.page()).isEqualTo(1);

        GetMessagesQuery queryNegative = new GetMessagesQuery(-5, 20);
        assertThat(queryNegative.page()).isEqualTo(1);
    }

    @Test
    @DisplayName("should clamp limit to minimum of 5 via Bedrock PaginationHelper")
    void shouldClampLimit_ToMinimumOfFive() {
        GetMessagesQuery queryBelowMin = new GetMessagesQuery(1, 2);
        assertThat(queryBelowMin.size()).isEqualTo(5);

        GetMessagesQuery queryZeroLimit = new GetMessagesQuery(1, 0);
        assertThat(queryZeroLimit.size()).isEqualTo(5);
    }

    @Test
    @DisplayName("should clamp limit to maximum of 100 via Bedrock PaginationHelper")
    void shouldClampLimit_ToMaximumOfHundred() {
        GetMessagesQuery queryAboveMax = new GetMessagesQuery(1, 500);
        assertThat(queryAboveMax.size()).isEqualTo(100);
    }

    @Test
    @DisplayName("should preserve valid page and size within Bedrock boundaries")
    void shouldPreserveValidPageAndSize() {
        GetMessagesQuery query = new GetMessagesQuery(3, 25);
        assertThat(query.page()).isEqualTo(3);
        assertThat(query.size()).isEqualTo(25);
    }

    @Test
    @DisplayName("should default to page 1 and size 10 in no-arg constructor")
    void shouldDefaultToOneAndTen_InNoArgConstructor() {
        GetMessagesQuery query = new GetMessagesQuery();
        assertThat(query.page()).isEqualTo(1);
        assertThat(query.size()).isEqualTo(10);
    }

    @Test
    @DisplayName("toPageQuery should convert to Bedrock PageQuery correctly")
    void toPageQuery_ShouldConvertToBedrockPageQuery() {
        GetMessagesQuery query = new GetMessagesQuery(2, 20);
        PageQuery pageQuery = query.toPageQuery();

        assertThat(pageQuery.page()).isEqualTo(2);
        assertThat(pageQuery.size()).isEqualTo(20);
    }
}