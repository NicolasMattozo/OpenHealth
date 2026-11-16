import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { aoSessaoExpirar, definirToken } from '../api/client'
import { autenticacao } from '../api/endpoints'
import type { LoginResponse, UsuarioLogado } from '../api/types'
import { tokenExpirado } from '../utils/token'
import { capacidades, type Capacidades } from './permissoes'

const CHAVE_SESSAO = 'prontuarios.sessao'
const CHAVE_INSTITUICAO = 'prontuarios.instituicao'
export const CHAVE_AVISO = 'prontuarios.aviso'

interface Sessao {
  token: string
  usuario: UsuarioLogado
}

interface Autenticacao {
  usuario: UsuarioLogado | null
  /** CNPJ da instituicao em que o usuario esta atuando agora. */
  instituicaoAtiva: string | null
  nomeInstituicaoAtiva: string | null
  caps: Capacidades
  entrar: (identificador: string, senha: string) => Promise<UsuarioLogado>
  trocarSenha: (senhaAtual: string, novaSenha: string) => Promise<UsuarioLogado>
  sair: () => void
  selecionarInstituicao: (cnpj: string) => void
}

const Contexto = createContext<Autenticacao | null>(null)

/** Recupera a sessao da aba (sessionStorage some ao fechar a aba) e descarta token vencido. */
function lerSessao(): Sessao | null {
  try {
    const texto = sessionStorage.getItem(CHAVE_SESSAO)
    if (!texto) return null
    const sessao = JSON.parse(texto) as Sessao
    if (!sessao.token || tokenExpirado(sessao.token)) {
      sessionStorage.removeItem(CHAVE_SESSAO)
      return null
    }
    definirToken(sessao.token)
    return sessao
  } catch {
    return null
  }
}

function instituicaoInicial(usuario: UsuarioLogado | null): string | null {
  if (!usuario || usuario.instituicoes.length === 0) return null
  const guardada = sessionStorage.getItem(CHAVE_INSTITUICAO)
  return usuario.instituicoes.some((v) => v.cnpj === guardada) ? guardada : usuario.instituicoes[0].cnpj
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [sessao, setSessao] = useState<Sessao | null>(lerSessao)
  const [instituicaoAtiva, setInstituicaoAtiva] = useState<string | null>(() => instituicaoInicial(sessao?.usuario ?? null))

  const guardar = useCallback((resposta: LoginResponse) => {
    definirToken(resposta.token)
    const nova: Sessao = { token: resposta.token, usuario: resposta.usuario }
    sessionStorage.setItem(CHAVE_SESSAO, JSON.stringify(nova))
    setSessao(nova)
    setInstituicaoAtiva(instituicaoInicial(resposta.usuario))
    return resposta.usuario
  }, [])

  const sair = useCallback(() => {
    definirToken(null)
    sessionStorage.removeItem(CHAVE_SESSAO)
    sessionStorage.removeItem(CHAVE_INSTITUICAO)
    setSessao(null)
    setInstituicaoAtiva(null)
  }, [])

  // Token recusado pelo servidor: volta ao login avisando o motivo.
  useEffect(() => {
    aoSessaoExpirar(() => {
      sessionStorage.setItem(CHAVE_AVISO, 'Sua sessão expirou. Entre novamente.')
      sair()
    })
    return () => aoSessaoExpirar(null)
  }, [sair])

  // Atualiza os dados do usuario (papeis podem ter mudado desde o login).
  useEffect(() => {
    if (!sessao) return
    let cancelado = false
    autenticacao
      .eu()
      .then((usuario) => {
        if (cancelado) return
        const atualizada: Sessao = { token: sessao.token, usuario }
        sessionStorage.setItem(CHAVE_SESSAO, JSON.stringify(atualizada))
        setSessao(atualizada)
        setInstituicaoAtiva((atual) => (usuario.instituicoes.some((v) => v.cnpj === atual) ? atual : instituicaoInicial(usuario)))
      })
      .catch(() => undefined)
    return () => {
      cancelado = true
    }
    // so ao carregar a pagina; o token nao muda sem passar por entrar/trocarSenha
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const selecionarInstituicao = useCallback((cnpj: string) => {
    sessionStorage.setItem(CHAVE_INSTITUICAO, cnpj)
    setInstituicaoAtiva(cnpj)
  }, [])

  const valor = useMemo<Autenticacao>(() => {
    const usuario = sessao?.usuario ?? null
    return {
      usuario,
      instituicaoAtiva,
      nomeInstituicaoAtiva: usuario?.instituicoes.find((v) => v.cnpj === instituicaoAtiva)?.nomeInstituicao ?? null,
      caps: capacidades(usuario, instituicaoAtiva),
      entrar: async (identificador, senha) => guardar(await autenticacao.entrar(identificador.trim(), senha)),
      trocarSenha: async (senhaAtual, novaSenha) => guardar(await autenticacao.trocarSenha(senhaAtual, novaSenha)),
      sair,
      selecionarInstituicao,
    }
  }, [sessao, instituicaoAtiva, guardar, sair, selecionarInstituicao])

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): Autenticacao {
  const contexto = useContext(Contexto)
  if (!contexto) throw new Error('useAuth precisa estar dentro de <AuthProvider>')
  return contexto
}
