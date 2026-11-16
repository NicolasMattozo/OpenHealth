import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { atendimentos } from '../../api/endpoints'
import type { RegistroClinico } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { useAvisos } from '../../components/Avisos'
import { Abas } from '../../components/Abas'
import { Botao } from '../../components/Botao'
import { CabecalhoPagina } from '../../components/CabecalhoPagina'
import { Confirmacao } from '../../components/Confirmacao'
import { CaixaErro, Resultado } from '../../components/Estados'
import { LinhaDoTempo } from '../../components/LinhaDoTempo'
import { SeloAtendimento } from '../../components/Selo'
import { formatarCpf, formatarDataHora } from '../../utils/format'
import { mensagemDoErro } from '../../api/client'
import { useAsync } from '../../utils/useAsync'
import { FormDadoClinico, FormDiagnostico, FormExame, FormPrescricao, FormTriagem, ResultadoModal } from './formularios'

type IdAba = 'triagem' | 'dado' | 'diagnostico' | 'prescricao' | 'exame'

/** Prontuario de um atendimento: linha do tempo e, para quem pode, formularios de registro. */
export function AtendimentoPage() {
  const { id } = useParams()
  const idAtendimento = Number(id)
  const { instituicaoAtiva, caps, usuario } = useAuth()
  const cnpj = instituicaoAtiva as string
  const avisos = useAvisos()
  const valido = Number.isInteger(idAtendimento) && idAtendimento > 0
  const prontuario = useAsync(() => atendimentos.prontuario(cnpj, idAtendimento), [cnpj, idAtendimento], valido)
  const [aba, setAba] = useState<IdAba | null>(null)
  const [alvoResultado, setAlvoResultado] = useState<RegistroClinico | null>(null)
  const [finalizando, setFinalizando] = useState(false)
  const [iniciando, setIniciando] = useState(false)

  if (!valido) return <CaixaErro mensagem="Atendimento inválido." />

  const iniciar = async () => {
    setIniciando(true)
    try {
      await atendimentos.iniciar(cnpj, idAtendimento)
      avisos.sucesso('Atendimento iniciado.')
      prontuario.recarregar()
    } catch (e) {
      avisos.erro(mensagemDoErro(e))
    } finally {
      setIniciando(false)
    }
  }

  return (
    <Resultado {...prontuario}>
      {(dados) => {
        const a = dados.atendimento
        const daInstituicao = a.cnpjInstituicao === cnpj
        const aberto = a.status === 'ABERTO'
        const emAndamento = a.status === 'EM_ANDAMENTO'
        // O servidor confere quem e o medico responsavel; aqui o nome so decide se vale mostrar o botao.
        const souOMedico = caps.medico && !!usuario && a.nomeMedico === usuario.nome

        const abas: { id: IdAba; rotulo: string }[] = []
        if (daInstituicao && (aberto || emAndamento)) {
          if (caps.enfermeiro) abas.push({ id: 'triagem', rotulo: 'Triagem' }, { id: 'dado', rotulo: 'Sinais vitais e dados' })
          if (caps.medico && emAndamento) {
            abas.push(
              { id: 'diagnostico', rotulo: 'Diagnóstico' },
              { id: 'prescricao', rotulo: 'Prescrição' },
              { id: 'exame', rotulo: 'Exame' },
              { id: 'dado', rotulo: 'Dado clínico' },
            )
          }
        }
        const abaAtual = abas.find((x) => x.id === aba)?.id ?? abas[0]?.id
        const props = { cnpj, idAtendimento, aoSalvar: prontuario.recarregar }

        return (
          <>
            <CabecalhoPagina
              secao="Clínica"
              titulo={dados.nomePaciente}
              descricao={`CPF ${formatarCpf(dados.cpfPaciente)} · atendimento nº ${a.id}`}
              acao={
                <div className="grupo-botoes">
                  <Link to={`/historico/${dados.cpfPaciente}`} className="btn btn-secundario">
                    Ver histórico do paciente
                  </Link>
                  {daInstituicao && caps.medico && aberto && (
                    <Botao carregando={iniciando} onClick={iniciar}>
                      Iniciar atendimento
                    </Botao>
                  )}
                  {daInstituicao && souOMedico && emAndamento && (
                    <Botao variante="perigo" onClick={() => setFinalizando(true)}>
                      Finalizar atendimento
                    </Botao>
                  )}
                </div>
              }
            />

            <section className="painel">
              <dl className="detalhes detalhes-caixa">
                <div className="detalhe larga">
                  <dt>Motivo</dt>
                  <dd>{a.motivo}</dd>
                </div>
                <div className="detalhe">
                  <dt>Situação</dt>
                  <dd>
                    <SeloAtendimento status={a.status} />
                  </dd>
                </div>
                <div className="detalhe">
                  <dt>Instituição</dt>
                  <dd>{a.nomeInstituicao}</dd>
                </div>
                <div className="detalhe">
                  <dt>Médico responsável</dt>
                  <dd>{a.nomeMedico ?? 'Ainda não assumido'}</dd>
                </div>
                <div className="detalhe">
                  <dt>Abertura</dt>
                  <dd>{formatarDataHora(a.dataHora)}</dd>
                </div>
                <div className="detalhe">
                  <dt>Encerramento</dt>
                  <dd>{formatarDataHora(a.dataFinalizacao)}</dd>
                </div>
              </dl>
              {!daInstituicao && (
                <div className="faixa faixa-aviso">
                  Atendimento de outra instituição, visível porque o paciente autorizou o acesso. Somente leitura.
                </div>
              )}
            </section>

            {daInstituicao && caps.medico && !caps.enfermeiro && aberto && (
              <div className="faixa faixa-aviso">Inicie o atendimento para registrar diagnóstico, prescrição e exames.</div>
            )}

            {abas.length > 0 && abaAtual && (
              <section className="painel">
                <h2 className="titulo-painel">Novo registro</h2>
                <Abas
                  itens={[...new Map(abas.map((x) => [x.id, x])).values()]}
                  ativa={abaAtual}
                  aoMudar={setAba}
                />
                {abaAtual === 'triagem' && <FormTriagem {...props} />}
                {abaAtual === 'dado' && <FormDadoClinico {...props} />}
                {abaAtual === 'diagnostico' && <FormDiagnostico {...props} />}
                {abaAtual === 'prescricao' && <FormPrescricao {...props} />}
                {abaAtual === 'exame' && <FormExame {...props} />}
              </section>
            )}

            <section className="painel">
              <h2 className="titulo-painel">Linha do tempo</h2>
              <LinhaDoTempo
                registros={dados.registros}
                aoLancarResultado={daInstituicao && caps.profissional ? setAlvoResultado : undefined}
              />
            </section>

            {alvoResultado && (
              <ResultadoModal
                cnpj={cnpj}
                requisicao={alvoResultado}
                aoFechar={() => setAlvoResultado(null)}
                aoSalvar={() => {
                  setAlvoResultado(null)
                  avisos.sucesso('Resultado registrado.')
                  prontuario.recarregar()
                }}
              />
            )}

            {finalizando && (
              <Confirmacao
                titulo="Finalizar atendimento"
                mensagem="Depois de finalizado não é mais possível registrar triagem, diagnóstico, prescrição ou exames neste atendimento. Resultados de exames ainda poderão ser lançados."
                rotuloConfirmar="Finalizar"
                perigo
                aoCancelar={() => setFinalizando(false)}
                aoConfirmar={async () => {
                  await atendimentos.finalizar(cnpj, idAtendimento)
                  avisos.sucesso('Atendimento finalizado.')
                  setFinalizando(false)
                  prontuario.recarregar()
                }}
              />
            )}
          </>
        )
      }}
    </Resultado>
  )
}
