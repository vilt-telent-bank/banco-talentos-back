package com.vilt.talentos.controller;

import com.vilt.talentos.dto.ProfileRequest;
import com.vilt.talentos.dto.ProfileResponse;
import com.vilt.talentos.dto.ProfileSelfUpdateRequest;
import com.vilt.talentos.mapper.ProfileMapper;
import com.vilt.talentos.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Perfil", description = "Operações do recurso — visualizar e submeter o próprio perfil")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;
    private final ProfileMapper profileMapper;

    @GetMapping
    @Operation(summary = "Buscar meu perfil", description = "Retorna os detalhes do perfil do usuário autenticado.")
    public ProfileResponse getMyProfile(Authentication auth) {
        return profileMapper.toResponse(profileService.getByUserId(UUID.fromString(auth.getName())));
    }

    @PostMapping
    @Operation(summary = "Criar ou atualizar perfil", description = "Submete o perfil para análise (status PENDENTE). A IA avalia e classifica o nível automaticamente com base na Matriz Porto Seguro.")
    public ProfileResponse submit(@RequestBody @Valid ProfileRequest req, Authentication auth) {
        return profileMapper.toResponse(profileService.createOrUpdate(UUID.fromString(auth.getName()), req));
    }

    @PatchMapping
    @Operation(summary = "Atualizar meu perfil", description = "Atualiza identificação, skills, contato/endereço e links sem alterar o status do perfil.")
    public ProfileResponse updateMyProfile(@RequestBody @Valid ProfileSelfUpdateRequest req, Authentication auth) {
        return profileMapper.toResponse(profileService.updateMyProfile(UUID.fromString(auth.getName()), req));
    }
}
