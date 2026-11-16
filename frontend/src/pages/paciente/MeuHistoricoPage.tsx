import { Link, useParams } from 'react-router-dom'
import { meusDados } from '../../api/endpoints'
import { CabecalhoPagina } from '../../components/CabecalhoPagina'
import { CaixaErro, Resultado, Vazio } from '../../components/Estados'
import { LinhaDoTempo } from '../../components/LinhaDoTempo'
import { SeloAtendimento } from '../../components/Selo'
import { formatarDataHora } from '../../utils/format'
import { useAsync } from '../../utils/useAsync'

export function MeuHistoricoPage() {
  const lista = useAsync(() => meusDados.historico(), [])
  return (
    <>
      <CabecalhoPagina secao="Meu prontuário" titulo="Meu histórico" descricao="Os seus atendimentos em todas as instituições da rede." />
      <section className="painel">
        <Resultado {...lista}>
          {(dados) =>
            dados.length === 0 ? (
              <Vazio icone="documento" titulo="Você ainda não tem atendimentos" />
            ) : (
              <div className="tabela-rolagem">
                <table className="tabela">
                  <thead>
                    <tr>
                      <th>Data</th>
                      <th>Instituição</th>
                      <th>Motivo</th>
                      <th>Situação</th>
                      <th>Médico</th>
                      <th aria-label="Ações" />
                    </tr>
                  </thead>
                  <tbody>
                    {dados.map((a) => (
                      <tr key={a.id}>
                        <td>{formatarDataHora(a.dataHora)}</td>
                        <td>{a.nomeInstituicao}</td>
                        <td>{a.motivo}</td>
                        <td>
                          <SeloAtendimento status={a.status} />
                        </td>
                        <td>{a.nomeMedico ?? '—'}</td>
                        <td className="acoes">
                          <Link to={`/meu/historico/${a.id}`} className="btn btn-secundario btn-pequeno">
                            Ver detalhes
                          </Link>
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
    </>
  )
}

export function MeuAtendimentoPage() {
  const { id } = useParams()
  const idAtendimento = Number(id)
  const valido = Number.isInteger(idAtendimento) && idAtendimento > 0
  const detalhe = useAsync(() => meusDados.atendimento(idAtendimento), [idAtendimento], valido)

  if (!valido) return <CaixaErro mensagem="Atendimento inválido." />

  return (
    <Resultado {...detalhe}>
      {(dados) => (
        <>
          <CabecalhoPagina
            secao="Meu prontuário"
            titulo={`Atendimento de ${formatarDataHora(dados.atendimento.dataHora)}`}
            descricao={`${dados.atendimento.nomeInstituicao}${dados.atendimento.nomeMedico ? ` · ${dados.atendimento.nomeMedico}` : ''}`}
            acao={
              <Link to="/meu/historico" className="btn btn-secundario">
                Voltar
              </Link>
            }
          />
          <section className="painel">
            <dl className="detalhes detalhes-caixa">
              <div className="detalhe larga">
                <dt>Motivo</dt>
                <dd>{dados.atendimento.motivo}</dd>
              </div>
              <div className="detalhe">
                <dt>Situação</dt>
                <dd>
                  <SeloAtendimento status={dados.atendimento.status} />
                </dd>
              </div>
              <div className="detalhe">
                <dt>Encerramento</dt>
                <dd>{formatarDataHora(dados.atendimento.dataFinalizacao)}</dd>
              </div>
            </dl>
          </section>
          <section className="painel">
            <h2 className="titulo-painel">O que foi registrado</h2>
            <p className="texto-suave">Você vê os registros que a equipe marcou como visíveis ao paciente.</p>
            <LinhaDoTempo registros={dados.registros} mostrarVisibilidade={false} />
          </section>
        </>
      )}
    </Resultado>
  )
}
