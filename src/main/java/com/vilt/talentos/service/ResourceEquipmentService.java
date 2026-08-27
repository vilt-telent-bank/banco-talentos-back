package com.vilt.talentos.service;

import com.vilt.talentos.dto.ResourceEquipmentRequest;
import com.vilt.talentos.entity.EquipmentStatus;
import com.vilt.talentos.entity.Profile;
import com.vilt.talentos.entity.ResourceEquipment;
import com.vilt.talentos.exception.BadRequestException;
import com.vilt.talentos.exception.ResourceNotFoundException;
import com.vilt.talentos.repository.ProfileRepository;
import com.vilt.talentos.repository.ResourceEquipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceEquipmentService {

    private final ResourceEquipmentRepository equipmentRepo;
    private final ProfileRepository profileRepo;

    @Transactional(readOnly = true)
    public List<ResourceEquipment> listByProfile(UUID profileId) {
        ensureProfileExists(profileId);
        return equipmentRepo.findByProfileIdOrderByCreatedAtAsc(profileId);
    }

    @Transactional
    public ResourceEquipment create(UUID profileId, ResourceEquipmentRequest req) {
        Profile profile = profileRepo.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado"));

        EquipmentStatus status = parseStatus(req.status());
        String notes = resolveNotes(status, req.notes());

        ResourceEquipment equipment = ResourceEquipment.builder()
                .profile(profile)
                .tag(req.tag())
                .hostname(req.hostname())
                .assetNumber(req.assetNumber())
                .brandOs(req.brandOs())
                .processor(req.processor())
                .status(status)
                .notes(notes)
                .build();

        ResourceEquipment saved = equipmentRepo.save(equipment);
        log.info("Equipamento {} criado para o perfil {}", saved.getId(), profileId);
        return saved;
    }

    @Transactional
    public ResourceEquipment update(UUID profileId, UUID equipmentId, ResourceEquipmentRequest req) {
        ResourceEquipment equipment = equipmentRepo.findByIdAndProfileId(equipmentId, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado"));

        if (req.tag() != null) equipment.setTag(req.tag());
        if (req.hostname() != null) equipment.setHostname(req.hostname());
        if (req.assetNumber() != null) equipment.setAssetNumber(req.assetNumber());
        if (req.brandOs() != null) equipment.setBrandOs(req.brandOs());
        if (req.processor() != null) equipment.setProcessor(req.processor());

        EquipmentStatus status = req.status() != null ? parseStatus(req.status()) : equipment.getStatus();
        if (req.status() != null) {
            equipment.setStatus(status);
        }
        equipment.setNotes(resolveNotes(status, req.notes()));

        ResourceEquipment saved = equipmentRepo.save(equipment);
        log.info("Equipamento {} atualizado no perfil {}", equipmentId, profileId);
        return saved;
    }

    @Transactional
    public void delete(UUID profileId, UUID equipmentId) {
        ResourceEquipment equipment = equipmentRepo.findByIdAndProfileId(equipmentId, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado"));
        equipmentRepo.delete(equipment);
        log.info("Equipamento {} removido do perfil {}", equipmentId, profileId);
    }

    private void ensureProfileExists(UUID profileId) {
        if (!profileRepo.existsById(profileId)) {
            throw new ResourceNotFoundException("Perfil não encontrado");
        }
    }

    private EquipmentStatus parseStatus(String status) {
        try {
            return EquipmentStatus.valueOf(status);
        } catch (Exception e) {
            throw new BadRequestException("Status de máquina inválido: " + status);
        }
    }

    private String resolveNotes(EquipmentStatus status, String notes) {
        if (status == EquipmentStatus.INACTIVE) {
            if (notes == null || notes.isBlank()) {
                throw new BadRequestException("A observação é obrigatória quando o status da máquina for Inativo.");
            }
            return notes.trim();
        }
        return null;
    }
}
