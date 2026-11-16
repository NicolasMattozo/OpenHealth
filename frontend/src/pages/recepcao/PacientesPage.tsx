import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../../api/client'
import { pacientes } from '../../api/endpoints'
import type { Paciente, PacienteCriado } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { useAvisos } from '../../components/Avisos'
import { Botao } from '../../components/Botao'
import { CabecalhoPagina } from '../../components/CabecalhoPagina'
import { CaixaSenha } from '../../components/CaixaSenha'
import { CampoAreaTexto, CampoTexto } from '../../components/Campos'
import { Icone } from '../../components/Icone'
import { Modal } from '../../components/Modal'
import { Resultado, Vazio } from '../../components/Estados'
import { formatarCpf, formatarData, mascaraCpf, somenteDigitos } from '../../utils/format'
import { useAsync } from '../../utils/useAsync'
import { useEnvio } from '../../utils/useEnvio'
import { AbrirAtendimentoModal, SolicitarAcessoModal } from './modais'

function ResumoPaciente({ paciente }: { paciente: Paciente }) {
  return (
    <dl className="detalhes detalhes-caixa">
      <div className="detalhe">
        <dt>Nome</dt>
        <dd>{paciente.nome}</dd>
      </div>
      <div className="detalhe">
        <dt>CPF</dt>
        <dd>{formatarCpf(paciente.cpf)}</dd>
      </div>
      <div className="detalhe">
        <dt>Nascimento</dt>
        <dd>{formatarData(paciente.dataNascimento)}</dd>
      </div>
      <div className="detalhe">
        <dt>E-mail</dt>
        <dd>{paciente.email}</dd>
      </div>
      <div className="detalhe">
        <dt>Telefone</dt>
        <dd>{paciente.telefone ?? '—'}</dd>
      </div>
      <div className="detalhe">
        <dt>Endereço</dt>
        <dd>{paciente.endereco ?? '—'}</dd>
      </div>
    </dl>
  )
}

/** Cadastro de paciente (caso de uso 2). Se o CPF ja existe, mostra os dados cadastrados (fluxo A1). */
function CadastroPacienteModal({
  cnpj,
  aoFechar,
  aoConcluir,
}: {
  cnpj: string
  aoFechar: () => void
  aoConcluir: (paciente: Paciente) => void
}) {
  const [nome, setNome] = useState('')
  const [cpf, setCpf] = useState('')
  const [email, setEmail] = useState('')
  const [nascimento, setNascimento] = useState('')
  const [telefone, setTelefone] = useState('')
  const [endereco, setEndereco] = useState('')
  const [existente, setExistente] = useState<Paciente | null>(null)
  const [criado, setCriado] = useState<PacienteCriado | null>(null)
  const { enviando, erro, campos, enviar } = useEnvio()

  const submeter = async (e: FormEvent) => {
    e.preventDefault()
    setExistente(null)
    const resultado = await enviar(async () => {
      try {
        return await pacientes.cadastrar(cnpj, { nome, cpf, email, dataNascimento: nascimento, telefone, endereco })
      } catch (erroApi) {
        if (erroApi instanceof ApiError && erroApi.status === 409) {
          const corpo = erroApi.corpo as { paciente?: Paciente } | null
          if (corpo?.paciente) setExistente(corpo.paciente)
        }
        throw erroApi
      }
    })
    if (resultado) setCriado(resultado)
  }

  if (criado) {
    return (
      <Modal titulo="Paciente cadastrado" aoFechar={() => aoConcluir(criado.paciente)}>
        <ResumoPaciente paciente={criado.paciente} />
        <p className="texto-modal">
          Entregue esta senha temporária ao paciente. Ele entra com o CPF e precisará trocá-la no primeiro acesso.
        </p>
        <CaixaSenha senha={criado.senhaTemporaria} />
        <p className="faixa faixa-aviso">Esta senha aparece somente agora.</p>
        <div className="modal-acoes">
          <Botao onClick={() => aoConcluir(criado.paciente)}>Concluir</Botao>
        </div>
      </Modal>
    )
  }

  return (
    <Modal titulo="Cadastrar paciente" subtitulo="Preencha os dados pessoais para concluir o cadastro." aoFechar={aoFechar} larga>
      <form onSubmit={submeter}>
        <div className="grade-formulario">
          <CampoTexto rotulo="Nome completo" value={nome} onChange={(e) => setNome(e.target.value)} erro={campos.nome} larga required autoFocus />
          <CampoTexto
            rotulo="CPF"
            value={cpf}
            onChange={(e) => setCpf(mascaraCpf(e.target.value))}
            placeholder="000.000.000-00"
            erro={campos.cpf}
            required
          />
          <CampoTexto
            rotulo="Data de nascimento"
            type="date"
            value={nascimento}
            onChange={(e) => setNascimento(e.target.value)}
            max={new Date().toISOString().slice(0, 10)}
            erro={campos.dataNascimento}
            required
          />
          <CampoTexto rotulo="E-mail" type="email" value={email} onChange={(e) => setEmail(e.target.value)} erro={campos.email} larga required />
          <CampoTexto rotulo="Telefone" value={telefone} onChange={(e) => setTelefone(e.target.value)} erro={campos.telefone} />
          <CampoTexto rotulo="Endereço" value={endereco} onChange={(e) => setEndereco(e.target.value)} erro={campos.endereco} larga />
        </div>

        {existente && (
          <div className="faixa faixa-aviso">
            <div>
              <strong>Este CPF já está cadastrado.</strong> O novo cadastro foi cancelado. Dados existentes:
              <ResumoPaciente paciente={existente} />
            </div>
          </div>
        )}
        {erro && !existente && <p className="erro-formulario">{erro}</p>}

        <div className="modal-acoes">
          <Botao variante="secundario" onClick={aoFechar} disabled={enviando}>
            Cancelar
          </Botao>
          <Botao type="submit" carregando={enviando}>
            Salvar cadastro
          </Botao>
        </div>
      </form>
    </Modal>
  )
}

