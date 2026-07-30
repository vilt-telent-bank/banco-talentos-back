package com.vilt.talentos.dto;

import java.util.List;
import java.util.UUID;

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
    String contato,
    String contactEmail,
    String telefone,
    String endereco,
    String cep,
    String cidadeUf,
    Integer experienceYears,
    UUID groupId,
    List<SkillEntry> skills,
    List<SkillEntry> softSkills
) {
}
