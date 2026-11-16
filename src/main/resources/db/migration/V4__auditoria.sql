-- V4: auditoria (log_auditoria passa a ser consultavel e protegido contra alteracao)

-- Consultas mais comuns: "quem acessou o paciente X", "o que aconteceu na instituicao Y", "o que fez o usuario Z"
CREATE INDEX ix_log_cpf  ON log_auditoria ((chave_recurso ->> 'cpf'),  instante DESC);
CREATE INDEX ix_log_cnpj ON log_auditoria ((chave_recurso ->> 'cnpj'), instante DESC);
CREATE INDEX ix_log_acao ON log_auditoria (acao, instante DESC);

-- O log so aceita insercao: nao se altera nem se apaga registro de auditoria
CREATE FUNCTION log_auditoria_somente_insercao() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'log_auditoria aceita apenas insercao';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_log_auditoria_somente_insercao
    BEFORE UPDATE OR DELETE ON log_auditoria
    FOR EACH ROW EXECUTE FUNCTION log_auditoria_somente_insercao();
