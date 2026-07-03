package com.vilt.talentos.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "maquinas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Maquina {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Column(name = "tag_numero_serie")
    private String tagNumeroSerie;

    @Column(name = "hostname")
    private String hostname;

    @Column(name = "numero_ativo")
    private String numeroAtivo;

    @Column(name = "marca_sistema_operacional")
    private String marcaSistemaOperacional;

    @Column(name = "processador")
    private String processador;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_maquina", nullable = false)
    @Builder.Default
    private StatusMaquina statusMaquina = StatusMaquina.VAZIO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
