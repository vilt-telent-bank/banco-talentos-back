package com.vilt.talentos.dto;

import com.vilt.talentos.entity.SkillCategory;
import com.vilt.talentos.entity.SkillType;

import java.util.List;
import java.util.UUID;

public record AdminSkillListResponse(
    UUID id,
    String name,
    SkillType type,
    boolean active,
    String description,
    SkillCategory category,
    long resourcesCount,
    double averageProficiency,
    List<AvatarResponse> avatars
) {}
