ALTER TABLE precos_confirmados
    DROP CONSTRAINT precos_confirmados_fornecedor_id_data_comercial_chave_item__key,
    ADD COLUMN ordem INTEGER DEFAULT 1 CHECK (ordem > 0),
    ADD COLUMN resolucao TEXT,
    ADD COLUMN preco_referencia_id BIGINT REFERENCES precos_confirmados(id),
    ADD COLUMN motivo_resolucao TEXT,
    ADD COLUMN revisao_conflito TEXT,
    ADD CONSTRAINT precos_confirmados_ordem_unica
        UNIQUE (fornecedor_id, data_comercial, chave_item, unidade, embalagem_chave, modalidade, ordem)
        DEFERRABLE INITIALLY DEFERRED,
    ADD CONSTRAINT precos_confirmados_resolucao_check CHECK (
        (resolucao IS NULL AND preco_referencia_id IS NULL AND motivo_resolucao IS NULL AND revisao_conflito IS NULL)
        OR
        (resolucao IS NOT NULL AND preco_referencia_id IS NOT NULL AND revisao_conflito IS NOT NULL AND (
            (resolucao IN ('ANTES', 'DEPOIS') AND motivo_resolucao IS NULL)
            OR (resolucao = 'SUBSTITUIR' AND motivo_resolucao IS NOT NULL AND length(trim(motivo_resolucao)) BETWEEN 1 AND 500)
        ))
    );

CREATE UNIQUE INDEX precos_confirmados_substituicao_unica
    ON precos_confirmados (preco_referencia_id) WHERE resolucao = 'SUBSTITUIR';
