package com.vilt.talentos.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SkillCategory {
    FRONTEND("Frontend"),
    BACKEND("Backend"),
    DESIGN("Design"),
    QA("Qualidade"),
    MOBILE("Mobile"),
    DATA_SCIENCE("Ciência de Dados"),
    DEVOPS("DevOps"),
    MANAGEMENT("Gestão"),
    COMMUNICATION("Comunicação"),
    TEAMWORK("Trabalho em Equipe"),
    LEADERSHIP("Liderança"),
    EMOTIONAL_INTELLIGENCE("Inteligência Emocional"),
    PROBLEM_SOLVING("Resolução de Problemas"),
    CRITICAL_THINKING("Pensamento Crítico"),
    ADAPTABILITY("Adaptabilidade"),
    TIME_MANAGEMENT("Gestão de Tempo"),
    ORGANIZATION("Organização"),
    CREATIVITY("Criatividade"),
    PROACTIVITY("Proatividade"),
    NEGOTIATION("Negociação"),
    DECISION_MAKING("Tomada de Decisão"),
    EMPATHY("Empatia"),
    COLLABORATION("Colaboração"),
    CONTINUOUS_LEARNING("Aprendizado Contínuo"),
    RESULTS_ORIENTATION("Orientação para Resultados"),
    CONFLICT_MANAGEMENT("Gestão de Conflitos"),
    CUSTOMER_SERVICE("Atendimento ao Cliente"),
    INFLUENCE_AND_PERSUASION("Influência e Persuasão");

    private final String displayName;
}
