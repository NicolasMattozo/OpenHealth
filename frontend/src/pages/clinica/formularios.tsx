import { useState, type FormEvent, type ReactNode } from 'react'
import { clinica } from '../../api/endpoints'
import type { ClassificacaoRisco, RegistroClinico } from '../../api/types'
import { useAvisos } from '../../components/Avisos'
import { Botao } from '../../components/Botao'
import { CampoAreaTexto, CampoCheck, CampoSelect, CampoTexto } from '../../components/Campos'
import { Modal } from '../../components/Modal'
import { ROTULO_RISCO } from '../../utils/rotulos'
import { useEnvio } from '../../utils/useEnvio'

interface Props {
  cnpj: string
  idAtendimento: number
  aoSalvar: () => void
}

function Moldura({
  aoEnviar,
  enviando,
  erro,
  rotulo,
  visivel,
  aoMudarVisivel,
  children,
}: {
  aoEnviar: (e: FormEvent) => void
  enviando: boolean
  erro: string | null
  rotulo: string
  visivel: boolean
  aoMudarVisivel: (v: boolean) => void
  children: ReactNode
}) {
  return (
    <form onSubmit={aoEnviar}>
      <div className="grade-formulario">{children}</div>
      <CampoCheck
        rotulo="Visível ao paciente"
        dica="Desmarque para guardar este registro só para os profissionais."
        checked={visivel}
        onChange={(e) => aoMudarVisivel(e.target.checked)}
      />
      {erro && <p className="erro-formulario">{erro}</p>}
      <div className="modal-acoes">
        <Botao type="submit" carregando={enviando}>
          {rotulo}
        </Botao>
      </div>
    </form>
  )
}

export function FormTriagem({ cnpj, idAtendimento, aoSalvar }: Props) {
  const avisos = useAvisos()
  const [risco, setRisco] = useState<ClassificacaoRisco>('VERDE')
  const [sintomas, setSintomas] = useState('')
  const [visivel, setVisivel] = useState(true)
  const { enviando, erro, campos, enviar } = useEnvio()

  const submeter = async (e: FormEvent) => {
    e.preventDefault()
    const feito = await enviar(() => clinica.triagem(cnpj, idAtendimento, { classificacaoRisco: risco, sintomas, visivelAoPaciente: visivel }))
    if (feito) {
      avisos.sucesso('Triagem registrada.')
      setSintomas('')
      aoSalvar()
    }
  }

  return (
    <Moldura aoEnviar={submeter} enviando={enviando} erro={erro} rotulo="Registrar triagem" visivel={visivel} aoMudarVisivel={setVisivel}>
      <CampoSelect rotulo="Classificação de risco" value={risco} onChange={(e) => setRisco(e.target.value as ClassificacaoRisco)} larga>
        {(Object.keys(ROTULO_RISCO) as ClassificacaoRisco[]).map((r) => (
          <option key={r} value={r}>
            {ROTULO_RISCO[r]}
          </option>
        ))}
      </CampoSelect>
      <CampoAreaTexto
        rotulo="Queixa principal e sintomas"
        value={sintomas}
        onChange={(e) => setSintomas(e.target.value)}
        maxLength={2000}
        erro={campos.sintomas}
        larga
      />
    </Moldura>
  )
}

const SUGESTOES_DADOS = ['Pressão arterial', 'Temperatura', 'Frequência cardíaca', 'Frequência respiratória', 'Saturação de oxigênio', 'Peso', 'Altura', 'Glicemia']

