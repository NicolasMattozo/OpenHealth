/** Cliente HTTP do backend: coloca o token, converte erros e avisa quando a sessao expira. */

export class ApiError extends Error {
  status: number
  campos: Record<string, string>
  corpo: unknown

  constructor(status: number, mensagem: string, campos: Record<string, string> = {}, corpo: unknown = null) {
    super(mensagem)
    this.name = 'ApiError'
    this.status = status
    this.campos = campos
    this.corpo = corpo
  }
}

const BASE = (import.meta.env.VITE_API_URL as string | undefined) ?? ''

let token: string | null = null
let aoExpirar: (() => void) | null = null

export function definirToken(novo: string | null) {
  token = novo
}

/** Chamado quando o servidor responde 401 a uma requisicao autenticada (token vencido ou revogado). */
export function aoSessaoExpirar(acao: (() => void) | null) {
  aoExpirar = acao
}

function mensagemPadrao(status: number): string {
  switch (status) {
    case 400:
      return 'Dados inválidos.'
    case 401:
      return 'Sessão expirada. Entre novamente.'
    case 403:
      return 'Você não tem permissão para esta ação.'
    case 404:
      return 'Não encontrado.'
    case 409:
      return 'A operação conflita com o estado atual.'
    default:
      return status >= 500 ? 'Erro no servidor. Tente novamente.' : 'Não foi possível concluir a operação.'
  }
}

/** Monta ?a=1&b=2 ignorando valores vazios. */
export function consulta(params: Record<string, string | number | undefined | null>): string {
  const busca = new URLSearchParams()
  for (const [chave, valor] of Object.entries(params)) {
    if (valor !== undefined && valor !== null && String(valor).trim() !== '') busca.set(chave, String(valor))
  }
  const texto = busca.toString()
  return texto ? `?${texto}` : ''
}

async function requisitar<T>(metodo: string, caminho: string, corpo?: unknown): Promise<T> {
  const cabecalhos: Record<string, string> = { Accept: 'application/json' }
  if (corpo !== undefined) cabecalhos['Content-Type'] = 'application/json'
  if (token) cabecalhos.Authorization = `Bearer ${token}`

  let resposta: Response
  try {
    resposta = await fetch(`${BASE}${caminho}`, {
      method: metodo,
      headers: cabecalhos,
      body: corpo !== undefined ? JSON.stringify(corpo) : undefined,
    })
  } catch {
    throw new ApiError(0, 'Não foi possível conectar ao servidor. Verifique se o backend está ligado.')
  }

  const texto = await resposta.text()
  let dados: unknown = null
  if (texto) {
    try {
      dados = JSON.parse(texto)
    } catch {
      dados = texto
    }
  }

  if (!resposta.ok) {
    if (resposta.status === 401 && token && aoExpirar) aoExpirar()
    const erro = dados && typeof dados === 'object' ? (dados as { erro?: string; campos?: Record<string, string> }) : null
    throw new ApiError(resposta.status, erro?.erro ?? mensagemPadrao(resposta.status), erro?.campos ?? {}, dados)
  }
  return dados as T
}

export const http = {
  get: <T>(caminho: string) => requisitar<T>('GET', caminho),
  post: <T>(caminho: string, corpo?: unknown) => requisitar<T>('POST', caminho, corpo ?? {}),
  put: <T>(caminho: string, corpo: unknown) => requisitar<T>('PUT', caminho, corpo),
  delete: <T>(caminho: string) => requisitar<T>('DELETE', caminho),
}

/** Texto amigavel para mostrar ao usuario a partir de qualquer erro. */
export function mensagemDoErro(erro: unknown): string {
  if (erro instanceof ApiError) {
    const detalhes = Object.entries(erro.campos)
    if (detalhes.length > 0) return `${erro.message} ${detalhes.map(([campo, msg]) => `${campo}: ${msg}`).join('; ')}`
    return erro.message
  }
  return erro instanceof Error ? erro.message : 'Ocorreu um erro inesperado.'
}

/** Erros de validacao por campo vindos do servidor ({"campos": {"nome": "..."}}). */
export function camposDoErro(erro: unknown): Record<string, string> {
  return erro instanceof ApiError ? erro.campos : {}
}
