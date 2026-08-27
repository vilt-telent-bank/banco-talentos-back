package com.vilt.talentos.service;

import com.vilt.talentos.config.AppProperties;
import com.vilt.talentos.dto.AdminUpdateRequest;
import com.vilt.talentos.dto.ProfileRequest;
import com.vilt.talentos.dto.ProfileSelfUpdateRequest;
import com.vilt.talentos.dto.SkillEntry;
import com.vilt.talentos.entity.*;
import com.vilt.talentos.exception.BadRequestException;
import com.vilt.talentos.exception.ResourceNotFoundException;
import com.vilt.talentos.mapper.ProfileMapper;
import com.vilt.talentos.repository.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final ProfileRepository profileRepo;
    private final SkillRepository skillRepo;
    private final UserRepository userRepo;
    private final GroupRepository groupRepo;
    private final ProjectRepository projectRepo;
    private final SquadRepository squadRepo;
    private final TalentEvaluationService evaluationService;
    private final EmailService emailService;
    private final AppProperties appProperties;
    private final ProfileMapper profileMapper;

    @Transactional
    public Profile createOrUpdate(UUID userId, ProfileRequest req) {
        log.info("Criando ou atualizando perfil para o usuário ID: {}", userId);

        var user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Falha na submissão de perfil: Usuário ID: {} não encontrado.", userId);
                    return new ResourceNotFoundException("Usuário não encontrado.");
                });

        var evaluation = evaluationService.evaluate(req);

        var profile = profileRepo.findByUserId(userId).orElseGet(() -> Profile.builder()
                .user(user).status(DomainStatus.PENDING).build());

        // Toda submissão pelo recurso deve passar por revisão (status PENDENTE)
        boolean wasActive = DomainStatus.ACTIVE == profile.getStatus();
        profile.setStatus(DomainStatus.PENDING);

        profileMapper.updateEntity(req, profile);

        // Matrícula é preenchida apenas pelo admin
        profile.setLevel(evaluation.nivel().name());
        profile.setLevelScore(evaluation.score());
        profile.setLevelJustification(evaluation.justificativa());
        // Reconciliação de skills HARD (recursos só mexem em HARD)
        reconcileSkills(profile, req.skills(), null);

        Profile saved = profileRepo.save(profile);
        log.info("Perfil salvo e alterado para PENDING. ID do Perfil: {}", saved.getId());

        // Notificar admins se o perfil foi submetido (se estava ATIVO ou se é novo)
        if (wasActive || profileRepo.findByUserId(userId).isEmpty()) {
            List<String> adminEmails = userRepo.findAllByRoleAndStatus(UserRole.ADMIN, DomainStatus.ACTIVE, Pageable.unpaged())
                    .getContent().stream().map(User::getEmail).toList();
            if (!adminEmails.isEmpty()) {
                log.info("Notificando {} administrador(es) sobre o novo perfil pendente do usuário: {}", adminEmails.size(), user.getName());
                emailService.sendAdminNewProfileSubmissionEmail(
                    adminEmails, user.getName(), saved.getJobTitle(), saved.getLevel(), appProperties.getUrl()
                );
            }
        }

        return saved;
    }

    @Transactional
    public Profile updateMyProfile(UUID userId, ProfileSelfUpdateRequest req) {
        log.info("Atualizando perfil do recurso autenticado. Usuário ID: {}", userId);

        Profile profile = profileRepo.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Atualização falhou: Perfil não encontrado para o usuário ID: {}", userId);
                    return new ResourceNotFoundException("Perfil não encontrado para o usuário");
                });

        if (req.photoUrl() != null) profile.setPhotoUrl(blankToNull(req.photoUrl()));
        if (req.jobTitle() != null) profile.setJobTitle(blankToNull(req.jobTitle()));
        if (req.area() != null) profile.setArea(blankToNull(req.area()));
        if (req.about() != null) profile.setAbout(blankToNull(req.about()));
        if (req.experienceYears() != null) profile.setExperienceYears(req.experienceYears());
        if (req.linkedinUrl() != null) profile.setLinkedinUrl(blankToNull(req.linkedinUrl()));
        if (req.githubUrl() != null) profile.setGithubUrl(blankToNull(req.githubUrl()));
        if (req.contact() != null) profile.setContact(blankToNull(req.contact()));
        if (req.contactEmail() != null) profile.setContactEmail(blankToNull(req.contactEmail()));
        if (req.phone() != null) profile.setPhone(blankToNull(req.phone()));
        if (req.address() != null) profile.setAddress(blankToNull(req.address()));
        if (req.postalCode() != null) profile.setPostalCode(normalizePostalCode(req.postalCode()));
        if (req.cityState() != null) profile.setCityState(blankToNull(req.cityState()));

        if (req.skills() != null) {
            reconcileSkills(profile, req.skills(), null);
        }

        Profile saved = profileRepo.save(profile);
        log.info("Perfil ID: {} atualizado pelo recurso com sucesso.", saved.getId());
        return sanitizeProfileForResource(saved);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizePostalCode(String postalCode) {
        if (postalCode == null || postalCode.isBlank()) return null;
        String digits = postalCode.replaceAll("\\D", "");
        if (digits.length() == 8) {
            return digits.substring(0, 5) + "-" + digits.substring(5);
        }
        return digits.isBlank() ? null : digits;
    }

    public Profile getByUserId(UUID userId) {
        Profile profile = profileRepo.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Busca falhou: Perfil não encontrado para o usuário ID: {}", userId);
                    return new ResourceNotFoundException("Perfil não encontrado para o usuário");
                });
        
        return sanitizeProfileForResource(profile);
    }

    private Profile sanitizeProfileForResource(Profile profile) {
        profile.getSkills().removeIf(ps -> ps.getSkill().getType() == SkillType.SOFT);
        return profile;
    }

    public Profile getById(UUID id) {
        return profileRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Busca falhou: Perfil não encontrado para o ID: {}", id);
                    return new ResourceNotFoundException("Perfil não encontrado para o ID: " + id);
                });
    }

    public Page<Profile> getByStatus(DomainStatus status, Pageable pageable) {
        Page<Profile> page = profileRepo.findByStatus(status, pageable);

        if (page.isEmpty()) {
            log.warn("Busca por perfis com status '{}' retornou vazia.", status);
            throw new ResourceNotFoundException("No profiles found with status: " + status);
        }

        return page;
    }

    public Page<Profile> getAll(Pageable pageable) {
        Page<Profile> page = profileRepo.findAll(pageable);

        if (page.isEmpty()) {
            log.warn("Busca geral por perfis retornou vazia.");
            throw new ResourceNotFoundException("No profiles found");
        }

        return page;
    }

    public Page<Profile> getAllWithFilters(DomainStatus status, String skillName, Pageable pageable) {
        return profileRepo.findAll((root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (skillName != null && !skillName.isBlank()) {
                Join<Profile, ProfileSkill> ps = root.join("skills");
                predicates.add(cb.equal(cb.upper(ps.get("skill").get("name")), skillName.trim().toUpperCase()));

                if (query.getResultType() != Long.class && pageable.getSort().isUnsorted()) {
                    query.orderBy(
                        cb.asc(cb.selectCase()
                            .when(cb.equal(root.get("allocationStatus"), "Disponível (Bench)"), 1)
                            .when(cb.equal(root.get("allocationStatus"), "Alocado Parcial"), 2)
                            .when(cb.equal(root.get("allocationStatus"), "Em Transição (saindo de projeto)"), 3)
                            .when(cb.equal(root.get("allocationStatus"), "Alocado Integral (100%)"), 4)
                            .otherwise(5)),
                        cb.desc(ps.get("proficiencyLevel"))
                    );
                }
            } else if (query.getResultType() != Long.class && pageable.getSort().isUnsorted()) {
                 query.orderBy(
                        cb.asc(cb.selectCase()
                            .when(cb.equal(root.get("allocationStatus"), "Disponível (Bench)"), 1)
                            .when(cb.equal(root.get("allocationStatus"), "Alocado Parcial"), 2)
                            .when(cb.equal(root.get("allocationStatus"), "Em Transição (saindo de projeto)"), 3)
                            .when(cb.equal(root.get("allocationStatus"), "Alocado Integral (100%)"), 4)
                            .otherwise(5))
                    );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    @Transactional
    public Profile adminUpdate(UUID profileId, AdminUpdateRequest req) {
        log.info("Iniciando atualização administrativa para o perfil ID: {}", profileId);

        var profile = profileRepo.findById(profileId)
                .orElseThrow(() -> {
                    log.warn("Falha na atualização do perfil: Perfil ID: {} não encontrado.", profileId);
                    return new ResourceNotFoundException("Perfil não encontrado");
                });
        
        DomainStatus oldStatus = profile.getStatus();
        boolean wasAllocatedToProject = profile.getAllocationProject() != null;
        
        if (req.status() != null) {
            try {
                profile.setStatus(DomainStatus.valueOf(req.status()));
            } catch (IllegalArgumentException e) {
                log.warn("Tentativa inválida de alterar perfil ID: {} para o status desconhecido: '{}'. Status ignorado.", profileId, req.status());
            }
        }

        profileMapper.updateEntityFromAdmin(req, profile);

        if (req.registrationStatus() != null) {
            try {
                profile.setRegistrationStatus(RegistrationStatus.valueOf(req.registrationStatus()));
            } catch (IllegalArgumentException e) {
                log.warn("Tentativa inválida de alterar perfil ID: {} para o status de registro desconhecido: '{}'. Status de registro ignorado.", profileId, req.registrationStatus());
            }
        }

        if (profile.getRegistrationStatus() == RegistrationStatus.NOT_REQUIRED) {
            profile.setRegistrationNumber(null);
            profile.setRegistrationRequestedAt(null);
            profile.setRegistrationNotes(null);
        } else {
            if (req.registrationNumber() != null) {
                String number = req.registrationNumber().trim();
                profile.setRegistrationNumber(number.isEmpty() ? null : number);
            }
            if (req.registrationRequestedAt() != null) {
                profile.setRegistrationRequestedAt(req.registrationRequestedAt());
            }
            if (req.registrationNotes() != null) {
                String notes = req.registrationNotes().trim();
                profile.setRegistrationNotes(notes.isEmpty() ? null : notes);
            }
        }

        if (req.technicalProposalStatus() != null && !req.technicalProposalStatus().isBlank()) {
            try {
                profile.setTechnicalProposalStatus(TechnicalProposalStatus.valueOf(req.technicalProposalStatus()));
            } catch (IllegalArgumentException e) {
                log.warn("Status de proposta técnica inválido '{}' para o perfil {}. Valor ignorado.", req.technicalProposalStatus(), profileId);
            }
        } else if (req.technicalProposalStatus() != null) {
            profile.setTechnicalProposalStatus(null);
        }

        if (req.allocationProjectId() != null) {
            Project project = projectRepo.findById(req.allocationProjectId())
                    .orElseThrow(() -> new BadRequestException("Projeto não encontrado"));
            profile.setAllocationProject(project);
        } else {
            profile.setAllocationProject(null);
        }

        if (req.allocationSquadId() != null) {
            Squad squad = squadRepo.findById(req.allocationSquadId())
                    .orElseThrow(() -> new BadRequestException("Squad não encontrada"));
            profile.setAllocationSquad(squad);
        } else {
            profile.setAllocationSquad(null);
        }

        applyResourceStatusAutomation(profile, wasAllocatedToProject);

        if (req.groupId() != null) {
            var group = groupRepo.findById(req.groupId())
                    .orElseThrow(() -> {
                        log.warn("Tentativa de associar grupo ao perfil ID: {} falhou: Grupo não encontrado.", profileId);
                        return new BadRequestException("Group not found");
                    });
            profile.getUser().setGroup(group);
            userRepo.save(profile.getUser());
        }

        // Reconciliação de skills HARD e SOFT
        if (req.skills() != null || req.softSkills() != null) {
            reconcileSkills(profile, req.skills(), req.softSkills());
        }

        Profile saved = profileRepo.save(profile);
        log.info("Perfil ID: {} atualizado administrativamente com sucesso.", profileId);

        // Se o status mudou para ATIVO, notifica o colaborador
        if (DomainStatus.ACTIVE == saved.getStatus() && DomainStatus.ACTIVE != oldStatus) {
            log.info("Status do perfil ID: {} mudou para ACTIVE. Disparando notificação para o usuário.", profileId);
            emailService.sendResourceProfileApprovedEmail(
                saved.getUser().getEmail(), saved.getUser().getName(), appProperties.getUrl()
            );
        }

        return saved;
    }

    private void applyResourceStatusAutomation(Profile profile, boolean wasAllocatedToProject) {
        boolean deallocated = wasAllocatedToProject && profile.getAllocationProject() == null;
        if (deallocated) {
            profile.setRegistrationStatus(RegistrationStatus.NOT_REQUIRED);
            profile.setRegistrationNumber(null);
            profile.setRegistrationRequestedAt(null);
            profile.setRegistrationNotes(null);
            profile.setResourceStatus(ResourceStatus.AVAILABLE);
            log.info("Perfil {}: desalocado — matrícula resetada para NOT_REQUIRED e status do recurso AVAILABLE.",
                    profile.getId());
            return;
        }

        ResourceStatus next = ResourceStatus.fromRegistrationStatus(profile.getRegistrationStatus());
        profile.setResourceStatus(next);
        log.debug("Perfil {}: status do recurso sincronizado para {} a partir da matrícula {}.",
                profile.getId(), next, profile.getRegistrationStatus());
    }

    private void reconcileSkills(Profile profile, List<SkillEntry> hardEntries, List<SkillEntry> softEntries) {
        // Build maps of request skills if they are provided
        Map<String, SkillEntry> requestedHard = null;
        if (hardEntries != null) {
            requestedHard = hardEntries.stream()
                    .filter(s -> s.name() != null && !s.name().isBlank())
                    .collect(Collectors.toMap(
                            s -> s.name().trim().toUpperCase(),
                            s -> s,
                            (existing, replacement) -> existing
                    ));
        }

        Map<String, SkillEntry> requestedSoft = null;
        if (softEntries != null) {
            requestedSoft = softEntries.stream()
                    .filter(s -> s.name() != null && !s.name().isBlank())
                    .collect(Collectors.toMap(
                            s -> s.name().trim().toUpperCase(),
                            s -> s,
                            (existing, replacement) -> existing
                    ));
        }

        // 1. Process existing profile skills and update in place or remove
        List<ProfileSkill> toRemove = new ArrayList<>();
        for (ProfileSkill ps : profile.getSkills()) {
            String name = ps.getSkill().getName().trim().toUpperCase();
            SkillType currentType = ps.getSkill().getType();

            if (currentType == SkillType.HARD) {
                if (requestedHard != null) {
                    if (requestedHard.containsKey(name)) {
                        // Keep and update
                        SkillEntry entry = requestedHard.remove(name);
                        ps.setProficiencyLevel(entry.proficiencyLevel());
                    } else if (requestedSoft != null && requestedSoft.containsKey(name)) {
                        // Changed type to SOFT, update in place
                        SkillEntry entry = requestedSoft.remove(name);
                        ps.setProficiencyLevel(entry.proficiencyLevel());
                        ps.getSkill().setType(SkillType.SOFT);
                        skillRepo.save(ps.getSkill());
                    } else {
                        // Removed from HARD (and not added to SOFT)
                        toRemove.add(ps);
                    }
                } else {
                    // We are not reconciling HARD skills, so do not touch them
                }
            } else if (currentType == SkillType.SOFT) {
                if (requestedSoft != null) {
                    if (requestedSoft.containsKey(name)) {
                        // Keep and update
                        SkillEntry entry = requestedSoft.remove(name);
                        ps.setProficiencyLevel(entry.proficiencyLevel());
                    } else if (requestedHard != null && requestedHard.containsKey(name)) {
                        // Changed type to HARD, update in place
                        SkillEntry entry = requestedHard.remove(name);
                        ps.setProficiencyLevel(entry.proficiencyLevel());
                        ps.getSkill().setType(SkillType.HARD);
                        skillRepo.save(ps.getSkill());
                    } else {
                        // Removed from SOFT (and not added to HARD)
                        toRemove.add(ps);
                    }
                } else {
                    // We are not reconciling SOFT skills, so do not touch them
                }
            }
        }

        // Remove marked skills
        profile.getSkills().removeAll(toRemove);

        // 2. Add remaining new HARD skills
        if (requestedHard != null) {
            for (var entry : requestedHard.values()) {
                String name = entry.name().trim().toUpperCase();
                Integer level = entry.proficiencyLevel();
                
                var skill = skillRepo.findByName(name)
                        .orElseGet(() -> skillRepo.save(Skill.builder().name(name).type(SkillType.HARD).build()));

                if (skill.getType() != SkillType.HARD) {
                    skill.setType(SkillType.HARD);
                    skillRepo.save(skill);
                }

                profile.getSkills().add(ProfileSkill.builder()
                        .profile(profile)
                        .skill(skill)
                        .proficiencyLevel(level)
                        .build());
            }
        }

        // 3. Add remaining new SOFT skills
        if (requestedSoft != null) {
            for (var entry : requestedSoft.values()) {
                String name = entry.name().trim().toUpperCase();
                Integer level = entry.proficiencyLevel();
                
                var skill = skillRepo.findByName(name)
                        .orElseGet(() -> skillRepo.save(Skill.builder().name(name).type(SkillType.SOFT).build()));

                if (skill.getType() != SkillType.SOFT) {
                    skill.setType(SkillType.SOFT);
                    skillRepo.save(skill);
                }

                profile.getSkills().add(ProfileSkill.builder()
                        .profile(profile)
                        .skill(skill)
                        .proficiencyLevel(level)
                        .build());
            }
        }
    }

    public String getInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }

        String[] nameParts = fullName.trim().split("\\s+");

        if (nameParts.length == 1) {
            return nameParts[0]
                    .substring(0, 1)
                    .toUpperCase();
        }

        return (
                nameParts[0].substring(0, 1)
                        + nameParts[nameParts.length - 1].substring(0, 1)
        ).toUpperCase();
    }
}
