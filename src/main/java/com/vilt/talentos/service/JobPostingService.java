package com.vilt.talentos.service;

import com.vilt.talentos.dto.JobPostingRequest;
import com.vilt.talentos.dto.JobPostingResponse;
import com.vilt.talentos.dto.JobPostingSkillRequest;
import com.vilt.talentos.entity.*;
import com.vilt.talentos.exception.BadRequestException;
import com.vilt.talentos.exception.ResourceNotFoundException;
import com.vilt.talentos.exception.UnauthorizedException;
import com.vilt.talentos.mapper.JobPostingMapper;
import com.vilt.talentos.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
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
public class JobPostingService {
    private final JobPostingRepository jobPostingRepo;
    private final ProjectRepository projectRepo;
    private final SquadRepository squadRepo;
    private final SkillRepository skillRepo;
    private final UserRepository userRepo;
    private final JobPostingMapper mapper;

    public Page<JobPostingResponse> findAllActive(Pageable pageable){
        Page<JobPostingResponse> page = jobPostingRepo.findByActive(true, pageable)
                .map(mapper::toResponse);

        if (page.isEmpty()) {
            log.warn("Busca por vagas ativas retornou vazia.");
            throw new ResourceNotFoundException("Nenhuma vaga ativa encontrada");
        }

        return page;
    }

    public Page<JobPostingResponse> findAllInactive(Pageable pageable){
        Page<JobPostingResponse> page = jobPostingRepo.findByActive(false, pageable)
                .map(mapper::toResponse);

        if (page.isEmpty()) {
            log.warn("Busca por vagas inativas retornou vazia.");
            throw new ResourceNotFoundException("Nenhuma vaga inativa encontrada");
        }
        return page;
    }

    public JobPostingResponse findById(UUID id){
        return jobPostingRepo.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> {
                    log.warn("Busca falhou: Vaga ID '{}' não encontrada.", id);
                    return new ResourceNotFoundException("Vaga não encontrada");
                });
    }

    @Transactional
    public JobPostingResponse create(JobPostingRequest request){
        log.info("Iniciando criação de uma nova vaga para o projeto ID: {}", request.projectId());

        Project project = projectRepo.findById(request.projectId())
                .orElseThrow(() -> {
                    log.warn("Falha ao criar vaga: Projeto ID '{}' não encontrado.", request.projectId());
                    return new ResourceNotFoundException("Projeto não encontrado");
                });
        
        Squad squad = squadRepo.findById(request.squadId())
                .orElseThrow(() -> {
                    log.warn("Falha ao criar vaga: Squad ID '{}' não encontrado.", request.squadId());
                    return new ResourceNotFoundException("Squad não encontrada");
                });

        JobPosting jobPosting = mapper.toEntity(request);
        jobPosting.setProject(project);
        jobPosting.setSquad(squad);
        jobPosting.setCreatedBy(getCurrentUser());
        
        reconcileSkills(jobPosting, request.skills());

        JobPosting savedJobPosting = jobPostingRepo.save(jobPosting);
        log.info("Vaga criada com sucesso. ID gerado: {}", savedJobPosting.getId());

        return mapper.toResponse(savedJobPosting);
    }

    @Transactional
    public JobPostingResponse update(UUID id, JobPostingRequest request){
        log.info("Iniciando atualização da vaga ID: {}", id);

        JobPosting jobPosting = jobPostingRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Falha ao atualizar: Vaga ID '{}' não encontrada.", id);
                    return new ResourceNotFoundException("Vaga não encontrada");
                });

        Project project = projectRepo.findById(request.projectId())
                .orElseThrow(() -> {
                    log.warn("Falha ao atualizar vaga: Projeto ID '{}' não encontrado.", request.projectId());
                    return new ResourceNotFoundException("Projeto não encontrado");
                });
        
        Squad squad = squadRepo.findById(request.squadId())
                .orElseThrow(() -> {
                    log.warn("Falha ao atualizar vaga: Squad ID '{}' não encontrado.", request.squadId());
                    return new ResourceNotFoundException("Squad não encontrada");
                });

        mapper.updateEntity(request, jobPosting);
        jobPosting.setProject(project);
        jobPosting.setSquad(squad);
        jobPosting.setUpdatedBy(getCurrentUser());

        reconcileSkills(jobPosting, request.skills());

        JobPosting updatedJobPosting = jobPostingRepo.save(jobPosting);
        log.info("Vaga ID: {} atualizada com sucesso.", id);

        return mapper.toResponse(updatedJobPosting);
    }

    private void reconcileSkills(JobPosting jobPosting, List<JobPostingSkillRequest> skillRequests) {
        if (skillRequests == null || skillRequests.isEmpty()) {
            jobPosting.getSkills().clear();
            log.debug("Lista de skills da vaga enviada vazia. Removendo todas as skills da vaga.");
            return;
        }

        Map<String, JobPostingSkillRequest> requested = skillRequests.stream()
                .filter(s -> s.name() != null && !s.name().isBlank())
                .collect(Collectors.toMap(
                        s -> s.name().trim().toUpperCase(),
                        s -> s,
                        (existing, replacement) -> existing
                ));

        List<JobPostingSkill> toRemove = new ArrayList<>();
        for (JobPostingSkill jps : jobPosting.getSkills()) {
            String name = jps.getSkill().getName().trim().toUpperCase();
            if (requested.containsKey(name)) {
                JobPostingSkillRequest req = requested.remove(name);
                jps.setImportanceWeight(req.importanceWeight());
                jps.setType(req.type());
                jps.setMinLevel(req.minLevel());
                jps.setDescription(req.description());
            } else {
                toRemove.add(jps);
            }
        }

        jobPosting.getSkills().removeAll(toRemove);

        for (var entry : requested.values()) {
            String name = entry.name().trim().toUpperCase();
            var skill = skillRepo.findByName(name)
                    .orElseGet(() -> skillRepo.save(Skill.builder().name(name).type(SkillType.HARD).build()));

            jobPosting.getSkills().add(JobPostingSkill.builder()
                    .jobPosting(jobPosting)
                    .skill(skill)
                    .type(entry.type())
                    .minLevel(entry.minLevel())
                    .importanceWeight(entry.importanceWeight())
                    .description(entry.description())
                    .build());
        }
    }

    public void setActiveStatus(UUID id, boolean active){
        log.info("Alterando status da vaga ID: {} para ativo={}", id, active);

        JobPosting jobPosting = jobPostingRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Falha ao atualizar status da vaga: Vaga ID '{}' não encontrada.", id);
                    return new ResourceNotFoundException("Vaga não encontrada");
                });

        jobPosting.setActive(active);
        jobPosting.setUpdatedBy(getCurrentUser());
        jobPostingRepo.save(jobPosting);

        log.info("Status da vaga ID: {} atualizado para: {}", id, active);
    }

    private User getCurrentUser() {
        String userIdStr = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepo.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> {
                    log.error("Inconsistência de segurança: Token válido, mas usuário ID: {} não encontrado no banco.", userIdStr);
                    return new UnauthorizedException("Usuário não autenticado");
                });
    }
}
