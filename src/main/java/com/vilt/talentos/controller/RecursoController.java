package com.vilt.talentos.controller;

import com.vilt.talentos.dto.*;
import com.vilt.talentos.entity.User;
import com.vilt.talentos.service.RecursoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Recursos", description = "Gestão do ciclo de vida dos recursos")
@SecurityRequirement(name = "bearerAuth")
public class RecursoController {

    private final RecursoService recursoService;

    // ── Admin: Consulta e gestão ──────────────────────────────────────────────

    @GetMapping("/api/v1/admin/recursos")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Lista recursos com filtros paginados")
    public Page<RecursoResponse> listar(
            RecursoFilterParams filtros,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return recursoService.listar(filtros, pageable);
    }

    @GetMapping("/api/v1/admin/recursos/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Busca recurso por ID de profile")
    public RecursoResponse buscar(@PathVariable UUID id) {
        return recursoService.buscar(id);
    }

    @PatchMapping("/api/v1/admin/recursos/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Atualiza campos do lifecycle (Seções 1, 3 e 4)")
    public RecursoResponse atualizar(
            @PathVariable UUID id,
            @RequestBody @Valid RecursoUpdateRequest req) {
        return recursoService.atualizar(id, req);
    }

    @PatchMapping("/api/v1/admin/recursos/{id}/matricula")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Atualiza status de matrícula com automação de statusRecurso e registro de histórico")
    public RecursoResponse atualizarMatricula(
            @PathVariable UUID id,
            @RequestBody @Valid MatriculaUpdateRequest req,
            Authentication authentication) {
        UUID userId = ((User) authentication.getPrincipal()).getId();
        return recursoService.atualizarStatusMatricula(id, req.statusMatricula(), userId);
    }

    @GetMapping("/api/v1/admin/recursos/{id}/historico")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Retorna histórico de alterações do status de matrícula")
    public List<MatriculaHistoricoResponse> listarHistorico(@PathVariable UUID id) {
        return recursoService.listarHistorico(id);
    }

    // ── Admin: Máquinas ───────────────────────────────────────────────────────

    @PostMapping("/api/v1/admin/recursos/{id}/maquinas")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adiciona uma máquina ao recurso")
    public MaquinaResponse adicionarMaquina(
            @PathVariable UUID id,
            @RequestBody @Valid MaquinaRequest req) {
        return recursoService.adicionarMaquina(id, req);
    }

    @PutMapping("/api/v1/admin/recursos/{id}/maquinas/{maqId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Atualiza dados de uma máquina")
    public MaquinaResponse atualizarMaquina(
            @PathVariable UUID id,
            @PathVariable UUID maqId,
            @RequestBody @Valid MaquinaRequest req) {
        return recursoService.atualizarMaquina(maqId, req);
    }

    @DeleteMapping("/api/v1/admin/recursos/{id}/maquinas/{maqId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove uma máquina do recurso")
    public void removerMaquina(
            @PathVariable UUID id,
            @PathVariable UUID maqId) {
        recursoService.removerMaquina(maqId);
    }

    // ── Recurso: edição da própria Seção 5 (contato e endereço) ─────────────

    @PatchMapping("/api/v1/recurso/me/contato")
    @PreAuthorize("hasAuthority('RECURSO')")
    @Operation(summary = "Recurso atualiza seu próprio contato e endereço (Seção 5)")
    public RecursoResponse atualizarContato(
            @RequestBody @Valid ContatoUpdateRequest req,
            Authentication authentication) {
        UUID userId = ((User) authentication.getPrincipal()).getId();
        return recursoService.atualizarContatoPorUserId(userId, req);
    }
}
