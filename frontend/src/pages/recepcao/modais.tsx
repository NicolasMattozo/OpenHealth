import { useState, type FormEvent } from 'react'
import { atendimentos, solicitacoes } from '../../api/endpoints'
import type { Atendimento, SolicitacaoAcesso } from '../../api/types'
import { Botao } from '../../components/Botao'
import { CampoAreaTexto, CampoTexto } from '../../components/Campos'
import { Modal } from '../../components/Modal'
import { mascaraCpf } from '../../utils/format'
import { useEnvio } from '../../utils/useEnvio'

interface PacienteResumo {
  cpf: string
  nome: string
}

/** Abre o atendimento de um paciente (caso de uso 4). Sem "paciente", pede o CPF. */
export function AbrirAtendimentoModal({
  cnpj,
  paciente,
  aoFechar,
  aoAberto,
}: {
  cnpj: string
  paciente?: PacienteResumo
  aoFechar: () => void
  aoAberto: (atendimento: Atendimento) => void
}) {
  const [cpf, setCpf] = useState(paciente?.cpf ?? '')
  const [motivo, setMotivo] = useState('')
  const { enviando, erro, campos, enviar } = useEnvio()

  const submeter = async (e: FormEvent) => {
    e.preventDefault()
    const aberto = await enviar(() => atendimentos.abrir(cnpj, cpf, motivo))
    if (aberto) aoAberto(aberto)
  }

  return (
    <Modal
      titulo="Abrir atendimento"
      subtitulo={paciente ? `Paciente: ${paciente.nome}` : 'Informe o CPF de um paciente já cadastrado.'}
      aoFechar={aoFechar}
    >
      <form onSubmit={submeter}>
        <div className="grade-formulario">
          {!paciente && (
            <CampoTexto
              rotulo="CPF do paciente"
              value={mascaraCpf(cpf)}
              onChange={(e) => setCpf(e.target.value)}
              placeholder="000.000.000-00"
              erro={campos.cpfPaciente}
              larga
              required
              autoFocus
            />
          )}
          <CampoAreaTexto
            rotulo="Motivo do atendimento"
            value={motivo}
            onChange={(e) => setMotivo(e.target.value)}
            maxLength={300}
            erro={campos.motivo}
            larga
            required
            autoFocus={!!paciente}
          />
        </div>
        {erro && <p className="erro-formulario">{erro}</p>}
        <div className="modal-acoes">
          <Botao variante="secundario" onClick={aoFechar} disabled={enviando}>
            Cancelar
          </Botao>
          <Botao type="submit" carregando={enviando}>
            Abrir atendimento
          </Botao>
        </div>
      </form>
    </Modal>
  )
}

/** Pede acesso aos dados de um paciente em nome da instituicao (caso de uso 18). */
export function SolicitarAcessoModal({
  cnpj,
  paciente,
  aoFechar,
  aoSolicitado,
}: {
  cnpj: string
  paciente?: PacienteResumo
  aoFechar: () => void
  aoSolicitado: (solicitacao: SolicitacaoAcesso) => void
}) {
  const [cpf, setCpf] = useState(paciente?.cpf ?? '')
  const [finalidade, setFinalidade] = useState('')
  const { enviando, erro, campos, enviar } = useEnvio()

  const submeter = async (e: FormEvent) => {
    e.preventDefault()
    const criada = await enviar(() => solicitacoes.solicitar(cnpj, cpf, finalidade))
    if (criada) aoSolicitado(criada)
  }

  return (
    <Modal
      titulo="Solicitar acesso aos dados"
      subtitulo={
        paciente
          ? `Paciente: ${paciente.nome}. O acesso só é liberado se o paciente aprovar.`
          : 'O acesso só é liberado se o paciente aprovar.'
      }
      aoFechar={aoFechar}
    >
      <form onSubmit={submeter}>
        <div className="grade-formulario">
          {!paciente && (
            <CampoTexto
              rotulo="CPF do paciente"
              value={mascaraCpf(cpf)}
              onChange={(e) => setCpf(e.target.value)}
              placeholder="000.000.000-00"
              erro={campos.cpfPaciente}
              larga
              required
              autoFocus
            />
          )}
          <CampoAreaTexto
            rotulo="Finalidade"
            dica="O paciente verá este texto ao decidir. Explique por que a instituição precisa dos dados."
            value={finalidade}
            onChange={(e) => setFinalidade(e.target.value)}
            maxLength={300}
            erro={campos.finalidade}
            larga
            required
            autoFocus={!!paciente}
          />
        </div>
        {erro && <p className="erro-formulario">{erro}</p>}
        <div className="modal-acoes">
          <Botao variante="secundario" onClick={aoFechar} disabled={enviando}>
            Cancelar
          </Botao>
          <Botao type="submit" carregando={enviando}>
            Enviar solicitação
          </Botao>
        </div>
      </form>
    </Modal>
  )
}
