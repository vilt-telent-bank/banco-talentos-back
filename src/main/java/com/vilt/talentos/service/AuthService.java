package com.vilt.talentos.service;

import com.vilt.talentos.dto.*;
import com.vilt.talentos.entity.DomainStatus;
import com.vilt.talentos.entity.User;
import com.vilt.talentos.entity.UserRole;
import com.vilt.talentos.exception.BadRequestException;
import com.vilt.talentos.exception.ForbiddenException;
import com.vilt.talentos.exception.ResourceNotFoundException;
import com.vilt.talentos.exception.UnauthorizedException;
import com.vilt.talentos.mapper.UserMapper;
import com.vilt.talentos.repository.GroupRepository;
import com.vilt.talentos.repository.ProfileRepository;
import com.vilt.talentos.repository.UserRepository;
import com.vilt.talentos.security.JwtService;
import com.vilt.talentos.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepo;
    private final ProfileRepository profileRepo;
    private final GroupRepository groupRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final AppProperties appProperties;
    private final UserMapper userMapper;

    public AuthResponse login(AuthRequest req) {
        String email = normalizeAndValidateEmail(req.email());
        log.info("Tentando login para: {}", email);

        var user = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Usuário não cadastrado."));

        if (!user.isEmailVerified()) {
            throw new ForbiddenException("E-mail não verificado. Verifique seu e-mail para continuar.");
        }

        if (user.getStatus() == DomainStatus.PENDING) {
            throw new ForbiddenException("Usuário pendente de aprovação por um administrador.");
        }

        if (user.getStatus() == DomainStatus.INACTIVE) {
            throw new ForbiddenException("Usuário inativo.");
        }

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new UnauthorizedException("Credenciais inválidas.");
        }

        String token = jwtService.generate(user.getId().toString(), Map.of(
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole().name()
        ));

        String refreshToken = UUID.randomUUID().toString();

        Instant refreshTokenExpiration = Instant.now().plus(7, ChronoUnit.DAYS);

        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpires(refreshTokenExpiration);
        userRepo.save(user);

        return new AuthResponse(
                token,
                refreshToken,
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                profileRepo.existsByUserId(user.getId())
        );
    }

    public AuthResponse refreshToken(RefreshTokenRequest req) {
        User user = userRepo.findByRefreshToken(req.refreshToken())
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido ou não encontrado."));

        if (user.getRefreshTokenExpires() == null || user.getRefreshTokenExpires().isBefore(Instant.now())) {
            user.setRefreshToken(null);
            user.setRefreshTokenExpires(null);
            userRepo.save(user);
            throw new UnauthorizedException("Sessão expirada. Por favor, faça login novamente.");
        }

        String newToken = jwtService.generate(user.getId().toString(), Map.of(
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole().name()
        ));

        String newRefreshToken = UUID.randomUUID().toString();
        Instant newRefreshTokenExpiration = Instant.now().plus(7, ChronoUnit.DAYS);

        user.setRefreshToken(newRefreshToken);
        user.setRefreshTokenExpires(newRefreshTokenExpiration);
        userRepo.save(user);

        return new AuthResponse(
                newToken,
                newRefreshToken,
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                profileRepo.existsByUserId(user.getId())
        );
    }

    public void logout(RefreshTokenRequest req) {
        User user = userRepo.findByRefreshToken(req.refreshToken())
                .orElseThrow(() -> new UnauthorizedException("Token inválido ou sessão já encerrada."));

        user.setRefreshToken(null);
        user.setRefreshTokenExpires(null);

        userRepo.save(user);
        log.info("Sessão encerrada com sucesso para o usuário: {}", user.getEmail());
    }

    public void register(RegisterRequest request){
        String email = normalizeAndValidateEmail(request.email());

        if (userRepo.findByEmailIgnoreCase(email).isPresent()) {
            throw new BadRequestException("E-mail já em uso.");
        }

        var group = groupRepo.findById(request.groupId())
                .orElseThrow(() -> new BadRequestException("Grupo não encontrado."));

        String verificationCode = String.format("%06d", new Random().nextInt(1000000));

        User user = userMapper.toEntity(request);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setStatus(request.role() == UserRole.ADMIN ? DomainStatus.PENDING : DomainStatus.ACTIVE);
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        user.setEmailVerified(false);
        user.setGroup(group);

        userRepo.save(user);
        
        sendVerificationEmail(user, verificationCode);
    }

    public void verifyEmail(VerificationRequest req) {
        String email = normalizeAndValidateEmail(req.email());
        User user = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        if (user.isEmailVerified()) {
            throw new BadRequestException("E-mail já verificado.");
        }

        if (user.getVerificationCode() != null && 
            user.getVerificationCode().equals(req.code()) &&
            user.getVerificationCodeExpiresAt() != null &&
            user.getVerificationCodeExpiresAt().isAfter(Instant.now())) {
            
            user.setEmailVerified(true);
            user.setVerificationCode(null);
            user.setVerificationCodeExpiresAt(null);
            userRepo.save(user);

            if (user.getRole() == UserRole.ADMIN && user.getStatus() == DomainStatus.PENDING) {
                notifyAdmins(user);
            }
        } else {
            throw new BadRequestException("Código de verificação inválido ou expirado.");
        }
    }

    public void resendVerificationCode(String rawEmail) {
        String email = normalizeAndValidateEmail(rawEmail);
        User user = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        if (user.isEmailVerified()) {
            throw new BadRequestException("E-mail já verificado.");
        }

        String verificationCode = String.format("%06d", new Random().nextInt(1000000));
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        userRepo.save(user);

        sendVerificationEmail(user, verificationCode);
    }

    private void sendVerificationEmail(User user, String verificationCode) {
        emailService.sendTemplatedEmail(
            List.of(user.getEmail()), 
            "Banco de Talentos - Verificação de E-mail", 
            "emails/email-verification", 
            Map.of("userName", user.getName(), "code", verificationCode)
        );
    }

    public void forgotPassword(String rawEmail) {
        String email = normalizeAndValidateEmail(rawEmail);
        User user = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("E-mail não encontrado em nossa base de dados."));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpires(Instant.now().plus(1, ChronoUnit.HOURS));
        userRepo.save(user);

        String resetUrl = appProperties.getUrl() + "/reset-password?token=" + token + "&email=" + email;
        emailService.sendTemplatedEmail(
            List.of(user.getEmail()), 
            "Banco de Talentos - Redefinição de Senha", 
            "emails/password-reset", 
            Map.of("userName", user.getName(), "resetUrl", resetUrl)
        );
    }

    public void validateResetToken(String rawEmail, String token) {
        findValidResetUser(rawEmail, token);
    }

    public void resetPassword(PasswordResetRequest req) {
        User user = findValidResetUser(req.email(), req.token());

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        user.setResetToken(null);
        user.setResetTokenExpires(null);
        userRepo.save(user);
    }

    private User findValidResetUser(String rawEmail, String token) {
        String email = normalizeAndValidateEmail(rawEmail);

        User user = userRepo.findByResetToken(token)
                .orElseThrow(() -> new BadRequestException("Token inválido ou expirado."));

        if (!user.getEmail().equalsIgnoreCase(email)) {
            throw new BadRequestException("Token inválido ou expirado.");
        }

        if (user.getResetTokenExpires() == null || !user.getResetTokenExpires().isAfter(Instant.now())) {
            throw new BadRequestException("Token inválido ou expirado.");
        }

        return user;
    }

    private String normalizeAndValidateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("O e-mail é obrigatório.");
        }
        String normalizedEmail = email.trim().toLowerCase();
        String domain = appProperties.getAllowedEmailDomain();
        if (!normalizedEmail.endsWith("@" + domain)) {
            throw new BadRequestException("E-mail deve ser do domínio '" + domain + "'");
        }
        return normalizedEmail;
    }

    private void notifyAdmins(User newUser) {
        List<User> activeAdmins = userRepo.findAllByRoleAndStatus(UserRole.ADMIN, DomainStatus.ACTIVE, org.springframework.data.domain.Pageable.unpaged()).getContent();
        if (activeAdmins.isEmpty()) return;

        List<String> adminEmails = activeAdmins.stream().map(User::getEmail).toList();
        String portalUrl = appProperties.getUrl() + "/admin/usuarios";
        
        emailService.sendTemplatedEmail(
            adminEmails, 
            "Banco de Talentos - Novo Administrador Pendente de Aprovação", 
            "emails/admin-approval-notification", 
            Map.of("userName", newUser.getName(), "userEmail", newUser.getEmail(), "portalUrl", portalUrl)
        );
    }
}
