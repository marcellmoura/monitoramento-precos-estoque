CREATE TABLE fornecedores (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome VARCHAR(255) NOT NULL CHECK (length(btrim(nome)) > 0),
    cnpj VARCHAR(32),
    telefone VARCHAR(40),
    email VARCHAR(254)
);
