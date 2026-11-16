import { consulta, http } from './client'
import type {
  AcessoAoPaciente,
  AlteracaoPapeis,
  Atendimento,
  AtendimentoHistorico,
  ClassificacaoRisco,
  Departamento,
  FiltrosAuditoria,
  Instituicao,
  LoginResponse,
  LogAuditoria,
  NovaInstituicao,
  NovoPaciente,
  NovoUsuario,
  Paciente,
  PacienteCriado,
  ProntuarioDetalhado,
  RegistroClinico,
  ResultadoExame,
  SolicitacaoAcesso,
  SolicitacaoParaPaciente,
  StatusAtendimento,
  UsuarioCriado,
  UsuarioInstituicao,
  UsuarioLogado,
} from './types'

const inst = (cnpj: string) => `/api/instituicoes/${encodeURIComponent(cnpj)}`
const cpfUrl = (cpf: string) => encodeURIComponent(cpf.replace(/\D/g, ''))

// ------------------------------------------------------------------ autenticacao
export const autenticacao = {
  entrar: (identificador: string, senha: string) =>
    http.post<LoginResponse>('/api/auth/login', { identificador, senha }),
  eu: () => http.get<UsuarioLogado>('/api/auth/me'),
  trocarSenha: (senhaAtual: string, novaSenha: string) =>
    http.post<LoginResponse>('/api/auth/trocar-senha', { senhaAtual, novaSenha }),
}

// ------------------------------------------------------------------ administracao
export const instituicoes = {
  listar: () => http.get<Instituicao[]>('/api/instituicoes'),
  cadastrar: (dados: NovaInstituicao) => http.post<Instituicao>('/api/instituicoes', dados),
  inativar: (cnpj: string) => http.delete<Instituicao>(inst(cnpj)),
  reativar: (cnpj: string) => http.post<Instituicao>(`${inst(cnpj)}/reativar`),
}

export const departamentos = {
  listar: (cnpj: string) => http.get<Departamento[]>(`${inst(cnpj)}/departamentos`),
  criar: (cnpj: string, nome: string) => http.post<Departamento>(`${inst(cnpj)}/departamentos`, { nome }),
  inativar: (cnpj: string, id: number) => http.delete<Departamento>(`${inst(cnpj)}/departamentos/${id}`),
  reativar: (cnpj: string, id: number) => http.post<Departamento>(`${inst(cnpj)}/departamentos/${id}/reativar`),
}

export const usuarios = {
  listar: (cnpj: string) => http.get<UsuarioInstituicao[]>(`${inst(cnpj)}/usuarios`),
  cadastrar: (cnpj: string, dados: NovoUsuario) => http.post<UsuarioCriado>(`${inst(cnpj)}/usuarios`, dados),
  alterarPapeis: (cnpj: string, id: number, dados: AlteracaoPapeis) =>
    http.put<UsuarioInstituicao>(`${inst(cnpj)}/usuarios/${id}/papeis`, dados),
  inativar: (cnpj: string, id: number) => http.delete<UsuarioInstituicao>(`${inst(cnpj)}/usuarios/${id}`),
  reativar: (cnpj: string, id: number) => http.post<UsuarioInstituicao>(`${inst(cnpj)}/usuarios/${id}/reativar`),
}

// ------------------------------------------------------------------ recepcao
export const pacientes = {
  buscar: (cnpj: string, busca: string) =>
    http.get<Paciente[]>(`${inst(cnpj)}/pacientes${consulta({ busca })}`),
  consultar: (cnpj: string, cpf: string) => http.get<Paciente>(`${inst(cnpj)}/pacientes/${cpfUrl(cpf)}`),
  cadastrar: (cnpj: string, dados: NovoPaciente) => http.post<PacienteCriado>(`${inst(cnpj)}/pacientes`, dados),
  atualizarContato: (cnpj: string, cpf: string, dados: { telefone: string; endereco: string }) =>
    http.put<Paciente>(`${inst(cnpj)}/pacientes/${cpfUrl(cpf)}`, dados),
}

export const atendimentos = {
  listar: (cnpj: string, status?: StatusAtendimento) =>
    http.get<Atendimento[]>(`${inst(cnpj)}/atendimentos${consulta({ status })}`),
  abrir: (cnpj: string, cpfPaciente: string, motivo: string) =>
    http.post<Atendimento>(`${inst(cnpj)}/atendimentos`, { cpfPaciente, motivo }),
  iniciar: (cnpj: string, id: number) => http.post<Atendimento>(`${inst(cnpj)}/atendimentos/${id}/iniciar`),
  finalizar: (cnpj: string, id: number) => http.post<Atendimento>(`${inst(cnpj)}/atendimentos/${id}/finalizar`),
  prontuario: (cnpj: string, id: number) =>
    http.get<ProntuarioDetalhado>(`${inst(cnpj)}/atendimentos/${id}/prontuario`),
}

