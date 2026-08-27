package com.vilt.talentos.service;

import com.vilt.talentos.config.AppProperties;
import com.vilt.talentos.dto.CreateResourceRequest;
import com.vilt.talentos.entity.DomainStatus;
import com.vilt.talentos.entity.Group;
import com.vilt.talentos.entity.Profile;
import com.vilt.talentos.entity.RegistrationStatus;
import com.vilt.talentos.entity.ResourceStatus;
import com.vilt.talentos.entity.User;
import com.vilt.talentos.entity.UserRole;
import com.vilt.talentos.exception.BadRequestException;
import com.vilt.talentos.repository.GroupRepository;
import com.vilt.talentos.repository.ProfileRepository;
import com.vilt.talentos.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private UserRepository userRepo;
    @Mock
    private ProfileRepository profileRepo;
    @Mock
    private GroupRepository groupRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private ResourceService resourceService;

    @Test
    void createByAdmin_ShouldCreatePendingProfileAndSendWelcomeEmail() {
        UUID groupId = UUID.randomUUID();
        Group group = Group.builder().id(groupId).name("Delivery").build();
        CreateResourceRequest request = new CreateResourceRequest(
                "João Silva",
                "joao@vilt-group.com",
                "12345678901",
                groupId
        );

        when(appProperties.getAllowedEmailDomain()).thenReturn("vilt-group.com");
        when(userRepo.findByEmailIgnoreCase("joao@vilt-group.com")).thenReturn(Optional.empty());
        when(profileRepo.existsByCpf("12345678901")).thenReturn(false);
        when(groupRepo.findById(groupId)).thenReturn(Optional.of(group));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(profileRepo.save(any(Profile.class))).thenAnswer(invocation -> {
            Profile profile = invocation.getArgument(0);
            profile.setId(UUID.randomUUID());
            return profile;
        });
        when(appProperties.getUrl()).thenReturn("http://localhost:5173");

        var response = resourceService.createByAdmin(request);

        assertEquals("João Silva", response.name());
        assertEquals("joao@vilt-group.com", response.email());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(UserRole.RESOURCE, savedUser.getRole());
        assertEquals(DomainStatus.ACTIVE, savedUser.getStatus());
        assertTrue(savedUser.isEmailVerified());

        ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);
        verify(profileRepo).save(profileCaptor.capture());
        Profile savedProfile = profileCaptor.getValue();
        assertEquals("12345678901", savedProfile.getCpf());
        assertEquals(DomainStatus.PENDING, savedProfile.getStatus());
        assertEquals(ResourceStatus.AVAILABLE, savedProfile.getResourceStatus());
        assertEquals(RegistrationStatus.NOT_REQUIRED, savedProfile.getRegistrationStatus());

        verify(emailService).sendResourceWelcomeEmail(
                eq("joao@vilt-group.com"),
                eq("João Silva"),
                eq("joao@vilt-group.com"),
                anyString(),
                eq("http://localhost:5173/login")
        );
    }

    @Test
    void createByAdmin_WhenEmailAlreadyExists_ShouldThrowBadRequest() {
        when(appProperties.getAllowedEmailDomain()).thenReturn("vilt-group.com");
        when(userRepo.findByEmailIgnoreCase("joao@vilt-group.com"))
                .thenReturn(Optional.of(User.builder().email("joao@vilt-group.com").build()));

        CreateResourceRequest request = new CreateResourceRequest(
                "João Silva",
                "joao@vilt-group.com",
                "12345678901",
                UUID.randomUUID()
        );

        assertThrows(BadRequestException.class, () -> resourceService.createByAdmin(request));
    }

    @Test
    void createByAdmin_WithFormattedCpf_ShouldPersistNormalizedCpf() {
        UUID groupId = UUID.randomUUID();
        Group group = Group.builder().id(groupId).name("Delivery").build();
        CreateResourceRequest request = new CreateResourceRequest(
                "João Silva",
                "joao@vilt-group.com",
                "123.456.789-01",
                groupId
        );

        when(appProperties.getAllowedEmailDomain()).thenReturn("vilt-group.com");
        when(userRepo.findByEmailIgnoreCase("joao@vilt-group.com")).thenReturn(Optional.empty());
        when(profileRepo.existsByCpf("12345678901")).thenReturn(false);
        when(groupRepo.findById(groupId)).thenReturn(Optional.of(group));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(profileRepo.save(any(Profile.class))).thenAnswer(invocation -> {
            Profile profile = invocation.getArgument(0);
            profile.setId(UUID.randomUUID());
            return profile;
        });
        when(appProperties.getUrl()).thenReturn("http://localhost:5173");

        resourceService.createByAdmin(request);

        ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);
        verify(profileRepo).save(profileCaptor.capture());
        assertEquals("12345678901", profileCaptor.getValue().getCpf());
    }

    @Test
    void createByAdmin_WhenCpfAlreadyExists_ShouldThrowBadRequest() {
        when(appProperties.getAllowedEmailDomain()).thenReturn("vilt-group.com");
        when(userRepo.findByEmailIgnoreCase("joao@vilt-group.com")).thenReturn(Optional.empty());
        when(profileRepo.existsByCpf("12345678901")).thenReturn(true);

        CreateResourceRequest request = new CreateResourceRequest(
                "João Silva",
                "joao@vilt-group.com",
                "12345678901",
                UUID.randomUUID()
        );

        assertThrows(BadRequestException.class, () -> resourceService.createByAdmin(request));
    }

    @Test
    void createByAdmin_WhenCpfIsInvalid_ShouldThrowBadRequest() {
        when(appProperties.getAllowedEmailDomain()).thenReturn("vilt-group.com");

        CreateResourceRequest request = new CreateResourceRequest(
                "João Silva",
                "joao@vilt-group.com",
                "123",
                UUID.randomUUID()
        );

        assertThrows(BadRequestException.class, () -> resourceService.createByAdmin(request));
    }
}
