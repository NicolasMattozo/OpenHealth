import { Link } from 'react-router-dom'
import { atendimentos, meusDados } from '../api/endpoints'
import { useAuth } from '../auth/AuthContext'
import { montarMenu } from '../auth/permissoes'
import { CabecalhoPagina } from '../components/CabecalhoPagina'
import { Icone } from '../components/Icone'
import { useAsync } from '../utils/useAsync'

const DESCRICAO: Record<string, string> = {
  '/instituicoes': 'Cadastre e gerencie as instituições da rede.',
  '/departamentos': 'Organize os departamentos de cada instituição.',
  '/usuarios': 'Cadastre usuários e defina os papéis de cada um.',
  '/auditoria': 'Veja quem acessou ou alterou o quê, e quando.',
  '/pacientes': 'Cadastre e consulte pacientes.',
  '/atendimentos': 'Fila de atendimentos da instituição.',
  '/solicitacoes': 'Peça acesso aos dados de pacientes de outras instituições.',
  '/historico': 'Consulte o histórico clínico de um paciente pelo CPF.',
  '/meu/historico': 'Os seus atendimentos em toda a rede.',
  '/meu/medicamentos': 'Medicamentos prescritos para você.',
  '/meu/exames': 'Exames requisitados e seus resultados.',
  '/meu/solicitacoes': 'Aprove, negue ou revogue o acesso de instituições.',
  '/meu/acessos': 'Quem consultou ou alterou os seus dados.',
}

function saudacao(): string {
  const hora = new Date().getHours()
  if (hora < 12) return 'Bom dia'
  return hora < 18 ? 'Boa tarde' : 'Boa noite'
}

export function InicioPage() {
  const { usuario, caps, instituicaoAtiva, nomeInstituicaoAtiva } = useAuth()
  const menu = montarMenu(caps).filter((item) => item.caminho !== '/')

  const fila = useAsync(() => atendimentos.listar(instituicaoAtiva ?? ''), [instituicaoAtiva], (caps.recepcionista || caps.profissional) && !!instituicaoAtiva)
  const pendencias = useAsync(() => meusDados.solicitacoes(), [], caps.paciente)

  const abertos = (fila.dados ?? []).filter((a) => a.status === 'ABERTO' || a.status === 'EM_ANDAMENTO').length
  const pedidos = (pendencias.dados ?? []).filter((s) => s.status === 'PENDENTE').length

  return (
    <>
      <CabecalhoPagina
        secao="Início"
        titulo={`${saudacao()}, ${usuario?.nome.split(' ')[0]}`}
        descricao={nomeInstituicaoAtiva ? `Você está atuando em ${nomeInstituicaoAtiva}.` : 'Escolha uma opção no menu para começar.'}
      />

      {caps.paciente && pedidos > 0 && (
        <Link to="/meu/solicitacoes" className="faixa faixa-aviso faixa-link">
          <Icone nome="alerta" />
          <span>
            Você tem <strong>{pedidos}</strong> {pedidos === 1 ? 'pedido de acesso aguardando' : 'pedidos de acesso aguardando'} a sua
            decisão.
          </span>
          <Icone nome="seta" />
        </Link>
      )}

      {(caps.recepcionista || caps.profissional) && instituicaoAtiva && (
        <div className="resumo">
          <div className="resumo-item">
            <span className="stat-icone tom-azul">
              <Icone nome="prancheta" tamanho={22} />
            </span>
            <div>
              <strong>{fila.carregando && fila.dados === null ? '…' : abertos}</strong>
              <span>atendimentos em aberto ou em andamento</span>
            </div>
          </div>
        </div>
      )}

      <div className="grade-cartoes">
        {menu.map((item) => (
          <Link key={item.caminho} to={item.caminho} className="cartao-atalho">
            <span className="stat-icone tom-azul">
              <Icone nome={item.icone} tamanho={22} />
            </span>
            <div>
              <h3>{item.rotulo}</h3>
              <p>{DESCRICAO[item.caminho]}</p>
            </div>
            <Icone nome="seta" />
          </Link>
        ))}
      </div>

      {menu.length === 0 && (
        <div className="faixa faixa-aviso">
          Seu usuário ainda não tem nenhum papel ativo. Peça ao administrador da instituição para liberar o seu acesso.
        </div>
      )}
    </>
  )
}
