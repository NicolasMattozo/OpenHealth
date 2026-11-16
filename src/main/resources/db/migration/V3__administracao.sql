-- V3: administracao (casos de uso 19 a 27)

-- Tipo da instituicao (caso de uso 24)
ALTER TABLE instituicao ADD COLUMN tipo VARCHAR(50);

-- CNPJ alfanumerico: desde 31/07/2026 a Receita emite CNPJs com letras nas 12 primeiras posicoes
ALTER TABLE instituicao DROP CONSTRAINT ck_instituicao_cnpj;
ALTER TABLE instituicao ADD CONSTRAINT ck_instituicao_cnpj CHECK (cnpj ~ '^[0-9A-Z]{12}[0-9]{2}$');

-- Senha temporaria: o usuario precisa trocar a senha no primeiro acesso
ALTER TABLE usuario ADD COLUMN deve_trocar_senha BOOLEAN NOT NULL DEFAULT FALSE;

-- Departamento do usuario dentro da instituicao (o departamento tem que ser da mesma instituicao)
ALTER TABLE departamento
    ADD CONSTRAINT uq_departamento_id_instituicao UNIQUE (id_departamento, cnpj_instituicao);
ALTER TABLE usuario_instituicao ADD COLUMN id_departamento BIGINT;
ALTER TABLE usuario_instituicao
    ADD CONSTRAINT fk_usuario_instituicao_departamento
    FOREIGN KEY (id_departamento, cnpj_instituicao)
    REFERENCES departamento (id_departamento, cnpj_instituicao);
