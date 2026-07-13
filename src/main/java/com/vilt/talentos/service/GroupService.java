package com.vilt.talentos.service;

import com.vilt.talentos.dto.GroupRequest;
import com.vilt.talentos.dto.GroupResponse;
import com.vilt.talentos.entity.Group;
import com.vilt.talentos.entity.User;
import com.vilt.talentos.exception.ResourceNotFoundException;
import com.vilt.talentos.exception.UnauthorizedException;
import com.vilt.talentos.mapper.GroupMapper;
import com.vilt.talentos.repository.GroupRepository;
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
public class GroupService {

    private final GroupRepository groupRepo;
    private final UserRepository userRepo;
    private final GroupMapper mapper;

    public Page<GroupResponse> findAllActive(Pageable pageable) {
        Page<GroupResponse> page = groupRepo.findByActive(true, pageable)
                .map(mapper::toResponse);

        if (page.isEmpty()) {
            log.warn("Nenhum grupo ativo encontrado");
            throw new ResourceNotFoundException("Nenhum grupo ativo encontrado");
        }

        return page;
    }

    public Page<GroupResponse> findAllInactive(Pageable pageable) {
        Page<GroupResponse> page = groupRepo.findByActive(false, pageable)
                .map(mapper::toResponse);

        if (page.isEmpty()) {
            log.warn("Nenhum grupo inativo encontrado");
            throw new ResourceNotFoundException("Nenhum grupo inativo encontrado");
        }

        return page;
    }

    public GroupResponse findById(UUID id) {
        return groupRepo.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> {
                    log.warn("Busca falhou: Grupo ID '{}' não encontrado.", id);
                    return new ResourceNotFoundException("Grupo não encontrado");
                });
    }

    public GroupResponse create(GroupRequest request) {
        log.info("Iniciando criação de um novo grupo.");

        User currentUser = getCurrentUser();
        Group group = mapper.toEntity(request);
        group.setCreatedBy(currentUser);

        Group savedGroup = groupRepo.save(group);
        log.info("Grupo criado com sucesso. ID gerado: {}", savedGroup.getId());

        return mapper.toResponse(savedGroup);
    }

    public GroupResponse update(UUID id, GroupRequest request) {
        log.info("Iniciando atualização do grupo ID: {}", id);

        Group group = groupRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Atualização falhou: Grupo ID '{}' não encontrado.", id);
                    return new ResourceNotFoundException("Grupo não encontrado");
                });

        mapper.updateEntity(request, group);
        group.setUpdatedBy(getCurrentUser());

        Group updatedGroup = groupRepo.save(group);
        log.info("Grupo atualizado com sucesso. ID: {}", updatedGroup.getId());

        return mapper.toResponse(updatedGroup);
    }

    public void setActiveStatus(UUID id, boolean active) {
        log.info("Alterando status do grupo ID: {} para ativo={}", id, active);

        Group group = groupRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Alteração de status falhou: Grupo ID '{}' não encontrado.", id);
                    return new ResourceNotFoundException("Grupo não encontrado");
                });

        group.setActive(active);
        group.setUpdatedBy(getCurrentUser());
        groupRepo.save(group);

        log.info("Status do grupo ID: {} atualizado com sucesso.", id);
    }

    private User getCurrentUser() {
        String userIdStr = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepo.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> {
                    log.error("Falha de segurança: Token válido, mas usuário ID: {} não encontrado no banco.", userIdStr);
                    return new UnauthorizedException("Usuário não autenticado");
                });
    }
}
