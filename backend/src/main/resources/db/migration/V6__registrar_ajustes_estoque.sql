ALTER TABLE produtos
    ADD COLUMN versao BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN unidade_bloqueada BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE historico_ajustes_estoque (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    produto_id BIGINT NOT NULL REFERENCES produtos(id),
    ator_id BIGINT NOT NULL REFERENCES usuarios(id),
    unidade VARCHAR(2) NOT NULL CHECK (unidade IN ('KG', 'G', 'L', 'ML', 'UN')),
    atual_anterior NUMERIC(19,3) NOT NULL,
    atual_novo NUMERIC(19,3) NOT NULL,
    minimo_anterior NUMERIC(19,3) NOT NULL,
    minimo_novo NUMERIC(19,3) NOT NULL,
    ocorrido_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX historico_ajustes_estoque_produto_idx ON historico_ajustes_estoque (produto_id, id);
