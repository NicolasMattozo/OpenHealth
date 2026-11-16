import { meusDados } from '../../api/endpoints'
import { CabecalhoPagina } from '../../components/CabecalhoPagina'
import { Resultado, Vazio } from '../../components/Estados'
import { Selo } from '../../components/Selo'
import { formatarDataHora } from '../../utils/format'
import { rotuloAcao } from '../../utils/rotulos'
import { useAsync } from '../../utils/useAsync'

export function QuemAcessouPage() {
  const lista = useAsync(() => meusDados.acessos(), [])
  return (
    <>
      <CabecalhoPagina
        secao="Privacidade"
        titulo="Quem acessou os meus dados"
        descricao="Tudo o que outras pessoas consultaram ou registraram sobre você, inclusive tentativas que foram bloqueadas."
      />
      <section className="painel">
        <Resultado {...lista}>
          {(dados) =>
            dados.length === 0 ? (
              <Vazio icone="olho" titulo="Ninguém acessou os seus dados ainda" />
            ) : (
              <>
                <p className="texto-suave">As 100 ações mais recentes.</p>
                <div className="tabela-rolagem">
                  <table className="tabela">
                    <thead>
                      <tr>
                        <th>Quando</th>
                        <th>Quem</th>
                        <th>Instituição</th>
                        <th>O que fez</th>
                        <th>Resultado</th>
                      </tr>
                    </thead>
                    <tbody>
                      {dados.map((a, i) => (
                        <tr key={`${a.instante}-${i}`}>
                          <td>{formatarDataHora(a.instante)}</td>
                          <td>{a.ator ?? '—'}</td>
                          <td>
                            {a.instituicao ?? '—'}
                            {a.entreInstituicoes && (
                              <>
                                {' '}
                                <Selo tom="roxo">Acesso entre instituições</Selo>
                              </>
                            )}
                          </td>
                          <td>{rotuloAcao(a.acao)}</td>
                          <td>
                            {a.status >= 200 && a.status < 300 ? (
                              <Selo tom="verde">Realizado</Selo>
                            ) : a.status === 403 || a.status === 401 ? (
                              <Selo tom="vermelho">Bloqueado</Selo>
                            ) : (
                              <Selo tom="cinza">Não concluído</Selo>
                            )}
                          </td>
                        </tr>
                      ))}
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
