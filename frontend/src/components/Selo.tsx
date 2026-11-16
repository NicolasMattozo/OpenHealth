import type { ReactNode } from 'react'
import type { ClassificacaoRisco, StatusAtendimento, StatusSolicitacao } from '../api/types'
import { ROTULO_STATUS_ATENDIMENTO, ROTULO_STATUS_SOLICITACAO } from '../utils/rotulos'

export type Tom = 'azul' | 'verde' | 'vermelho' | 'amarelo' | 'laranja' | 'cinza' | 'roxo'

export function Selo({ tom = 'cinza', children }: { tom?: Tom; children: ReactNode }) {
  return <span className={`selo selo-${tom}`}>{children}</span>
}

const TOM_ATENDIMENTO: Record<StatusAtendimento, Tom> = {
  ABERTO: 'azul',
  EM_ANDAMENTO: 'amarelo',
  FINALIZADO: 'verde',
  CANCELADO: 'cinza',
}

const TOM_SOLICITACAO: Record<StatusSolicitacao, Tom> = {
  PENDENTE: 'amarelo',
  APROVADA: 'verde',
  NEGADA: 'vermelho',
  REVOGADA: 'cinza',
}

const TOM_RISCO: Record<ClassificacaoRisco, Tom> = {
  VERMELHO: 'vermelho',
  LARANJA: 'laranja',
  AMARELO: 'amarelo',
  VERDE: 'verde',
  AZUL: 'azul',
}

export const SeloAtendimento = ({ status }: { status: StatusAtendimento }) => (
  <Selo tom={TOM_ATENDIMENTO[status]}>{ROTULO_STATUS_ATENDIMENTO[status]}</Selo>
)

export const SeloSolicitacao = ({ status }: { status: StatusSolicitacao }) => (
  <Selo tom={TOM_SOLICITACAO[status]}>{ROTULO_STATUS_SOLICITACAO[status]}</Selo>
)

export const SeloRisco = ({ risco }: { risco: ClassificacaoRisco }) => (
  <Selo tom={TOM_RISCO[risco]}>{risco.charAt(0) + risco.slice(1).toLowerCase()}</Selo>
)

export const SeloAtivo = ({ ativo }: { ativo: boolean }) => (
  <Selo tom={ativo ? 'verde' : 'cinza'}>{ativo ? 'Ativo' : 'Inativo'}</Selo>
)
