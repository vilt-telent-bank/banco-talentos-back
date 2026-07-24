package com.vilt.talentos.controller;

import com.vilt.talentos.dto.ProjectRequest;
import com.vilt.talentos.dto.ProjectResponse;
import com.vilt.talentos.service.ProjectService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminProjectController.class)
class AdminProjectControllerTest extends BaseControllerTest {

    @MockBean
    private ProjectService projectService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve listar projetos ativos com sucesso")
    void listActive_Success() throws Exception {
        ProjectResponse response = new ProjectResponse(UUID.randomUUID(), "Projeto X", "Desc", true, Instant.now(), Instant.now(), "admin", "admin", List.of());
        when(projectService.findAllActive(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/admin/projects/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Projeto X"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve listar projetos inativos com sucesso")
    void listInactive_Success() throws Exception {
        ProjectResponse response = new ProjectResponse(UUID.randomUUID(), "Projeto Y", "Desc", false, Instant.now(), Instant.now(), "admin", "admin", List.of());
        when(projectService.findAllInactive(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/admin/projects/inactive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Projeto Y"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve buscar projeto por ID com sucesso")
    void getById_Success() throws Exception {
        UUID id = UUID.randomUUID();
        ProjectResponse response = new ProjectResponse(id, "Projeto X", "Desc", true, Instant.now(), Instant.now(), "admin", "admin", List.of());
        when(projectService.findById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/projects/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve criar novo projeto com sucesso")
    void create_Success() throws Exception {
        ProjectRequest request = new ProjectRequest("Novo Projeto", "Desc", null, null);
        ProjectResponse response = new ProjectResponse(UUID.randomUUID(), "Novo Projeto", "Desc", true, Instant.now(), Instant.now(), "admin", "admin", List.of());
        
        when(projectService.create(any(ProjectRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/projects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Novo Projeto"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve atualizar projeto com sucesso")
    void update_Success() throws Exception {
        UUID id = UUID.randomUUID();
        ProjectRequest request = new ProjectRequest("Projeto Atualizado", "Desc", null, null);
        ProjectResponse response = new ProjectResponse(id, "Projeto Atualizado", "Desc", true, Instant.now(), Instant.now(), "admin", "admin", List.of());

        when(projectService.update(eq(id), any(ProjectRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/admin/projects/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Projeto Atualizado"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve ativar projeto com sucesso")
    void activate_Success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch("/api/v1/admin/projects/{id}/activate", id)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(projectService).setActiveStatus(id, true);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve inativar projeto com sucesso")
    void inactivate_Success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch("/api/v1/admin/projects/{id}/inactivate", id)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(projectService).setActiveStatus(id, false);
    }
}
