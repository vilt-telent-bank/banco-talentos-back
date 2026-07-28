package com.vilt.talentos.controller;

import com.vilt.talentos.dto.SquadRequest;
import com.vilt.talentos.dto.SquadResponse;
import com.vilt.talentos.service.SquadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/squads")
@RequiredArgsConstructor
@Tag(name = "Admin Squads", description = "Gerenciamento de squads — requer role ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class AdminSquadController {

    private final SquadService squadService;

    @GetMapping("/unlinked")
    @Operation(summary = "Listar squads ativas sem vínculo com projeto (Admin)")
    public List<SquadResponse> listUnlinked() {
        return squadService.findAllUnlinked();
    }

    @GetMapping("/active")
    @Operation(summary = "Listar squads ativas (Admin)")
    public Page<SquadResponse> listActive(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return squadService.findAllActive(search, pageable);
    }

    @GetMapping("/inactive")
    @Operation(summary = "Listar squads inativas (Admin)")
    public Page<SquadResponse> listInactive(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return squadService.findAllInactive(search, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar squad por ID")
    public SquadResponse getById(@PathVariable UUID id) {
        return squadService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar nova squad")
    public SquadResponse create(@RequestBody @Valid SquadRequest request) {
        return squadService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar squad existente")
    public SquadResponse update(@PathVariable UUID id, @RequestBody @Valid SquadRequest request) {
        return squadService.update(id, request);
    }

    @PatchMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Ativar squad")
    public void activate(@PathVariable UUID id) {
        squadService.setActiveStatus(id, true);
    }

    @PatchMapping("/{id}/inactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Inativar squad")
    public void inactivate(@PathVariable UUID id) {
        squadService.setActiveStatus(id, false);
    }
}
