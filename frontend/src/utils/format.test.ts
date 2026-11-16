import { describe, expect, it } from 'vitest'
import { formatarCnpj, formatarCpf, formatarData, formatarDataHora, localParaIso, mascaraCnpj, mascaraCpf, somenteDigitos } from './format'

describe('mascaras', () => {
  it('formata CPF aos poucos e limita a 11 digitos', () => {
    expect(mascaraCpf('529')).toBe('529')
    expect(mascaraCpf('52998')).toBe('529.98')
    expect(mascaraCpf('529982247')).toBe('529.982.247')
    expect(mascaraCpf('52998224725')).toBe('529.982.247-25')
    expect(mascaraCpf('529.982.247-25999')).toBe('529.982.247-25')
    expect(mascaraCpf('abc')).toBe('')
  })

  it('formata CNPJ numerico e alfanumerico', () => {
    expect(mascaraCnpj('11222333000181')).toBe('11.222.333/0001-81')
    expect(mascaraCnpj('12abc34501de35')).toBe('12.ABC.345/01DE-35')
    expect(mascaraCnpj('1122')).toBe('11.22')
    expect(formatarCnpj(null)).toBe('—')
  })

  it('extrai so digitos', () => {
    expect(somenteDigitos('529.982.247-25')).toBe('52998224725')
    expect(formatarCpf(undefined)).toBe('—')
  })
})

describe('datas', () => {
  it('mostra a data de nascimento sem deslocar pelo fuso horario', () => {
    expect(formatarData('1990-05-20')).toBe('20/05/1990')
    expect(formatarData('2026-01-01T00:00:00Z')).toBe('01/01/2026')
    expect(formatarData(null)).toBe('—')
  })

  it('entende o instante com microssegundos que o servidor envia', () => {
    expect(formatarDataHora('2026-11-12T15:20:35.123456Z')).not.toBe('—')
    expect(formatarDataHora('2026-11-12T15:20:35Z')).not.toBe('—')
    expect(formatarDataHora(null)).toBe('—')
    expect(formatarDataHora('lixo')).toBe('—')
  })

  it('converte datetime-local para ISO e ignora vazio ou invalido', () => {
    expect(localParaIso('')).toBeUndefined()
    expect(localParaIso('lixo')).toBeUndefined()
    expect(localParaIso('2026-11-12T10:30')).toMatch(/^2026-11-12T\d{2}:30:00\.000Z$/)
  })
})
