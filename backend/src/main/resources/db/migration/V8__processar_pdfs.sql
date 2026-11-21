ALTER TABLE tentativas_importacao DROP CONSTRAINT tentativas_importacao_status_check;
ALTER TABLE tentativas_importacao ADD CONSTRAINT tentativas_importacao_status_check CHECK (
    status IN ('PENDENTE','REJEITADA','FALHA_ENVIO','DUPLICADA','AGUARDANDO_CONFIRMACAO',
               'PROCESSANDO','AGUARDANDO_CONFERENCIA','FALHA_EXTRACAO')
);

CREATE TABLE execucoes_extracao (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tentativa_id BIGINT NOT NULL REFERENCES tentativas_importacao(id),
    iniciada_em TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    expira_em TIMESTAMPTZ NOT NULL,
    finalizada_em TIMESTAMPTZ,
    motivo VARCHAR(300),
    previa JSONB
);

ALTER TABLE tentativas_importacao ADD COLUMN execucao_atual BIGINT REFERENCES execucoes_extracao(id);
CREATE INDEX importacoes_pendentes_idx ON tentativas_importacao(id) WHERE status = 'PENDENTE';
