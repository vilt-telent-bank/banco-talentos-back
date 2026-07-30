package com.vilt.talentos.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public record ProfileSelfUpdateRequest(
        String photoUrl,
        String jobTitle,
        String area,
        @Size(max = 2000) String about,
        Integer experienceYears,
        String linkedinUrl,
        String githubUrl,
        String registrationNumber,
        String contato,
        String contactEmail,
        String telefone,
        String endereco,
        String cep,
        String cidadeUf,
        List<SkillEntry> skills
) {
}
