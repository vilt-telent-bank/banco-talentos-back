package com.vilt.talentos.service;

import com.vilt.talentos.dto.ProjectRequest;
import com.vilt.talentos.dto.ProjectResponse;
import com.vilt.talentos.entity.Project;
import com.vilt.talentos.entity.User;
import com.vilt.talentos.exception.ResourceNotFoundException;
import com.vilt.talentos.exception.UnauthorizedException;
import com.vilt.talentos.mapper.ProjectMapper;
import com.vilt.talentos.repository.ProjectRepository;
import com.vilt.talentos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepo;
    private final UserRepository userRepo;
    private final ProjectMapper mapper;

    public Page<ProjectResponse> findAllActive(Pageable pageable) {
        Page<ProjectResponse> page = projectRepo.findByActive(true, pageable)
                .map(mapper::toResponse);
        if (page.isEmpty()) {
            log.warn("Busca por projetos ativos retornou vazia.");
            throw new ResourceNotFoundException("Nenhum projeto ativo encontrado");
        }
        return page;
    }

    public Page<ProjectResponse> findAllInactive(Pageable pageable) {
        Page<ProjectResponse> page = projectRepo.findByActive(false, pageable)
                .map(mapper::toResponse);
        if (page.isEmpty()) {
            log.warn("Busca por projetos inativos retornou vazia.");
            throw new ResourceNotFoundException("Nenhum projeto inativo encontrado");
        }
        return page;
    }

    public ProjectResponse findById(UUID id) {
        return projectRepo.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> {
                    log.warn("Busca falhou: Projeto ID '{}' não encontrado.", id);
                    return new ResourceNotFoundException("Projeto não encontrado");
                });
    }

    public ProjectResponse create(ProjectRequest request) {
        log.info("Iniciando criação de um novo projeto.");

        User currentUser = getCurrentUser();
        Project project = mapper.toEntity(request);
        project.setCreatedBy(currentUser);

        Project savedProject = projectRepo.save(project);
        log.info("Projeto criado com sucesso. ID gerado: {}", savedProject.getId());

        return mapper.toResponse(savedProject);
    }

    public ProjectResponse update(UUID id, ProjectRequest request) {
        log.info("Iniciando atualização do projeto ID: {}", id);

        Project project = projectRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Falha ao atualizar: Projeto ID '{}' não encontrado.", id);
                    return new ResourceNotFoundException("Projeto não encontrado");
                });
        
        mapper.updateEntity(request, project);
        project.setUpdatedBy(getCurrentUser());

        Project updatedProject = projectRepo.save(project);
        log.info("Projeto ID: {} atualizado com sucesso.", id);

        return mapper.toResponse(updatedProject);
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

    private User getCurrentUser() {
        String userIdStr = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> {
                    log.error("Falha de segurança: Token válido, mas usuário ID: {} não encontrado no banco de dados.", userIdStr);
                    return new UnauthorizedException("Usuário não autenticado");
                });
    }
}
