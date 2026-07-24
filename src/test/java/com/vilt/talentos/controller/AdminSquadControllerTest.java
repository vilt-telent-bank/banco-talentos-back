package com.vilt.talentos.controller;

import com.vilt.talentos.dto.SquadRequest;
import com.vilt.talentos.dto.SquadResponse;
import com.vilt.talentos.service.SquadService;
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

@WebMvcTest(AdminSquadController.class)
class AdminSquadControllerTest extends BaseControllerTest {

    @MockBean
    private SquadService squadService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve listar squads ativas com sucesso")
    void listActive_Success() throws Exception {
        SquadResponse response = new SquadResponse(UUID.randomUUID(), "Squad 1", "Desc", "Coord", "GP", "Projeto", UUID.randomUUID(), true, Instant.now(), Instant.now(), "admin", "admin");
        when(squadService.findAllActive(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/admin/squads/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Squad 1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve listar squads inativas com sucesso")
    void listInactive_Success() throws Exception {
        SquadResponse response = new SquadResponse(UUID.randomUUID(), "Squad 2", "Desc", "Coord", "GP", "Projeto", UUID.randomUUID(), false, Instant.now(), Instant.now(), "admin", "admin");
        when(squadService.findAllInactive(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/admin/squads/inactive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Squad 2"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve buscar squad por ID com sucesso")
    void getById_Success() throws Exception {
        UUID id = UUID.randomUUID();
        SquadResponse response = new SquadResponse(id, "Squad 1", "Desc", "Coord", "GP", "Projeto", UUID.randomUUID(), true, Instant.now(), Instant.now(), "admin", "admin");
        when(squadService.findById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/squads/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve criar nova squad com sucesso")
    void create_Success() throws Exception {
        SquadRequest request = new SquadRequest("Nova Squad", "Desc", "Coord", "GP");
        SquadResponse response = new SquadResponse(UUID.randomUUID(), "Nova Squad", "Desc", "Coord", "GP", "Projeto", null, true, Instant.now(), Instant.now(), "admin", "admin");
        
        when(squadService.create(any(SquadRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/squads")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Nova Squad"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve atualizar squad com sucesso")
    void update_Success() throws Exception {
        UUID id = UUID.randomUUID();
        SquadRequest request = new SquadRequest("Squad Atualizada", "Desc", "Coord", "GP");
        SquadResponse response = new SquadResponse(id, "Squad Atualizada", "Desc", "Coord", "GP", "Projeto", null, true, Instant.now(), Instant.now(), "admin", "admin");

        when(squadService.update(eq(id), any(SquadRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/admin/squads/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Squad Atualizada"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve ativar squad com sucesso")
    void activate_Success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch("/api/v1/admin/squads/{id}/activate", id)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(squadService).setActiveStatus(id, true);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve inativar squad com sucesso")
    void inactivate_Success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch("/api/v1/admin/squads/{id}/inactivate", id)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(squadService).setActiveStatus(id, false);
    }
}
