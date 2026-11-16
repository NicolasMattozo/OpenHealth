import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { Icone } from './Icone'

interface Aviso {
  id: number
  tipo: 'sucesso' | 'erro'
  texto: string
}

interface Avisos {
  sucesso: (texto: string) => void
  erro: (texto: string) => void
}

const Contexto = createContext<Avisos | null>(null)
let contador = 0

export function AvisosProvider({ children }: { children: ReactNode }) {
  const [avisos, setAvisos] = useState<Aviso[]>([])

  const remover = useCallback((id: number) => setAvisos((lista) => lista.filter((a) => a.id !== id)), [])
  const adicionar = useCallback(
    (tipo: Aviso['tipo'], texto: string) => {
      const id = ++contador
      setAvisos((lista) => [...lista, { id, tipo, texto }])
      window.setTimeout(() => remover(id), tipo === 'erro' ? 7000 : 4000)
    },
    [remover],
  )

  const valor = useMemo<Avisos>(
    () => ({ sucesso: (t) => adicionar('sucesso', t), erro: (t) => adicionar('erro', t) }),
    [adicionar],
  )

  return (
    <Contexto.Provider value={valor}>
      {children}
      <div className="avisos" role="status" aria-live="polite">
        {avisos.map((a) => (
          <div key={a.id} className={`aviso aviso-${a.tipo}`}>
            <Icone nome={a.tipo === 'sucesso' ? 'ok' : 'alerta'} />
            <span>{a.texto}</span>
            <button type="button" className="btn-icone" onClick={() => remover(a.id)} aria-label="Fechar aviso">
              <Icone nome="x" tamanho={14} />
            </button>
          </div>
        ))}
      </div>
    </Contexto.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAvisos(): Avisos {
  const contexto = useContext(Contexto)
  if (!contexto) throw new Error('useAvisos precisa estar dentro de <AvisosProvider>')
  return contexto
}
