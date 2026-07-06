-- ── Ciclo de Vida do Recurso ─────────────────────────────────────────────────

-- Seção 1: Status automatizado e controle de matrícula
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS status_recurso       VARCHAR(20)  NOT NULL DEFAULT 'DISPONIVEL';
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS status_matricula     VARCHAR(60)  NOT NULL DEFAULT 'NAO_NECESSARIO';
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS numero_matricula     VARCHAR(100);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS data_solicitacao_matricula DATE;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS observacoes_matricula TEXT;

-- Seção 2: Máquina do cliente
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS possui_maquina_cliente BOOLEAN NOT NULL DEFAULT FALSE;

-- Seção 3: Proposta técnica
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS status_proposta_tecnica VARCHAR(60);

-- Seção 4: Dados da contratação
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS area_contratante          VARCHAR(200);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS centro_custo_contratante  VARCHAR(200);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS data_entrada_projeto       DATE;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS recurso_billable           BOOLEAN;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS onboarding_porto_realizado BOOLEAN;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS gerente_projeto            VARCHAR(200);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS projeto_alocacao           VARCHAR(200);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS squad_alocacao             VARCHAR(200);

-- Seção 5: Dados de contato e endereço (editável pelo próprio recurso)
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS contato  VARCHAR(200);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS endereco TEXT;

-- ── Máquinas do Cliente (grid 1:N) ───────────────────────────────────────────

CREATE TABLE IF NOT EXISTS maquinas (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id               UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    tag_numero_serie         VARCHAR(200),
    hostname                 VARCHAR(200),
    numero_ativo             VARCHAR(200),
    marca_sistema_operacional VARCHAR(200),
    processador              VARCHAR(200),
    status_maquina           VARCHAR(60) NOT NULL DEFAULT 'VAZIO',
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_maquinas_profile_id ON maquinas(profile_id);

-- ── Histórico de Alterações do Status da Matrícula (RN005 / CA005) ───────────

CREATE TABLE IF NOT EXISTS matricula_historico (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id     UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    valor_anterior VARCHAR(60),
    valor_novo     VARCHAR(60) NOT NULL,
    alterado_por   UUID REFERENCES users(id),
    alterado_em    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_matricula_historico_profile_id ON matricula_historico(profile_id);
