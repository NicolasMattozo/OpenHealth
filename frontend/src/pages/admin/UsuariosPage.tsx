import { useState, type FormEvent } from 'react'
import { departamentos, usuarios } from '../../api/endpoints'
import type { Papel, UsuarioCriado, UsuarioInstituicao } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { useAvisos } from '../../components/Avisos'
import { Botao } from '../../components/Botao'
import { CabecalhoPagina } from '../../components/CabecalhoPagina'
import { CaixaSenha } from '../../components/CaixaSenha'
import { CampoCheck, CampoSelect, CampoTexto } from '../../components/Campos'
import { Confirmacao } from '../../components/Confirmacao'
import { Icone } from '../../components/Icone'
import { Modal } from '../../components/Modal'
import { Carregando, CaixaErro, Resultado, Vazio } from '../../components/Estados'
import { SeletorInstituicao, useInstituicoesAdministraveis } from '../../components/SeletorInstituicao'
import { Selo, SeloAtivo } from '../../components/Selo'
import { ROTULO_PAPEL } from '../../utils/rotulos'
import { useAsync } from '../../utils/useAsync'
import { useEnvio } from '../../utils/useEnvio'

const PAPEIS: Papel[] = ['RECEPCIONISTA', 'MEDICO', 'ENFERMEIRO', 'ADMIN_INSTITUICAO']
const UFS = ['AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG', 'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO']

interface FormularioProps {
  cnpj: string
  /** Ausente = cadastro de usuario novo; presente = alteracao dos papeis de quem ja esta na instituicao. */
  existente?: UsuarioInstituicao
  aoFechar: () => void
  aoSalvar: (resultado: UsuarioCriado | UsuarioInstituicao) => void
}

function UsuarioModal({ cnpj, existente, aoFechar, aoSalvar }: FormularioProps) {
  const edicao = !!existente
  const [nome, setNome] = useState(existente?.nome ?? '')
  const [email, setEmail] = useState(existente?.email ?? '')
  const [papeis, setPapeis] = useState<Papel[]>(existente?.papeis ?? [])
  const [departamentoId, setDepartamentoId] = useState<string>(existente?.departamentoId ? String(existente.departamentoId) : '')
  const [numeroConselho, setNumeroConselho] = useState(existente?.numeroConselho ?? '')
  const [ufConselho, setUfConselho] = useState(existente?.ufConselho ?? 'RS')
  const [especialidade, setEspecialidade] = useState(existente?.especialidade ?? '')
  const [erroLocal, setErroLocal] = useState<string | null>(null)
  const { enviando, erro, campos, enviar } = useEnvio()

  const deps = useAsync(() => departamentos.listar(cnpj), [cnpj])
  const opcoesDepartamento = (deps.dados ?? []).filter((d) => d.ativo || String(d.id) === departamentoId)

  const profissional = papeis.includes('MEDICO') || papeis.includes('ENFERMEIRO')
  const alternar = (papel: Papel) =>
    setPapeis((atuais) => (atuais.includes(papel) ? atuais.filter((p) => p !== papel) : [...atuais, papel]))

  const submeter = async (e: FormEvent) => {
    e.preventDefault()
    setErroLocal(null)
    if (papeis.length === 0) return setErroLocal('Escolha pelo menos um papel.')
    if (papeis.includes('MEDICO') && papeis.includes('ENFERMEIRO'))
      return setErroLocal('Escolha apenas um perfil profissional: médico ou enfermeiro.')

    const corpo = {
      papeis,
      departamentoId: departamentoId ? Number(departamentoId) : null,
      ...(profissional ? { numeroConselho, ufConselho, especialidade } : {}),
    }
    const resultado = await enviar<UsuarioCriado | UsuarioInstituicao>(async () =>
      existente ? await usuarios.alterarPapeis(cnpj, existente.id, corpo) : await usuarios.cadastrar(cnpj, { nome, email, ...corpo }),
    )
    if (resultado) aoSalvar(resultado)
  }

  return (
    <Modal
      titulo={edicao ? 'Alterar papéis e departamento' : 'Cadastrar usuário'}
      subtitulo={edicao ? `${existente?.nome} · ${existente?.email}` : 'Preencha os dados para concluir o cadastro.'}
      aoFechar={aoFechar}
      larga
    >
      <form onSubmit={submeter}>
        <div className="grade-formulario">
          {!edicao && (
            <>
              <CampoTexto rotulo="Nome completo" value={nome} onChange={(e) => setNome(e.target.value)} erro={campos.nome} larga required autoFocus />
              <CampoTexto
                rotulo="E-mail"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                erro={campos.email}
                dica="Se este e-mail já existir na rede, a pessoa só é vinculada a esta instituição."
                larga
                required
              />
            </>
          )}

          <fieldset className="grupo-campos larga">
            <legend>Papéis na instituição</legend>
            {PAPEIS.map((papel) => (
              <CampoCheck key={papel} rotulo={ROTULO_PAPEL[papel]} checked={papeis.includes(papel)} onChange={() => alternar(papel)} />
            ))}
          </fieldset>

          <CampoSelect rotulo="Departamento" value={departamentoId} onChange={(e) => setDepartamentoId(e.target.value)} larga>
            <option value="">Sem departamento</option>
            {opcoesDepartamento.map((d) => (
              <option key={d.id} value={d.id}>
                {d.nome}
                {d.ativo ? '' : ' (inativo)'}
              </option>
            ))}
          </CampoSelect>

          {profissional && (
            <>
              <CampoTexto
                rotulo={papeis.includes('MEDICO') ? 'Número do CRM' : 'Número do COREN'}
                value={numeroConselho}
                onChange={(e) => setNumeroConselho(e.target.value)}
                dica={existente?.numeroConselho ? 'Já cadastrado: só é usado se ainda não existir registro.' : undefined}
                required={!existente?.numeroConselho}
              />
              <CampoSelect rotulo="UF do conselho" value={ufConselho} onChange={(e) => setUfConselho(e.target.value)}>
                {UFS.map((uf) => (
                  <option key={uf}>{uf}</option>
                ))}
              </CampoSelect>
              {papeis.includes('MEDICO') && (
                <CampoTexto rotulo="Especialidade" value={especialidade} onChange={(e) => setEspecialidade(e.target.value)} larga />
              )}
            </>
          )}
        </div>

        {(erroLocal || erro) && <p className="erro-formulario">{erroLocal ?? erro}</p>}
        <div className="modal-acoes">
          <Botao variante="secundario" onClick={aoFechar} disabled={enviando}>
            Cancelar
          </Botao>
          <Botao type="submit" carregando={enviando}>
            {edicao ? 'Salvar alterações' : 'Salvar cadastro'}
          </Botao>
        </div>
      </form>
    </Modal>
  )
}

