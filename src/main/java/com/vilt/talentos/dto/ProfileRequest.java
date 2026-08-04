package com.vilt.talentos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProfileRequest(
    // Identificação
    String photoUrl,

    String jobTitle,

    // Perfil Técnico
    @NotBlank(message = "Área é obrigatória.")
    String area,

    String skillsText,

    @Size(max = 2000)
    String about,

    String stackReadiness,

    // Alocação e Potencial
    String allocationStatus,
    Integer mentorshipLevel,
    String autonomy,
    String careerPath,
    String certificationsCount,
    String monitoringLevel,

    // Code Review (Matriz de Conhecimentos)
    String codeReviewRole,

    // Extras
    Integer experienceYears,
    Integer projectsCount,
    String availability,
    String certifications,
    String linkedinUrl,
    String githubUrl,

    String contact,
    String contactEmail,
    String phone,
    String address,
    String postalCode,
    String cityState,

    List<SkillEntry> skills
) {
}