export const solicitacoes = {
  listar: (cnpj: string) => http.get<SolicitacaoAcesso[]>(`${inst(cnpj)}/solicitacoes-acesso`),
  solicitar: (cnpj: string, cpfPaciente: string, finalidade: string) =>
    http.post<SolicitacaoAcesso>(`${inst(cnpj)}/solicitacoes-acesso`, { cpfPaciente, finalidade }),
}

// ------------------------------------------------------------------ parte clinica
type Visivel = { visivelAoPaciente?: boolean }

export const clinica = {
  triagem: (cnpj: string, id: number, dados: { classificacaoRisco: ClassificacaoRisco; sintomas?: string } & Visivel) =>
    http.post<RegistroClinico>(`${inst(cnpj)}/atendimentos/${id}/triagem`, dados),
  dadoClinico: (cnpj: string, id: number, dados: { tipo: string; valor: string } & Visivel) =>
    http.post<RegistroClinico>(`${inst(cnpj)}/atendimentos/${id}/dados-clinicos`, dados),
  diagnostico: (cnpj: string, id: number, dados: { descricao: string; cid?: string } & Visivel) =>
    http.post<RegistroClinico>(`${inst(cnpj)}/atendimentos/${id}/diagnosticos`, dados),
  prescricao: (
    cnpj: string,
    id: number,
    dados: { medicamento: string; dosagem: string; via?: string; frequencia?: string } & Visivel,
  ) => http.post<RegistroClinico>(`${inst(cnpj)}/atendimentos/${id}/prescricoes`, dados),
  requisicaoExame: (cnpj: string, id: number, dados: { tipoExame: string } & Visivel) =>
    http.post<RegistroClinico>(`${inst(cnpj)}/atendimentos/${id}/requisicoes-exames`, dados),
  lancarResultado: (cnpj: string, idRequisicao: number, resultado: string) =>
    http.post<ResultadoExame>(`${inst(cnpj)}/requisicoes-exames/${idRequisicao}/resultados`, { resultado }),

  historico: (cnpj: string, cpf: string) =>
    http.get<AtendimentoHistorico[]>(`${inst(cnpj)}/pacientes/${cpfUrl(cpf)}/historico`),
  medicamentos: (cnpj: string, cpf: string) =>
    http.get<RegistroClinico[]>(`${inst(cnpj)}/pacientes/${cpfUrl(cpf)}/medicamentos`),
  requisicoes: (cnpj: string, cpf: string) =>
    http.get<RegistroClinico[]>(`${inst(cnpj)}/pacientes/${cpfUrl(cpf)}/requisicoes-exames`),
  exames: (cnpj: string, cpf: string) =>
    http.get<RegistroClinico[]>(`${inst(cnpj)}/pacientes/${cpfUrl(cpf)}/exames`),
}

// ------------------------------------------------------------------ area do paciente
export const meusDados = {
  historico: () => http.get<AtendimentoHistorico[]>('/api/paciente/historico'),
  atendimento: (id: number) => http.get<ProntuarioDetalhado>(`/api/paciente/atendimentos/${id}`),
  medicamentos: () => http.get<RegistroClinico[]>('/api/paciente/medicamentos'),
  requisicoes: () => http.get<RegistroClinico[]>('/api/paciente/requisicoes-exames'),
  exames: () => http.get<RegistroClinico[]>('/api/paciente/exames'),
  solicitacoes: () => http.get<SolicitacaoParaPaciente[]>('/api/paciente/solicitacoes-acesso'),
  aprovar: (id: number) => http.post<SolicitacaoParaPaciente>(`/api/paciente/solicitacoes-acesso/${id}/aprovar`),
  negar: (id: number) => http.post<SolicitacaoParaPaciente>(`/api/paciente/solicitacoes-acesso/${id}/negar`),
  revogar: (id: number) => http.post<SolicitacaoParaPaciente>(`/api/paciente/solicitacoes-acesso/${id}/revogar`),
  acessos: () => http.get<AcessoAoPaciente[]>('/api/paciente/acessos'),
}

// ------------------------------------------------------------------ auditoria
export const auditoria = {
  /** Sem cnpj: log global (admin do sistema). Com cnpj: so a instituicao. */
  consultar: (filtros: FiltrosAuditoria, cnpj?: string) => {
    const { cnpj: cnpjFiltro, ...resto } = filtros
    const parametros = consulta({ ...resto })
    if (cnpj) return http.get<LogAuditoria[]>(`${inst(cnpj)}/auditoria${parametros}`)
    return http.get<LogAuditoria[]>(`/api/auditoria${consulta({ ...resto, cnpj: cnpjFiltro })}`)
  },
}
