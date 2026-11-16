import { useState } from 'react'
import { Botao } from './Botao'

/** Mostra a senha temporaria (so aparece uma vez) com botao de copiar. */
export function CaixaSenha({ senha }: { senha: string }) {
  const [copiada, setCopiada] = useState(false)

  const copiar = async () => {
    try {
      await navigator.clipboard.writeText(senha)
      setCopiada(true)
      window.setTimeout(() => setCopiada(false), 2500)
    } catch {
      // sem permissao de area de transferencia: o usuario seleciona e copia manualmente
    }
  }

  return (
    <div className="caixa-senha">
      <code aria-label="Senha temporária">{senha}</code>
      <Botao variante="secundario" pequeno icone={copiada ? 'ok' : 'copiar'} onClick={copiar}>
        {copiada ? 'Copiada' : 'Copiar'}
      </Botao>
    </div>
  )
}
