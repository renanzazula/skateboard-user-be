package com.skateboard.user.adapter.in.rest;

import com.skateboard.application.dto.ChangePasswordRequest;
import com.skateboard.application.dto.ChangeUsernameRequest;
import com.skateboard.application.dto.ProblemReportRequest;
import com.skateboard.application.dto.ProblemReportResponse;
import com.skateboard.application.dto.UpdateUserRequest;
import com.skateboard.application.dto.UserResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTest {

    @Mock
    private UserFacadeService userFacadeService;

    private UserController controller;

    private UUID keycloakUserId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new UserController(userFacadeService);
        keycloakUserId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsJwt(String preferredUsername) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(keycloakUserId.toString())
                .claim("sub", keycloakUserId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
        if (preferredUsername != null) {
            builder.claim("preferred_username", preferredUsername);
        }
        Jwt jwt = builder.build();
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, List.of());
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    private void authenticateWithoutJwt() {
        TestingAuthenticationToken token = new TestingAuthenticationToken(keycloakUserId.toString(), "n/a");
        token.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Test
    void getCurrentUserDelegatesWithJwtSubjectAndUsernameHint() {
        authenticateAsJwt("rzazula");
        UserResponse facadeResponse = new UserResponse();
        when(userFacadeService.getCurrentUser(keycloakUserId, "rzazula")).thenReturn(facadeResponse);

        ResponseEntity<UserResponse> response = controller.getCurrentUser();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(facadeResponse);
        verify(userFacadeService).getCurrentUser(keycloakUserId, "rzazula");
    }

    @Test
    void getCurrentUserUsesNullUsernameHintWhenNotAJwtAuthentication() {
        authenticateWithoutJwt();
        UserResponse facadeResponse = new UserResponse();
        when(userFacadeService.getCurrentUser(keycloakUserId, null)).thenReturn(facadeResponse);

        ResponseEntity<UserResponse> response = controller.getCurrentUser();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userFacadeService).getCurrentUser(keycloakUserId, null);
    }

    @Test
    void getCurrentUserUsesNullUsernameHintWhenJwtHasNoPreferredUsernameClaim() {
        authenticateAsJwt(null);
        UserResponse facadeResponse = new UserResponse();
        when(userFacadeService.getCurrentUser(keycloakUserId, null)).thenReturn(facadeResponse);

        ResponseEntity<UserResponse> response = controller.getCurrentUser();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userFacadeService).getCurrentUser(keycloakUserId, null);
    }

    @Test
    void updateCurrentUserDelegatesToFacadeWithCallerIdAndRequest() {
        authenticateWithoutJwt();
        UpdateUserRequest request = new UpdateUserRequest().displayName("New Name");
        UserResponse facadeResponse = new UserResponse();
        when(userFacadeService.updateCurrentUser(keycloakUserId, request)).thenReturn(facadeResponse);

        ResponseEntity<UserResponse> response = controller.updateCurrentUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(facadeResponse);
        verify(userFacadeService).updateCurrentUser(keycloakUserId, request);
    }

    @Test
    void deleteCurrentUserDelegatesAndReturnsNoContent() {
        authenticateWithoutJwt();

        ResponseEntity<Void> response = controller.deleteCurrentUser();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(userFacadeService).deleteCurrentUser(keycloakUserId);
    }

    @Test
    void uploadProfilePictureDelegatesWithCallerIdAndFile() {
        authenticateWithoutJwt();
        MultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {1, 2, 3});
        UserResponse facadeResponse = new UserResponse();
        when(userFacadeService.uploadProfilePicture(keycloakUserId, file)).thenReturn(facadeResponse);

        ResponseEntity<UserResponse> response = controller.uploadProfilePicture(file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(facadeResponse);
        verify(userFacadeService).uploadProfilePicture(keycloakUserId, file);
    }

    @Test
    void changeUsernameDelegatesToFacadeWithCallerIdAndRequest() {
        authenticateWithoutJwt();
        ChangeUsernameRequest request = new ChangeUsernameRequest().username("newname");
        UserResponse facadeResponse = new UserResponse();
        when(userFacadeService.changeUsername(keycloakUserId, request)).thenReturn(facadeResponse);

        ResponseEntity<UserResponse> response = controller.changeUsername(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(facadeResponse);
        verify(userFacadeService).changeUsername(keycloakUserId, request);
    }

    @Test
    void changePasswordDelegatesAndReturnsNoContent() {
        authenticateWithoutJwt();
        ChangePasswordRequest request = new ChangePasswordRequest().newPassword("s3cret-password");

        ResponseEntity<Void> response = controller.changePassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(userFacadeService).changePassword(keycloakUserId, request);
    }

    @Test
    void deactivateCurrentUserDelegatesToFacade() {
        authenticateWithoutJwt();
        UserResponse facadeResponse = new UserResponse();
        when(userFacadeService.deactivateCurrentUser(keycloakUserId)).thenReturn(facadeResponse);

        ResponseEntity<UserResponse> response = controller.deactivateCurrentUser();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(facadeResponse);
        verify(userFacadeService).deactivateCurrentUser(keycloakUserId);
    }

    @Test
    void reportProblemDelegatesAndReturnsCreated() {
        authenticateWithoutJwt();
        ProblemReportRequest request = new ProblemReportRequest()
                .category(com.skateboard.application.dto.ProblemReportCategory.APP_ERROR)
                .message("Crashes on save");
        ProblemReportResponse facadeResponse = new ProblemReportResponse();
        when(userFacadeService.reportProblem(keycloakUserId, request)).thenReturn(facadeResponse);

        ResponseEntity<ProblemReportResponse> response = controller.reportProblem(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(facadeResponse);
        verify(userFacadeService).reportProblem(keycloakUserId, request);
    }
}
