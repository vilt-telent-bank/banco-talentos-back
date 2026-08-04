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
        log.info("Iniciando tentativa de login para o e-mail: {}", email);

        var user = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("Falha no login: E-mail '{}' não cadastrado.", email);
                    return new UnauthorizedException("Usuário não cadastrado.");
                });

        if (!user.isEmailVerified()) {
            log.warn("Acesso negado: E-mail '{}' pendente de verificação.", email);
            throw new ForbiddenException("E-mail não verificado. Verifique seu e-mail para continuar.");
        }

        if (user.getStatus() == DomainStatus.PENDING) {
            log.warn("Acesso negado: Usuário '{}' pendente de aprovação do admin.", email);
            throw new ForbiddenException("Usuário pendente de aprovação por um administrador.");
        }

        if (user.getStatus() == DomainStatus.INACTIVE) {
            log.warn("Acesso negado: Usuário '{}' inativo.", email);
            throw new ForbiddenException("Usuário inativo.");
        }

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            log.warn("Falha no login: Credenciais inválidas para o e-mail: {}", email);
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

        log.info("Login realizado com sucesso para o e-mail: {}", email);
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
                .orElseThrow(() -> {
                    log.warn("Falha ao renovar token: Refresh token inválido ou não encontrado.");
                    return new UnauthorizedException("Refresh token inválido ou não encontrado.");
                });

        if (user.getRefreshTokenExpires() == null || user.getRefreshTokenExpires().isBefore(Instant.now())) {
            user.setRefreshToken(null);
            user.setRefreshTokenExpires(null);
            userRepo.save(user);
            log.warn("Falha ao renovar token: Sessão expirada. Por favor, faça login novamente.");
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

        log.info("Refresh token renovado com sucesso para o usuário ID: {}", user.getId());
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
                .orElseThrow(() -> {
                    log.warn("Tentativa de logout com token inválido ou sessão já encerrada.");
                    return new UnauthorizedException("Token inválido ou sessão já encerrada.");
                });

        user.setRefreshToken(null);
        user.setRefreshTokenExpires(null);

        userRepo.save(user);

        log.info("Sessão encerrada com sucesso para o usuário: {}", user.getEmail());
    }

    public void register(RegisterRequest request){
        String email = normalizeAndValidateEmail(request.email());
        log.info("Iniciando processo de registro para o e-mail: {}", email);

        if (request.role() == UserRole.RESOURCE) {
            log.warn("Falha no registro: tentativa de auto-cadastro como RESOURCE para '{}'.", email);
            throw new BadRequestException("Recursos devem ser cadastrados por um administrador.");
        }

        if (userRepo.findByEmailIgnoreCase(email).isPresent()) {
            log.warn("Falha no registro: O e-mail '{}' já está em uso.", email);
            throw new BadRequestException("E-mail já em uso.");
        }

        var group = groupRepo.findById(request.groupId())
                .orElseThrow(() -> {
                    log.warn("Falha no registro: O grupo ID '{}' não foi encontrado.", request.groupId());
                    return new BadRequestException("Grupo não encontrado.");
                });

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
        log.info("Usuário '{}' registrado no banco de dados com ID: {}. Enviando e-mail de verificação.", email, user.getId());

        sendVerificationEmail(user, verificationCode);
    }

    public void verifyEmail(VerificationRequest req) {
        String email = normalizeAndValidateEmail(req.email());
        log.info("Tentativa de verificação de e-mail para: {}", email);

        User user = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("Falha na verificação de e-mail: Usuário com e-mail '{}' não encontrado.", email);
                    return new ResourceNotFoundException("Usuário não encontrado.");
                });

        if (user.isEmailVerified()) {
            log.warn("Falha na verificação: O e-mail '{}' já se encontra verificado.", email);
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

            log.info("E-mail '{}' verificado com sucesso.", email);

            if (user.getRole() == UserRole.ADMIN && user.getStatus() == DomainStatus.PENDING) {
                notifyAdmins(user);
            }
        } else {
            log.warn("Falha na verificação do e-mail '{}': Código inválido ou expirado.", email);
            throw new BadRequestException("Código de verificação inválido ou expirado.");
        }
    }

    public void resendVerificationCode(String rawEmail) {
        String email = normalizeAndValidateEmail(rawEmail);
        log.info("Solicitação de reenvio de código de verificação para: {}", email);

        User user = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("Falha no reenvio de código: Usuário com e-mail '{}' não encontrado.", email);
                    return new ResourceNotFoundException("Usuário não encontrado.");
                });

        if (user.isEmailVerified()) {
            log.warn("Falha no reenvio de código: O e-mail '{}' já se encontra verificado.", email);
            throw new BadRequestException("E-mail já verificado.");
        }

        String verificationCode = String.format("%06d", new Random().nextInt(1000000));
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        userRepo.save(user);

        sendVerificationEmail(user, verificationCode);

        log.info("Novo código gerado e e-mail enviado para: {}", email);
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
        log.info("Solicitação de recuperação de senha para: {}", email);

        User user = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("Recuperação de senha falhou: E-mail '{}' não encontrado na base.", email);
                    return new ResourceNotFoundException("E-mail não encontrado em nossa base de dados.");
                });

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpires(Instant.now().plus(1, ChronoUnit.HOURS));
        userRepo.save(user);

        log.info("Token de recuperação gerado para '{}'. Disparando e-mail.", email);

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
        log.info("Iniciando processo de redefinição de senha para: {}", req.email());

        User user = findValidResetUser(req.email(), req.token());

        if (passwordEncoder.matches(req.newPassword(), user.getPassword())) {
            log.warn("Falha ao redefinir senha: O usuário '{}' tentou usar uma senha igual à atual.", req.email());
            throw new BadRequestException("A nova senha não pode ser igual à senha atual.");
        }

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        user.setResetToken(null);
        user.setResetTokenExpires(null);
        userRepo.save(user);

        log.info("Senha redefinida com sucesso para o usuário: {}", req.email());
    }

    private User findValidResetUser(String rawEmail, String token) {
        String email = normalizeAndValidateEmail(rawEmail);

        User user = userRepo.findByResetToken(token)
                .orElseThrow(() -> {
                    log.warn("Validação de token falhou: Token '{}' não encontrado para redefinição.", token);
                    return new BadRequestException("Token inválido ou expirado.");
                });

        if (!user.getEmail().equalsIgnoreCase(email)) {
            log.warn("Validação de token falhou: Token não pertence ao e-mail '{}'.", email);
            throw new BadRequestException("Token inválido ou expirado.");
        }

        if (user.getResetTokenExpires() == null || !user.getResetTokenExpires().isAfter(Instant.now())) {
            log.warn("Validação de token falhou: Token '{}' expirado.", token);
            throw new BadRequestException("Token inválido ou expirado.");
        }

        return user;
    }

    private String normalizeAndValidateEmail(String email) {
        if (email == null || email.isBlank()) {
            log.warn("Validação de e-mail falhou: E-mail é obrigatório.");
            throw new BadRequestException("O e-mail é obrigatório.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        String domain = appProperties.getAllowedEmailDomain();

        if (!normalizedEmail.endsWith("@" + domain)) {
            log.warn("Validação de domínio falhou: E-mail '{}' não pertence ao domínio permitido '{}'.", normalizedEmail, domain);
            throw new BadRequestException("E-mail deve ser do domínio '" + domain + "'");
        }

        return normalizedEmail;
    }

    private void notifyAdmins(User newUser) {
        log.info("Notificando administradores sobre novo usuário: {}", newUser.getEmail());
        log.info("Buscando administradores ativos.");

        List<User> activeAdmins = userRepo.findAllByRoleAndStatus(UserRole.ADMIN, DomainStatus.ACTIVE, org.springframework.data.domain.Pageable.unpaged()).getContent();
        if (activeAdmins.isEmpty()){
            log.info("Nenhum administrador ativo encontrado.");
            return;
        }

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
