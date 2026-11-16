import type { ReactNode } from 'react'
import { Botao } from './Botao'
import { Icone, type NomeIconeUi } from './Icone'

export function Carregando({ texto = 'Carregando...' }: { texto?: string }) {
  return (
    <div className="estado" role="status">
      <span className="spinner" aria-hidden="true" />
      <span>{texto}</span>
    </div>
  )
}

export function CaixaErro({ mensagem, aoTentarNovamente }: { mensagem: string; aoTentarNovamente?: () => void }) {
  return (
    <div className="estado estado-erro" role="alert">
      <Icone nome="alerta" tamanho={22} />
      <span>{mensagem}</span>
      {aoTentarNovamente && (
        <Botao variante="secundario" pequeno onClick={aoTentarNovamente}>
          Tentar novamente
        </Botao>
      )}
    </div>
  )
}

export function Vazio({ icone = 'documento', titulo, children }: { icone?: NomeIconeUi; titulo: string; children?: ReactNode }) {
  return (
    <div className="estado vazio">
      <Icone nome={icone} tamanho={28} />
      <strong>{titulo}</strong>
      {children && <span>{children}</span>}
    </div>
  )
}

/** Mostra carregando / erro / conteudo, conforme o resultado de useAsync. */
export function Resultado<T>({
  carregando,
  erro,
  dados,
  recarregar,
  children,
}: {
  carregando: boolean
  erro: string | null
  dados: T | null
  recarregar?: () => void
  children: (dados: T) => ReactNode
}) {
  if (carregando && dados === null) return <Carregando />
  if (erro) return <CaixaErro mensagem={erro} aoTentarNovamente={recarregar} />
  if (dados === null) return null
  return <>{children(dados)}</>
}
