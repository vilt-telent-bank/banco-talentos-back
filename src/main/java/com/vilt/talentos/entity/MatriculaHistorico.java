package com.vilt.talentos.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "matricula_historico")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MatriculaHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Column(name = "valor_anterior")
    private String valorAnterior;

    @Column(name = "valor_novo", nullable = false)
    private String valorNovo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alterado_por")
    private User alteradoPor;

    @CreationTimestamp
    @Column(name = "alterado_em", updatable = false, nullable = false)
    private Instant alteradoEm;
}
