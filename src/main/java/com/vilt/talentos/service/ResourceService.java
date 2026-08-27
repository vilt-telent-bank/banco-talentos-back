package com.vilt.talentos.service;

import com.vilt.talentos.config.AppProperties;
import com.vilt.talentos.dto.CreateResourceRequest;
import com.vilt.talentos.dto.CreateResourceResponse;
import com.vilt.talentos.entity.*;
import com.vilt.talentos.exception.BadRequestException;
import com.vilt.talentos.repository.GroupRepository;
import com.vilt.talentos.repository.ProfileRepository;
import com.vilt.talentos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceService {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghjkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL = "!@#$%&*";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepo;
    private final ProfileRepository profileRepo;
    private final GroupRepository groupRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AppProperties appProperties;

    @Transactional
    public CreateResourceResponse createByAdmin(CreateResourceRequest request) {
        String email = normalizeAndValidateEmail(request.email());
        String cpf = normalizeAndValidateCpf(request.cpf());
        log.info("Iniciando cadastro administrativo de recurso para o e-mail: {}", email);

        if (userRepo.findByEmailIgnoreCase(email).isPresent()) {
            log.warn("Falha no cadastro de recurso: e-mail '{}' já está em uso.", email);
            throw new BadRequestException("E-mail já em uso.");
        }

        if (profileRepo.existsByCpf(cpf)) {
            log.warn("Falha no cadastro de recurso: CPF '{}' já está em uso.", cpf);
            throw new BadRequestException("CPF já cadastrado.");
        }

        var group = groupRepo.findById(request.groupId())
                .orElseThrow(() -> {
                    log.warn("Falha no cadastro de recurso: grupo ID '{}' não encontrado.", request.groupId());
                    return new BadRequestException("Grupo não encontrado.");
                });

        String provisionalPassword = generateProvisionalPassword();

        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .password(passwordEncoder.encode(provisionalPassword))
                .role(UserRole.RESOURCE)
                .status(DomainStatus.ACTIVE)
                .emailVerified(true)
                .group(group)
                .build();
        userRepo.save(user);

        Profile profile = Profile.builder()
                .user(user)
                .cpf(cpf)
                .status(DomainStatus.ACTIVE)
                .registrationStatus(RegistrationStatus.NOT_REQUIRED)
                .resourceStatus(ResourceStatus.AVAILABLE)
                .build();
        profileRepo.save(profile);

        log.info("Recurso cadastrado com sucesso. Usuário ID: {}, Perfil ID: {}. Enviando credenciais provisórias.", user.getId(), profile.getId());

        emailService.sendResourceWelcomeEmail(
                user.getEmail(),
                user.getName(),
                user.getEmail(),
                provisionalPassword,
                appProperties.getUrl() + "/login"
        );

        return new CreateResourceResponse(profile.getId(), user.getId(), user.getName(), user.getEmail());
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

    private String normalizeAndValidateCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            throw new BadRequestException("O CPF é obrigatório.");
        }

        String normalized = cpf.replaceAll("\\D", "");
        if (normalized.length() != 11) {
            throw new BadRequestException("CPF inválido.");
        }

        return normalized;
    }

    private String generateProvisionalPassword() {
        List<Character> chars = new ArrayList<>();
        chars.add(randomChar(UPPER));
        chars.add(randomChar(LOWER));
        chars.add(randomChar(DIGITS));
        chars.add(randomChar(SPECIAL));

        String all = UPPER + LOWER + DIGITS + SPECIAL;
        for (int i = 0; i < 8; i++) {
            chars.add(randomChar(all));
        }

        Collections.shuffle(chars, RANDOM);
        StringBuilder password = new StringBuilder(chars.size());
        for (char c : chars) {
            password.append(c);
        }
        return password.toString();
    }

    private char randomChar(String source) {
        return source.charAt(RANDOM.nextInt(source.length()));
    }
}
