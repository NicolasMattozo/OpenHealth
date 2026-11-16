import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { Icone, type NomeIconeUi } from './Icone'

type Variante = 'primario' | 'secundario' | 'perigo' | 'fantasma'

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variante?: Variante
  pequeno?: boolean
  carregando?: boolean
  icone?: NomeIconeUi
  children?: ReactNode
}

export function Botao({ variante = 'primario', pequeno, carregando, icone, children, className, disabled, type, ...resto }: Props) {
  const classes = ['btn', `btn-${variante}`, pequeno ? 'btn-pequeno' : '', className ?? ''].filter(Boolean).join(' ')
  return (
    <button type={type ?? 'button'} className={classes} disabled={disabled || carregando} {...resto}>
      {carregando ? <span className="spinner spinner-pequeno" aria-hidden="true" /> : icone ? <Icone nome={icone} tamanho={16} /> : null}
      {children}
    </button>
  )
}
