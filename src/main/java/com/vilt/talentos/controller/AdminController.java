package com.vilt.talentos.controller;

import com.vilt.talentos.dto.AdminUpdateRequest;
import com.vilt.talentos.dto.CreateResourceRequest;
import com.vilt.talentos.dto.CreateResourceResponse;
import com.vilt.talentos.dto.DashboardKpisResponse;
import com.vilt.talentos.dto.ProfileResponse;
import com.vilt.talentos.entity.DomainStatus;
import com.vilt.talentos.entity.Profile;
import com.vilt.talentos.entity.User;
import com.vilt.talentos.mapper.ProfileMapper;
import com.vilt.talentos.service.AdminService;
import com.vilt.talentos.service.ProfileService;
import com.vilt.talentos.service.ResourceService;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Operações administrativas — requer role ADMIN")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    private final ProfileService profileService;
    private final AdminService adminService;
    private final ResourceService resourceService;
    private final ProfileMapper profileMapper;

    @GetMapping("/profiles")
    @Operation(summary = "Listar todos os perfis", description = "Retorna uma página com todos os perfis cadastrados no sistema.")
    public Page<ProfileResponse> all(@RequestParam(required = false) String skill, @PageableDefault(size = 20) Pageable pageable) {
        return profileService.getAllWithFilters(null, skill, pageable).map(profileMapper::toResponse);
    }

    @GetMapping("/profiles/pending")
    @Operation(summary = "Fila de revisão", description = "Retorna perfis com status PENDENTE que aguardam aprovação.")
    public Page<ProfileResponse> pendentes(@PageableDefault(size = 20) Pageable pageable) {
        return profileService.getByStatus(DomainStatus.PENDING, pageable).map(profileMapper::toResponse);
    }

    @GetMapping("/profiles/active")
    @Operation(summary = "Banco de talentos", description = "Retorna perfis com status ATIVO.")
    public Page<ProfileResponse> ativos(@RequestParam(required = false) String skill, @PageableDefault(size = 20) Pageable pageable) {
        return profileService.getAllWithFilters(DomainStatus.ACTIVE, skill, pageable).map(profileMapper::toResponse);
    }

    @GetMapping("/profiles/{id}")
    @Operation(summary = "Buscar perfil por id", description = "Retorna os detalhes completos de um perfil específico.")
    public ProfileResponse getById(@PathVariable UUID id) {
        return profileMapper.toResponse(profileService.getById(id));
    }

    @PatchMapping("/profiles/{id}")
    @Operation(summary = "Atualizar perfil", description = "Permite alterar status, nivel_override, área, grupo e outras informações do colaborador.")
    public ProfileResponse update(@PathVariable UUID id, @RequestBody @Valid AdminUpdateRequest req) {
        return profileMapper.toResponse(profileService.adminUpdate(id, req));
    }

    @PostMapping("/resoucr")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar recurso", description = "Cria um usuário recurso com senha provisória e envia as credenciais por e-mail.")
    public CreateResourceResponse createResource(@RequestBody @Valid CreateResourceRequest request) {
        return resourceService.createByAdmin(request);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "KPIs do dashboard", description = "Total, ativos, pendentes, top skills, distribuição por nível.")
    public DashboardKpisResponse dashboard() {
        return adminService.getDashboardKpis();
    }

    @GetMapping("/users/pending")
    @Operation(summary = "Listar usuários pendentes", description = "Retorna todos os usuários que aguardam aprovação (Status PENDING).")
    public List<User> getPendingUsers() {
        return adminService.getPendingUsers();
    }

    @PostMapping("/users/{id}/approve")
    @Operation(summary = "Aprovar usuário", description = "Altera o status de um usuário para ACTIVE e registra quem aprovou.")
    public void approveUser(@PathVariable UUID id) {
        String adminIdStr = SecurityContextHolder.getContext().getAuthentication().getName();

        adminService.approveUser(id, UUID.fromString(adminIdStr));
    }

    @PostMapping("/users/{id}/reject")
    @Operation(summary = "Rejeitar usuário", description = "Altera o status de um usuário para INACTIVE.")
    public void rejectUser(@PathVariable UUID id) {
        adminService.rejectUser(id);
    }
}
