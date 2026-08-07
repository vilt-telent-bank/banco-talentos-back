package com.vilt.talentos.repository;

import com.vilt.talentos.entity.DomainStatus;
import com.vilt.talentos.entity.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID>, JpaSpecificationExecutor<Profile> {
    @EntityGraph(attributePaths = {"user", "skills", "skills.skill"})
    Optional<Profile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    boolean existsByCpf(String cpf);

    @EntityGraph(attributePaths = {"user", "skills", "skills.skill"})
    Page<Profile> findByStatus(DomainStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"user", "skills", "skills.skill"})
    Page<Profile> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"user", "skills", "skills.skill"})
    java.util.List<Profile> findAll();
}