function EditarContatoModal({
  cnpj,
  paciente,
  aoFechar,
  aoSalvar,
}: {
  cnpj: string
  paciente: Paciente
  aoFechar: () => void
  aoSalvar: () => void
}) {
  const [telefone, setTelefone] = useState(paciente.telefone ?? '')
  const [endereco, setEndereco] = useState(paciente.endereco ?? '')
  const { enviando, erro, campos, enviar } = useEnvio()

  const submeter = async (e: FormEvent) => {
    e.preventDefault()
    if (await enviar(() => pacientes.atualizarContato(cnpj, paciente.cpf, { telefone, endereco }))) aoSalvar()
  }

  return (
    <Modal titulo="Dados de contato" subtitulo={`${paciente.nome} · CPF ${formatarCpf(paciente.cpf)}`} aoFechar={aoFechar}>
      <form onSubmit={submeter}>
        <div className="grade-formulario">
          <CampoTexto rotulo="Telefone" value={telefone} onChange={(e) => setTelefone(e.target.value)} erro={campos.telefone} larga autoFocus />
          <CampoAreaTexto rotulo="Endereço" value={endereco} onChange={(e) => setEndereco(e.target.value)} erro={campos.endereco} larga />
        </div>
        <p className="texto-suave">Nome, CPF e data de nascimento não podem ser alterados por aqui.</p>
        {erro && <p className="erro-formulario">{erro}</p>}
        <div className="modal-acoes">
          <Botao variante="secundario" onClick={aoFechar} disabled={enviando}>
            Cancelar
          </Botao>
          <Botao type="submit" carregando={enviando}>
            Salvar
          </Botao>
        </div>
      </form>
    </Modal>
  )
}

type Acao = { tipo: 'cadastro' } | { tipo: 'contato' | 'atendimento' | 'acesso'; paciente: Paciente }

