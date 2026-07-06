package com.vilt.talentos.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Profile extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Identificação
    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "job_title")
    private String jobTitle;

    // Perfil Técnico
    private String area;

    @Column(columnDefinition = "TEXT")
    private String about;

    @Column(name = "stack_readiness")
    private String stackReadiness;

    // Alocação e Potencial
    @Column(name = "allocation_status")
    private String allocationStatus;

    @Column(name = "mentorship_level")
    private Integer mentorshipLevel;

    @Column
    private String autonomy;

    @Column(name = "career_path")
    private String careerPath;

    @Column(name = "certifications_count")
    private String certificationsCount;

    @Column(name = "monitoring_level")
    private String monitoringLevel;

    // Avaliação IA
    @Column(name = "level")
    private String level;

    @Column(name = "level_override")
    private String levelOverride;

    @Column(name = "level_score")
    private Integer levelScore;

    @Column(name = "level_justification", columnDefinition = "TEXT")
    private String levelJustification;

    // Outros
    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "projects_count")
    private Integer projectsCount;

    private String availability;

    @Column(columnDefinition = "TEXT")
    private String certifications;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "github_url")
    private String githubUrl;

    @Column(name = "code_review_role")
    private String codeReviewRole;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status")
    @Builder.Default
    private RegistrationStatus registrationStatus = RegistrationStatus.NOT_REQUESTED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DomainStatus status = DomainStatus.PENDING;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProfileSkill> skills = new ArrayList<>();

    // ── Ciclo de Vida do Recurso ──────────────────────────────────────────────

    // Seção 1 — Status automatizado e matrícula
    @Enumerated(EnumType.STRING)
    @Column(name = "status_recurso", nullable = false)
    @Builder.Default
    private StatusRecurso statusRecurso = StatusRecurso.DISPONIVEL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_matricula", nullable = false)
    @Builder.Default
    private StatusMatricula statusMatricula = StatusMatricula.NAO_NECESSARIO;

    @Column(name = "numero_matricula")
    private String numeroMatricula;

    @Column(name = "data_solicitacao_matricula")
    private java.time.LocalDate dataSolicitacaoMatricula;

    @Column(name = "observacoes_matricula", columnDefinition = "TEXT")
    private String observacoesMatricula;

    // Seção 2 — Máquina do cliente
    @Column(name = "possui_maquina_cliente", nullable = false)
    @Builder.Default
    private boolean possuiMaquinaCliente = false;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Maquina> maquinas = new ArrayList<>();

    // Seção 3 — Proposta técnica
    @Enumerated(EnumType.STRING)
    @Column(name = "status_proposta_tecnica")
    private StatusPropostaTecnica statusPropostaTecnica;

    // Seção 4 — Dados da contratação
    @Column(name = "area_contratante")
    private String areaContratante;

    @Column(name = "centro_custo_contratante")
    private String centroCustoContratante;

    @Column(name = "data_entrada_projeto")
    private java.time.LocalDate dataEntradaProjeto;

    @Column(name = "recurso_billable")
    private Boolean recursoBillable;

    @Column(name = "onboarding_porto_realizado")
    private Boolean onboardingPortoRealizado;

    @Column(name = "gerente_projeto")
    private String gerenteProjeto;

    @Column(name = "projeto_alocacao")
    private String projetoAlocacao;

    @Column(name = "squad_alocacao")
    private String squadAlocacao;

    // Seção 5 — Contato e endereço (editável pelo próprio recurso)
    @Column(name = "contato")
    private String contato;

    @Column(name = "endereco", columnDefinition = "TEXT")
    private String endereco;
}
