import { useState } from 'react'
import { ApiError, mensagemDoErro } from '../api/client'

/** Controla o envio de um formulario: estado "enviando", mensagem de erro e erros por campo. */
export function useEnvio() {
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [campos, setCampos] = useState<Record<string, string>>({})

  /** Executa a acao; devolve o resultado, ou undefined se deu erro (que fica em "erro" e "campos"). */
  async function enviar<T>(acao: () => Promise<T>): Promise<T | undefined> {
    setEnviando(true)
    setErro(null)
    setCampos({})
    try {
      return await acao()
    } catch (e) {
      setErro(e instanceof ApiError && Object.keys(e.campos).length > 0 ? e.message : mensagemDoErro(e))
      if (e instanceof ApiError) setCampos(e.campos)
      return undefined
    } finally {
      setEnviando(false)
    }
  }

  return { enviando, erro, campos, enviar, limparErro: () => setErro(null) }
}
