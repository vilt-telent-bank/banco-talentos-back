package com.vilt.talentos.repository;

import com.vilt.talentos.entity.Maquina;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MaquinaRepository extends JpaRepository<Maquina, UUID> {
    List<Maquina> findByProfileIdOrderByCreatedAtAsc(UUID profileId);
}
