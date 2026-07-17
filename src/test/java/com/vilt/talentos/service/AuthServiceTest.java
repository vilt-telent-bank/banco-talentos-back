package com.vilt.talentos.service;

import com.vilt.talentos.config.AppProperties;
import com.vilt.talentos.dto.AuthRequest;
import com.vilt.talentos.dto.PasswordResetRequest;
import com.vilt.talentos.dto.RefreshTokenRequest;
import com.vilt.talentos.entity.DomainStatus;
import com.vilt.talentos.entity.User;
import com.vilt.talentos.exception.BadRequestException;
import com.vilt.talentos.mapper.UserMapper;
import com.vilt.talentos.repository.GroupRepository;
import com.vilt.talentos.repository.ProfileRepository;
import com.vilt.talentos.repository.UserRepository;
import com.vilt.talentos.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepo;
    @Mock
    private ProfileRepository profileRepo;
    @Mock
    private GroupRepository groupRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private EmailService emailService;
    @Mock
    private AppProperties appProperties;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_UserWithProfile_ReturnsHasProfileTrue() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("test@vilt-group.com")
                .name("Test User")
                .password("encoded")
                .emailVerified(true)
                .status(DomainStatus.ACTIVE)
                .build();

        when(userRepo.findByEmailIgnoreCase("test@vilt-group.com")).thenReturn(Optional.of(user));
        when(appProperties.getAllowedEmailDomain()).thenReturn("vilt-group.com");
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);
        when(profileRepo.existsByUserId(userId)).thenReturn(true);
        when(jwtService.generate(eq(userId.toString()), anyMap())).thenReturn("jwt-token");

        var response = authService.login(new AuthRequest("test@vilt-group.com", "password123"));

        assertTrue(response.hasProfile());
        assertEquals("jwt-token", response.token());
        assertTrue(response.refreshToken() != null && !response.refreshToken().isBlank());

        verify(userRepo).save(any(User.class));
        verify(userRepo).save(user);
    }

    @Test
    void login_UserWithoutProfile_ReturnsHasProfileFalse() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("test@vilt-group.com")
                .name("Test User")
                .password("encoded")
                .emailVerified(true)
                .status(DomainStatus.ACTIVE)
                .build();

        when(userRepo.findByEmailIgnoreCase("test@vilt-group.com")).thenReturn(Optional.of(user));
        when(appProperties.getAllowedEmailDomain()).thenReturn("vilt-group.com");
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);
        when(profileRepo.existsByUserId(userId)).thenReturn(false);
        when(jwtService.generate(eq(userId.toString()), anyMap())).thenReturn("jwt-token");

        var response = authService.login(new AuthRequest("test@vilt-group.com", "password123"));
        assertTrue(response.refreshToken() != null && !response.refreshToken().isBlank());

        verify(userRepo).save(any(User.class));
        verify(userRepo).save(user);
        assertFalse(response.hasProfile());
    }

    @Test
    void refreshToken_ValidToken_ReturnsNewTokens() {
        String oldRefreshToken = "old-refresh-token";
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("test@vilt-group.com")
                .name("Test User")
                .refreshToken(oldRefreshToken)
                .refreshTokenExpires(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        RefreshTokenRequest req = new RefreshTokenRequest(oldRefreshToken);

        when(userRepo.findByRefreshToken(oldRefreshToken)).thenReturn(Optional.of(user));
        when(jwtService.generate(eq(userId.toString()), anyMap())).thenReturn("new-jwt-token");
        when(profileRepo.existsByUserId(userId)).thenReturn(true);

        var response = authService.refreshToken(req);

        assertEquals("new-jwt-token", response.token());
        assertTrue(response.refreshToken() != null && !response.refreshToken().isEmpty());
        assertTrue(response.refreshToken() != null && !response.refreshToken().isBlank());

        verify(userRepo).save(any(User.class));
        verify(userRepo).save(user);
    }

    @Test
    void refreshToken_ExpiredToken_ThrowsUnauthorizedAndClearsToken() {
        String expiredToken = "expired-refresh-token";
        User user = User.builder()
                .email("test@vilt-group.com")
                .refreshToken(expiredToken)
                .refreshTokenExpires(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        RefreshTokenRequest req = new RefreshTokenRequest(expiredToken);

        when(userRepo.findByRefreshToken(expiredToken)).thenReturn(Optional.of(user));

        assertThrows(com.vilt.talentos.exception.UnauthorizedException.class, () -> authService.refreshToken(req));

        verify(userRepo).save(user);
        assertTrue(user.getRefreshToken() == null);
        assertTrue(user.getRefreshTokenExpires() == null);
    }

    @Test
    void refreshToken_InvalidToken_ThrowsUnauthorized() {
        String invalidToken = "invalid-token";
        RefreshTokenRequest req = new RefreshTokenRequest(invalidToken);

        when(userRepo.findByRefreshToken(invalidToken)).thenReturn(Optional.empty());

        assertThrows(com.vilt.talentos.exception.UnauthorizedException.class, () -> authService.refreshToken(req));
    }

    @Test
    void logout_ValidToken_ClearsUserTokens() {
        String validToken = "valid-refresh-token";
        User user = User.builder()
                .email("test@vilt-group.com")
                .refreshToken(validToken)
                .refreshTokenExpires(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();

        RefreshTokenRequest req = new RefreshTokenRequest(validToken);

        when(userRepo.findByRefreshToken(validToken)).thenReturn(Optional.of(user));

        authService.logout(req);

        verify(userRepo).save(user);
        assertTrue(user.getRefreshToken() == null);
        assertTrue(user.getRefreshTokenExpires() == null);
    }

    @Test
    void logout_InvalidToken_ThrowsUnauthorized() {
        String invalidToken = "invalid-token";
        RefreshTokenRequest req = new RefreshTokenRequest(invalidToken);

        when(userRepo.findByRefreshToken(invalidToken)).thenReturn(Optional.empty());

        assertThrows(com.vilt.talentos.exception.UnauthorizedException.class, () -> authService.logout(req));
    }

    @Test
    void resetPassword_ValidToken_UpdatesPassword() {
        String token = "valid-token";
        String email = "test@vilt-group.com";
        User user = User.builder()
                .email(email)
                .resetToken(token)
                .resetTokenExpires(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        PasswordResetRequest req = new PasswordResetRequest(email, token, "new-password");

        when(userRepo.findByResetToken(token)).thenReturn(Optional.of(user));
        when(appProperties.getAllowedEmailDomain()).thenReturn("vilt-group.com");
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-password");

        authService.resetPassword(req);

        verify(passwordEncoder).encode("new-password");
        verify(userRepo).save(user);
        assertEquals("encoded-password", user.getPassword());
    }

    @Test
    void resetPassword_ExpiredToken_ThrowsBadRequestException() {
        String token = "expired-token";
        String email = "test@vilt-group.com";
        User user = User.builder()
                .email(email)
                .resetToken(token)
                .resetTokenExpires(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        PasswordResetRequest req = new PasswordResetRequest(email, token, "new-password");

        when(userRepo.findByResetToken(token)).thenReturn(Optional.of(user));
        when(appProperties.getAllowedEmailDomain()).thenReturn("vilt-group.com");

        assertThrows(BadRequestException.class, () -> authService.resetPassword(req));
    }

    @Test
    void resetPassword_InvalidToken_ThrowsBadRequestException() {
        String token = "invalid-token";
        PasswordResetRequest req = new PasswordResetRequest("test@vilt-group.com", token, "new-password");

        when(userRepo.findByResetToken(token)).thenReturn(Optional.empty());
        when(appProperties.getAllowedEmailDomain()).thenReturn("vilt-group.com");

        assertThrows(BadRequestException.class, () -> authService.resetPassword(req));
    }

    @Test
    void validateResetToken_ValidToken_DoesNotThrow() {
        String token = "valid-token";
        String email = "test@vilt-group.com";
        User user = User.builder()
                .email(email)
                .resetToken(token)
                .resetTokenExpires(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        when(userRepo.findByResetToken(token)).thenReturn(Optional.of(user));
        when(appProperties.getAllowedEmailDomain()).thenReturn("vilt-group.com");

        authService.validateResetToken(email, token);
    }

    @Test
    void validateResetToken_ExpiredToken_ThrowsBadRequestException() {
        String token = "expired-token";
        String email = "test@vilt-group.com";
        User user = User.builder()
                .email(email)
                .resetToken(token)
                .resetTokenExpires(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        when(userRepo.findByResetToken(token)).thenReturn(Optional.of(user));
        when(appProperties.getAllowedEmailDomain()).thenReturn("vilt-group.com");

        assertThrows(BadRequestException.class, () -> authService.validateResetToken(email, token));
    }

    @Test
    void validateResetToken_EmailMismatch_ThrowsBadRequestException() {
        String token = "valid-token";
        User user = User.builder()
                .email("owner@vilt-group.com")
                .resetToken(token)
                .resetTokenExpires(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        when(userRepo.findByResetToken(token)).thenReturn(Optional.of(user));
        when(appProperties.getAllowedEmailDomain()).thenReturn("vilt-group.com");

        assertThrows(BadRequestException.class, () -> authService.validateResetToken("other@vilt-group.com", token));
    }
}
