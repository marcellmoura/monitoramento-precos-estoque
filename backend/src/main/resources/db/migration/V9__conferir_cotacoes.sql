CREATE TABLE conferencias_cotacao (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tentativa_id BIGINT NOT NULL REFERENCES tentativas_importacao(id),
    ator_id BIGINT NOT NULL REFERENCES usuarios(id),
    layout VARCHAR(32) NOT NULL,
    colunas TEXT NOT NULL,
    coluna VARCHAR(100) NOT NULL,
    unidade VARCHAR(2) NOT NULL CHECK (unidade IN ('KG', 'G', 'L', 'ML', 'UN')),
    embalagem VARCHAR(120) NOT NULL CHECK (length(btrim(embalagem)) > 0),
    data_comercial DATE NOT NULL,
    confirmada_em TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX conferencias_cotacao_tentativa_idx ON conferencias_cotacao (tentativa_id, id DESC);