function SenhaTemporariaModal({ criado, aoFechar }: { criado: UsuarioCriado; aoFechar: () => void }) {
  return (
    <Modal titulo="Usuário cadastrado" aoFechar={aoFechar}>
      <p className="texto-modal">
        <strong>{criado.usuario.nome}</strong> ({criado.usuario.email}) foi cadastrado.
      </p>
      {criado.senhaTemporaria ? (
        <>
          <p className="texto-modal">Entregue esta senha temporária à pessoa. Ela precisará trocá-la no primeiro acesso.</p>
          <CaixaSenha senha={criado.senhaTemporaria} />
          <p className="faixa faixa-aviso">Esta senha aparece somente agora. Depois que você fechar esta janela, não será possível vê-la de novo.</p>
        </>
      ) : (
        <p className="texto-modal">Esta pessoa já tinha cadastro na rede, então foi apenas vinculada à instituição e continua com a senha dela.</p>
      )}
      <div className="modal-acoes">
        <Botao onClick={aoFechar}>Concluir</Botao>
      </div>
    </Modal>
  )
}

export function UsuariosPage() {
  const { usuario: eu } = useAuth()
  const avisos = useAvisos()
  const inst = useInstituicoesAdministraveis()
  const lista = useAsync(() => usuarios.listar(inst.cnpj ?? ''), [inst.cnpj], !!inst.cnpj)
  const [busca, setBusca] = useState('')
  const [novo, setNovo] = useState(false)
  const [editando, setEditando] = useState<UsuarioInstituicao | null>(null)
  const [alvo, setAlvo] = useState<UsuarioInstituicao | null>(null)
  const [criado, setCriado] = useState<UsuarioCriado | null>(null)

  if (inst.carregando) return <Carregando />
  if (inst.erro) return <CaixaErro mensagem={inst.erro} aoTentarNovamente={inst.recarregar} />

  const termo = busca.trim().toLowerCase()
  const visiveis = (lista.dados ?? []).filter((u) => !termo || u.nome.toLowerCase().includes(termo) || u.email.toLowerCase().includes(termo))

  return (
    <>
      <CabecalhoPagina
        secao="Administração"
        titulo="Usuários"
        descricao="Cadastre usuários da instituição e defina o que cada um pode fazer."
        acao={
          inst.cnpj ? (
            <Botao icone="mais" onClick={() => setNovo(true)}>
              Novo usuário
            </Botao>
          ) : undefined
        }
      />

      {inst.opcoes.length === 0 ? (
        <section className="painel">
          <Vazio icone="predio" titulo="Nenhuma instituição disponível">
            Cadastre uma instituição primeiro.
          </Vazio>
        </section>
      ) : (
        <section className="painel">
          <div className="filtros">
            <SeletorInstituicao opcoes={inst.opcoes} valor={inst.cnpj} aoMudar={inst.escolher} />
            <div className="campo-busca">
              <Icone nome="busca" />
              <input placeholder="Buscar por nome ou e-mail..." value={busca} onChange={(e) => setBusca(e.target.value)} aria-label="Buscar usuários" />
            </div>
          </div>

          <Resultado {...lista}>
            {() =>
              visiveis.length === 0 ? (
                <Vazio icone="usuarios" titulo="Nenhum usuário encontrado" />
              ) : (
                <div className="tabela-rolagem">
                  <table className="tabela">
                    <thead>
                      <tr>
                        <th>Usuário</th>
                        <th>Papéis</th>
                        <th>Departamento</th>
                        <th>Registro profissional</th>
                        <th>Situação</th>
                        <th aria-label="Ações" />
                      </tr>
                    </thead>
                    <tbody>
                      {visiveis.map((u) => (
                        <tr key={u.id} className={u.ativo ? '' : 'linha-inativa'}>
                          <td>
                            <strong>{u.nome}</strong>
                            <small className="sub">{u.email}</small>
                          </td>
                          <td>
                            <div className="selos">
                              {u.papeis.map((p) => (
                                <Selo key={p} tom="azul">
                                  {ROTULO_PAPEL[p]}
                                </Selo>
                              ))}
                            </div>
                          </td>
                          <td>{u.departamentoNome ?? '—'}</td>
                          <td>
                            {u.numeroConselho ? `${u.numeroConselho}/${u.ufConselho}` : '—'}
                            {u.especialidade && <small className="sub">{u.especialidade}</small>}
                          </td>
                          <td>
                            <SeloAtivo ativo={u.ativo} />
                          </td>
                          <td className="acoes">
                            {u.ativo && (
                              <Botao variante="secundario" pequeno icone="editar" onClick={() => setEditando(u)}>
                                Papéis
                              </Botao>
                            )}
                            <Botao
                              variante={u.ativo ? 'perigo' : 'secundario'}
                              pequeno
                              disabled={u.ativo && u.id === eu?.id}
                              title={u.ativo && u.id === eu?.id ? 'Você não pode inativar o próprio vínculo' : undefined}
                              onClick={() => setAlvo(u)}
                            >
                              {u.ativo ? 'Inativar' : 'Reativar'}
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
        </section>
      )}

      {novo && inst.cnpj && (
        <UsuarioModal
          cnpj={inst.cnpj}
          aoFechar={() => setNovo(false)}
          aoSalvar={(resultado) => {
            setNovo(false)
            setCriado(resultado as UsuarioCriado)
            lista.recarregar()
          }}
        />
      )}

      {editando && inst.cnpj && (
        <UsuarioModal
          cnpj={inst.cnpj}
          existente={editando}
          aoFechar={() => setEditando(null)}
          aoSalvar={() => {
            setEditando(null)
            avisos.sucesso('Papéis atualizados.')
            lista.recarregar()
          }}
        />
      )}

      {criado && <SenhaTemporariaModal criado={criado} aoFechar={() => setCriado(null)} />}

      {alvo && inst.cnpj && (
        <Confirmacao
          titulo={alvo.ativo ? 'Inativar usuário' : 'Reativar usuário'}
          mensagem={
            alvo.ativo
              ? `${alvo.nome} perderá o acesso a esta instituição imediatamente. O histórico de registros dele é mantido.`
              : `${alvo.nome} voltará a ter acesso a esta instituição com os papéis que tinha.`
          }
          rotuloConfirmar={alvo.ativo ? 'Inativar' : 'Reativar'}
          perigo={alvo.ativo}
          aoCancelar={() => setAlvo(null)}
          aoConfirmar={async () => {
            const cnpj = inst.cnpj as string
            await (alvo.ativo ? usuarios.inativar(cnpj, alvo.id) : usuarios.reativar(cnpj, alvo.id))
            avisos.sucesso(alvo.ativo ? 'Usuário inativado.' : 'Usuário reativado.')
            setAlvo(null)
            lista.recarregar()
          }}
        />
      )}
    </>
  )
}
