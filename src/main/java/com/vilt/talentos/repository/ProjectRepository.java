package com.vilt.talentos.repository;

import com.vilt.talentos.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Page<Project> findByActive(boolean active, Pageable pageable);

    @EntityGraph(attributePaths = {"squads"})
    Optional<Project> findWithSquadsById(UUID id);

    @Override
    Page<Project> findAll(Pageable pageable);
}

