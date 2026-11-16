import { useState } from 'react'
import { instituicoes } from '../api/endpoints'
import { useAuth } from '../auth/AuthContext'
import { formatarCnpj } from '../utils/format'
import { useAsync } from '../utils/useAsync'

export interface OpcaoInstituicao {
  cnpj: string
  nome: string
  ativo: boolean
}

/**
 * Instituicoes que o usuario pode administrar: todas (admin do sistema) ou so aquelas em que
 * e administrador. Guarda a escolhida; por padrao usa a instituicao ativa do menu superior.
 */
export function useInstituicoesAdministraveis() {
  const { usuario, caps, instituicaoAtiva } = useAuth()
  const todas = useAsync(() => instituicoes.listar(), [], caps.adminSistema)
  const [escolhida, setEscolhida] = useState<string | null>(null)

  const opcoes: OpcaoInstituicao[] = caps.adminSistema
    ? (todas.dados ?? []).map((i) => ({ cnpj: i.cnpj, nome: i.nome, ativo: i.ativo }))
    : (usuario?.instituicoes ?? [])
        .filter((v) => v.papeis.includes('ADMIN_INSTITUICAO'))
        .map((v) => ({ cnpj: v.cnpj, nome: v.nomeInstituicao, ativo: true }))

  const padrao = opcoes.find((o) => o.cnpj === instituicaoAtiva) ?? opcoes[0]
  const selecionada = opcoes.find((o) => o.cnpj === escolhida) ?? padrao ?? null

  return {
    opcoes,
    selecionada,
    cnpj: selecionada?.cnpj ?? null,
    escolher: setEscolhida,
    carregando: caps.adminSistema && todas.carregando && todas.dados === null,
    erro: todas.erro,
    recarregar: todas.recarregar,
  }
}

export function SeletorInstituicao({
  opcoes,
  valor,
  aoMudar,
}: {
  opcoes: OpcaoInstituicao[]
  valor: string | null
  aoMudar: (cnpj: string) => void
}) {
  return (
    <label className="seletor">
      <span>Instituição</span>
      <select className="entrada" value={valor ?? ''} onChange={(e) => aoMudar(e.target.value)}>
        {opcoes.map((o) => (
          <option key={o.cnpj} value={o.cnpj}>
            {o.nome} — {formatarCnpj(o.cnpj)}
            {o.ativo ? '' : ' (inativa)'}
          </option>
        ))}
      </select>
    </label>
  )
}
