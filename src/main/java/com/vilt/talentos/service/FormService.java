package com.vilt.talentos.service;

import com.vilt.talentos.dto.FormCreateRequest;
import com.vilt.talentos.dto.FormDefinitionResponse;
import com.vilt.talentos.dto.FormListResponse;
import com.vilt.talentos.dto.FormUpdateRequest;
import com.vilt.talentos.entity.FormDefinition;
import com.vilt.talentos.exception.ResourceNotFoundException;
import com.vilt.talentos.mapper.FormMapper;
import com.vilt.talentos.repository.FormDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FormService {

    private final FormDefinitionRepository repository;
    private final FormMapper mapper;

    @Transactional
    public FormDefinitionResponse create(FormCreateRequest request) {
        log.info("Iniciando criação de um novo formulário.");

        var formDefinition = mapper.toEntity(request);
        repository.save(formDefinition);

        log.info("Formulário criado com sucesso. ID gerado: {}", formDefinition.getId());
        return mapper.toResponse(formDefinition);
    }

    public Page<FormListResponse> findAll(Pageable pagination) {
        return repository.findAll(pagination).map(mapper::toListResponse);
    }

    @Transactional
    public void update(FormUpdateRequest request) {
        log.info("Iniciando atualização do formulário ID: {}", request.id());

        var formDefinition = repository.findById(request.id())
                .orElseThrow(() -> {
                    log.warn("Falha ao atualizar: Formulário ID: {} não encontrado.", request.id());
                    return new ResourceNotFoundException("Formulário não encontrado.");
                });

        mapper.updateEntity(request, formDefinition);

        log.info("Formulário ID: {} atualizado com sucesso.", request.id());
    }

    public FormListResponse findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toListResponse)
                .orElseThrow(() -> {
                    log.warn("Falha ao buscar: Formulário ID: {} não encontrado.", id);
                    return new ResourceNotFoundException("Formulário não encontrado.");
                });
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Iniciando exclusão do formulário ID: {}", id);

        if (!repository.existsById(id)) {
            log.warn("Falha ao excluir: Formulário ID: {} não encontrado.", id);
            throw new ResourceNotFoundException("Formulário não encontrado.");
        }
        repository.deleteById(id);
        log.info("Formulário ID: {} excluído com sucesso.", id);
    }

    public java.util.List<FormDefinition> findAllByGroupId(UUID groupId) {
        return repository.findAllByGroup_Id(groupId);
    }

    public FormDefinition getReferenceById(UUID id) {
        return repository.getReferenceById(id);
    }
}
