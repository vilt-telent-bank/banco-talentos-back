package com.vilt.talentos.service;

import com.vilt.talentos.config.BrevoProperties;
import com.vilt.talentos.dto.BrevoDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final WebClient brevoWebClient;
    private final BrevoProperties brevoProperties;
    private final TemplateEngine templateEngine;

    @Async
    public void sendTemplatedEmail(List<String> recipients, String subject, String templateName, Map<String, Object> variables) {
        try {
            log.info("Processando template HTML '{}' para os destinatários: {}", templateName, recipients);

            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);

            sendHtmlEmail(recipients, subject, htmlContent);
        } catch (Exception e) {
            log.error("Erro ao processar o template '{}' para {}: {}", templateName, recipients, e.getMessage(), e);
        }
    }

    @Async
    public void sendHtmlEmail(List<String> recipients, String subject, String htmlContent) {
        log.info("Enviando e-mail para: {} com assunto: {}", recipients, subject);

        List<BrevoDtos.BrevoRecipient> toList = recipients.stream()
                .map(BrevoDtos.BrevoRecipient::new)
                .toList();

        BrevoDtos.BrevoEmailRequest request = new BrevoDtos.BrevoEmailRequest(
                new BrevoDtos.BrevoSender(brevoProperties.fromName(), brevoProperties.fromEmail()),
                toList,
                subject,
                htmlContent
        );

        brevoWebClient.post()
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(response -> log.info("E-mail enviado com sucesso para: {}", recipients))
                .doOnError(error -> log.error("Erro ao enviar e-mail para: {}. Erro: {}", recipients, error.getMessage(), error))
                .block();
    }

    @Async
    public void sendAdminApprovalConfirmedEmail(String toEmail, String userName, String portalUrl) {
        Map<String, Object> variables = Map.of(
            "userName", userName,
            "portalUrl", portalUrl
        );
        sendTemplatedEmail(List.of(toEmail), "ÍRIS | Acesso Aprovado", "emails/admin-approval-confirmed", variables);
    }

    @Async
    public void sendAdminNewProfileSubmissionEmail(List<String> adminEmails, String userName, String jobTitle, String experienceLevel, String portalUrl) {
        Map<String, Object> variables = Map.of(
            "userName", userName,
            "cargo", jobTitle != null ? jobTitle : "Não informado",
            "nivelIA", experienceLevel != null ? experienceLevel : "Não avaliado",
            "portalUrl", portalUrl
        );
        sendTemplatedEmail(adminEmails, "ÍRIS | Novo Perfil Pendente", "emails/admin-new-profile-submission", variables);
    }

    @Async
    public void sendResourceProfileApprovedEmail(String toEmail, String userName, String portalUrl) {
        Map<String, Object> variables = Map.of(
            "userName", userName,
            "portalUrl", portalUrl
        );
        sendTemplatedEmail(List.of(toEmail), "ÍRIS | Perfil Aprovado", "emails/resource-profile-approved", variables);
    }

    @Async
    public void sendResourceWelcomeEmail(String toEmail, String userName, String userEmail, String provisionalPassword, String loginUrl) {
        Map<String, Object> variables = Map.of(
            "userName", userName,
            "userEmail", userEmail,
            "provisionalPassword", provisionalPassword,
            "loginUrl", loginUrl
        );
        sendTemplatedEmail(
            List.of(toEmail),
            "ÍRIS | Suas credenciais de acesso",
            "emails/resource-welcome",
            variables
        );
    }
}
