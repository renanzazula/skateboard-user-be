package com.skateboard.user;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class UserApplicationTest {

    @Test
    void mainDelegatesToSpringApplicationWithApplicationClassAndArgs() {
        String[] args = {"--spring.profiles.active=test"};
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            springApplication.when(() -> SpringApplication.run(eq(UserApplication.class), eq(args)))
                    .thenReturn(context);

            UserApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(UserApplication.class, args));
        }
    }
}
