import { describe, expect, it } from 'vitest'
import type { UsuarioLogado } from '../api/types'
import { capacidades, montarMenu } from './permissoes'

const base: UsuarioLogado = { id: 1, nome: 'Teste', email: 't@t.com', cpf: null, roles: [], instituicoes: [], deveTrocarSenha: false }
const caminhos = (u: UsuarioLogado, cnpj: string | null) => montarMenu(capacidades(u, cnpj)).map((i) => i.caminho)

describe('menu por papel', () => {
  it('admin do sistema ve instituicoes e administracao, mas nao recepcao nem clinica', () => {
    const c = caminhos({ ...base, roles: ['ROLE_ADMIN_SISTEMA'] }, null)
    expect(c).toEqual(['/', '/instituicoes', '/departamentos', '/usuarios', '/auditoria'])
  })

  it('recepcionista ve pacientes, atendimentos e acesso a dados', () => {
    const u: UsuarioLogado = { ...base, instituicoes: [{ cnpj: 'A', nomeInstituicao: 'A', papeis: ['RECEPCIONISTA'] }] }
    expect(caminhos(u, 'A')).toEqual(['/', '/pacientes', '/atendimentos', '/solicitacoes'])
  })

  it('medico ve fila e historico; enfermeiro idem', () => {
    for (const papel of ['MEDICO', 'ENFERMEIRO'] as const) {
      const u: UsuarioLogado = { ...base, instituicoes: [{ cnpj: 'A', nomeInstituicao: 'A', papeis: [papel] }] }
      expect(caminhos(u, 'A')).toEqual(['/', '/atendimentos', '/historico'])
    }
  })

  it('o papel vale so na instituicao ativa', () => {
    const u: UsuarioLogado = {
      ...base,
      instituicoes: [
        { cnpj: 'A', nomeInstituicao: 'A', papeis: ['MEDICO'] },
        { cnpj: 'B', nomeInstituicao: 'B', papeis: ['RECEPCIONISTA'] },
      ],
    }
    expect(capacidades(u, 'A').medico).toBe(true)
    expect(capacidades(u, 'A').recepcionista).toBe(false)
    expect(capacidades(u, 'B').recepcionista).toBe(true)
    expect(capacidades(u, 'B').medico).toBe(false)
  })

  it('paciente ve so a propria area', () => {
    const c = caminhos({ ...base, roles: ['ROLE_PACIENTE'] }, null)
    expect(c).toEqual(['/', '/meu/historico', '/meu/medicamentos', '/meu/exames', '/meu/solicitacoes', '/meu/acessos'])
  })

  it('quem e medico e recepcionista nao ganha item repetido', () => {
    const u: UsuarioLogado = { ...base, instituicoes: [{ cnpj: 'A', nomeInstituicao: 'A', papeis: ['RECEPCIONISTA', 'MEDICO'] }] }
    const c = caminhos(u, 'A')
    expect(c.filter((x) => x === '/atendimentos')).toHaveLength(1)
  })

  it('usuario sem papel so tem o inicio', () => {
    expect(caminhos(base, null)).toEqual(['/'])
  })
})
