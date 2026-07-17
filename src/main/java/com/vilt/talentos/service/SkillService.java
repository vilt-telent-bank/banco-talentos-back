package com.vilt.talentos.service;

import com.vilt.talentos.dto.AdminSkillListResponse;
import com.vilt.talentos.dto.AvatarResponse;
import com.vilt.talentos.dto.SkillRequest;
import com.vilt.talentos.dto.SkillResponse;
import com.vilt.talentos.entity.AvatarType;
import com.vilt.talentos.entity.Profile;
import com.vilt.talentos.entity.Skill;
import com.vilt.talentos.entity.SkillCategory;
import com.vilt.talentos.exception.ConflictException;
import com.vilt.talentos.exception.ResourceNotFoundException;
import com.vilt.talentos.mapper.SkillMapper;
import com.vilt.talentos.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepo;
    private final SkillMapper mapper;
    private final ProfileService profileService;

    public Page<SkillResponse> findAllActive(Pageable pageable) {
        Page<SkillResponse> page = skillRepo.findByActive(true, pageable)
                .map(mapper::toResponse);
        if (page.isEmpty()) {
            throw new ResourceNotFoundException("Nenhuma skill ativa encontrada.");
        }
        return page;
    }

    public Page<SkillResponse> findAllInactive(Pageable pageable) {
        Page<SkillResponse> page = skillRepo.findByActive(false, pageable)
                .map(mapper::toResponse);
        if (page.isEmpty()) {
            throw new ResourceNotFoundException("Nenhuma skill inativa encontrada.");
        }
        return page;
    }

    public SkillResponse findById(UUID id) {
        return skillRepo.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Skill não encontrada"));
    }

    @Transactional(readOnly = true)
    public Page<AdminSkillListResponse> getAdminSkills(String name, SkillCategory category, Pageable pageable) {

        Skill filterSample = Skill.builder()
                .active(true)
                .name(name != null && !name.isBlank() ? name.trim() : null)
                .category(category)
                .build();

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreCase()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnorePaths("id", "type", "profileSkills");

        Example<Skill> example = Example.of(filterSample, matcher);
        Page<Skill> page = skillRepo.findAll(example, pageable);

        return page.map(skill -> {
            long resourcesCount = skill.getProfileSkills().size();

            double avgProficiency = skill.getProfileSkills().stream()
                    .mapToDouble(ps -> ps.getProficiencyLevel())
                    .average()
                    .orElse(0.0);


            List<AvatarResponse> avatars = skill.getProfileSkills().stream()
                    .filter(ps -> ps.getProfile() != null)
                    .sorted((ps1, ps2) -> Double.compare(ps2.getProficiencyLevel(), ps1.getProficiencyLevel()))
                    .limit(5)
                    .map(ps -> {
                        Profile profile = ps.getProfile();
                        AvatarType avatarType = profile.getPhotoUrl() != null ? AvatarType.PHOTO : AvatarType.INITIALS;
                        String avatar = avatarType == AvatarType.PHOTO ? profile.getPhotoUrl() : profileService.getInitials(profile.getUser().getName());
                        return new AvatarResponse(
                                avatarType,
                                avatar
                        );
                    })
                    .toList();

            return new AdminSkillListResponse(
                    skill.getId(),
                    skill.getName(),
                    skill.getType(),
                    skill.isActive(),
                    skill.getDescription(),
                    skill.getCategory(),
                    resourcesCount,
                    avgProficiency,
                    avatars
            );
        });
    }
    public SkillResponse create(SkillRequest request) {
        String nameUpper = request.name().trim().toUpperCase();

        return skillRepo.findByName(nameUpper)
                .map(existing -> {
                    if (!existing.isActive()) {
                        existing.setActive(true);
                        existing.setDescription(request.description());
                        existing.setCategory(request.category());
                        return mapper.toResponse(skillRepo.save(existing));
                    }
                    return mapper.toResponse(existing);
                })
                .orElseGet(() -> {
                    Skill skill = mapper.toEntity(request);
                    skill.setName(nameUpper);
                    return mapper.toResponse(skillRepo.save(skill));
                });
    }

    public SkillResponse update(UUID id, SkillRequest request) {
        Skill skill = skillRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill não encontrada"));

        String nameUpper = request.name().trim().toUpperCase();

        skillRepo.findByName(nameUpper)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new ConflictException("Já existe outra skill com este nome");
                    }
                });

        mapper.updateEntity(request, skill);
        skill.setName(nameUpper);

        return mapper.toResponse(skillRepo.save(skill));
    }

    public void setActiveStatus(UUID id, boolean active) {
        Skill skill = skillRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill não encontrada"));
        skill.setActive(active);
        skillRepo.save(skill);
    }
}
