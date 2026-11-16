CREATE TABLE usuarios (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    perfil VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL,
    troca_senha_obrigatoria BOOLEAN NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_usuarios_nome CHECK (btrim(nome) <> ''),
    CONSTRAINT ck_usuarios_email CHECK (btrim(email) <> ''),
    CONSTRAINT ck_usuarios_senha_hash CHECK (btrim(senha_hash) <> ''),
    CONSTRAINT ck_usuarios_perfil CHECK (
        perfil IN ('ADMINISTRADOR', 'COMPRAS', 'ESTOQUE', 'CONSULTA')
    )
);

CREATE UNIQUE INDEX uk_usuarios_email ON usuarios (lower(email));
