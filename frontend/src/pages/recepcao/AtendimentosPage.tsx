import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { atendimentos } from '../../api/endpoints'
import type { Atendimento, StatusAtendimento } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { useAvisos } from '../../components/Avisos'
import { Botao } from '../../components/Botao'
import { CabecalhoPagina } from '../../components/CabecalhoPagina'
import { Resultado, Vazio } from '../../components/Estados'
import { SeloAtendimento } from '../../components/Selo'
import { formatarCpf, formatarDataHora } from '../../utils/format'
import { ROTULO_STATUS_ATENDIMENTO } from '../../utils/rotulos'
import { useAsync } from '../../utils/useAsync'
import { mensagemDoErro } from '../../api/client'
import { AbrirAtendimentoModal } from './modais'

const FILTROS: (StatusAtendimento | 'TODOS')[] = ['TODOS', 'ABERTO', 'EM_ANDAMENTO', 'FINALIZADO', 'CANCELADO']

/** Fila de atendimentos da instituicao: a recepcao abre; medico e enfermeiro atendem. */
export function AtendimentosPage() {
  const { instituicaoAtiva, caps } = useAuth()
  const cnpj = instituicaoAtiva as string
  const avisos = useAvisos()
  const navegar = useNavigate()
  const [filtro, setFiltro] = useState<StatusAtendimento | 'TODOS'>('TODOS')
  const lista = useAsync(
    () => atendimentos.listar(cnpj, filtro === 'TODOS' ? undefined : filtro),
    [cnpj, filtro],
  )
  const [novo, setNovo] = useState(false)
  const [iniciando, setIniciando] = useState<number | null>(null)
  const [cancelando, setCancelando] = useState<number | null>(null)

  // A fila muda sozinha (outras pessoas abrem e iniciam atendimentos): atualiza a cada 30 segundos.
  const { recarregar } = lista
  useEffect(() => {
    const id = window.setInterval(recarregar, 30000)
    return () => window.clearInterval(id)
  }, [recarregar])

  const iniciar = async (a: Atendimento) => {
    setIniciando(a.id)
    try {
      await atendimentos.iniciar(cnpj, a.id)
      avisos.sucesso('Atendimento iniciado.')
      navegar(`/atendimentos/${a.id}`)
    } catch (e) {
      avisos.erro(mensagemDoErro(e))
      recarregar()
    } finally {
      setIniciando(null)
    }
  }

  const cancelar = async (a: Atendimento) => {
    if (!window.confirm(`Tem certeza que deseja cancelar o atendimento de ${a.nomePaciente}?`)) {
      return
    }
    setCancelando(a.id)
    try {
      await atendimentos.cancelar(cnpj, a.id)
      avisos.sucesso('Atendimento cancelado.')
      recarregar()
    } catch (e) {
      avisos.erro(mensagemDoErro(e))
      recarregar()
    } finally {
      setCancelando(null)
    }
  }

  const visiveis = lista.dados ?? []
  
  return (
    <>
      <CabecalhoPagina
        secao={caps.recepcionista ? 'Recepção' : 'Clínica'}
        titulo="Atendimentos"
        descricao="Acompanhe a fila de atendimentos da instituição (os 50 mais recentes)."
        acao={
          caps.recepcionista ? (
            <Botao icone="mais" onClick={() => setNovo(true)}>
              Novo atendimento
            </Botao>
          ) : undefined
        }
      />

      <section className="painel">
        <div className="filtros">
          <div className="chips" role="group" aria-label="Filtrar por situação">
            {FILTROS.map((f) => (
              <button
                key={f}
                type="button"
                className={f === filtro ? 'chip ativo' : 'chip'}
                aria-pressed={f === filtro}
                onClick={() => setFiltro(f)}
              >
                {f === 'TODOS' ? 'Todos' : ROTULO_STATUS_ATENDIMENTO[f]}
              </button>
            ))}
          </div>
          <Botao variante="fantasma" pequeno icone="atividade" onClick={recarregar}>
            Atualizar
          </Botao>
        </div>

        <Resultado {...lista}>
          {() =>
            visiveis.length === 0 ? (
              <Vazio icone="prancheta" titulo="Nenhum atendimento nesta situação" />
            ) : (
              <div className="tabela-rolagem">
                <table className="tabela">
                  <thead>
                    <tr>
                      <th>Paciente</th>
                      <th>Motivo</th>
                      <th>Abertura</th>
                      <th>Situação</th>
                      <th>Médico</th>
                      <th aria-label="Ações" />
                    </tr>
                  </thead>
                  <tbody>
                    {visiveis.map((a) => (
                      <tr key={a.id}>
                        <td>
                          <strong>{a.nomePaciente}</strong>
                          <small className="sub">{formatarCpf(a.cpfPaciente)}</small>
                        </td>
                        <td>{a.motivo}</td>
                        <td>{formatarDataHora(a.dataHora)}</td>
                        <td>
                          <SeloAtendimento status={a.status} />
                        </td>
                        <td>{a.nomeMedico ?? '—'}</td>
                        <td className="acoes">
                          {caps.medico && a.status === 'ABERTO' && (
                            <Botao pequeno carregando={iniciando === a.id} onClick={() => iniciar(a)}>
                              Iniciar
                            </Botao>
                          )}
                          {caps.profissional && a.status !== 'CANCELADO' && (
                            <Botao variante="secundario" pequeno onClick={() => navegar(`/atendimentos/${a.id}`)}>
                              {a.status === 'FINALIZADO' ? 'Ver prontuário' : 'Abrir'}
                            </Botao>
                          )}
                          {(a.status === 'ABERTO' || a.status === 'EM_ANDAMENTO') && (
                            <Botao
                              variante="perigo"
                              pequeno
                              carregando={cancelando === a.id}
                              onClick={() => cancelar(a)}
                            >
                              Cancelar
                            </Botao>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
          }
        </Resultado>
      </section>

      {novo && (
        <AbrirAtendimentoModal
          cnpj={cnpj}
          aoFechar={() => setNovo(false)}
          aoAberto={() => {
            setNovo(false)
            avisos.sucesso('Atendimento aberto.')
            recarregar()
          }}
        />
      )}
    </>
  )
}