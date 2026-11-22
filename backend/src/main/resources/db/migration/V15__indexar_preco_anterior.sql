CREATE INDEX precos_confirmados_preco_anterior
    ON precos_confirmados (fornecedor_id, chave_item, unidade, embalagem_chave, modalidade, data_comercial DESC, ordem DESC)
    WHERE ordem IS NOT NULL;
