import { useState, type FormEvent } from 'react'
import { departamentos } from '../../api/endpoints'
import type { Departamento } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { useAvisos } from '../../components/Avisos'
import { Botao } from '../../components/Botao'
import { CabecalhoPagina } from '../../components/CabecalhoPagina'
import { CampoTexto } from '../../components/Campos'
import { Confirmacao } from '../../components/Confirmacao'
import { Icone } from '../../components/Icone'
import { Modal } from '../../components/Modal'
import { Carregando, CaixaErro, Resultado, Vazio } from '../../components/Estados'
import { SeletorInstituicao, useInstituicoesAdministraveis } from '../../components/SeletorInstituicao'
import { SeloAtivo } from '../../components/Selo'
import { useAsync } from '../../utils/useAsync'
import { useEnvio } from '../../utils/useEnvio'

function NovoDepartamentoModal({ cnpj, aoFechar, aoSalvar }: { cnpj: string; aoFechar: () => void; aoSalvar: (d: Departamento) => void }) {
  const [nome, setNome] = useState('')
  const { enviando, erro, campos, enviar } = useEnvio()

  const submeter = async (e: FormEvent) => {
    e.preventDefault()
    const criado = await enviar(() => departamentos.criar(cnpj, nome))
    if (criado) aoSalvar(criado)
  }

  return (
    <Modal titulo="Cadastrar departamento" subtitulo="O departamento ficará vinculado à instituição selecionada." aoFechar={aoFechar}>
      <form onSubmit={submeter}>
        <div className="grade-formulario">
          <CampoTexto rotulo="Nome do departamento" value={nome} onChange={(e) => setNome(e.target.value)} erro={campos.nome} larga required autoFocus />
        </div>
        {erro && <p className="erro-formulario">{erro}</p>}
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

export function DepartamentosPage() {
  const { caps } = useAuth()
  const avisos = useAvisos()
  const inst = useInstituicoesAdministraveis()
  const lista = useAsync(() => departamentos.listar(inst.cnpj ?? ''), [inst.cnpj], !!inst.cnpj)
  const [novo, setNovo] = useState(false)
  const [alvo, setAlvo] = useState<Departamento | null>(null)

  if (inst.carregando) return <Carregando />
  if (inst.erro) return <CaixaErro mensagem={inst.erro} aoTentarNovamente={inst.recarregar} />

  return (
    <>
      <CabecalhoPagina
        secao="Administração"
        titulo="Departamentos"
        descricao="Organize os departamentos e seus vínculos institucionais."
        acao={
          caps.adminSistema && inst.cnpj ? (
            <Botao icone="mais" onClick={() => setNovo(true)}>
              Novo departamento
            </Botao>
          ) : undefined
        }
      />

      {inst.opcoes.length === 0 ? (
        <section className="painel">
          <Vazio icone="predio" titulo="Nenhuma instituição disponível">
            {caps.adminSistema ? 'Cadastre uma instituição primeiro.' : 'Você não administra nenhuma instituição.'}
          </Vazio>
        </section>
      ) : (
        <section className="painel">
          <div className="filtros">
            <SeletorInstituicao opcoes={inst.opcoes} valor={inst.cnpj} aoMudar={inst.escolher} />
          </div>
          {!caps.adminSistema && <p className="texto-suave">Somente o administrador do sistema cadastra ou inativa departamentos.</p>}

          <Resultado {...lista}>
            {(dados) =>
              dados.length === 0 ? (
                <Vazio icone="camadas" titulo="Nenhum departamento cadastrado" />
              ) : (
                <div className="grade-cartoes">
                  {dados.map((d) => (
                    <div key={d.id} className={`cartao-departamento ${d.ativo ? '' : 'inativo'}`}>
                      <span className="stat-icone tom-teal">
                        <Icone nome="camadas" tamanho={22} />
                      </span>
                      <div>
                        <h3>{d.nome}</h3>
                        <SeloAtivo ativo={d.ativo} />
                      </div>
                      {caps.adminSistema && (
                        <Botao variante={d.ativo ? 'fantasma' : 'secundario'} pequeno onClick={() => setAlvo(d)}>
                          {d.ativo ? 'Inativar' : 'Reativar'}
                        </Botao>
                      )}
                    </div>
                  ))}
                </div>
              )
            }
          </Resultado>
        </section>
      )}

      {novo && inst.cnpj && (
        <NovoDepartamentoModal
          cnpj={inst.cnpj}
          aoFechar={() => setNovo(false)}
          aoSalvar={(d) => {
            setNovo(false)
            avisos.sucesso(`Departamento ${d.nome} criado.`)
            lista.recarregar()
          }}
        />
      )}

      {alvo && inst.cnpj && (
        <Confirmacao
          titulo={alvo.ativo ? 'Inativar departamento' : 'Reativar departamento'}
          mensagem={
            alvo.ativo
              ? `${alvo.nome} deixará de poder ser escolhido para novos usuários. Quem já está nele continua vinculado.`
              : `${alvo.nome} voltará a poder ser escolhido.`
          }
          rotuloConfirmar={alvo.ativo ? 'Inativar' : 'Reativar'}
          perigo={alvo.ativo}
          aoCancelar={() => setAlvo(null)}
          aoConfirmar={async () => {
            const cnpj = inst.cnpj as string
            await (alvo.ativo ? departamentos.inativar(cnpj, alvo.id) : departamentos.reativar(cnpj, alvo.id))
            avisos.sucesso(alvo.ativo ? 'Departamento inativado.' : 'Departamento reativado.')
            setAlvo(null)
            lista.recarregar()
          }}
        />
      )}
    </>
  )
}
