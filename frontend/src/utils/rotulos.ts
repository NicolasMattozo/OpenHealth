import type { ClassificacaoRisco, Papel, StatusAtendimento, StatusSolicitacao } from '../api/types'

export const ROTULO_PAPEL: Record<Papel, string> = {
  RECEPCIONISTA: 'Recepcionista',
  MEDICO: 'Médico',
  ENFERMEIRO: 'Enfermeiro',
  ADMIN_INSTITUICAO: 'Administrador da instituição',
}

export const ROTULO_STATUS_ATENDIMENTO: Record<StatusAtendimento, string> = {
  ABERTO: 'Aberto',
  EM_ANDAMENTO: 'Em andamento',
  FINALIZADO: 'Finalizado',
  CANCELADO: 'Cancelado',
}

export const ROTULO_STATUS_SOLICITACAO: Record<StatusSolicitacao, string> = {
  PENDENTE: 'Pendente',
  APROVADA: 'Aprovada',
  NEGADA: 'Negada',
  REVOGADA: 'Revogada',
}

export const ROTULO_RISCO: Record<ClassificacaoRisco, string> = {
  VERMELHO: 'Vermelho — emergência',
  LARANJA: 'Laranja — muito urgente',
  AMARELO: 'Amarelo — urgente',
  VERDE: 'Verde — pouco urgente',
  AZUL: 'Azul — não urgente',
}

export const ROTULO_ACAO: Record<string, string> = {
  LOGIN: 'Login',
  TROCAR_SENHA: 'Troca de senha',
  CADASTRAR_INSTITUICAO: 'Cadastro de instituição',
  INATIVAR_INSTITUICAO: 'Inativação de instituição',
  REATIVAR_INSTITUICAO: 'Reativação de instituição',
  CRIAR_DEPARTAMENTO: 'Criação de departamento',
  INATIVAR_DEPARTAMENTO: 'Inativação de departamento',
  REATIVAR_DEPARTAMENTO: 'Reativação de departamento',
  CADASTRAR_USUARIO: 'Cadastro de usuário',
  ALTERAR_PAPEIS: 'Alteração de papéis',
  INATIVAR_USUARIO: 'Inativação de usuário',
  REATIVAR_USUARIO: 'Reativação de usuário',
  CADASTRAR_PACIENTE: 'Cadastro de paciente',
  BUSCAR_PACIENTE: 'Busca de paciente',
  CONSULTAR_CADASTRO: 'Consulta de cadastro',
  ATUALIZAR_CADASTRO: 'Atualização de cadastro',
  ABRIR_ATENDIMENTO: 'Abertura de atendimento',
  INICIAR_ATENDIMENTO: 'Início de atendimento',
  FINALIZAR_ATENDIMENTO: 'Fim de atendimento',
  SOLICITAR_ACESSO: 'Solicitação de acesso',
  APROVAR_ACESSO: 'Aprovação de acesso',
  NEGAR_ACESSO: 'Negação de acesso',
  REVOGAR_ACESSO: 'Revogação de acesso',
  REGISTRAR_TRIAGEM: 'Registro de triagem',
  REGISTRAR_DADO_CLINICO: 'Registro de dado clínico',
  REGISTRAR_DIAGNOSTICO: 'Registro de diagnóstico',
  REGISTRAR_PRESCRICAO: 'Registro de prescrição',
  REGISTRAR_REQUISICAO_EXAME: 'Requisição de exame',
  LANCAR_RESULTADO_EXAME: 'Resultado de exame',
  CONSULTAR_PRONTUARIO: 'Consulta de prontuário',
  CONSULTAR_HISTORICO: 'Consulta de histórico',
  CONSULTAR_MEDICAMENTOS: 'Consulta de medicamentos',
  CONSULTAR_REQUISICOES: 'Consulta de requisições',
  CONSULTAR_EXAMES: 'Consulta de exames',
  CONSULTAR_AUDITORIA: 'Consulta de auditoria',
}

export const rotuloAcao = (acao: string) => ROTULO_ACAO[acao] ?? acao

export const TIPOS_INSTITUICAO = ['Hospital', 'Clínica', 'Laboratório', 'Posto de saúde', 'Pronto atendimento']
