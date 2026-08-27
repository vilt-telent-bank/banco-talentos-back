package com.vilt.talentos.dto;

import java.util.List;

public final class AdminUpdateRequestFixtures {

    private AdminUpdateRequestFixtures() {}

    public static AdminUpdateRequest withStatusAndSkills(String status, String levelOverride,
                                                         List<SkillEntry> skills, List<SkillEntry> softSkills) {
        return AdminUpdateRequest.builder()
                .status(status)
                .levelOverride(levelOverride)
                .skills(skills)
                .softSkills(softSkills)
                .build();
    }

    public static AdminUpdateRequest withRegistrationStatus(String registrationStatus) {
        return AdminUpdateRequest.builder()
                .registrationStatus(registrationStatus)
                .build();
    }
}
