/** Le a data de expiracao (em ms) do payload do JWT, sem validar assinatura (quem valida e o servidor). */
export function lerExpiracao(token: string): number | null {
  try {
    const partes = token.split('.')
    if (partes.length < 2) return null
    const base64 = partes[1].replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(
      atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '='))
        .split('')
        .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
        .join(''),
    )
    const exp = (JSON.parse(json) as { exp?: number }).exp
    return typeof exp === 'number' ? exp * 1000 : null
  } catch {
    return null
  }
}

export function tokenExpirado(token: string, agora: number = Date.now()): boolean {
  const exp = lerExpiracao(token)
  return exp !== null && exp <= agora
}
