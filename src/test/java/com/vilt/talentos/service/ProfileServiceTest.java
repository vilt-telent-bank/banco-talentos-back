package com.vilt.talentos.service;

import com.vilt.talentos.config.AppProperties;
import com.vilt.talentos.dto.AdminUpdateRequest;
import com.vilt.talentos.dto.AdminUpdateRequestFixtures;
import com.vilt.talentos.dto.ProfileRequest;
import com.vilt.talentos.dto.SkillEntry;
import com.vilt.talentos.entity.DomainStatus;
import com.vilt.talentos.entity.ExperienceLevel;
import com.vilt.talentos.entity.Profile;
import com.vilt.talentos.entity.ProfileSkill;
import com.vilt.talentos.entity.Project;
import com.vilt.talentos.entity.RegistrationStatus;
import com.vilt.talentos.entity.ResourceStatus;
import com.vilt.talentos.entity.Skill;
import com.vilt.talentos.entity.SkillType;
import com.vilt.talentos.entity.User;
import com.vilt.talentos.mapper.ProfileMapper;
import com.vilt.talentos.repository.GroupRepository;
import com.vilt.talentos.repository.ProfileRepository;
import com.vilt.talentos.repository.ProjectRepository;
import com.vilt.talentos.repository.SkillRepository;
import com.vilt.talentos.repository.SquadRepository;
import com.vilt.talentos.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private TalentEvaluationService evaluationService;
    @Mock
    private ProfileMapper profileMapper;
    @Mock
    private EmailService emailService;
    @Mock
    private AppProperties appProperties;
    @Mock
    private SkillRepository skillRepo;
    @Mock
    private GroupRepository groupRepo;
    @Mock
    private ProjectRepository projectRepo;
    @Mock
    private SquadRepository squadRepo;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void createOrUpdate_WhenProfileIsActive_ShouldBecomePending() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("test@vilt-group.com").build();
        Profile profile = Profile.builder().user(user).status(DomainStatus.ACTIVE).build();
        ProfileRequest req = new ProfileRequest(
                null, "Dev", "IT",
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null,
                null
        );

        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(profileRepo.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(userRepo.findAllByRoleAndStatus(any(), any(), any())).thenReturn(org.springframework.data.domain.Page.empty());
        when(evaluationService.evaluate(any())).thenReturn(new TalentEvaluationService.Evaluation(ExperienceLevel.PLENO, 50, "Justification"));
        when(profileRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Profile result = profileService.createOrUpdate(userId, req);

        assertEquals(DomainStatus.PENDING, result.getStatus(), "Profile status should be PENDING after update by resource");
    }

    @Test
    void adminUpdate_reconcilesHardAndSoftSkills_savesWithoutDuplicates() {
        UUID profileId = UUID.randomUUID();
        User user = User.builder().email("test@vilt-group.com").build();
        
        Skill skill = Skill.builder()
                .id(UUID.randomUUID())
                .name("ADAPTABILIDADE E FLEXIBILIDADE")
                .type(SkillType.HARD)
                .build();
                
        ProfileSkill ps = ProfileSkill.builder()
                .skill(skill)
                .proficiencyLevel(5)
                .build();
                
        Profile profile = Profile.builder()
                .id(profileId)
                .user(user)
                .status(DomainStatus.ACTIVE)
                .skills(new java.util.ArrayList<>(java.util.List.of(ps)))
                .build();
        ps.setProfile(profile);

        AdminUpdateRequest req = AdminUpdateRequestFixtures.withStatusAndSkills(
                "ACTIVE", "SENIOR",
                java.util.List.of(new SkillEntry("TYPESCRIPT", 7)),
                java.util.List.of(new SkillEntry("ADAPTABILIDADE E FLEXIBILIDADE", 10))
        );

        when(profileRepo.findById(profileId)).thenReturn(Optional.of(profile));
        when(skillRepo.findByName("TYPESCRIPT")).thenReturn(Optional.empty());
        when(skillRepo.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileRepo.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        Profile result = profileService.adminUpdate(profileId, req);

        assertEquals(SkillType.SOFT, skill.getType());
        assertEquals(10, ps.getProficiencyLevel());
        
        assertEquals(2, result.getSkills().size());
        boolean hasTypeScript = result.getSkills().stream()
                .anyMatch(pSkill -> pSkill.getSkill().getName().equals("TYPESCRIPT") && pSkill.getSkill().getType() == SkillType.HARD);
        org.junit.jupiter.api.Assertions.assertTrue(hasTypeScript);
    }

    @Test
    void adminUpdate_shouldSetWaitingWhenRegistrationIsInProgress() {
        UUID profileId = UUID.randomUUID();
        Profile profile = Profile.builder()
                .id(profileId)
                .user(User.builder().email("test@vilt-group.com").build())
                .status(DomainStatus.ACTIVE)
                .registrationStatus(RegistrationStatus.NOT_REQUIRED)
                .resourceStatus(ResourceStatus.AVAILABLE)
                .skills(new java.util.ArrayList<>())
                .build();

        AdminUpdateRequest req = AdminUpdateRequestFixtures.withRegistrationStatus("REQUESTED_VIA_TICKET");

        when(profileRepo.findById(profileId)).thenReturn(Optional.of(profile));
        when(profileRepo.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        Profile result = profileService.adminUpdate(profileId, req);

        assertEquals(RegistrationStatus.REQUESTED_VIA_TICKET, result.getRegistrationStatus());
        assertEquals(ResourceStatus.WAITING, result.getResourceStatus());
    }

    @Test
    void adminUpdate_ca009_shouldSetWaitingForAnyRegistrationOtherThanNotRequiredExceptReleased() {
        for (String status : java.util.List.of(
                "REQUESTED_VIA_TICKET",
                "TICKET_AWAITING_APPROVAL",
                "TICKET_AWAITING_SERVICE"
        )) {
            UUID profileId = UUID.randomUUID();
            Profile profile = Profile.builder()
                    .id(profileId)
                    .user(User.builder().email("test@vilt-group.com").build())
                    .status(DomainStatus.ACTIVE)
                    .registrationStatus(RegistrationStatus.NOT_REQUIRED)
                    .resourceStatus(ResourceStatus.AVAILABLE)
                    .skills(new java.util.ArrayList<>())
                    .build();

            when(profileRepo.findById(profileId)).thenReturn(Optional.of(profile));
            when(profileRepo.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

            Profile result = profileService.adminUpdate(
                    profileId,
                    AdminUpdateRequestFixtures.withRegistrationStatus(status)
            );

            assertEquals(ResourceStatus.WAITING, result.getResourceStatus(),
                    "CA009: matrícula " + status + " deve gerar Status do Recurso Aguardando");
        }
    }

    @Test
    void adminUpdate_ca010_shouldSetAllocatedWhenRegistrationIsReleased() {
        UUID profileId = UUID.randomUUID();
        Profile profile = Profile.builder()
                .id(profileId)
                .user(User.builder().email("test@vilt-group.com").build())
                .status(DomainStatus.ACTIVE)
                .registrationStatus(RegistrationStatus.REQUESTED_VIA_TICKET)
                .resourceStatus(ResourceStatus.WAITING)
                .skills(new java.util.ArrayList<>())
                .build();

        AdminUpdateRequest req = AdminUpdateRequestFixtures.withRegistrationStatus("RELEASED");

        when(profileRepo.findById(profileId)).thenReturn(Optional.of(profile));
        when(profileRepo.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        Profile result = profileService.adminUpdate(profileId, req);

        assertEquals(RegistrationStatus.RELEASED, result.getRegistrationStatus());
        assertEquals(ResourceStatus.ALLOCATED, result.getResourceStatus(),
                "CA010: matrícula Liberada deve gerar Status do Recurso Alocado");
    }

    @Test
    void adminUpdate_ca011_shouldSetAvailableWhenRegistrationReturnsToNotRequired() {
        UUID profileId = UUID.randomUUID();
        Profile profile = Profile.builder()
                .id(profileId)
                .user(User.builder().email("test@vilt-group.com").build())
                .status(DomainStatus.ACTIVE)
                .registrationStatus(RegistrationStatus.RELEASED)
                .resourceStatus(ResourceStatus.ALLOCATED)
                .skills(new java.util.ArrayList<>())
                .build();

        AdminUpdateRequest req = AdminUpdateRequestFixtures.withRegistrationStatus("NOT_REQUIRED");

        when(profileRepo.findById(profileId)).thenReturn(Optional.of(profile));
        when(profileRepo.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        Profile result = profileService.adminUpdate(profileId, req);

        assertEquals(RegistrationStatus.NOT_REQUIRED, result.getRegistrationStatus());
        assertEquals(ResourceStatus.AVAILABLE, result.getResourceStatus(),
                "CA011: matrícula Não Necessário deve retornar Status do Recurso para Disponível");
    }

    @Test
    void adminUpdate_ca011_shouldResetRegistrationAndResourceStatusOnProjectDeallocation() {
        UUID profileId = UUID.randomUUID();
        Project project = Project.builder().id(UUID.randomUUID()).name("Portal").build();
        Profile profile = Profile.builder()
                .id(profileId)
                .user(User.builder().email("test@vilt-group.com").build())
                .status(DomainStatus.ACTIVE)
                .registrationStatus(RegistrationStatus.RELEASED)
                .resourceStatus(ResourceStatus.ALLOCATED)
                .allocationProject(project)
                .skills(new java.util.ArrayList<>())
                .build();

        AdminUpdateRequest req = AdminUpdateRequestFixtures.withRegistrationStatus("RELEASED");

        when(profileRepo.findById(profileId)).thenReturn(Optional.of(profile));
        when(profileRepo.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        Profile result = profileService.adminUpdate(profileId, req);

        assertEquals(null, result.getAllocationProject());
        assertEquals(RegistrationStatus.NOT_REQUIRED, result.getRegistrationStatus());
        assertEquals(ResourceStatus.AVAILABLE, result.getResourceStatus());
    }
}
