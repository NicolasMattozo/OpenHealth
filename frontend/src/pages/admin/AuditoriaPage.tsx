import { Fragment, useState, type FormEvent } from 'react'
import { auditoria } from '../../api/endpoints'
import type { FiltrosAuditoria, LogAuditoria } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { Botao } from '../../components/Botao'
import { CabecalhoPagina } from '../../components/CabecalhoPagina'
import { CampoSelect, CampoTexto } from '../../components/Campos'
import { Resultado, Vazio } from '../../components/Estados'
import { useInstituicoesAdministraveis } from '../../components/SeletorInstituicao'
import { Selo } from '../../components/Selo'
import { formatarCnpj, formatarCpf, formatarDataHora, localParaIso, mascaraCpf, somenteDigitos } from '../../utils/format'
import { ROTULO_ACAO, rotuloAcao } from '../../utils/rotulos'
import { useAsync } from '../../utils/useAsync'

interface Chave {
  status?: number
  cpf?: string
  cnpj?: string
  ip?: string
  entreInstituicoes?: boolean
  [outro: string]: unknown
}

function lerChave(texto: string): Chave {
  try {
    const dados: unknown = JSON.parse(texto)
    return dados && typeof dados === 'object' ? (dados as Chave) : {}
  } catch {
    return {}
  }
}

function SeloStatus({ status }: { status: number | undefined }) {
  if (status === undefined) return <>—</>
  if (status >= 200 && status < 300) return <Selo tom="verde">{status} · Permitido</Selo>
  if (status === 401 || status === 403) return <Selo tom="vermelho">{status} · Negado</Selo>
  if (status >= 500) return <Selo tom="vermelho">{status} · Erro</Selo>
  return <Selo tom="amarelo">{status}</Selo>
}

