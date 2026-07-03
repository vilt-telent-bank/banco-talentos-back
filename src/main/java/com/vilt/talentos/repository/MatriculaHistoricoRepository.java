package com.vilt.talentos.repository;

import com.vilt.talentos.entity.MatriculaHistorico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MatriculaHistoricoRepository extends JpaRepository<MatriculaHistorico, UUID> {
    List<MatriculaHistorico> findByProfileIdOrderByAlteradoEmDesc(UUID profileId);
}
