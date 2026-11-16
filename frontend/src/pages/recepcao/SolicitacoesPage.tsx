import { useState } from 'react'
import { solicitacoes } from '../../api/endpoints'
import { useAuth } from '../../auth/AuthContext'
import { useAvisos } from '../../components/Avisos'
import { Botao } from '../../components/Botao'
import { CabecalhoPagina } from '../../components/CabecalhoPagina'
import { Resultado, Vazio } from '../../components/Estados'
import { SeloSolicitacao } from '../../components/Selo'
import { formatarCpf, formatarDataHora } from '../../utils/format'
import { useAsync } from '../../utils/useAsync'
import { SolicitarAcessoModal } from './modais'

/** Solicitacoes de acesso da instituicao (caso de uso 18) e a situacao de cada uma. */
export function SolicitacoesPage() {
  const { instituicaoAtiva } = useAuth()
  const cnpj = instituicaoAtiva as string
  const avisos = useAvisos()
  const lista = useAsync(() => solicitacoes.listar(cnpj), [cnpj])
  const [nova, setNova] = useState(false)

  return (
    <>
      <CabecalhoPagina
        secao="Recepção"
        titulo="Acesso aos dados de pacientes"
        descricao="Peça autorização ao paciente para ver o histórico dele em outras instituições."
        acao={
          <Botao icone="mais" onClick={() => setNova(true)}>
            Nova solicitação
          </Botao>
        }
      />

      <section className="painel">
        <p className="texto-suave">
          Depois que o paciente aprova, médicos e enfermeiros da instituição passam a ver o histórico completo dele. Se ele revogar, o
          acesso termina na hora.
        </p>
        <Resultado {...lista}>
          {(dados) =>
            dados.length === 0 ? (
              <Vazio icone="chave" titulo="Nenhuma solicitação feita ainda" />
            ) : (
              <div className="tabela-rolagem">
                <table className="tabela">
                  <thead>
                    <tr>
                      <th>Paciente</th>
                      <th>Finalidade</th>
                      <th>Solicitada em</th>
                      <th>Situação</th>
                      <th>Decisão</th>
                      <th>Solicitante</th>
                    </tr>
                  </thead>
                  <tbody>
                    {dados.map((s) => (
                      <tr key={s.id}>
                        <td>
                          <strong>{s.nomePaciente}</strong>
                          <small className="sub">{formatarCpf(s.cpfPaciente)}</small>
                        </td>
                        <td>{s.finalidade}</td>
                        <td>{formatarDataHora(s.dataSolicitacao)}</td>
                        <td>
                          <SeloSolicitacao status={s.status} />
                        </td>
                        <td>{formatarDataHora(s.dataRevogacao ?? s.dataDecisao)}</td>
                        <td>{s.nomeRecepcionista}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
          }
        </Resultado>
      </section>

      {nova && (
        <SolicitarAcessoModal
          cnpj={cnpj}
          aoFechar={() => setNova(false)}
          aoSolicitado={() => {
            setNova(false)
            avisos.sucesso('Solicitação enviada ao paciente.')
            lista.recarregar()
          }}
        />
      )}
    </>
  )
}