export function AuditoriaPage() {
  const { caps } = useAuth()
  const inst = useInstituicoesAdministraveis()

  // Admin do sistema pode olhar todas as instituicoes ("" = todas); admin de instituicao so a dele.
  const [instituicaoFiltro, setInstituicaoFiltro] = useState('')
  const [acao, setAcao] = useState('')
  const [cpf, setCpf] = useState('')
  const [idUsuario, setIdUsuario] = useState('')
  const [de, setDe] = useState('')
  const [ate, setAte] = useState('')
  const [limite, setLimite] = useState('100')
  const [aplicados, setAplicados] = useState<FiltrosAuditoria>({ limite: 100 })
  const [aberto, setAberto] = useState<number | null>(null)

  const cnpjDaConsulta = caps.adminSistema ? undefined : (inst.cnpj ?? undefined)
  const filtrosFinais: FiltrosAuditoria = { ...aplicados, cnpj: caps.adminSistema ? instituicaoFiltro || undefined : undefined }
  const lista = useAsync(
    () => auditoria.consultar(filtrosFinais, cnpjDaConsulta),
    [JSON.stringify(filtrosFinais), cnpjDaConsulta],
    caps.adminSistema || !!cnpjDaConsulta,
  )

  const aplicar = (e: FormEvent) => {
    e.preventDefault()
    setAberto(null)
    setAplicados({
      acao: acao || undefined,
      cpf: somenteDigitos(cpf) || undefined,
      idUsuario: idUsuario ? Number(idUsuario) : undefined,
      de: localParaIso(de),
      ate: localParaIso(ate),
      limite: Number(limite),
    })
  }

  const limpar = () => {
    setAcao('')
    setCpf('')
    setIdUsuario('')
    setDe('')
    setAte('')
    setLimite('100')
    setInstituicaoFiltro('')
    setAplicados({ limite: 100 })
  }

  return (
    <>
      <CabecalhoPagina
        secao="Administração"
        titulo="Auditoria"
        descricao={
          caps.adminSistema
            ? 'Registro de quem acessou ou alterou dados em toda a rede.'
            : `Registro das ações feitas em ${inst.selecionada?.nome ?? 'sua instituição'}.`
        }
      />

      <section className="painel">
        <form onSubmit={aplicar}>
          <div className="grade-filtros">
            {caps.adminSistema && (
              <CampoSelect rotulo="Instituição" value={instituicaoFiltro} onChange={(e) => setInstituicaoFiltro(e.target.value)}>
                <option value="">Todas</option>
                {inst.opcoes.map((o) => (
                  <option key={o.cnpj} value={o.cnpj}>
                    {o.nome}
                  </option>
                ))}
              </CampoSelect>
            )}
            <CampoSelect rotulo="Ação" value={acao} onChange={(e) => setAcao(e.target.value)}>
              <option value="">Todas</option>
              {Object.entries(ROTULO_ACAO)
                .sort((a, b) => a[1].localeCompare(b[1], 'pt-BR'))
                .map(([codigo, rotulo]) => (
                  <option key={codigo} value={codigo}>
                    {rotulo}
                  </option>
                ))}
            </CampoSelect>
            <CampoTexto rotulo="CPF do paciente" value={cpf} onChange={(e) => setCpf(mascaraCpf(e.target.value))} placeholder="000.000.000-00" />
            <CampoTexto rotulo="Código do usuário" type="number" min={1} value={idUsuario} onChange={(e) => setIdUsuario(e.target.value)} />
            <CampoTexto rotulo="De" type="datetime-local" value={de} onChange={(e) => setDe(e.target.value)} />
            <CampoTexto rotulo="Até" type="datetime-local" value={ate} onChange={(e) => setAte(e.target.value)} />
            <CampoSelect rotulo="Quantidade" value={limite} onChange={(e) => setLimite(e.target.value)}>
              {[50, 100, 200, 500].map((n) => (
                <option key={n}>{n}</option>
              ))}
            </CampoSelect>
          </div>
          <div className="filtros-acoes">
            <Botao type="submit" icone="busca">
              Consultar
            </Botao>
            <Botao variante="fantasma" onClick={limpar}>
              Limpar filtros
            </Botao>
          </div>
        </form>

        <Resultado {...lista}>
          {(dados: LogAuditoria[]) =>
            dados.length === 0 ? (
              <Vazio icone="escudo" titulo="Nenhum registro encontrado para esses filtros" />
            ) : (
              <>
                <p className="texto-suave">{dados.length} registro(s), do mais recente para o mais antigo. Clique em uma linha para ver os detalhes.</p>
                <div className="tabela-rolagem">
                  <table className="tabela tabela-clicavel">
                    <thead>
                      <tr>
                        <th>Quando</th>
                        <th>Quem</th>
                        <th>Ação</th>
                        <th>Resultado</th>
                        <th>Paciente (CPF)</th>
                        <th>Instituição</th>
                        <th aria-label="Observações" />
                      </tr>
                    </thead>
                    <tbody>
                      {dados.map((registro) => {
                        const chave = lerChave(registro.chaveRecurso)
                        return (
                          <Fragment key={registro.id}>
                            <tr onClick={() => setAberto(aberto === registro.id ? null : registro.id)}>
                              <td>{formatarDataHora(registro.instante)}</td>
                              <td>{registro.nomeUsuario ?? <span className="texto-suave">não identificado</span>}</td>
                              <td>{rotuloAcao(registro.acao)}</td>
                              <td>
                                <SeloStatus status={chave.status} />
                              </td>
                              <td>{chave.cpf ? formatarCpf(chave.cpf) : '—'}</td>
                              <td>{chave.cnpj ? formatarCnpj(chave.cnpj) : '—'}</td>
                              <td>{chave.entreInstituicoes && <Selo tom="roxo">Entre instituições</Selo>}</td>
                            </tr>
                            {aberto === registro.id && (
                              <tr className="linha-detalhe">
                                <td colSpan={7}>
                                  <pre>{JSON.stringify({ codigo: registro.id, recurso: registro.tipoRecurso, ...chave }, null, 2)}</pre>
                                </td>
                              </tr>
                            )}
                          </Fragment>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
              </>
            )
          }
        </Resultado>
      </section>
    </>
  )
}
