import type { RegistroClinico, TipoRegistro } from '../api/types'
import { formatarDataHora } from '../utils/format'
import { Botao } from './Botao'
import { Icone, type NomeIconeUi } from './Icone'
import { Selo, SeloRisco } from './Selo'

const TIPOS: Record<TipoRegistro, { rotulo: string; icone: NomeIconeUi; cor: string }> = {
  TRIAGEM: { rotulo: 'Triagem', icone: 'atividade', cor: 'laranja' },
  DADO_CLINICO: { rotulo: 'Dado clínico', icone: 'prancheta', cor: 'azul' },
  CONDICAO_DIAGNOSTICO: { rotulo: 'Diagnóstico', icone: 'documento', cor: 'roxo' },
  PRESCRICAO_MEDICAMENTO: { rotulo: 'Prescrição', icone: 'pilula', cor: 'verde' },
  REQUISICAO_EXAME: { rotulo: 'Requisição de exame', icone: 'frasco', cor: 'teal' },
}

function Detalhe({ rotulo, valor }: { rotulo: string; valor: string | null | undefined }) {
  if (!valor) return null
  return (
    <div className="detalhe">
      <dt>{rotulo}</dt>
      <dd>{valor}</dd>
    </div>
  )
}

interface Props {
  registros: RegistroClinico[]
  /** Quando informado, cada requisicao de exame ganha o botao "Lançar resultado". */
  aoLancarResultado?: (requisicao: RegistroClinico) => void
  /** Em telas do paciente nao faz sentido marcar "restrito ao paciente". */
  mostrarVisibilidade?: boolean
}

export function LinhaDoTempo({ registros, aoLancarResultado, mostrarVisibilidade = true }: Props) {
  if (registros.length === 0) {
    return <p className="texto-suave">Nenhum registro neste atendimento ainda.</p>
  }
  return (
    <ol className="linha-do-tempo">
      {registros.map((r) => {
        const tipo = TIPOS[r.tipo] ?? { rotulo: r.tipo, icone: 'documento' as NomeIconeUi, cor: 'azul' }
        return (
          <li key={r.id} className="registro">
            <span className={`registro-icone tom-${tipo.cor}`}>
              <Icone nome={tipo.icone} />
            </span>
            <div className="registro-corpo">
              <div className="registro-topo">
                <strong>{tipo.rotulo}</strong>
                {r.tipo === 'TRIAGEM' && r.classificacaoRisco && <SeloRisco risco={r.classificacaoRisco} />}
                {r.tipo === 'REQUISICAO_EXAME' && r.statusExame && (
                  <Selo tom={r.statusExame === 'REALIZADO' ? 'verde' : 'amarelo'}>
                    {r.statusExame === 'REALIZADO' ? 'Realizado' : 'Pendente'}
                  </Selo>
                )}
                {mostrarVisibilidade && !r.visivelAoPaciente && <Selo tom="cinza">Restrito aos profissionais</Selo>}
                <span className="registro-meta">
                  {r.autor} · {formatarDataHora(r.dataRegistro)}
                </span>
              </div>

              <dl className="detalhes">
                <Detalhe rotulo="Sintomas" valor={r.sintomas} />
                <Detalhe rotulo={r.tipoDado ?? 'Dado'} valor={r.tipo === 'DADO_CLINICO' ? r.valor : null} />
                <Detalhe rotulo="Descrição" valor={r.descricao} />
                <Detalhe rotulo="CID" valor={r.cid} />
                <Detalhe rotulo="Medicamento" valor={r.medicamento} />
                <Detalhe rotulo="Dosagem" valor={r.dosagem} />
                <Detalhe rotulo="Via" valor={r.via} />
                <Detalhe rotulo="Frequência" valor={r.frequencia} />
                <Detalhe rotulo="Exame" valor={r.tipoExame} />
              </dl>

              {r.tipo === 'REQUISICAO_EXAME' && (
                <div className="resultados">
                  {(r.resultados ?? []).length === 0 ? (
                    <p className="texto-suave">Aguardando resultado.</p>
                  ) : (
                    <ul>
                      {[...(r.resultados ?? [])]
                        .sort((a, b) => b.versao - a.versao)
                        .map((res, i) => (
                          <li key={res.versao} className={i === 0 ? 'vigente' : 'anterior'}>
                            <div className="resultado-topo">
                              <strong>Versão {res.versao}</strong>
                              {i === 0 ? <Selo tom="verde">Vigente</Selo> : <Selo tom="cinza">Substituída</Selo>}
                              <span className="registro-meta">
                                {res.registradoPor} · {formatarDataHora(res.dataResultado)}
                              </span>
                            </div>
                            <p>{res.resultado}</p>
                          </li>
                        ))}
                    </ul>
                  )}
                  {aoLancarResultado && (
                    <Botao variante="secundario" pequeno icone="mais" onClick={() => aoLancarResultado(r)}>
                      {(r.resultados ?? []).length === 0 ? 'Lançar resultado' : 'Lançar nova versão'}
                    </Botao>
                  )}
                </div>
              )}
            </div>
          </li>
        )
      })}
    </ol>
  )
}
