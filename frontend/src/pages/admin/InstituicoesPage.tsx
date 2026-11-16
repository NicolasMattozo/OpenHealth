import { useState, type FormEvent } from 'react'
import { instituicoes } from '../../api/endpoints'
import type { Instituicao } from '../../api/types'
import { useAvisos } from '../../components/Avisos'
import { Botao } from '../../components/Botao'
import { CabecalhoPagina } from '../../components/CabecalhoPagina'
import { CampoSelect, CampoTexto } from '../../components/Campos'
import { Confirmacao } from '../../components/Confirmacao'
import { Icone } from '../../components/Icone'
import { Modal } from '../../components/Modal'
import { Resultado, Vazio } from '../../components/Estados'
import { SeloAtivo } from '../../components/Selo'
import { formatarCnpj, mascaraCnpj } from '../../utils/format'
import { TIPOS_INSTITUICAO } from '../../utils/rotulos'
import { useAsync } from '../../utils/useAsync'
import { useEnvio } from '../../utils/useEnvio'

function NovaInstituicaoModal({ aoFechar, aoSalvar }: { aoFechar: () => void; aoSalvar: (i: Instituicao) => void }) {
  const [nome, setNome] = useState('')
  const [cnpj, setCnpj] = useState('')
  const [tipo, setTipo] = useState('Hospital')
  const [endereco, setEndereco] = useState('')
  const { enviando, erro, campos, enviar } = useEnvio()

  const submeter = async (e: FormEvent) => {
    e.preventDefault()
    const criada = await enviar(() => instituicoes.cadastrar({ nome, cnpj, tipo, endereco }))
    if (criada) aoSalvar(criada)
  }

  return (
    <Modal titulo="Cadastrar instituição" subtitulo="Preencha os dados para concluir o cadastro." aoFechar={aoFechar}>
      <form onSubmit={submeter}>
        <div className="grade-formulario">
          <CampoTexto rotulo="Nome da instituição" value={nome} onChange={(e) => setNome(e.target.value)} erro={campos.nome} larga required autoFocus />
          <CampoTexto
            rotulo="CNPJ"
            value={cnpj}
            onChange={(e) => setCnpj(mascaraCnpj(e.target.value))}
            placeholder="00.000.000/0000-00"
            dica="Aceita CNPJ numérico ou alfanumérico."
            erro={campos.cnpj}
            required
          />
          <CampoSelect rotulo="Tipo" value={tipo} onChange={(e) => setTipo(e.target.value)}>
            {TIPOS_INSTITUICAO.map((t) => (
              <option key={t}>{t}</option>
            ))}
          </CampoSelect>
          <CampoTexto rotulo="Endereço completo" value={endereco} onChange={(e) => setEndereco(e.target.value)} erro={campos.endereco} larga />
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

export function InstituicoesPage() {
  const avisos = useAvisos()
  const lista = useAsync(() => instituicoes.listar(), [])
  const [busca, setBusca] = useState('')
  const [filtro, setFiltro] = useState<'todas' | 'ativas' | 'inativas'>('todas')
  const [novo, setNovo] = useState(false)
  const [alvo, setAlvo] = useState<Instituicao | null>(null)

  const termo = busca.trim().toLowerCase()
  const visiveis = (lista.dados ?? []).filter(
    (i) =>
      (filtro === 'todas' || (filtro === 'ativas') === i.ativo) &&
      (!termo || i.nome.toLowerCase().includes(termo) || i.cnpj.toLowerCase().includes(termo.replace(/[^0-9a-z]/g, ''))),
  )

  return (
    <>
      <CabecalhoPagina
        secao="Administração"
        titulo="Instituições"
        descricao="Gerencie as instituições participantes da rede unificada."
        acao={
          <Botao icone="mais" onClick={() => setNovo(true)}>
            Nova instituição
          </Botao>
        }
      />

      <section className="painel">
        <div className="filtros">
          <div className="campo-busca">
            <Icone nome="busca" />
            <input placeholder="Buscar por nome ou CNPJ..." value={busca} onChange={(e) => setBusca(e.target.value)} aria-label="Buscar instituições" />
          </div>
          <select className="entrada" value={filtro} onChange={(e) => setFiltro(e.target.value as typeof filtro)} aria-label="Filtrar por situação">
            <option value="todas">Todas</option>
            <option value="ativas">Ativas</option>
            <option value="inativas">Inativas</option>
          </select>
        </div>

        <Resultado {...lista}>
          {() =>
            visiveis.length === 0 ? (
              <Vazio icone="predio" titulo="Nenhuma instituição encontrada" />
            ) : (
              <div className="tabela-rolagem">
                <table className="tabela">
                  <thead>
                    <tr>
                      <th>Instituição</th>
                      <th>CNPJ</th>
                      <th>Tipo</th>
                      <th>Endereço</th>
                      <th>Situação</th>
                      <th aria-label="Ações" />
                    </tr>
                  </thead>
                  <tbody>
                    {visiveis.map((i) => (
                      <tr key={i.cnpj}>
                        <td>
                          <strong>{i.nome}</strong>
                        </td>
                        <td>{formatarCnpj(i.cnpj)}</td>
                        <td>{i.tipo ?? '—'}</td>
                        <td>{i.endereco ?? '—'}</td>
                        <td>
                          <SeloAtivo ativo={i.ativo} />
                        </td>
                        <td className="acoes">
                          <Botao variante={i.ativo ? 'perigo' : 'secundario'} pequeno onClick={() => setAlvo(i)}>
                            {i.ativo ? 'Inativar' : 'Reativar'}
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

      {novo && (
        <NovaInstituicaoModal
          aoFechar={() => setNovo(false)}
          aoSalvar={(i) => {
            setNovo(false)
            avisos.sucesso(`Instituição ${i.nome} cadastrada.`)
            lista.recarregar()
          }}
        />
      )}

      {alvo && (
        <Confirmacao
          titulo={alvo.ativo ? 'Inativar instituição' : 'Reativar instituição'}
          mensagem={
            alvo.ativo
              ? `${alvo.nome} deixará de aparecer como ativa e os seus usuários perderão o acesso até que ela seja reativada. O histórico é mantido.`
              : `${alvo.nome} voltará a ficar ativa.`
          }
          rotuloConfirmar={alvo.ativo ? 'Inativar' : 'Reativar'}
          perigo={alvo.ativo}
          aoCancelar={() => setAlvo(null)}
          aoConfirmar={async () => {
            await (alvo.ativo ? instituicoes.inativar(alvo.cnpj) : instituicoes.reativar(alvo.cnpj))
            avisos.sucesso(alvo.ativo ? 'Instituição inativada.' : 'Instituição reativada.')
            setAlvo(null)
            lista.recarregar()
          }}
        />
      )}
    </>
  )
}