export function FormDadoClinico({ cnpj, idAtendimento, aoSalvar }: Props) {
  const avisos = useAvisos()
  const [tipo, setTipo] = useState('')
  const [valor, setValor] = useState('')
  const [visivel, setVisivel] = useState(true)
  const { enviando, erro, campos, enviar } = useEnvio()

  const submeter = async (e: FormEvent) => {
    e.preventDefault()
    const feito = await enviar(() => clinica.dadoClinico(cnpj, idAtendimento, { tipo, valor, visivelAoPaciente: visivel }))
    if (feito) {
      avisos.sucesso('Dado clínico registrado.')
      setTipo('')
      setValor('')
      aoSalvar()
    }
  }

  return (
    <Moldura aoEnviar={submeter} enviando={enviando} erro={erro} rotulo="Registrar dado clínico" visivel={visivel} aoMudarVisivel={setVisivel}>
      <CampoTexto rotulo="Dado" value={tipo} onChange={(e) => setTipo(e.target.value)} list="sugestoes-dados" maxLength={60} erro={campos.tipo} required />
      <datalist id="sugestoes-dados">
        {SUGESTOES_DADOS.map((s) => (
          <option key={s} value={s} />
        ))}
      </datalist>
      <CampoTexto rotulo="Valor" value={valor} onChange={(e) => setValor(e.target.value)} maxLength={500} placeholder="Ex.: 120/80 mmHg" erro={campos.valor} required />
    </Moldura>
  )
}

export function FormDiagnostico({ cnpj, idAtendimento, aoSalvar }: Props) {
  const avisos = useAvisos()
  const [descricao, setDescricao] = useState('')
  const [cid, setCid] = useState('')
  const [visivel, setVisivel] = useState(true)
  const { enviando, erro, campos, enviar } = useEnvio()

  const submeter = async (e: FormEvent) => {
    e.preventDefault()
    const feito = await enviar(() => clinica.diagnostico(cnpj, idAtendimento, { descricao, cid, visivelAoPaciente: visivel }))
    if (feito) {
      avisos.sucesso('Diagnóstico registrado.')
      setDescricao('')
      setCid('')
      aoSalvar()
    }
  }

  return (
    <Moldura aoEnviar={submeter} enviando={enviando} erro={erro} rotulo="Registrar diagnóstico" visivel={visivel} aoMudarVisivel={setVisivel}>
      <CampoAreaTexto rotulo="Condição / diagnóstico" value={descricao} onChange={(e) => setDescricao(e.target.value)} maxLength={2000} erro={campos.descricao} larga required />
      <CampoTexto rotulo="CID" value={cid} onChange={(e) => setCid(e.target.value.toUpperCase())} maxLength={10} placeholder="Ex.: J06.9" erro={campos.cid} />
    </Moldura>
  )
}

const VIAS = ['Oral', 'Intravenosa', 'Intramuscular', 'Subcutânea', 'Tópica', 'Inalatória', 'Sublingual', 'Retal']
const FREQUENCIAS = ['A cada 4 horas', 'A cada 6 horas', 'A cada 8 horas', 'A cada 12 horas', 'Uma vez ao dia', 'Se necessário']

export function FormPrescricao({ cnpj, idAtendimento, aoSalvar }: Props) {
  const avisos = useAvisos()
  const [medicamento, setMedicamento] = useState('')
  const [dosagem, setDosagem] = useState('')
  const [via, setVia] = useState('Oral')
  const [frequencia, setFrequencia] = useState('')
  const [visivel, setVisivel] = useState(true)
  const { enviando, erro, campos, enviar } = useEnvio()

  const submeter = async (e: FormEvent) => {
    e.preventDefault()
    const feito = await enviar(() =>
      clinica.prescricao(cnpj, idAtendimento, { medicamento, dosagem, via, frequencia, visivelAoPaciente: visivel }),
    )
    if (feito) {
      avisos.sucesso('Prescrição registrada.')
      setMedicamento('')
      setDosagem('')
      setFrequencia('')
      aoSalvar()
    }
  }

  return (
    <Moldura aoEnviar={submeter} enviando={enviando} erro={erro} rotulo="Registrar prescrição" visivel={visivel} aoMudarVisivel={setVisivel}>
      <CampoTexto rotulo="Medicamento" value={medicamento} onChange={(e) => setMedicamento(e.target.value)} maxLength={200} erro={campos.medicamento} larga required />
      <CampoTexto rotulo="Dosagem" value={dosagem} onChange={(e) => setDosagem(e.target.value)} maxLength={100} placeholder="Ex.: 500 mg" erro={campos.dosagem} required />
      <CampoSelect rotulo="Via" value={via} onChange={(e) => setVia(e.target.value)}>
        {VIAS.map((v) => (
          <option key={v}>{v}</option>
        ))}
      </CampoSelect>
      <CampoTexto rotulo="Frequência" value={frequencia} onChange={(e) => setFrequencia(e.target.value)} list="sugestoes-frequencia" maxLength={100} erro={campos.frequencia} larga />
      <datalist id="sugestoes-frequencia">
        {FREQUENCIAS.map((f) => (
          <option key={f} value={f} />
        ))}
      </datalist>
    </Moldura>
  )
}

