package com.vilt.talentos.service;

import com.vilt.talentos.dto.FormListResponse;
import com.vilt.talentos.dto.FormSubmissionRequest;
import com.vilt.talentos.dto.FormSubmissionResponse;
import com.vilt.talentos.exception.ResourceNotFoundException;
import com.vilt.talentos.repository.FormDefinitionRepository;
import com.vilt.talentos.repository.FormSubmissionRepository;
import com.vilt.talentos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FormSubmissionService {

    private final FormSubmissionRepository formSubmissionRepository;
    private final FormDefinitionRepository formDefinitionRepository;
    private final UserRepository userRepository;
    private final com.vilt.talentos.mapper.FormMapper mapper;

    public List<FormListResponse> getFormsByUserGroup(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Busca de formulários por grupo falhou: Usuário ID: {} não encontrado.", userId);
                    return new ResourceNotFoundException("Usuário não encontrado.");
                });

        if (user.getGroup() == null) {
            log.debug("Usuário ID: {} não pertence a nenhum grupo.", userId);
            return List.of();
        }

        UUID groupId = user.getGroup().getId();
        return formDefinitionRepository.findAllByGroup_Id(groupId).stream()
                .map(mapper::toListResponse)
                .toList();
    }

    @Transactional
    public FormSubmissionResponse createSubmission(UUID userId, FormSubmissionRequest request) {
        log.info("Iniciando nova submissão de formulário pelo usuário ID: {}", userId);

        var formDefinition = formDefinitionRepository.findById(request.formDefinitionId())
                .orElseThrow(() -> {
                    log.warn("Falha ao criar submissão: Formulário ID: {} não encontrado.", request.formDefinitionId());
                    return new ResourceNotFoundException("Formulário não encontrado.");
                });

        var user = userRepository.getReferenceById(userId);

        var formSubmission = mapper.toEntity(request);
        formSubmission.setFormDefinition(formDefinition);
        formSubmission.setUser(user);

        formSubmissionRepository.save(formSubmission);

        log.info("Submissão do formulário '{}' criada com sucesso. ID da submissão: {}", formDefinition.getTitle(), formSubmission.getId());
        return mapper.toSubmissionResponse(formSubmission);
    }

    public FormSubmissionResponse getSubmissionById(UUID id) {
        var submission = formSubmissionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Busca falhou: Submissão de respostas ID: {} não encontrada.", id);
                    return new ResourceNotFoundException("Submissão de respostas não encontrada.");
                });
        return mapper.toSubmissionResponse(submission);
    }
}
