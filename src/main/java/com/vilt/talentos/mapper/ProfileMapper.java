package com.vilt.talentos.mapper;

import com.vilt.talentos.dto.AdminUpdateRequest;
import com.vilt.talentos.dto.ProfileRequest;
import com.vilt.talentos.dto.ProfileResponse;
import com.vilt.talentos.dto.ProfileSkillResponse;
import com.vilt.talentos.entity.Profile;
import com.vilt.talentos.entity.ProfileSkill;
import org.mapstruct.*;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ProfileMapper {

    @Mapping(target = "name", source = "user.name")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "groupName", source = "user.group.name")
    ProfileResponse toResponse(Profile profile);

    @Mapping(target = "name", source = "skill.name")
    @Mapping(target = "type", source = "skill.type")
    ProfileSkillResponse toSkillResponse(ProfileSkill profileSkill);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "level", ignore = true)
    @Mapping(target = "levelScore", ignore = true)
    @Mapping(target = "levelJustification", ignore = true)
    @Mapping(target = "skills", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "levelOverride", ignore = true)
    @Mapping(target = "registrationNumber", ignore = true)
    @Mapping(target = "registrationStatus", ignore = true)
    Profile toEntity(ProfileRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "level", ignore = true)
    @Mapping(target = "levelScore", ignore = true)
    @Mapping(target = "levelJustification", ignore = true)
    @Mapping(target = "skills", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "levelOverride", ignore = true)
    @Mapping(target = "registrationNumber", ignore = true)
    @Mapping(target = "registrationStatus", ignore = true)
    void updateEntity(ProfileRequest request, @MappingTarget Profile entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "level", ignore = true)
    @Mapping(target = "levelScore", ignore = true)
    @Mapping(target = "levelJustification", ignore = true)
    @Mapping(target = "skills", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "registrationStatus", ignore = true)
    void updateEntityFromAdmin(AdminUpdateRequest request, @MappingTarget Profile entity);
}