export function PacientesPage() {
  const { instituicaoAtiva } = useAuth()
  const cnpj = instituicaoAtiva as string
  const avisos = useAvisos()
  const navegar = useNavigate()
  const [texto, setTexto] = useState('')
  const [busca, setBusca] = useState<string | null>(null)
  const [erroBusca, setErroBusca] = useState<string | null>(null)
  const [acao, setAcao] = useState<Acao | null>(null)
  const resultado = useAsync(() => pacientes.buscar(cnpj, busca ?? ''), [cnpj, busca], busca !== null)

  const buscar = (e: FormEvent) => {
    e.preventDefault()
    const limpo = texto.trim()
    if (limpo.length < 3 && somenteDigitos(limpo).length < 11) {
      setErroBusca('Digite o CPF completo ou pelo menos 3 letras do nome.')
      return
    }
    setErroBusca(null)
    setBusca(limpo)
  }

  const fechar = () => setAcao(null)

  return (
    <>
      <CabecalhoPagina
        secao="Recepção"
        titulo="Pacientes"
        descricao="Busque um paciente pelo CPF ou pelo nome, ou faça um novo cadastro."
        acao={
          <Botao icone="mais" onClick={() => setAcao({ tipo: 'cadastro' })}>
            Cadastrar paciente
          </Botao>
        }
      />

      <section className="painel">
        <form className="filtros" onSubmit={buscar}>
          <div className="campo-busca">
            <Icone nome="busca" />
            <input
              placeholder="CPF ou nome do paciente..."
              value={texto}
              onChange={(e) => setTexto(e.target.value)}
              aria-label="Buscar paciente"
              autoFocus
            />
          </div>
          <Botao type="submit">Buscar</Botao>
        </form>
        {erroBusca && <p className="erro-formulario">{erroBusca}</p>}

        {busca === null ? (
          <Vazio icone="paciente" titulo="Faça uma busca para encontrar um paciente">
            Por segurança, a lista completa de pacientes não é exibida.
          </Vazio>
        ) : (
          <Resultado {...resultado}>
            {(dados) =>
              dados.length === 0 ? (
                <Vazio icone="paciente" titulo="Nenhum paciente encontrado">
                  Confira o CPF ou cadastre o paciente.
                </Vazio>
              ) : (
                <div className="tabela-rolagem">
                  <table className="tabela">
                    <thead>
                      <tr>
                        <th>Paciente</th>
                        <th>CPF</th>
                        <th>Nascimento</th>
                        <th>Contato</th>
                        <th aria-label="Ações" />
                      </tr>
                    </thead>
                    <tbody>
                      {dados.map((p) => (
                        <tr key={p.cpf}>
                          <td>
                            <strong>{p.nome}</strong>
                            <small className="sub">{p.email}</small>
                          </td>
                          <td>{formatarCpf(p.cpf)}</td>
                          <td>{formatarData(p.dataNascimento)}</td>
                          <td>{p.telefone ?? '—'}</td>
                          <td className="acoes">
                            <Botao pequeno icone="prancheta" onClick={() => setAcao({ tipo: 'atendimento', paciente: p })}>
                              Abrir atendimento
                            </Botao>
                            <Botao variante="secundario" pequeno icone="chave" onClick={() => setAcao({ tipo: 'acesso', paciente: p })}>
                              Solicitar acesso
                            </Botao>
                            <Botao variante="fantasma" pequeno icone="editar" onClick={() => setAcao({ tipo: 'contato', paciente: p })}>
                              Contato
                            </Botao>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )
            }
          </Resultado>
        )}
      </section>

      {acao?.tipo === 'cadastro' && (
        <CadastroPacienteModal
          cnpj={cnpj}
          aoFechar={fechar}
          aoConcluir={(p) => {
            fechar()
            avisos.sucesso(`Paciente ${p.nome} cadastrado.`)
            setTexto(p.cpf)
            setBusca(p.cpf)
          }}
        />
      )}
      {acao?.tipo === 'contato' && (
        <EditarContatoModal
          cnpj={cnpj}
          paciente={acao.paciente}
          aoFechar={fechar}
          aoSalvar={() => {
            fechar()
            avisos.sucesso('Contato atualizado.')
            resultado.recarregar()
          }}
        />
      )}
      {acao?.tipo === 'atendimento' && (
        <AbrirAtendimentoModal
          cnpj={cnpj}
          paciente={acao.paciente}
          aoFechar={fechar}
          aoAberto={() => {
            fechar()
            avisos.sucesso('Atendimento aberto.')
            navegar('/atendimentos')
          }}
        />
      )}
      {acao?.tipo === 'acesso' && (
        <SolicitarAcessoModal
          cnpj={cnpj}
          paciente={acao.paciente}
          aoFechar={fechar}
          aoSolicitado={() => {
            fechar()
            avisos.sucesso('Solicitação enviada ao paciente.')
            navegar('/solicitacoes')
          }}
        />
      )}
    </>
  )
}
