import { useState } from 'react'
import { meusDados } from '../../api/endpoints'
import { Abas } from '../../components/Abas'
import { CabecalhoPagina } from '../../components/CabecalhoPagina'
import { Resultado, Vazio } from '../../components/Estados'
import { LinhaDoTempo } from '../../components/LinhaDoTempo'
import { useAsync } from '../../utils/useAsync'

type IdAba = 'resultados' | 'requisicoes'

export function MeusExamesPage() {
  const [aba, setAba] = useState<IdAba>('resultados')
  const exames = useAsync(() => meusDados.exames(), [], aba === 'resultados')
  const requisicoes = useAsync(() => meusDados.requisicoes(), [], aba === 'requisicoes')
  const atual = aba === 'resultados' ? exames : requisicoes

  return (
    <>
      <CabecalhoPagina secao="Meu prontuário" titulo="Exames" descricao="Exames requisitados para você e os seus resultados." />
      <section className="painel">
        <Abas
          itens={[
            { id: 'resultados', rotulo: 'Com resultado' },
            { id: 'requisicoes', rotulo: 'Todas as requisições' },
          ]}
          ativa={aba}
          aoMudar={setAba}
        />
        <Resultado {...atual}>
          {(dados) =>
            dados.length === 0 ? (
              <Vazio icone="frasco" titulo={aba === 'resultados' ? 'Nenhum resultado disponível ainda' : 'Nenhum exame requisitado'} />
            ) : (
              <LinhaDoTempo registros={dados} mostrarVisibilidade={false} />
            )
          }
        </Resultado>
      </section>
    </>
  )
}
