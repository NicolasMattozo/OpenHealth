import { describe, expect, it } from 'vitest'
import { lerExpiracao, tokenExpirado } from './token'

function jwt(payload: object): string {
  // JWT usa JSON em UTF-8 codificado em base64url
  const b64 = (o: object) =>
    btoa(String.fromCharCode(...new TextEncoder().encode(JSON.stringify(o))))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '')
  return `${b64({ alg: 'HS256' })}.${b64(payload)}.assinatura`
}

describe('token', () => {
  it('le a expiracao do payload', () => {
    expect(lerExpiracao(jwt({ exp: 1800000000 }))).toBe(1800000000 * 1000)
  })

  it('funciona com acentos no payload (nome do usuario)', () => {
    expect(lerExpiracao(jwt({ nome: 'José Conceição', exp: 1800000000 }))).toBe(1800000000 * 1000)
  })

  it('devolve null para token malformado', () => {
    expect(lerExpiracao('nao-e-jwt')).toBeNull()
    expect(lerExpiracao('a.b.c')).toBeNull()
    expect(lerExpiracao(jwt({ sem: 'exp' }))).toBeNull()
  })

  it('detecta expirado', () => {
    const agora = 1_700_000_000_000
    expect(tokenExpirado(jwt({ exp: agora / 1000 - 1 }), agora)).toBe(true)
    expect(tokenExpirado(jwt({ exp: agora / 1000 + 60 }), agora)).toBe(false)
    expect(tokenExpirado('lixo', agora)).toBe(false)
  })
})
