import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, aoSessaoExpirar, consulta, definirToken, http, mensagemDoErro } from './client'

function resposta(status: number, corpo?: unknown): Response {
  return new Response(corpo === undefined ? null : typeof corpo === 'string' ? corpo : JSON.stringify(corpo), { status })
}

describe('cliente http', () => {
  beforeEach(() => {
    definirToken(null)
    aoSessaoExpirar(null)
  })
  afterEach(() => vi.restoreAllMocks())

  it('envia o token no cabecalho e devolve o json', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(resposta(200, { ok: true }))
    definirToken('abc')
    const dados = await http.get<{ ok: boolean }>('/api/x')
    expect(dados.ok).toBe(true)
    const cabecalhos = (fetchMock.mock.calls[0][1] as RequestInit).headers as Record<string, string>
    expect(cabecalhos.Authorization).toBe('Bearer abc')
  })

  it('converte o erro do servidor em ApiError com mensagem e campos', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(resposta(400, { erro: 'Dados inválidos.', campos: { nome: 'não deve estar em branco' } }))
    const erro = await http.post('/api/x', {}).catch((e: unknown) => e)
    expect(erro).toBeInstanceOf(ApiError)
    expect((erro as ApiError).status).toBe(400)
    expect((erro as ApiError).campos.nome).toBe('não deve estar em branco')
    expect(mensagemDoErro(erro)).toContain('Dados inválidos.')
    expect(mensagemDoErro(erro)).toContain('nome: não deve estar em branco')
  })

  it('usa mensagem padrao quando o corpo vem vazio (ex.: 403 do Spring Security)', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(resposta(403))
    const erro = (await http.get('/api/x').catch((e: unknown) => e)) as ApiError
    expect(erro.message).toBe('Você não tem permissão para esta ação.')
  })

  it('401 com token avisa a sessao expirada; 401 sem token (login errado) nao', async () => {
    const aviso = vi.fn()
    aoSessaoExpirar(aviso)
    vi.spyOn(globalThis, 'fetch').mockImplementation(async () => resposta(401, { erro: 'Credenciais inválidas.' }))

    await http.post('/api/auth/login', {}).catch(() => undefined)
    expect(aviso).not.toHaveBeenCalled()

    definirToken('vencido')
    await http.get('/api/x').catch(() => undefined)
    expect(aviso).toHaveBeenCalledTimes(1)
  })

  it('explica quando nao consegue falar com o servidor', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('Failed to fetch'))
    const erro = (await http.get('/api/x').catch((e: unknown) => e)) as ApiError
    expect(erro.status).toBe(0)
    expect(erro.message).toMatch(/backend/)
  })

  it('monta a query ignorando valores vazios', () => {
    expect(consulta({ a: '1', b: '', c: undefined, d: null, e: 0 })).toBe('?a=1&e=0')
    expect(consulta({})).toBe('')
  })
})
