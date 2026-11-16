import { useId, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes, type TextareaHTMLAttributes } from 'react'

interface CampoProps {
  id: string
  rotulo: string
  erro?: string
  dica?: string
  larga?: boolean
  children: ReactNode
}

/**
 * Rotulo + controle + dica/erro. O rotulo aponta para o controle (htmlFor) e a dica/erro ficam
 * em aria-describedby, entao o nome do campo para leitores de tela e so o rotulo.
 */
export function Campo({ id, rotulo, erro, dica, larga, children }: CampoProps) {
  return (
    <div className={`campo ${larga ? 'larga' : ''} ${erro ? 'com-erro' : ''}`}>
      <label htmlFor={id} className="campo-rotulo">
        {rotulo}
      </label>
      {children}
      {dica && !erro && (
        <small id={`${id}-dica`} className="campo-dica">
          {dica}
        </small>
      )}
      {erro && (
        <small id={`${id}-erro`} role="alert" className="campo-erro">
          {erro}
        </small>
      )}
    </div>
  )
}

type PropsComuns = { rotulo: string; erro?: string; dica?: string; larga?: boolean }

function descricaoDe(id: string, erro?: string, dica?: string) {
  return erro ? `${id}-erro` : dica ? `${id}-dica` : undefined
}

export function CampoTexto({ rotulo, erro, dica, larga, id: idFixo, ...resto }: PropsComuns & InputHTMLAttributes<HTMLInputElement>) {
  const gerado = useId()
  const id = idFixo ?? gerado
  return (
    <Campo id={id} rotulo={rotulo} erro={erro} dica={dica} larga={larga}>
      <input id={id} className="entrada" aria-describedby={descricaoDe(id, erro, dica)} aria-invalid={erro ? true : undefined} {...resto} />
    </Campo>
  )
}

export function CampoAreaTexto({ rotulo, erro, dica, larga, id: idFixo, ...resto }: PropsComuns & TextareaHTMLAttributes<HTMLTextAreaElement>) {
  const gerado = useId()
  const id = idFixo ?? gerado
  return (
    <Campo id={id} rotulo={rotulo} erro={erro} dica={dica} larga={larga}>
      <textarea id={id} className="entrada" rows={3} aria-describedby={descricaoDe(id, erro, dica)} aria-invalid={erro ? true : undefined} {...resto} />
    </Campo>
  )
}

export function CampoSelect({
  rotulo,
  erro,
  dica,
  larga,
  children,
  id: idFixo,
  ...resto
}: PropsComuns & SelectHTMLAttributes<HTMLSelectElement>) {
  const gerado = useId()
  const id = idFixo ?? gerado
  return (
    <Campo id={id} rotulo={rotulo} erro={erro} dica={dica} larga={larga}>
      <select id={id} className="entrada" aria-describedby={descricaoDe(id, erro, dica)} aria-invalid={erro ? true : undefined} {...resto}>
        {children}
      </select>
    </Campo>
  )
}

export function CampoCheck({
  rotulo,
  dica,
  id: idFixo,
  ...resto
}: { rotulo: string; dica?: string } & Omit<InputHTMLAttributes<HTMLInputElement>, 'type'>) {
  const gerado = useId()
  const id = idFixo ?? gerado
  return (
    <div className="campo-check">
      <input id={id} type="checkbox" aria-describedby={dica ? `${id}-dica` : undefined} {...resto} />
      <div>
        <label htmlFor={id}>{rotulo}</label>
        {dica && (
          <small id={`${id}-dica`} className="campo-dica">
            {dica}
          </small>
        )}
      </div>
    </div>
  )
}
