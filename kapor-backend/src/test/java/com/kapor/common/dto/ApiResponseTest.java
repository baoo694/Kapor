package com.kapor.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ApiResponse}.
 * Tests the standard API response wrapper factory methods.
 */
@DisplayName("ApiResponse DTO Unit Tests")
class ApiResponseTest {

    @Nested
    @DisplayName("Success Responses")
    class SuccessResponses {

        @Test
        @DisplayName("ok(data) should create success response with data")
        void okWithDataShouldCreateSuccessResponse() {
            ApiResponse<String> response = ApiResponse.ok("test_data");

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo("test_data");
            assertThat(response.getMessage()).isNull();
            assertThat(response.getError()).isNull();
        }

        @Test
        @DisplayName("ok(data, message) should create success response with message")
        void okWithMessageShouldCreateSuccessResponse() {
            ApiResponse<Integer> response = ApiResponse.ok(42, "Answer found");

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(42);
            assertThat(response.getMessage()).isEqualTo("Answer found");
            assertThat(response.getError()).isNull();
        }
    }

    @Nested
    @DisplayName("Error Responses")
    class ErrorResponses {

        @Test
        @DisplayName("error(error) should create error response")
        void errorShouldCreateErrorResponse() {
            ApiResponse<Void> response = ApiResponse.error("NOT_FOUND");

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getError()).isEqualTo("NOT_FOUND");
            assertThat(response.getData()).isNull();
        }

        @Test
        @DisplayName("error(error, message) should create error response with message")
        void errorWithMessageShouldCreateErrorResponse() {
            ApiResponse<Void> response = ApiResponse.error("VALIDATION_ERROR", "Email is required");

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getError()).isEqualTo("VALIDATION_ERROR");
            assertThat(response.getMessage()).isEqualTo("Email is required");
            assertThat(response.getData()).isNull();
        }
    }
}
