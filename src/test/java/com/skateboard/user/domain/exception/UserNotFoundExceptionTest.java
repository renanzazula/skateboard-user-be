package com.skateboard.user.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserNotFoundExceptionTest {

    @Test
    void messageIncludesTheGivenIdentifier() {
        UserNotFoundException exception = new UserNotFoundException("11111111-2222-3333-4444-555555555555");

        assertThat(exception.getMessage()).isEqualTo("User not found: 11111111-2222-3333-4444-555555555555");
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
