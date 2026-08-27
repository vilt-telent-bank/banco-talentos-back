package com.vilt.talentos.controller;

import com.vilt.talentos.dto.AdminUpdateRequest;
import com.vilt.talentos.dto.AdminUpdateRequestFixtures;
import com.vilt.talentos.dto.CreateResourceResponse;
import com.vilt.talentos.dto.DashboardKpisResponse;
import com.vilt.talentos.dto.ProfileResponse;
import com.vilt.talentos.dto.ProfileResponseFixtures;
import com.vilt.talentos.entity.DomainStatus;
import com.vilt.talentos.entity.Profile;
import com.vilt.talentos.entity.User;
import com.vilt.talentos.mapper.ProfileMapper;
import com.vilt.talentos.service.AdminService;
import com.vilt.talentos.service.ProfileService;
import com.vilt.talentos.service.ResourceEquipmentService;
import com.vilt.talentos.service.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
class AdminControllerTest extends BaseControllerTest {

    @MockBean
    private ProfileService profileService;

    @MockBean
    private AdminService adminService;

    @MockBean
    private ResourceService resourceService;

    @MockBean
    private ResourceEquipmentService equipmentService;

    @MockBean
    private ProfileMapper profileMapper;

    private Profile profile;
    private ProfileResponse profileResponse;

    @BeforeEach
    void setUp() {
        UUID id = UUID.randomUUID();
        profile = Profile.builder().id(id).status(DomainStatus.ACTIVE).user(User.builder().name("Test").build()).build();
        profileResponse = ProfileResponseFixtures.basic(id, "Test", "test@test.com", "Group", null, DomainStatus.ACTIVE);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve listar todos os perfis com sucesso")
    void all_Success() throws Exception {
        when(profileService.getAllWithFilters(isNull(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(profile)));
        when(profileMapper.toResponse(any(Profile.class))).thenReturn(profileResponse);

        mockMvc.perform(get("/api/v1/admin/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.content[0].name").value("Test"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve listar perfis ativos com sucesso")
    void ativos_Success() throws Exception {
        when(profileService.getAllWithFilters(eq(DomainStatus.ACTIVE), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(profile)));
        when(profileMapper.toResponse(any(Profile.class))).thenReturn(profileResponse);

        mockMvc.perform(get("/api/v1/admin/profiles/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve listar perfis pendentes com sucesso")
    void pending_Success() throws Exception {
        profile.setStatus(DomainStatus.PENDING);
        ProfileResponse pendingResponse = ProfileResponseFixtures.basic(
                profile.getId(), "Test", "test@test.com", "Group", null, DomainStatus.PENDING);
        
        when(profileService.getByStatus(eq(DomainStatus.PENDING), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(profile)));
        when(profileMapper.toResponse(any(Profile.class))).thenReturn(pendingResponse);

        mockMvc.perform(get("/api/v1/admin/profiles/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve buscar perfil por id com sucesso")
    void getById_Success() throws Exception {
        UUID id = profile.getId();
        when(profileService.getById(id)).thenReturn(profile);
        when(profileMapper.toResponse(profile)).thenReturn(profileResponse);

        mockMvc.perform(get("/api/v1/admin/profiles/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve atualizar perfil com sucesso")
    void update_Success() throws Exception {
        UUID id = profile.getId();
        AdminUpdateRequest req = AdminUpdateRequestFixtures.withStatusAndSkills(
                "ACTIVE", "SENIOR", null, null);
        
        when(profileService.adminUpdate(eq(id), any(AdminUpdateRequest.class))).thenReturn(profile);
        when(profileMapper.toResponse(profile)).thenReturn(profileResponse);

        mockMvc.perform(patch("/api/v1/admin/profiles/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve obter KPIs do dashboard com sucesso")
    void dashboard_Success() throws Exception {
        DashboardKpisResponse response = new DashboardKpisResponse(
                10, 5L, 5L, List.of(), java.util.Map.of());
        when(adminService.getDashboardKpis()).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve listar usuários pendentes com sucesso")
    void getPendingUsers_Success() throws Exception {
        User user = User.builder().id(UUID.randomUUID()).name("User Test").status(DomainStatus.PENDING).build();
        when(adminService.getPendingUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/v1/admin/users/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("User Test"));
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "00000000-0000-0000-0000-000000000001")
    @DisplayName("Deve aprovar usuário com sucesso")
    void approveUser_Success() throws Exception {
        UUID id = UUID.randomUUID();
        
        mockMvc.perform(post("/api/v1/admin/users/{id}/approve", id)
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(adminService).approveUser(eq(id), any(UUID.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve rejeitar usuário com sucesso")
    void rejectUser_Success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/admin/users/{id}/reject", id)
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(adminService).rejectUser(id);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve cadastrar recurso com sucesso")
    void createResource_Success() throws Exception {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CreateResourceResponse response = new CreateResourceResponse(profileId, userId, "João Silva", "joao@vilt-group.com");
        when(resourceService.createByAdmin(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/resources")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "João Silva",
                                  "email": "joao@vilt-group.com",
                                  "cpf": "12345678901",
                                  "groupId": "%s"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("João Silva"))
                .andExpect(jsonPath("$.email").value("joao@vilt-group.com"));

        verify(resourceService).createByAdmin(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Deve negar acesso para role não-ADMIN")
    void accessDenied_NonAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/profiles"))
                .andExpect(status().isForbidden());
    }
}
