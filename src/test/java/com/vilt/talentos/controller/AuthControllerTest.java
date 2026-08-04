package com.vilt.talentos.controller;

import com.vilt.talentos.dto.*;
import com.vilt.talentos.entity.UserRole;
import com.vilt.talentos.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest extends BaseControllerTest {

    @MockBean
    private AuthService authService;

    @Test
    void login_ValidRequest_ReturnsAuthResponse() throws Exception {
        AuthRequest req = new AuthRequest("test@vilt-group.com", "password123");
        AuthResponse res = new AuthResponse("jwt-token", "refresh-token","Test User", "test@vilt-group.com", "RESOURCE", true);

        when(authService.login(any(AuthRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.hasProfile").value(true));
    }

    @Test
    void refreshToken_ValidRequest_ReturnsAuthResponse() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest("valid-refresh-token");
        AuthResponse res = new AuthResponse("new-jwt-token", "new-refresh-token", "Test User", "test@vilt-group.com", "RESOURCE", true);

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    void logout_ValidRequest_ReturnsNoContent() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest("valid-refresh-token");

        doNothing().when(authService).logout(any(RefreshTokenRequest.class));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(req)))
                .andExpect(status().isNoContent());
    }

    @Test
    void register_ValidRequest_ReturnsSuccessMessage() throws Exception {
        RegisterRequest req = new RegisterRequest("Test User", "test@vilt-group.com", "Password123!", UserRole.ADMIN, UUID.randomUUID());

        doNothing().when(authService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registro realizado! Por favor, verifique seu e-mail para confirmar a conta."));
    }

    @Test
    void register_WeakPassword_ReturnsBadRequest() throws Exception {
        RegisterRequest req = new RegisterRequest("Test User", "test@vilt-group.com", "weak", UserRole.ADMIN, UUID.randomUUID());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_MissingSpecialChar_ReturnsBadRequest() throws Exception {
        RegisterRequest req = new RegisterRequest("Test User", "test@vilt-group.com", "Password123", UserRole.ADMIN, UUID.randomUUID());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verify_ValidRequest_ReturnsSuccessMessage() throws Exception {
        VerificationRequest req = new VerificationRequest("test@vilt-group.com", "123456");

        doNothing().when(authService).verifyEmail(any(VerificationRequest.class));

        mockMvc.perform(post("/api/v1/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("E-mail verificado com sucesso!"));
    }

    @Test
    void resendVerification_ValidRequest_ReturnsSuccessMessage() throws Exception {
        ResendVerificationRequest req = new ResendVerificationRequest("test@vilt-group.com");

        doNothing().when(authService).resendVerificationCode(any(String.class));

        mockMvc.perform(post("/api/v1/auth/resend-verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Novo código de verificação enviado para o seu e-mail."));
    }

    @Test
    void forgotPassword_ValidRequest_ReturnsSuccessMessage() throws Exception {
        ForgotPasswordRequest req = new ForgotPasswordRequest("test@vilt-group.com");

        doNothing().when(authService).forgotPassword(any(String.class));

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Um link de redefinição foi enviado para o seu e-mail."));
    }

    @Test
    void validateResetToken_ValidRequest_ReturnsSuccessMessage() throws Exception {
        doNothing().when(authService).validateResetToken(any(String.class), any(String.class));

        mockMvc.perform(get("/api/v1/auth/validate-reset-token")
                        .param("email", "test@vilt-group.com")
                        .param("token", "token123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token válido."));
    }

    @Test
    void resetPassword_ValidRequest_ReturnsSuccessMessage() throws Exception {
        PasswordResetRequest req = new PasswordResetRequest("test@vilt-group.com", "token123", "NewPassword123!");

        doNothing().when(authService).resetPassword(any(PasswordResetRequest.class));

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Senha alterada com sucesso!"));
    }

    @Test
    void resetPassword_WeakPassword_ReturnsBadRequest() throws Exception {
        PasswordResetRequest req = new PasswordResetRequest("test@vilt-group.com", "token123", "weak");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(req)))
                .andExpect(status().isBadRequest());
    }
}
