import { useEffect, type ReactNode } from 'react'
import { Icone } from './Icone'

interface Props {
  titulo: string
  subtitulo?: string
  aoFechar: () => void
  larga?: boolean
  children: ReactNode
}

export function Modal({ titulo, subtitulo, aoFechar, larga, children }: Props) {
  useEffect(() => {
    const aoTeclar = (e: KeyboardEvent) => {
      if (e.key === 'Escape') aoFechar()
    }
    document.addEventListener('keydown', aoTeclar)
    return () => document.removeEventListener('keydown', aoTeclar)
  }, [aoFechar])

  return (
    <div
      className="modal-fundo"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) aoFechar()
      }}
    >
      <div className={`modal ${larga ? 'modal-larga' : ''}`} role="dialog" aria-modal="true" aria-label={titulo}>
        <div className="modal-cabecalho">
          <div>
            <h2>{titulo}</h2>
            {subtitulo && <p>{subtitulo}</p>}
          </div>
          <button type="button" className="btn-icone" onClick={aoFechar} aria-label="Fechar">
            <Icone nome="x" />
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}
