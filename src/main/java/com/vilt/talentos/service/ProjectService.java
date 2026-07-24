package com.vilt.talentos.service;

import com.vilt.talentos.dto.ProjectRequest;
import com.vilt.talentos.dto.ProjectResponse;
import com.vilt.talentos.entity.Project;
import com.vilt.talentos.entity.Squad;
import com.vilt.talentos.entity.User;
import com.vilt.talentos.exception.ConflictException;
import com.vilt.talentos.exception.ResourceNotFoundException;
import com.vilt.talentos.exception.UnauthorizedException;
import com.vilt.talentos.mapper.ProjectMapper;
import com.vilt.talentos.mapper.SquadMapper;
import com.vilt.talentos.repository.ProjectRepository;
import com.vilt.talentos.repository.SquadRepository;
import com.vilt.talentos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepo;
    private final UserRepository userRepo;
    private final SquadRepository squadRepo;
    private final ProjectMapper mapper;
    private final SquadMapper squadMapper;

    public Page<ProjectResponse> findAllActive(Pageable pageable) {
        Page<ProjectResponse> page = projectRepo.findByActive(true, pageable)
                .map(p -> mapper.toResponse(p, squadMapper));
        if (page.isEmpty()) {
            log.warn("Busca por projetos ativos retornou vazia.");
            throw new ResourceNotFoundException("Nenhum projeto ativo encontrado");
        }
        return page;
    }

    public Page<ProjectResponse> findAllInactive(Pageable pageable) {
        Page<ProjectResponse> page = projectRepo.findByActive(false, pageable)
                .map(p -> mapper.toResponse(p, squadMapper));
        if (page.isEmpty()) {
            log.warn("Busca por projetos inativos retornou vazia.");
            throw new ResourceNotFoundException("Nenhum projeto inativo encontrado");
        }
        return page;
    }

    public ProjectResponse findById(UUID id) {
        return projectRepo.findWithSquadsById(id)
                .map(p -> mapper.toResponse(p, squadMapper))
                .orElseThrow(() -> {
                    log.warn("Busca falhou: Projeto ID '{}' não encontrado.", id);
                    return new ResourceNotFoundException("Projeto não encontrado");
                });
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        log.info("Iniciando criação de um novo projeto.");

        User currentUser = getCurrentUser();
        Project project = mapper.toEntity(request);
        project.setCreatedBy(currentUser);

        Project savedProject = projectRepo.save(project);
        log.info("Projeto criado com sucesso. ID gerado: {}", savedProject.getId());

        if (request.squad() != null) {
            Squad newSquad = squadMapper.toEntity(request.squad());
            newSquad.setProject(savedProject);
            newSquad.setCreatedBy(currentUser);
            squadRepo.save(newSquad);
            log.info("Squad criada e vinculada ao projeto com sucesso.");
        }

        if (request.squadIds() != null) {
            reconcileSquads(savedProject, request.squadIds(), currentUser);
        }

        return projectRepo.findWithSquadsById(savedProject.getId())
                .map(p -> mapper.toResponse(p, squadMapper))
                .orElseThrow();
    }

    @Transactional
    public ProjectResponse update(UUID id, ProjectRequest request) {
        log.info("Iniciando atualização do projeto ID: {}", id);

        Project project = projectRepo.findWithSquadsById(id)
                .orElseThrow(() -> {
                    log.warn("Falha ao atualizar: Projeto ID '{}' não encontrado.", id);
                    return new ResourceNotFoundException("Projeto não encontrado");
                });

        mapper.updateEntity(request, project);
        project.setUpdatedBy(getCurrentUser());

        Project updatedProject = projectRepo.save(project);
        log.info("Projeto ID: {} atualizado com sucesso.", id);

        if (request.squad() != null) {
            User currentUser = getCurrentUser();
            Squad newSquad = squadMapper.toEntity(request.squad());
            newSquad.setProject(updatedProject);
            newSquad.setCreatedBy(currentUser);
            squadRepo.save(newSquad);
            log.info("Nova Squad criada e vinculada ao projeto ID: {} durante atualização.", id);
        }

        if (request.squadIds() != null) {
            reconcileSquads(updatedProject, request.squadIds(), getCurrentUser());
        }

        return projectRepo.findWithSquadsById(updatedProject.getId())
                .map(p -> mapper.toResponse(p, squadMapper))
                .orElseThrow();
    }


    public void setActiveStatus(UUID id, boolean active) {
        log.info("Alterando status do projeto ID: {} para ativo={}", id, active);

        Project project = projectRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Falha ao definir status ativo: Projeto ID '{}' não encontrado.", id);
                    return new ResourceNotFoundException("Projeto não encontrado");
                });

        project.setActive(active);
        project.setUpdatedBy(getCurrentUser());
        projectRepo.save(project);
        log.info("Status do projeto ID: {} atualizado com sucesso.", id);
    }

    private void reconcileSquads(Project project, List<UUID> desiredIds, User currentUser) {
        List<UUID> currentIds = project.getSquads().stream()
                .map(Squad::getId)
                .toList();

        // Desvincula squads que não estão mais na lista desejada (houve ajuda da IA)
        for (UUID squadId : currentIds) {
            if (!desiredIds.contains(squadId)) {
                Squad squad = squadRepo.findById(squadId)
                        .orElseThrow(() -> new ResourceNotFoundException("Squad não encontrada: " + squadId));
                squad.setProject(null);
                squad.setUpdatedBy(currentUser);
                squadRepo.save(squad);
                log.info("Squad ID: {} desvinculada do projeto ID: {}.", squadId, project.getId());
            }
        }

        // Vincula squads novas que ainda não estão associadas (houve ajuda da IA)
        for (UUID squadId : desiredIds) {
            if (!currentIds.contains(squadId)) {
                Squad squad = squadRepo.findById(squadId)
                        .orElseThrow(() -> {
                            log.warn("Squad ID '{}' não encontrada para vínculo.", squadId);
                            return new ResourceNotFoundException("Squad não encontrada: " + squadId);
                        });

                if (squad.getProject() != null && !squad.getProject().getId().equals(project.getId())) {
                    log.warn("Squad ID '{}' já está vinculada ao projeto ID '{}'.", squadId, squad.getProject().getId());
                    throw new ConflictException("A squad já está vinculada a outro projeto: " + squadId);
                }

                squad.setProject(project);
                squad.setUpdatedBy(currentUser);
                squadRepo.save(squad);
                log.info("Squad ID: {} vinculada ao projeto ID: {}.", squadId, project.getId());
            }
        }
    }

    private User getCurrentUser() {
        String userIdStr = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> {
                    log.error("Falha de segurança: Token válido, mas usuário ID: {} não encontrado no banco de dados.", userIdStr);
                    return new UnauthorizedException("Usuário não autenticado");
                });
    }
}
