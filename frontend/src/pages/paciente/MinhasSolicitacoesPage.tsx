import { useState } from 'react'
import { meusDados } from '../../api/endpoints'
import type { SolicitacaoParaPaciente } from '../../api/types'
import { useAvisos } from '../../components/Avisos'
import { Botao } from '../../components/Botao'
import { CabecalhoPagina } from '../../components/CabecalhoPagina'
import { Confirmacao } from '../../components/Confirmacao'
import { Resultado, Vazio } from '../../components/Estados'
import { SeloSolicitacao } from '../../components/Selo'
import { formatarCnpj, formatarDataHora } from '../../utils/format'
import { useAsync } from '../../utils/useAsync'

type Decisao = { tipo: 'aprovar' | 'negar' | 'revogar'; solicitacao: SolicitacaoParaPaciente }

const TEXTOS: Record<Decisao['tipo'], { titulo: string; botao: string; perigo: boolean }> = {
  aprovar: { titulo: 'Aprovar acesso', botao: 'Aprovar acesso', perigo: false },
  negar: { titulo: 'Negar acesso', botao: 'Negar', perigo: true },
  revogar: { titulo: 'Revogar acesso', botao: 'Revogar acesso', perigo: true },
}

function mensagem(d: Decisao): string {
  const nome = d.solicitacao.nomeInstituicao
  if (d.tipo === 'aprovar') return `${nome} passará a poder ver o seu histórico completo, inclusive os atendimentos de outras instituições, até você revogar.`
  if (d.tipo === 'negar') return `${nome} não terá acesso aos seus dados de outras instituições. Ela poderá fazer um novo pedido depois.`
  return `${nome} perderá imediatamente o acesso aos seus dados de outras instituições.`
}

function Cartao({ s, aoDecidir }: { s: SolicitacaoParaPaciente; aoDecidir: (d: Decisao) => void }) {
  return (
    <div className="cartao-solicitacao">
      <div className="solicitacao-topo">
        <div>
          <h3>{s.nomeInstituicao}</h3>
          <small className="texto-suave">
            {s.tipoInstituicao ? `${s.tipoInstituicao} · ` : ''}CNPJ {formatarCnpj(s.cnpjInstituicao)}
          </small>
        </div>
        <SeloSolicitacao status={s.status} />
      </div>
      <p>
        <strong>Finalidade:</strong> {s.finalidade}
      </p>
      <p className="texto-suave">
        Pedido de {s.solicitadaPor} em {formatarDataHora(s.dataSolicitacao)}
        {s.dataDecisao && ` · decidido em ${formatarDataHora(s.dataDecisao)}`}
        {s.dataRevogacao && ` · revogado em ${formatarDataHora(s.dataRevogacao)}`}
      </p>
      {s.status === 'PENDENTE' && (
        <div className="grupo-botoes">
          <Botao onClick={() => aoDecidir({ tipo: 'aprovar', solicitacao: s })}>Aprovar</Botao>
          <Botao variante="perigo" onClick={() => aoDecidir({ tipo: 'negar', solicitacao: s })}>
            Negar
          </Botao>
        </div>
      )}
      {s.status === 'APROVADA' && (
        <div className="grupo-botoes">
          <Botao variante="perigo" onClick={() => aoDecidir({ tipo: 'revogar', solicitacao: s })}>
            Revogar acesso
          </Botao>
        </div>
      )}
    </div>
  )
}

export function MinhasSolicitacoesPage() {
  const avisos = useAvisos()
  const lista = useAsync(() => meusDados.solicitacoes(), [])
  const [decisao, setDecisao] = useState<Decisao | null>(null)

  return (
    <>
      <CabecalhoPagina
        secao="Privacidade"
        titulo="Quem pode ver meus dados"
        descricao="Instituições pedem acesso ao seu histórico em outras instituições. Você decide, e pode mudar de ideia quando quiser."
      />

      <Resultado {...lista}>
        {(dados) => {
          const pendentes = dados.filter((s) => s.status === 'PENDENTE')
          const aprovadas = dados.filter((s) => s.status === 'APROVADA')
          const historico = dados.filter((s) => s.status === 'NEGADA' || s.status === 'REVOGADA')
          if (dados.length === 0) {
            return (
              <section className="painel">
                <Vazio icone="chave" titulo="Nenhuma instituição pediu acesso aos seus dados">
                  Dentro da própria instituição onde você se atende, a equipe já vê o seu atendimento. Esta tela é só para o acesso entre instituições.
                </Vazio>
              </section>
            )
          }
          return (
            <>
              <section className="painel">
                <h2 className="titulo-painel">Aguardando a sua decisão ({pendentes.length})</h2>
                {pendentes.length === 0 ? (
                  <p className="texto-suave">Nenhum pedido pendente.</p>
                ) : (
                  <div className="lista-cartoes">
                    {pendentes.map((s) => (
                      <Cartao key={s.id} s={s} aoDecidir={setDecisao} />
                    ))}
                  </div>
                )}
              </section>
              <section className="painel">
                <h2 className="titulo-painel">Acessos concedidos ({aprovadas.length})</h2>
                {aprovadas.length === 0 ? (
                  <p className="texto-suave">Nenhuma instituição tem acesso ativo.</p>
                ) : (
                  <div className="lista-cartoes">
                    {aprovadas.map((s) => (
                      <Cartao key={s.id} s={s} aoDecidir={setDecisao} />
                    ))}
                  </div>
                )}
              </section>
              {historico.length > 0 && (
                <section className="painel">
                  <h2 className="titulo-painel">Histórico</h2>
                  <div className="lista-cartoes">
                    {historico.map((s) => (
                      <Cartao key={s.id} s={s} aoDecidir={setDecisao} />
                    ))}
                  </div>
                </section>
              )}
            </>
          )
        }}
      </Resultado>

      {decisao && (
        <Confirmacao
          titulo={TEXTOS[decisao.tipo].titulo}
          mensagem={mensagem(decisao)}
          rotuloConfirmar={TEXTOS[decisao.tipo].botao}
          perigo={TEXTOS[decisao.tipo].perigo}
          aoCancelar={() => setDecisao(null)}
          aoConfirmar={async () => {
            const { tipo, solicitacao } = decisao
            if (tipo === 'aprovar') await meusDados.aprovar(solicitacao.id)
            else if (tipo === 'negar') await meusDados.negar(solicitacao.id)
            else await meusDados.revogar(solicitacao.id)
            avisos.sucesso(tipo === 'aprovar' ? 'Acesso aprovado.' : tipo === 'negar' ? 'Pedido negado.' : 'Acesso revogado.')
            setDecisao(null)
            lista.recarregar()
          }}
        />
      )}
    </>
  )
}
