-- V5: integridade que antes dependia so do servico.

-- 1) Um paciente nao pode ter dois atendimentos em aberto na mesma instituicao.
CREATE UNIQUE INDEX uq_atendimento_aberto
    ON atendimento (cpf_paciente, cnpj_instituicao)
    WHERE status IN ('ABERTO', 'EM_ANDAMENTO');

-- 2) Datas coerentes com o status. NOT VALID = vale para linhas novas e nao falha por causa de dado antigo.
ALTER TABLE atendimento
    ADD CONSTRAINT ck_atendimento_finalizacao
    CHECK ((status = 'FINALIZADO') = (data_finalizacao IS NOT NULL)) NOT VALID;

ALTER TABLE solicitacao_acesso
    ADD CONSTRAINT ck_solicitacao_decisao
    CHECK ((status = 'PENDENTE') = (data_decisao IS NULL)) NOT VALID;

ALTER TABLE solicitacao_acesso
    ADD CONSTRAINT ck_solicitacao_revogacao
    CHECK ((status = 'REVOGADA') = (data_revogacao IS NOT NULL)) NOT VALID;