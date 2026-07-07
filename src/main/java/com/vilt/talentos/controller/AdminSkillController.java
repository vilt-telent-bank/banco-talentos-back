package com.vilt.talentos.controller;

import com.vilt.talentos.dto.AdminSkillListResponse;
import com.vilt.talentos.dto.SkillRequest;
import com.vilt.talentos.dto.SkillResponse;
import com.vilt.talentos.entity.SkillCategory;
import com.vilt.talentos.service.SkillService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/skills")
@RequiredArgsConstructor
@Tag(name = "Admin Skills", description = "Gerenciamento administrativo de skills — requer role ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class AdminSkillController {

    private final SkillService skillService;

    @GetMapping
    @Operation(summary = "Listar skills para gerenciamento (Admin)",
            description = "Retorna skills com dados agregados de recursos (avatares, média de proficiência) e suporte a filtros por nome, categoria e status (active=true/false, omitir para todos).")
    public Page<AdminSkillListResponse> listForManagement(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) SkillCategory category,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable) {
        return skillService.getAdminSkills(name, category, active, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar nova skill (Admin)", description = "Permite criar skills HARD ou SOFT manualmente.")
    public SkillResponse create(@RequestBody @Valid SkillRequest request) {
        return skillService.create(request);
    }

    @GetMapping("/active")
    @Operation(summary = "Listar skills ativas (Admin)")
    public Page<SkillResponse> listActive(@PageableDefault(size = 50) Pageable pageable) {
        return skillService.findAllActive(pageable);
    }

    @GetMapping("/inactive")
    @Operation(summary = "Listar skills inativas (Admin)")
    public Page<SkillResponse> listInactive(@PageableDefault(size = 50) Pageable pageable) {
        return skillService.findAllInactive(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar skill por ID")
    public SkillResponse getById(@PathVariable UUID id) {
        return skillService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar skill")
    public SkillResponse update(@PathVariable UUID id, @RequestBody @Valid SkillRequest request) {
        return skillService.update(id, request);
    }

    @PatchMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Ativar skill")
    public void activate(@PathVariable UUID id) {
        skillService.setActiveStatus(id, true);
    }

    @PatchMapping("/{id}/inactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Inativar skill (Exclusão Lógica)")
    public void inactivate(@PathVariable UUID id) {
        skillService.setActiveStatus(id, false);
    }
}
