package com.vilt.talentos.repository;

import com.vilt.talentos.entity.ResourceEquipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResourceEquipmentRepository extends JpaRepository<ResourceEquipment, UUID> {
    List<ResourceEquipment> findByProfileIdOrderByCreatedAtAsc(UUID profileId);

    Optional<ResourceEquipment> findByIdAndProfileId(UUID id, UUID profileId);
}
