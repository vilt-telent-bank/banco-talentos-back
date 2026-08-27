package com.vilt.talentos.dto;

import com.vilt.talentos.entity.DomainStatus;
import com.vilt.talentos.entity.ResourceStatus;

import java.util.List;
import java.util.UUID;

public final class ProfileResponseFixtures {

    private ProfileResponseFixtures() {}

    public static ProfileResponse basic(UUID id, String name, String email, String groupName,
                                        String jobTitle, DomainStatus status) {
        return ProfileResponse.builder()
                .id(id)
                .name(name)
                .email(email)
                .groupName(groupName)
                .jobTitle(jobTitle)
                .resourceStatus(ResourceStatus.AVAILABLE)
                .hasClientMachine(false)
                .equipments(List.of())
                .status(status)
                .skills(List.of())
                .build();
    }
}
