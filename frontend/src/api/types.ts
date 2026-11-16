// Tipos que espelham os DTOs do backend (pacote web.dto).

export type Papel = 'RECEPCIONISTA' | 'MEDICO' | 'ENFERMEIRO' | 'ADMIN_INSTITUICAO'
export type StatusAtendimento = 'ABERTO' | 'EM_ANDAMENTO' | 'FINALIZADO' | 'CANCELADO'
export type StatusSolicitacao = 'PENDENTE' | 'APROVADA' | 'NEGADA' | 'REVOGADA'
export type ClassificacaoRisco = 'VERMELHO' | 'LARANJA' | 'AMARELO' | 'VERDE' | 'AZUL'
export type TipoRegistro =
  | 'TRIAGEM'
  | 'DADO_CLINICO'
  | 'CONDICAO_DIAGNOSTICO'
  | 'PRESCRICAO_MEDICAMENTO'
  | 'REQUISICAO_EXAME'

export interface Vinculo {
  cnpj: string
  nomeInstituicao: string
  papeis: Papel[]
}

export interface UsuarioLogado {
  id: number
  nome: string
  email: string
  cpf: string | null
  roles: string[]
  instituicoes: Vinculo[]
  deveTrocarSenha: boolean
}

export interface LoginResponse {
  token: string
  tipo: string
  expiraEmSegundos: number
  usuario: UsuarioLogado
}

export interface Instituicao {
  cnpj: string
  nome: string
  tipo: string | null
  endereco: string | null
  ativo: boolean
}

export interface Departamento {
  id: number
  cnpjInstituicao: string
  nome: string
  ativo: boolean
}

export interface UsuarioInstituicao {
  id: number
  nome: string
  email: string
  ativo: boolean
  papeis: Papel[]
  departamentoId: number | null
  departamentoNome: string | null
  numeroConselho: string | null
  ufConselho: string | null
  especialidade: string | null
}

export interface UsuarioCriado {
  usuario: UsuarioInstituicao
  senhaTemporaria: string | null
}

export interface Paciente {
  cpf: string
  nome: string
  email: string
  dataNascimento: string
  telefone: string | null
  endereco: string | null
}

export interface PacienteCriado {
  paciente: Paciente
  senhaTemporaria: string
}

export interface Atendimento {
  id: number
  cpfPaciente: string
  nomePaciente: string
  motivo: string
  dataHora: string
  dataFinalizacao: string | null
  status: StatusAtendimento
  idSolicitacao: number | null
  nomeRecepcionista: string
  nomeMedico: string | null
}

export interface SolicitacaoAcesso {
  id: number
  cpfPaciente: string
  nomePaciente: string
  cnpjInstituicao: string
  finalidade: string
  status: StatusSolicitacao
  dataSolicitacao: string
  dataDecisao: string | null
  dataRevogacao: string | null
  nomeRecepcionista: string
}

export interface SolicitacaoParaPaciente {
  id: number
  cnpjInstituicao: string
  nomeInstituicao: string
  tipoInstituicao: string | null
  finalidade: string
  solicitadaPor: string
  status: StatusSolicitacao
  dataSolicitacao: string
  dataDecisao: string | null
  dataRevogacao: string | null
}

export interface ResultadoExame {
  versao: number
  resultado: string
  dataResultado: string
  registradoPor: string
  tipoRegistrador: string
}

/** Um registro da linha do tempo; "tipo" diz quais campos vem preenchidos. */
export interface RegistroClinico {
  id: number
  tipo: TipoRegistro
  dataRegistro: string
  autor: string
  tipoAutor: string
  visivelAoPaciente: boolean
  classificacaoRisco: ClassificacaoRisco | null
  sintomas: string | null
  tipoDado: string | null
  valor: string | null
  descricao: string | null
  cid: string | null
  medicamento: string | null
  dosagem: string | null
  via: string | null
  frequencia: string | null
  tipoExame: string | null
  statusExame: 'PENDENTE' | 'REALIZADO' | null
  resultados: ResultadoExame[] | null
}

export interface AtendimentoHistorico {
  id: number
  cnpjInstituicao: string
  nomeInstituicao: string
  motivo: string
  dataHora: string
  dataFinalizacao: string | null
  status: StatusAtendimento
  nomeMedico: string | null
}

export interface ProntuarioDetalhado {
  atendimento: AtendimentoHistorico
  cpfPaciente: string
  nomePaciente: string
  registros: RegistroClinico[]
}

export interface LogAuditoria {
  id: number
  instante: string
  idUsuario: number | null
  nomeUsuario: string | null
  acao: string
  tipoRecurso: string
  /** Texto JSON: cnpj, cpf, ids, status HTTP, ip... */
  chaveRecurso: string
}

export interface AcessoAoPaciente {
  instante: string
  acao: string
  ator: string | null
  instituicao: string | null
  status: number
  entreInstituicoes: boolean
}

// ---- corpos das requisicoes ----

export interface NovaInstituicao {
  nome: string
  cnpj: string
  tipo?: string
  endereco?: string
}

export interface NovoUsuario {
  nome: string
  email: string
  papeis: Papel[]
  departamentoId?: number | null
  numeroConselho?: string
  ufConselho?: string
  especialidade?: string
}

export interface AlteracaoPapeis {
  papeis: Papel[]
  departamentoId?: number | null
  numeroConselho?: string
  ufConselho?: string
  especialidade?: string
}

export interface NovoPaciente {
  nome: string
  cpf: string
  email: string
  dataNascimento: string
  telefone?: string
  endereco?: string
}

export interface FiltrosAuditoria {
  idUsuario?: number
  acao?: string
  cpf?: string
  cnpj?: string
  de?: string
  ate?: string
  limite?: number
}
