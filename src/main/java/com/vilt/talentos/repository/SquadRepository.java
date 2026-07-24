package com.vilt.talentos.repository;

import com.vilt.talentos.entity.Squad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SquadRepository extends JpaRepository<Squad, UUID> {
    @EntityGraph(attributePaths = {"project"})
    Page<Squad> findByActive(boolean active, Pageable pageable);

    @EntityGraph(attributePaths = {"project"})
    List<Squad> findByActiveAndProjectIsNull(boolean active);

    @EntityGraph(attributePaths = {"project"})
    List<Squad> findByProject_Id(UUID projectId);

    @Override
    @EntityGraph(attributePaths = {"project"})
    Page<Squad> findAll(Pageable pageable);
}
