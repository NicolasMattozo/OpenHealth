export const somenteDigitos = (valor: string) => valor.replace(/\D/g, '')

/** 000.000.000-00 (aceita CPF incompleto: formata o que ja foi digitado). */
export function mascaraCpf(valor: string): string {
  const d = somenteDigitos(valor).slice(0, 11)
  return d
    .replace(/^(\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1-$2')
}

export const formatarCpf = (cpf: string | null | undefined) => (cpf ? mascaraCpf(cpf) : '—')

/** CNPJ numerico ou alfanumerico (desde 2026): XX.XXX.XXX/XXXX-XX. */
export function mascaraCnpj(valor: string): string {
  const c = valor.replace(/[^0-9A-Za-z]/g, '').toUpperCase().slice(0, 14)
  let saida = c.slice(0, 2)
  if (c.length > 2) saida += `.${c.slice(2, 5)}`
  if (c.length > 5) saida += `.${c.slice(5, 8)}`
  if (c.length > 8) saida += `/${c.slice(8, 12)}`
  if (c.length > 12) saida += `-${c.slice(12, 14)}`
  return saida
}

export const formatarCnpj = (cnpj: string | null | undefined) => (cnpj ? mascaraCnpj(cnpj) : '—')

const formatoDataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
const formatoData = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short' })

/** O servidor manda microssegundos (…:35.123456Z); alguns navegadores so entendem ate milissegundos. */
const paraMilissegundos = (iso: string) => iso.replace(/(\.\d{3})\d+/, '$1')

export function formatarDataHora(iso: string | null | undefined): string {
  if (!iso) return '—'
  const data = new Date(paraMilissegundos(iso))
  return Number.isNaN(data.getTime()) ? '—' : formatoDataHora.format(data)
}

/** Recebe "1990-05-20" e mostra 20/05/1990 sem passar pelo fuso horario. */
export function formatarData(iso: string | null | undefined): string {
  if (!iso) return '—'
  const so = iso.slice(0, 10)
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(so)
  if (m) return `${m[3]}/${m[2]}/${m[1]}`
  const data = new Date(paraMilissegundos(iso))
  return Number.isNaN(data.getTime()) ? '—' : formatoData.format(data)
}

export const maiusculas = (texto: string) => texto.toUpperCase()

/** "datetime-local" (2026-11-12T14:30) para ISO em UTC; vazio vira undefined. */
export function localParaIso(valor: string): string | undefined {
  if (!valor) return undefined
  const data = new Date(valor)
  return Number.isNaN(data.getTime()) ? undefined : data.toISOString()
}
