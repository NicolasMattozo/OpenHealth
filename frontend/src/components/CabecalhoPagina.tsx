import type { ReactNode } from 'react'

export function CabecalhoPagina({
  secao,
  titulo,
  descricao,
  acao,
}: {
  secao?: string
  titulo: string
  descricao?: string
  acao?: ReactNode
}) {
  return (
    <header className="cabecalho-pagina">
      <div>
        {secao && <span className="secao">{secao}</span>}
        <h1>{titulo}</h1>
        {descricao && <p>{descricao}</p>}
      </div>
      {acao && <div className="cabecalho-acao">{acao}</div>}
    </header>
  )
}
