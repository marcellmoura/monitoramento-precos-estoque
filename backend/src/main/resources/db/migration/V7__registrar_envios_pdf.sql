CREATE TABLE arquivos_importacao (
    hash CHAR(64) PRIMARY KEY CHECK (hash ~ '^[a-f0-9]{64}$'),
    arquivo_id UUID UNIQUE NOT NULL,
    tamanho_bytes BIGINT NOT NULL CHECK (tamanho_bytes > 0),
    paginas INTEGER NOT NULL CHECK (paginas > 0)
);

CREATE TABLE tentativas_importacao (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fornecedor_id BIGINT NOT NULL REFERENCES fornecedores(id),
    ator_id BIGINT NOT NULL REFERENCES usuarios(id),
    nome_original VARCHAR(255) NOT NULL,
    enviada_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    arquivo_hash CHAR(64) REFERENCES arquivos_importacao(hash),
    status VARCHAR(32) NOT NULL CHECK (status IN ('PENDENTE','REJEITADA','FALHA_ENVIO','DUPLICADA','AGUARDANDO_CONFIRMACAO')),
    motivo VARCHAR(300),
    importacao_existente_id BIGINT REFERENCES tentativas_importacao(id),
    quantidade_itens INTEGER CHECK (quantidade_itens >= 0),
    confirmada_por BIGINT REFERENCES usuarios(id),
    confirmada_em TIMESTAMPTZ,
    CHECK ((status IN ('REJEITADA','FALHA_ENVIO')) = (arquivo_hash IS NULL)),
    CHECK ((status = 'DUPLICADA') = (importacao_existente_id IS NOT NULL))
);

CREATE UNIQUE INDEX tentativas_importacao_canonica_idx ON tentativas_importacao (fornecedor_id, arquivo_hash)
    WHERE status NOT IN ('DUPLICADA', 'REJEITADA', 'FALHA_ENVIO');
