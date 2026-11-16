import type { Papel, UsuarioLogado } from '../api/types'

export interface Capacidades {
  adminSistema: boolean
  paciente: boolean
  /** Papeis do usuario na instituicao ativa. */
  papeis: Papel[]
  adminInstituicao: boolean
  recepcionista: boolean
  medico: boolean
  enfermeiro: boolean
  /** Medico ou enfermeiro. */
  profissional: boolean
  /** Quem administra (sistema ou instituicao). */
  administrador: boolean
}

export function capacidades(usuario: UsuarioLogado | null, cnpjAtivo: string | null): Capacidades {
  const papeis = usuario?.instituicoes.find((v) => v.cnpj === cnpjAtivo)?.papeis ?? []
  const adminSistema = usuario?.roles.includes('ROLE_ADMIN_SISTEMA') ?? false
  const adminInstituicao = papeis.includes('ADMIN_INSTITUICAO')
  const medico = papeis.includes('MEDICO')
  const enfermeiro = papeis.includes('ENFERMEIRO')
  return {
    adminSistema,
    paciente: usuario?.roles.includes('ROLE_PACIENTE') ?? false,
    papeis,
    adminInstituicao,
    recepcionista: papeis.includes('RECEPCIONISTA'),
    medico,
    enfermeiro,
    profissional: medico || enfermeiro,
    administrador: adminSistema || adminInstituicao,
  }
}

export type NomeIcone =
  | 'inicio'
  | 'predio'
  | 'camadas'
  | 'usuarios'
  | 'escudo'
  | 'paciente'
  | 'prancheta'
  | 'chave'
  | 'documento'
  | 'pilula'
  | 'frasco'
  | 'olho'
  | 'coracao'

export interface ItemMenu {
  rotulo: string
  caminho: string
  icone: NomeIcone
  secao: string
}

/** Itens do menu lateral conforme os papeis do usuario na instituicao ativa. */
export function montarMenu(caps: Capacidades): ItemMenu[] {
  const itens: ItemMenu[] = [{ rotulo: 'Início', caminho: '/', icone: 'inicio', secao: 'Geral' }]

  if (caps.adminSistema) {
    itens.push({ rotulo: 'Instituições', caminho: '/instituicoes', icone: 'predio', secao: 'Administração' })
  }
  if (caps.administrador) {
    itens.push(
      { rotulo: 'Departamentos', caminho: '/departamentos', icone: 'camadas', secao: 'Administração' },
      { rotulo: 'Usuários', caminho: '/usuarios', icone: 'usuarios', secao: 'Administração' },
      { rotulo: 'Auditoria', caminho: '/auditoria', icone: 'escudo', secao: 'Administração' },
    )
  }
  if (caps.recepcionista) {
    itens.push(
      { rotulo: 'Pacientes', caminho: '/pacientes', icone: 'paciente', secao: 'Recepção' },
      { rotulo: 'Atendimentos', caminho: '/atendimentos', icone: 'prancheta', secao: 'Recepção' },
      { rotulo: 'Acesso a dados', caminho: '/solicitacoes', icone: 'chave', secao: 'Recepção' },
    )
  }
  if (caps.profissional) {
    itens.push(
      { rotulo: 'Atendimentos', caminho: '/atendimentos', icone: 'prancheta', secao: 'Clínica' },
      { rotulo: 'Histórico do paciente', caminho: '/historico', icone: 'documento', secao: 'Clínica' },
    )
  }
  if (caps.paciente) {
    itens.push(
      { rotulo: 'Meu histórico', caminho: '/meu/historico', icone: 'documento', secao: 'Meu prontuário' },
      { rotulo: 'Medicamentos', caminho: '/meu/medicamentos', icone: 'pilula', secao: 'Meu prontuário' },
      { rotulo: 'Exames', caminho: '/meu/exames', icone: 'frasco', secao: 'Meu prontuário' },
      { rotulo: 'Quem pode ver meus dados', caminho: '/meu/solicitacoes', icone: 'chave', secao: 'Privacidade' },
      { rotulo: 'Quem acessou', caminho: '/meu/acessos', icone: 'olho', secao: 'Privacidade' },
    )
  }

  // o mesmo caminho nao aparece duas vezes (ex.: usuario recepcionista e medico)
  return itens.filter((item, i) => itens.findIndex((o) => o.caminho === item.caminho) === i)
}
