import { useCallback, useEffect, useRef, useState } from 'react'
import { mensagemDoErro } from '../api/client'

interface Estado<T> {
  dados: T | null
  erro: string | null
  carregando: boolean
}

/**
 * Carrega dados quando o componente aparece ou quando "deps" mudam.
 * "recarregar()" repete a chamada (ex.: depois de salvar). "ativo = false" suspende a carga.
 */
export function useAsync<T>(buscar: () => Promise<T>, deps: unknown[], ativo = true) {
  const [estado, setEstado] = useState<Estado<T>>({ dados: null, erro: null, carregando: ativo })
  const [versao, setVersao] = useState(0)
  const buscarRef = useRef(buscar)
  buscarRef.current = buscar

  useEffect(() => {
    if (!ativo) {
      setEstado({ dados: null, erro: null, carregando: false })
      return
    }
    let cancelado = false
    setEstado((anterior) => ({ ...anterior, erro: null, carregando: true }))
    buscarRef
      .current()
      .then((dados) => {
        if (!cancelado) setEstado({ dados, erro: null, carregando: false })
      })
      .catch((erro: unknown) => {
        if (!cancelado) setEstado({ dados: null, erro: mensagemDoErro(erro), carregando: false })
      })
    return () => {
      cancelado = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, versao, ativo])

  const recarregar = useCallback(() => setVersao((v) => v + 1), [])
  return { ...estado, recarregar }
}
