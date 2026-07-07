package com.vilt.talentos.service;

import com.vilt.talentos.dto.AdminSkillListResponse;
import com.vilt.talentos.dto.SkillRequest;
import com.vilt.talentos.dto.SkillResponse;
import com.vilt.talentos.entity.Skill;
import com.vilt.talentos.entity.SkillCategory;
import com.vilt.talentos.exception.ConflictException;
import com.vilt.talentos.exception.ResourceNotFoundException;
import com.vilt.talentos.mapper.SkillMapper;
import com.vilt.talentos.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepo;
    private final SkillMapper mapper;

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
    public Page<AdminSkillListResponse> getAdminSkills(
            String nameFilter,
            SkillCategory categoryFilter,
            Boolean isActiveFilter,
            Pageable pageable
    ) {
        Specification<Skill> filterSpecification = (root, query, criteriaBuilder) -> {
            List<Predicate> filterPredicates = new ArrayList<>();

            if (isActiveFilter != null) {
                filterPredicates.add(criteriaBuilder.equal(root.get("active"), isActiveFilter));
            }
            if (nameFilter != null && !nameFilter.isBlank()) {
                String namePattern = "%" + nameFilter.trim().toUpperCase() + "%";
                filterPredicates.add(criteriaBuilder.like(criteriaBuilder.upper(root.get("name")), namePattern));
            }
            if (categoryFilter != null) {
                filterPredicates.add(criteriaBuilder.equal(root.get("category"), categoryFilter));
            }

            return criteriaBuilder.and(filterPredicates.toArray(new Predicate[0]));
        };

        Page<Skill> page = skillRepo.findAll(filterSpecification, pageable);

        return page.map(skill -> {
            long resourcesCount = skill.getProfileSkills().size();

            double avgProficiency = skill.getProfileSkills().stream()
                    .mapToDouble(ps -> ps.getProficiencyLevel())
                    .average()
                    .orElse(0.0);

            List<String> avatarUrls = skill.getProfileSkills().stream()
                    .filter(ps -> ps.getProfile() != null && ps.getProfile().getPhotoUrl() != null)
                    .sorted((ps1, ps2) -> Double.compare(ps2.getProficiencyLevel(), ps1.getProficiencyLevel()))
                    .limit(5)
                    .map(ps -> ps.getProfile().getPhotoUrl())
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
                    avatarUrls
            );
        });
    }

    public SkillResponse create(SkillRequest request) {
        String normalizedSkillName = request.name().trim().toUpperCase();

        skillRepo.findByName(normalizedSkillName).ifPresent(existingSkill -> {
            if (!existingSkill.isActive()) {
                throw new ConflictException(
                    String.format("A skill '%s' já está cadastrada, porém encontra-se inativa. Acesse o filtro de skills inativas para reativá-la.", existingSkill.getName())
                );
            }
            throw new ConflictException(
                String.format("Já existe uma skill ativa com o nome '%s'.", existingSkill.getName())
            );
        });

        Skill skill = mapper.toEntity(request);
        skill.setName(normalizedSkillName);
        return mapper.toResponse(skillRepo.save(skill));
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
