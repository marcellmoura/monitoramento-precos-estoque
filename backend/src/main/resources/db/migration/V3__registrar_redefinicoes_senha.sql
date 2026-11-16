CREATE TABLE historico_acesso_usuario (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 ator_id BIGINT NOT NULL REFERENCES usuarios(id),
 usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
 tipo VARCHAR(30) NOT NULL CHECK (tipo = 'SENHA_REDEFINIDA'),
 ocorrido_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX historico_acesso_usuario_destino_idx ON historico_acesso_usuario(usuario_id, ocorrido_em);
