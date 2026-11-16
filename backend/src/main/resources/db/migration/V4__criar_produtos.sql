CREATE TABLE produtos (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 nome VARCHAR(255) NOT NULL CHECK (length(btrim(nome)) > 0),
 categoria VARCHAR(100),
 unidade VARCHAR(2) NOT NULL CHECK (unidade IN ('KG','G','L','ML','UN')),
 preco_compra NUMERIC(19,2) CHECK (preco_compra >= 0),
 preco_venda NUMERIC(19,2) NOT NULL CHECK (preco_venda >= 0),
 estoque_atual NUMERIC(19,3) NOT NULL DEFAULT 0 CHECK (estoque_atual >= 0),
 estoque_minimo NUMERIC(19,3) NOT NULL DEFAULT 0 CHECK (estoque_minimo >= 0),
 ativo BOOLEAN NOT NULL DEFAULT true,
 CHECK (unidade <> 'UN' OR (estoque_atual = trunc(estoque_atual) AND estoque_minimo = trunc(estoque_minimo)))
);
