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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class SkillService {

    private final SkillRepository skillRepo;
    private final SkillMapper mapper;

    public Page<SkillResponse> findAllActive(Pageable pageable) {
        Page<SkillResponse> page = skillRepo.findByActive(true, pageable)
                .map(mapper::toResponse);

        if (page.isEmpty()) {
            log.warn("Busca por skills ativas retornou vazia.");
            throw new ResourceNotFoundException("Nenhuma skill ativa encontrada.");
        }

        return page;
    }

    public Page<SkillResponse> findAllInactive(Pageable pageable) {
        Page<SkillResponse> page = skillRepo.findByActive(false, pageable)
                .map(mapper::toResponse);

        if (page.isEmpty()) {
            log.warn("Busca por skills inativas retornou vazia.");
            throw new ResourceNotFoundException("Nenhuma skill inativa encontrada.");
        }

        return page;
    }

    public SkillResponse findById(UUID id) {
        return skillRepo.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> {
                    log.warn("Busca falhou: Skill ID '{}' não encontrada.", id);
                    return new ResourceNotFoundException("Skill não encontrada");
                });
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
        String nameUpper = request.name().trim().toUpperCase();
        log.info("Iniciando processo de criação/validação para a skill: '{}'", nameUpper);

        return skillRepo.findByName(nameUpper)
                .map(existing -> {
                    if (!existing.isActive()) {
                        log.info("A skill '{}' já existia, mas estava inativa. Reativando e atualizando dados (ID: {}).", nameUpper, existing.getId());
                        existing.setActive(true);
                        existing.setDescription(request.description());
                        existing.setCategory(request.category());
                        return mapper.toResponse(skillRepo.save(existing));
                    }
                    log.info("A skill '{}' já existe e encontra-se ativa. Retornando registro existente sem alterações.", nameUpper);
                    return mapper.toResponse(existing);
                })
                .orElseGet(() -> {
                    Skill skill = mapper.toEntity(request);
                    skill.setName(nameUpper);
                    Skill savedSkill = skillRepo.save(skill);

                    log.info("Nova skill '{}' criada com sucesso. ID gerado: {}", nameUpper, savedSkill.getId());
                    return mapper.toResponse(savedSkill);
                });
    }

    public SkillResponse update(UUID id, SkillRequest request) {
        log.info("Iniciando atualização da skill ID: {}", id);

        Skill skill = skillRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Falha ao atualizar: Skill ID '{}' não encontrada.", id);
                    return new ResourceNotFoundException("Skill não encontrada");
                });

        String nameUpper = request.name().trim().toUpperCase();

        skillRepo.findByName(nameUpper)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        log.warn("Falha ao atualizar: Skill com nome '{}' já existe.", nameUpper);
                        throw new ConflictException("Já existe outra skill com este nome");
                    }
                });

        mapper.updateEntity(request, skill);
        skill.setName(nameUpper);

        Skill updatedSkill = skillRepo.save(skill);
        log.info("Skill ID: {} atualizada com sucesso.", id);

        return mapper.toResponse(updatedSkill);
    }

    public void setActiveStatus(UUID id, boolean active) {
        log.info("Alterando status da skill ID: {} para ativo={}", id, active);

        Skill skill = skillRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Falha ao alterar status: Skill ID '{}' não encontrada.", id);
                    return new ResourceNotFoundException("Skill não encontrada");
                });

        skill.setActive(active);
        skillRepo.save(skill);

        log.info("Status da skill ID: {} atualizado com sucesso.", id);
    }
}
