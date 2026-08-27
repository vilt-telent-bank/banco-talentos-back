package com.vilt.talentos.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
public record AdminUpdateRequest(
    String status,
    String levelOverride,
    String jobTitle,
    String area,
    String about,
    String stackReadiness,
    String allocationStatus,
    Integer mentorshipLevel,
    String autonomy,
    String careerPath,
    String certificationsCount,
    String monitoringLevel,
    String linkedinUrl,
    String githubUrl,
    String availability,
    String codeReviewRole,
    String registrationNumber,
    String registrationStatus,
    LocalDate registrationRequestedAt,
    String registrationNotes,
    Boolean hasClientMachine,
    String contractingArea,
    String costCenter,
    LocalDate projectEntryDate,
    Boolean billable,
    Boolean portoOnboarding,
    String projectManagerName,
    UUID allocationProjectId,
    UUID allocationSquadId,
    String technicalProposalStatus,
    String technicalProposalNumber,
    LocalDate technicalProposalSentAt,
    String technicalProposalNotes,
    String contact,
    String contactEmail,
    String phone,
    String address,
    String postalCode,
    String cityState,
    Integer experienceYears,
    UUID groupId,
    List<SkillEntry> skills,
    List<SkillEntry> softSkills
) {
}
