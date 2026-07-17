package com.vilt.talentos.service;

import com.vilt.talentos.config.AppProperties;
import com.vilt.talentos.dto.AdminUpdateRequest;
import com.vilt.talentos.dto.ProfileRequest;
import com.vilt.talentos.dto.SkillEntry;
import com.vilt.talentos.entity.*;
import com.vilt.talentos.exception.BadRequestException;
import com.vilt.talentos.exception.ResourceNotFoundException;
import com.vilt.talentos.mapper.ProfileMapper;
import com.vilt.talentos.repository.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
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
public class ProfileService {

    private final ProfileRepository profileRepo;
    private final SkillRepository skillRepo;
    private final UserRepository userRepo;
    private final GroupRepository groupRepo;
    private final TalentEvaluationService evaluationService;
    private final EmailService emailService;
    private final AppProperties appProperties;
    private final ProfileMapper profileMapper;

    @Transactional
    public Profile createOrUpdate(UUID userId, ProfileRequest req) {
        var user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        var evaluation = evaluationService.evaluate(req);

        var profile = profileRepo.findByUserId(userId).orElseGet(() -> Profile.builder()
                .user(user).status(DomainStatus.PENDING).build());

        // Toda submissão pelo recurso deve passar por revisão (status PENDENTE)
        boolean wasActive = DomainStatus.ACTIVE == profile.getStatus();
        profile.setStatus(DomainStatus.PENDING);

        profileMapper.updateEntity(req, profile);
        
        // Lógica de Matrícula para o Recurso: envia para revisão administrativa
        profile.setRegistrationNumber(req.registrationNumber());
        profile.setRegistrationStatus(RegistrationStatus.AWAITING_APPROVAL);

        profile.setLevel(evaluation.nivel().name());
        profile.setLevelScore(evaluation.score());
        profile.setLevelJustification(evaluation.justificativa());
        // Reconciliação de skills HARD (recursos só mexem em HARD)
        reconcileSkills(profile, req.skills(), null);

        Profile saved = profileRepo.save(profile);

        // Notificar admins se o perfil foi submetido (se estava ATIVO ou se é novo)
        if (wasActive || profileRepo.findByUserId(userId).isEmpty()) {
            List<String> adminEmails = userRepo.findAllByRoleAndStatus(UserRole.ADMIN, DomainStatus.ACTIVE, Pageable.unpaged())
                    .getContent().stream().map(User::getEmail).toList();
            if (!adminEmails.isEmpty()) {
                emailService.sendAdminNewProfileSubmissionEmail(
                    adminEmails, user.getName(), saved.getJobTitle(), saved.getLevel(), appProperties.getUrl()
                );
            }
        }

        return saved;
    }

    public Profile getByUserId(UUID userId) {
        Profile profile = profileRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado para o usuário"));
        
        return sanitizeProfileForResource(profile);
    }

    private Profile sanitizeProfileForResource(Profile profile) {
        if (profile.getRegistrationStatus() == RegistrationStatus.REJECTED) {
            profile.setRegistrationStatus(null);
        }
        // Soft skills são visíveis apenas para admins
        profile.getSkills().removeIf(ps -> ps.getSkill().getType() == SkillType.SOFT);
        return profile;
    }

    public Profile getById(UUID id) {
        return profileRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado"));
    }

    public Page<Profile> getByStatus(DomainStatus status, Pageable pageable) {
        Page<Profile> page = profileRepo.findByStatus(status, pageable);
        if (page.isEmpty()) {
            throw new ResourceNotFoundException("No profiles found with status: " + status);
        }
        return page;
    }

    public Page<Profile> getAll(Pageable pageable) {
        Page<Profile> page = profileRepo.findAll(pageable);
        if (page.isEmpty()) {
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
        var profile = profileRepo.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado"));
        
        DomainStatus oldStatus = profile.getStatus();
        
        if (req.status() != null) {
            try {
                profile.setStatus(DomainStatus.valueOf(req.status()));
            } catch (IllegalArgumentException e) {
                // Keep old status or handle error
            }
        }

        profileMapper.updateEntityFromAdmin(req, profile);

        if (req.registrationStatus() != null) {
            try {
                profile.setRegistrationStatus(RegistrationStatus.valueOf(req.registrationStatus()));
            } catch (IllegalArgumentException e) {
                // Ignore invalid enum
            }
        }

        if (req.groupId() != null) {
            var group = groupRepo.findById(req.groupId())
                    .orElseThrow(() -> new BadRequestException("Group not found"));
            profile.getUser().setGroup(group);
            userRepo.save(profile.getUser());
        }

        // Reconciliação de skills HARD e SOFT
        if (req.skills() != null || req.softSkills() != null) {
            reconcileSkills(profile, req.skills(), req.softSkills());
        }

        Profile saved = profileRepo.save(profile);


        // Se o status mudou para ATIVO, notifica o colaborador
        if (DomainStatus.ACTIVE == saved.getStatus() && DomainStatus.ACTIVE != oldStatus) {
            emailService.sendResourceProfileApprovedEmail(
                saved.getUser().getEmail(), saved.getUser().getName(), appProperties.getUrl()
            );
        }

        return saved;
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
