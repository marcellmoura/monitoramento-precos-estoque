ALTER TABLE tentativas_importacao DROP CONSTRAINT tentativas_importacao_status_check;
ALTER TABLE tentativas_importacao ADD CONSTRAINT tentativas_importacao_status_check CHECK (
    status IN ('PENDENTE','REJEITADA','FALHA_ENVIO','DUPLICADA','AGUARDANDO_CONFIRMACAO',
               'PROCESSANDO','AGUARDANDO_CONFERENCIA','FALHA_EXTRACAO','CONCLUIDA_COM_PENDENCIAS','CONCLUIDA')
);

CREATE TABLE precos_confirmados (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tentativa_id BIGINT NOT NULL REFERENCES tentativas_importacao(id),
    linha INTEGER NOT NULL CHECK (linha > 0),
    fornecedor_id BIGINT NOT NULL REFERENCES fornecedores(id),
    produto_id BIGINT NOT NULL REFERENCES produtos(id),
    conferencia_id BIGINT NOT NULL REFERENCES conferencias_cotacao(id),
    revisao_item_id BIGINT REFERENCES revisoes_itens_cotacao(id),
    vinculo_origem_id BIGINT NOT NULL REFERENCES vinculos_itens_cotacao(id),
    ator_id BIGINT NOT NULL REFERENCES usuarios(id),
    chave_item TEXT NOT NULL,
    referencia TEXT,
    descricao TEXT NOT NULL,
    unidade VARCHAR(2) NOT NULL CHECK (unidade IN ('KG','G','L','ML','UN')),
    embalagem TEXT NOT NULL,
    embalagem_chave TEXT NOT NULL,
    modalidade TEXT NOT NULL,
    preco NUMERIC(14,2) NOT NULL CHECK (preco > 0),
    data_comercial DATE NOT NULL,
    enviada_em TIMESTAMPTZ NOT NULL,
    confirmada_em TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    UNIQUE (tentativa_id, linha),
    UNIQUE (fornecedor_id, data_comercial, chave_item, unidade, embalagem_chave, modalidade)
);
