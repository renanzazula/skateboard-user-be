package com.skateboard.user.infrastructure.web;

import com.skateboard.application.dto.ErrorResponse;
import com.skateboard.user.domain.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handlesAccessDeniedAsForbidden() {
        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(body.getError()).isEqualTo(HttpStatus.FORBIDDEN.getReasonPhrase());
        assertThat(body.getMessage()).isEqualTo("Access denied");
        assertThat(body.getTimestamp()).isNotNull();
    }

    @Test
    void handlesUserNotFoundAsNotFoundWithExceptionMessage() {
        UserNotFoundException ex = new UserNotFoundException("abc-123");

        ResponseEntity<ErrorResponse> response = handler.handleUserNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(body.getMessage()).isEqualTo("User not found: abc-123");
    }

    @Test
    void handlesIllegalArgumentAsBadRequestWithExceptionMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(new IllegalArgumentException("bad value"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo("bad value");
    }

    @Test
    void handlesTypeMismatchAsBadRequestNamingTheParameter() {
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("not-a-uuid", java.util.UUID.class, "userId", null, null);

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo("Invalid value for parameter 'userId'");
    }

    @Test
    void handlesGenericExceptionAsInternalServerErrorWithoutLeakingMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleGeneric(new RuntimeException("something exploded"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(body.getMessage()).isEqualTo("Internal server error");
    }
}
