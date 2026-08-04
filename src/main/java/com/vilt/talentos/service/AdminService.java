package com.vilt.talentos.service;

import com.vilt.talentos.config.AppProperties;
import com.vilt.talentos.dto.DashboardKpisResponse;
import com.vilt.talentos.entity.DomainStatus;
import com.vilt.talentos.entity.ExperienceLevel;
import com.vilt.talentos.entity.SkillType;
import com.vilt.talentos.entity.User;
import com.vilt.talentos.entity.UserRole;
import com.vilt.talentos.exception.BadRequestException;
import com.vilt.talentos.exception.ResourceNotFoundException;
import com.vilt.talentos.repository.ProfileRepository;
import com.vilt.talentos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final ProfileRepository profileRepo;
    private final UserRepository userRepo;
    private final EmailService emailService;
    private final AppProperties appProperties;

    public DashboardKpisResponse getDashboardKpis() {
        log.info("Calculando KPIs do dashboard geral.");

        var all = profileRepo.findAll();
        var total = all.size();
        var active = all.stream().filter(p -> DomainStatus.ACTIVE == p.getStatus()).count();
        var pending = all.stream().filter(p -> DomainStatus.PENDING == p.getStatus()).count();

        // Skills mais dominadas pelos recursos (Soma dos níveis de proficiência (domínio/conhecimento))
        var skillsByProficiency = all.stream()
            .flatMap(p -> p.getSkills().stream())
            .filter(ps -> ps.getSkill().getType() == SkillType.HARD)
            .collect(Collectors.groupingBy(
                ps -> ps.getSkill().getName(),
                Collectors.summingLong(ps -> ps.getProficiencyLevel() != null ? ps.getProficiencyLevel() : 0)
            ));

        var levelCount = all.stream()
            .map(p -> ExperienceLevel.fromValue(p.getLevelOverride() != null ? p.getLevelOverride() : p.getLevel()))
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return new DashboardKpisResponse(
            total,
            active,
            pending,
            mapToSkillKpiList(skillsByProficiency),
            levelCount
        );
    }

    private List<DashboardKpisResponse.SkillKpi> mapToSkillKpiList(Map<String, Long> skillMap) {
        return skillMap.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(8)
            .map(e -> new DashboardKpisResponse.SkillKpi(
                e.getKey(),
                e.getValue()))
            .toList();
    }

    public List<User> getPendingUsers() {
        log.info("Buscando lista de administradores pendentes de aprovação.");

        Page<User> users = userRepo.findAllByRoleAndStatus(UserRole.ADMIN, DomainStatus.PENDING, Pageable.unpaged());
        if (users.isEmpty()) {
            log.info("Nenhum usuário pendente de aprovação encontrado.");
            return List.of();
        }

        return users.getContent();
    }

    @Transactional
    public void approveUser(UUID userId, UUID adminId) {
        log.info("Iniciando processo de aprovação para o usuário ID: {} pelo admin ID: {}", userId, adminId);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("Falha ao aprovar: Usuário ID: {} não encontrado no banco de dados.", userId);
                    return new ResourceNotFoundException("Usuário não encontrado.");
                });

        if (user.getStatus() != DomainStatus.PENDING) {
            log.warn("Operação negada: Usuário ID: {} tem o status atual '{}', esperado era PENDING.", userId, user.getStatus());
            throw new BadRequestException("Usuário não está pendente de aprovação.");
        }

        User admin = userRepo.getReferenceById(adminId);

        user.setStatus(DomainStatus.ACTIVE);
        user.setApprovedBy(admin);
        user.setApprovedAt(Instant.now());
        
        userRepo.save(user);
        log.info("Status do usuário ID: {} alterado para ACTIVE. Disparando e-mail de notificação.", userId);

        emailService.sendAdminApprovalConfirmedEmail(user.getEmail(), user.getName(), appProperties.getUrl());
        log.info("Processo de aprovação do usuário ID: {} concluído com sucesso.", userId);
    }

    @Transactional
    public void rejectUser(UUID userId) {
        log.info("Iniciando processo de rejeição para o usuário ID: {}", userId);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("Falha ao rejeitar: Usuário ID: {} não encontrado.", userId);
                    return new ResourceNotFoundException("Usuário não encontrado.");
                });

        user.setStatus(DomainStatus.INACTIVE);
        userRepo.save(user);
        log.info("Usuário ID: {} rejeitado com sucesso. Novo status: INACTIVE.", userId);
    }
}
