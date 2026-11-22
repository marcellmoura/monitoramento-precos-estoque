CREATE INDEX vinculos_itens_cotacao_origens_idx
    ON vinculos_itens_cotacao (fornecedor_id, referencia)
    WHERE referencia IS NOT NULL;
