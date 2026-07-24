package com.vilt.talentos.service;

import com.vilt.talentos.dto.SquadRequest;
import com.vilt.talentos.dto.SquadResponse;
import com.vilt.talentos.entity.*;
import com.vilt.talentos.exception.ResourceNotFoundException;
import com.vilt.talentos.exception.UnauthorizedException;
import com.vilt.talentos.mapper.SquadMapper;
import com.vilt.talentos.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SquadService {

    private final SquadRepository squadRepo;
    private final UserRepository userRepo;
    private final SquadMapper mapper;

    public Page<SquadResponse> findAllActive(Pageable pageable) {
        Page<SquadResponse> page = squadRepo.findByActive(true, pageable)
                .map(mapper::toResponse);

        if (page.isEmpty()) {
            log.warn("Busca por squads ativas retornou vazia.");
            throw new ResourceNotFoundException("Nenhuma squad ativa encontrada.");
        }

        return page;
    }

    public Page<SquadResponse> findAllInactive(Pageable pageable) {
        Page<SquadResponse> page = squadRepo.findByActive(false, pageable)
                .map(mapper::toResponse);

        if (page.isEmpty()) {
            log.warn("Busca por squads inativas retornou vazia.");
            throw new ResourceNotFoundException("Nenhuma squad inativa encontrada.");
        }

        return page;
    }

    public List<SquadResponse> findAllUnlinked() {
        log.info("Buscando squads ativas sem vínculo com projeto.");
        List<SquadResponse> result = squadRepo.findByActiveAndProjectIsNull(true)
                .stream()
                .map(mapper::toResponse)
                .toList();

        log.info("Total de squads sem vínculo encontradas: {}", result.size());
        return result;
    }

    public SquadResponse findById(UUID id) {
        return squadRepo.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> {
                    log.warn("Squad ID '{}' não encontrada.", id);
                    return new ResourceNotFoundException("Squad não encontrada");
                });
    }

    public SquadResponse create(SquadRequest request) {
        log.info("Iniciando criação de uma nova squad.");

        User currentUser = getCurrentUser();

        Squad squad = mapper.toEntity(request);
        squad.setActive(true);
        squad.setCreatedBy(currentUser);

        Squad savedSquad = squadRepo.save(squad);
        log.info("Squad criada com sucesso. ID gerado: {}", savedSquad.getId());

        return mapper.toResponse(savedSquad);
    }

    public SquadResponse update(UUID id, SquadRequest request) {
        log.info("Iniciando atualização da squad ID: {}", id);

        Squad squad = squadRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Falha ao atualizar: Squad ID '{}' não encontrada.", id);
                    return new ResourceNotFoundException("Squad não encontrada");
                });

        mapper.updateEntity(request, squad);
        squad.setUpdatedBy(getCurrentUser());

        Squad updatedSquad = squadRepo.save(squad);
        log.info("Squad ID: {} atualizada com sucesso.", id);

        return mapper.toResponse(updatedSquad);
    }

    public void setActiveStatus(UUID id, boolean active) {
        log.info("Alterando status da squad ID: {} para ativo={}", id, active);

        Squad squad = squadRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Falha ao alterar status: Squad ID '{}' não encontrada.", id);
                    return new ResourceNotFoundException("Squad não encontrada");
                });

        squad.setActive(active);
        squad.setUpdatedBy(getCurrentUser());
        squadRepo.save(squad);

        log.info("Status da squad ID: {} atualizado com sucesso.", id);
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