const EXAMES = ['Hemograma completo', 'Glicemia de jejum', 'Raio-X de tórax', 'Eletrocardiograma', 'Ultrassonografia abdominal', 'Exame de urina']

export function FormExame({ cnpj, idAtendimento, aoSalvar }: Props) {
  const avisos = useAvisos()
  const [tipoExame, setTipoExame] = useState('')
  const [visivel, setVisivel] = useState(true)
  const { enviando, erro, campos, enviar } = useEnvio()

  const submeter = async (e: FormEvent) => {
    e.preventDefault()
    const feito = await enviar(() => clinica.requisicaoExame(cnpj, idAtendimento, { tipoExame, visivelAoPaciente: visivel }))
    if (feito) {
      avisos.sucesso('Exame requisitado.')
      setTipoExame('')
      aoSalvar()
    }
  }

  return (
    <Moldura aoEnviar={submeter} enviando={enviando} erro={erro} rotulo="Requisitar exame" visivel={visivel} aoMudarVisivel={setVisivel}>
      <CampoTexto rotulo="Exame" value={tipoExame} onChange={(e) => setTipoExame(e.target.value)} list="sugestoes-exames" maxLength={200} erro={campos.tipoExame} larga required />
      <datalist id="sugestoes-exames">
        {EXAMES.map((x) => (
          <option key={x} value={x} />
        ))}
      </datalist>
    </Moldura>
  )
}

export function ResultadoModal({
  cnpj,
  requisicao,
  aoFechar,
  aoSalvar,
}: {
  cnpj: string
  requisicao: RegistroClinico
  aoFechar: () => void
  aoSalvar: () => void
}) {
  const [texto, setTexto] = useState('')
  const { enviando, erro, campos, enviar } = useEnvio()
  const jaTem = (requisicao.resultados ?? []).length > 0

  const submeter = async (e: FormEvent) => {
    e.preventDefault()
    if (await enviar(() => clinica.lancarResultado(cnpj, requisicao.id, texto))) aoSalvar()
  }

  return (
    <Modal
      titulo={jaTem ? 'Lançar nova versão do resultado' : 'Lançar resultado'}
      subtitulo={requisicao.tipoExame ?? undefined}
      aoFechar={aoFechar}
    >
      <form onSubmit={submeter}>
        <div className="grade-formulario">
          <CampoAreaTexto rotulo="Resultado" value={texto} onChange={(e) => setTexto(e.target.value)} rows={6} maxLength={5000} erro={campos.resultado} larga required autoFocus />
        </div>
        {jaTem && <p className="texto-suave">A versão anterior não é apagada: continua no histórico como substituída.</p>}
        {erro && <p className="erro-formulario">{erro}</p>}
        <div className="modal-acoes">
          <Botao variante="secundario" onClick={aoFechar} disabled={enviando}>
            Cancelar
          </Botao>
          <Botao type="submit" carregando={enviando}>
            Salvar resultado
          </Botao>
        </div>
      </form>
    </Modal>
  )
}
