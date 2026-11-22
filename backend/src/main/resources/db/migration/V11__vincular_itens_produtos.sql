CREATE TABLE vinculos_itens_cotacao (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tentativa_id BIGINT NOT NULL REFERENCES tentativas_importacao(id),
    linha INTEGER NOT NULL CHECK (linha > 0),
    conferencia_id BIGINT NOT NULL REFERENCES conferencias_cotacao(id),
    revisao_item_id BIGINT REFERENCES revisoes_itens_cotacao(id),
    fornecedor_id BIGINT NOT NULL REFERENCES fornecedores(id),
    produto_id BIGINT NOT NULL REFERENCES produtos(id),
    referencia VARCHAR(120),
    descricao VARCHAR(255) NOT NULL,
    unidade VARCHAR(2) NOT NULL CHECK (unidade IN ('KG', 'G', 'L', 'ML', 'UN')),
    embalagem VARCHAR(120) NOT NULL,
    ator_id BIGINT NOT NULL REFERENCES usuarios(id),
    vinculado_em TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX vinculos_itens_cotacao_atual_idx ON vinculos_itens_cotacao (tentativa_id, linha, id DESC);
