CREATE TABLE revisoes_itens_cotacao (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tentativa_id BIGINT NOT NULL REFERENCES tentativas_importacao(id),
    linha INTEGER NOT NULL CHECK (linha > 0),
    conferencia_id BIGINT NOT NULL REFERENCES conferencias_cotacao(id),
    ator_id BIGINT NOT NULL REFERENCES usuarios(id),
    revisada_em TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    referencia VARCHAR(120),
    descricao VARCHAR(255) NOT NULL,
    unidade VARCHAR(2) CHECK (unidade IN ('KG', 'G', 'L', 'ML', 'UN')),
    embalagem VARCHAR(120),
    quantidade NUMERIC(15,3) CHECK (quantidade > 0),
    coluna VARCHAR(100),
    preco NUMERIC(14,2) CHECK (preco > 0),
    motivo VARCHAR(300) NOT NULL
);
CREATE INDEX revisoes_itens_cotacao_atual_idx ON revisoes_itens_cotacao (tentativa_id, linha, id DESC);
