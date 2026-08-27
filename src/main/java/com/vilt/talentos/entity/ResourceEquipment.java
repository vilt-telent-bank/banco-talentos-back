package com.vilt.talentos.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "resource_equipments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ResourceEquipment extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Column(length = 100)
    private String tag;

    @Column(length = 255)
    private String hostname;

    @Column(name = "asset_number", length = 100)
    private String assetNumber;

    @Column(name = "brand_os", length = 255)
    private String brandOs;

    @Column(length = 255)
    private String processor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private EquipmentStatus status = EquipmentStatus.EMPTY;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
