package com.vilt.talentos.controller;

import com.vilt.talentos.dto.*;
import com.vilt.talentos.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Auth", description = "Autenticação, Registro e Recuperação de Senha")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Retorna o token JWT se o e-mail estiver verificado e o usuário ativo.")
    public AuthResponse login(@Valid @RequestBody AuthRequest req) {
        return authService.login(req);
    }

    @PostMapping("/refresh")
    public AuthResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registro de usuário", description = "Criação de novo usuário. Envia código de verificação por e-mail.")
    public ApiResponse<?> register(@RequestBody @Valid RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success("Registro realizado! Por favor, verifique seu e-mail para confirmar a conta.");
    }

    @PostMapping("/verify")
    @Operation(summary = "Verificar e-mail", description = "Confirma o e-mail do usuário usando o código de 6 dígitos.")
    public ApiResponse<?> verify(@RequestBody @Valid VerificationRequest req) {
        authService.verifyEmail(req);
        return ApiResponse.success("E-mail verificado com sucesso!");
    }

    @PostMapping("/resend-verification-code")
    @Operation(summary = "Reenviar código de verificação", description = "Gera e envia um novo código de verificação para o e-mail informado.")
    public ApiResponse<?> resendVerification(@RequestBody @Valid ResendVerificationRequest req) {
        authService.resendVerificationCode(req.email());
        return ApiResponse.success("Novo código de verificação enviado para o seu e-mail.");
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Esqueci minha senha", description = "Envia um link de redefinição para o e-mail informado.")
    public ApiResponse<?> forgotPassword(@RequestBody @Valid ForgotPasswordRequest req) {
        authService.forgotPassword(req.email());
        return ApiResponse.success("Um link de redefinição foi enviado para o seu e-mail.");
    }

    @GetMapping("/validate-reset-token")
    @Operation(summary = "Validar token de redefinição", description = "Verifica se o link de redefinição de senha ainda é válido (máximo de 1 hora).")
    public ApiResponse<?> validateResetToken(
            @RequestParam @NotBlank(message = "O e-mail é obrigatório.") String email,
            @RequestParam @NotBlank(message = "O token é obrigatório.") String token) {
        authService.validateResetToken(email, token);
        return ApiResponse.success("Token válido.");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Redefinir senha", description = "Altera a senha do usuário usando o token enviado por e-mail.")
    public ApiResponse<?> resetPassword(@RequestBody @Valid PasswordResetRequest req) {
        authService.resetPassword(req);
        return ApiResponse.success("Senha alterada com sucesso!");
    }
}
