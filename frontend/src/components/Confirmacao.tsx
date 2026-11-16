import { useState } from 'react'
import { mensagemDoErro } from '../api/client'
import { Botao } from './Botao'
import { Modal } from './Modal'

interface Props {
  titulo: string
  mensagem: string
  rotuloConfirmar: string
  perigo?: boolean
  aoConfirmar: () => Promise<void>
  aoCancelar: () => void
}

export function Confirmacao({ titulo, mensagem, rotuloConfirmar, perigo, aoConfirmar, aoCancelar }: Props) {
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  const confirmar = async () => {
    setEnviando(true)
    setErro(null)
    try {
      await aoConfirmar()
    } catch (e) {
      setErro(mensagemDoErro(e))
      setEnviando(false)
    }
  }

  return (
    <Modal titulo={titulo} aoFechar={aoCancelar}>
      <p className="texto-modal">{mensagem}</p>
      {erro && <p className="erro-formulario">{erro}</p>}
      <div className="modal-acoes">
        <Botao variante="secundario" onClick={aoCancelar} disabled={enviando}>
          Cancelar
        </Botao>
        <Botao variante={perigo ? 'perigo' : 'primario'} onClick={confirmar} carregando={enviando}>
          {rotuloConfirmar}
        </Botao>
      </div>
    </Modal>
  )
}
