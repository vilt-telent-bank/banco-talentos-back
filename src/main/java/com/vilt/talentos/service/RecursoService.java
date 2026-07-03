package com.vilt.talentos.service;

import com.vilt.talentos.dto.*;
import com.vilt.talentos.entity.*;
import com.vilt.talentos.exception.ResourceNotFoundException;
import com.vilt.talentos.repository.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecursoService {

    private final ProfileRepository profileRepo;
    private final UserRepository userRepo;
    private final MaquinaRepository maquinaRepo;
    private final MatriculaHistoricoRepository historicoRepo;

    // ── Consulta ─────────────────────────────────────────────────────────────

    public Page<RecursoResponse> listar(RecursoFilterParams f, Pageable pageable) {
        Specification<Profile> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (f.nome() != null && !f.nome().isBlank()) {
                String pattern = "%" + f.nome().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.join("user").get("name")), pattern),
                    cb.like(cb.lower(root.join("user").get("email")), pattern)
                ));
            }
            if (f.statusRecurso() != null) {
                predicates.add(cb.equal(root.get("statusRecurso"), f.statusRecurso()));
            }
            if (f.statusMatricula() != null) {
                predicates.add(cb.equal(root.get("statusMatricula"), f.statusMatricula()));
            }
            if (f.gerenteProjeto() != null && !f.gerenteProjeto().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("gerenteProjeto")), "%" + f.gerenteProjeto().toLowerCase() + "%"));
            }
            if (f.projeto() != null && !f.projeto().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("projetoAlocacao")), "%" + f.projeto().toLowerCase() + "%"));
            }
            if (f.billable() != null) {
                predicates.add(cb.equal(root.get("recursoBillable"), f.billable()));
            }
            if (f.onboarding() != null) {
                predicates.add(cb.equal(root.get("onboardingPortoRealizado"), f.onboarding()));
            }
            if (f.dataEntradaDe() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dataEntradaProjeto"), f.dataEntradaDe()));
            }
            if (f.dataEntradaAte() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dataEntradaProjeto"), f.dataEntradaAte()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return profileRepo.findAll(spec, pageable).map(this::toResponse);
    }

    public RecursoResponse buscar(UUID profileId) {
        var profile = findProfile(profileId);
        return toResponse(profile);
    }

    // ── Atualização de campos lifecycle (Seções 1, 3, 4) ────────────────────

    @Transactional
    public RecursoResponse atualizar(UUID profileId, RecursoUpdateRequest req) {
        var profile = findProfile(profileId);

        if (req.numeroMatricula() != null)          profile.setNumeroMatricula(req.numeroMatricula());
        if (req.dataSolicitacaoMatricula() != null)  profile.setDataSolicitacaoMatricula(req.dataSolicitacaoMatricula());
        if (req.observacoesMatricula() != null)      profile.setObservacoesMatricula(req.observacoesMatricula());
        if (req.possuiMaquinaCliente() != null)      profile.setPossuiMaquinaCliente(req.possuiMaquinaCliente());
        if (req.statusPropostaTecnica() != null)     profile.setStatusPropostaTecnica(req.statusPropostaTecnica());
        if (req.areaContratante() != null)           profile.setAreaContratante(req.areaContratante());
        if (req.centroCustoContratante() != null)    profile.setCentroCustoContratante(req.centroCustoContratante());
        if (req.dataEntradaProjeto() != null)        profile.setDataEntradaProjeto(req.dataEntradaProjeto());
        if (req.recursoBillable() != null)           profile.setRecursoBillable(req.recursoBillable());
        if (req.onboardingPortoRealizado() != null)  profile.setOnboardingPortoRealizado(req.onboardingPortoRealizado());
        if (req.gerenteProjeto() != null)            profile.setGerenteProjeto(req.gerenteProjeto());
        if (req.projetoAlocacao() != null)           profile.setProjetoAlocacao(req.projetoAlocacao());
        if (req.squadAlocacao() != null)             profile.setSquadAlocacao(req.squadAlocacao());

        return toResponse(profileRepo.save(profile));
    }

    // ── Atualização de status de matrícula com automação (RN003 + RN005) ────

    @Transactional
    public RecursoResponse atualizarStatusMatricula(UUID profileId, StatusMatricula novoStatus, UUID userId) {
        var profile = findProfile(profileId);
        var user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        StatusMatricula anterior = profile.getStatusMatricula();

        // RN003: deriva statusRecurso a partir do statusMatricula
        StatusRecurso novoStatusRecurso = derivarStatusRecurso(novoStatus);

        profile.setStatusMatricula(novoStatus);
        profile.setStatusRecurso(novoStatusRecurso);
        profileRepo.save(profile);

        // RN005: registra histórico
        var historico = MatriculaHistorico.builder()
                .profile(profile)
                .valorAnterior(anterior != null ? anterior.name() : null)
                .valorNovo(novoStatus.name())
                .alteradoPor(user)
                .build();
        historicoRepo.save(historico);

        return toResponse(profile);
    }

    // ── Seção 5 — Contato/Endereço (editável pelo próprio recurso) ──────────

    @Transactional
    public RecursoResponse atualizarContato(UUID profileId, ContatoUpdateRequest req) {
        var profile = findProfile(profileId);
        if (req.contato() != null)  profile.setContato(req.contato());
        if (req.endereco() != null) profile.setEndereco(req.endereco());
        return toResponse(profileRepo.save(profile));
    }

    @Transactional
    public RecursoResponse atualizarContatoPorUserId(UUID userId, ContatoUpdateRequest req) {
        var profile = profileRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado para o usuário."));
        if (req.contato() != null)  profile.setContato(req.contato());
        if (req.endereco() != null) profile.setEndereco(req.endereco());
        return toResponse(profileRepo.save(profile));
    }

    // ── Histórico de matrícula ───────────────────────────────────────────────

    public List<MatriculaHistoricoResponse> listarHistorico(UUID profileId) {
        findProfile(profileId);
        return historicoRepo.findByProfileIdOrderByAlteradoEmDesc(profileId)
                .stream()
                .map(h -> new MatriculaHistoricoResponse(
                        h.getId(),
                        h.getValorAnterior(),
                        h.getValorNovo(),
                        h.getAlteradoPor() != null ? h.getAlteradoPor().getName() : null,
                        h.getAlteradoEm()
                ))
                .toList();
    }

    // ── Máquinas ─────────────────────────────────────────────────────────────

    @Transactional
    public MaquinaResponse adicionarMaquina(UUID profileId, MaquinaRequest req) {
        var profile = findProfile(profileId);
        var maquina = Maquina.builder()
                .profile(profile)
                .tagNumeroSerie(req.tagNumeroSerie())
                .hostname(req.hostname())
                .numeroAtivo(req.numeroAtivo())
                .marcaSistemaOperacional(req.marcaSistemaOperacional())
                .processador(req.processador())
                .statusMaquina(req.statusMaquina())
                .build();
        return toMaquinaResponse(maquinaRepo.save(maquina));
    }

    @Transactional
    public MaquinaResponse atualizarMaquina(UUID maquinaId, MaquinaRequest req) {
        var maquina = maquinaRepo.findById(maquinaId)
                .orElseThrow(() -> new ResourceNotFoundException("Máquina não encontrada."));

        if (req.tagNumeroSerie() != null)         maquina.setTagNumeroSerie(req.tagNumeroSerie());
        if (req.hostname() != null)               maquina.setHostname(req.hostname());
        if (req.numeroAtivo() != null)            maquina.setNumeroAtivo(req.numeroAtivo());
        if (req.marcaSistemaOperacional() != null) maquina.setMarcaSistemaOperacional(req.marcaSistemaOperacional());
        if (req.processador() != null)            maquina.setProcessador(req.processador());
        if (req.statusMaquina() != null)          maquina.setStatusMaquina(req.statusMaquina());

        return toMaquinaResponse(maquinaRepo.save(maquina));
    }

    @Transactional
    public void removerMaquina(UUID maquinaId) {
        if (!maquinaRepo.existsById(maquinaId)) {
            throw new ResourceNotFoundException("Máquina não encontrada.");
        }
        maquinaRepo.deleteById(maquinaId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Profile findProfile(UUID profileId) {
        return profileRepo.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurso não encontrado."));
    }

    /**
     * RN003: LIBERADA → ALOCADO | qualquer outro ativo → AGUARDANDO | NAO_NECESSARIO → DISPONIVEL
     */
    private StatusRecurso derivarStatusRecurso(StatusMatricula statusMatricula) {
        return switch (statusMatricula) {
            case LIBERADA -> StatusRecurso.ALOCADO;
            case NAO_NECESSARIO -> StatusRecurso.DISPONIVEL;
            default -> StatusRecurso.AGUARDANDO;
        };
    }

    private RecursoResponse toResponse(Profile p) {
        List<MaquinaResponse> maquinas = maquinaRepo.findByProfileIdOrderByCreatedAtAsc(p.getId())
                .stream().map(this::toMaquinaResponse).toList();

        return new RecursoResponse(
                p.getId(),
                p.getUser().getName(),
                p.getUser().getEmail(),
                p.getPhotoUrl(),
                p.getJobTitle(),
                p.getArea(),
                p.getStatus(),
                p.getStatusRecurso(),
                p.getStatusMatricula(),
                p.getNumeroMatricula(),
                p.getDataSolicitacaoMatricula(),
                p.getObservacoesMatricula(),
                p.isPossuiMaquinaCliente(),
                maquinas,
                p.getStatusPropostaTecnica(),
                p.getAreaContratante(),
                p.getCentroCustoContratante(),
                p.getDataEntradaProjeto(),
                p.getRecursoBillable(),
                p.getOnboardingPortoRealizado(),
                p.getGerenteProjeto(),
                p.getProjetoAlocacao(),
                p.getSquadAlocacao(),
                p.getContato(),
                p.getEndereco(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    private MaquinaResponse toMaquinaResponse(Maquina m) {
        return new MaquinaResponse(
                m.getId(),
                m.getTagNumeroSerie(),
                m.getHostname(),
                m.getNumeroAtivo(),
                m.getMarcaSistemaOperacional(),
                m.getProcessador(),
                m.getStatusMaquina(),
                m.getCreatedAt(),
                m.getUpdatedAt()
        );
    }
}
