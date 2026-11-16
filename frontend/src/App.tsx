import type { ReactNode } from 'react'
import { Link, Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import type { Capacidades } from './auth/permissoes'
import { Layout } from './components/Layout'
import { AuditoriaPage } from './pages/admin/AuditoriaPage'
import { DepartamentosPage } from './pages/admin/DepartamentosPage'
import { InstituicoesPage } from './pages/admin/InstituicoesPage'
import { UsuariosPage } from './pages/admin/UsuariosPage'
import { AtendimentoPage } from './pages/clinica/AtendimentoPage'
import { HistoricoPage } from './pages/clinica/HistoricoPage'
import { InicioPage } from './pages/InicioPage'
import { LoginPage } from './pages/LoginPage'
import { MeuAtendimentoPage, MeuHistoricoPage } from './pages/paciente/MeuHistoricoPage'
import { MeusExamesPage } from './pages/paciente/MeusExamesPage'
import { MeusMedicamentosPage } from './pages/paciente/MeusMedicamentosPage'
import { MinhasSolicitacoesPage } from './pages/paciente/MinhasSolicitacoesPage'
import { QuemAcessouPage } from './pages/paciente/QuemAcessouPage'
import { AtendimentosPage } from './pages/recepcao/AtendimentosPage'
import { PacientesPage } from './pages/recepcao/PacientesPage'
import { SolicitacoesPage } from './pages/recepcao/SolicitacoesPage'
import { TrocarSenhaPage } from './pages/TrocarSenhaPage'

/** So deixa passar quem entrou e ja trocou a senha temporaria. */
function RotaAutenticada() {
  const { usuario } = useAuth()
  const local = useLocation()
  if (!usuario) return <Navigate to="/login" replace state={{ de: local.pathname }} />
  if (usuario.deveTrocarSenha) return <Navigate to="/trocar-senha" replace />
  return <Outlet />
}

function AcessoNegado() {
  return (
    <section className="painel">
      <div className="estado vazio">
        <strong>Você não tem acesso a esta tela</strong>
        <span>O seu papel nesta instituição não permite esta função. Se precisar, fale com o administrador.</span>
        <Link to="/" className="btn btn-secundario">
          Voltar ao início
        </Link>
      </div>
    </section>
  )
}

/** Mostra a tela somente se o usuario tiver a capacidade exigida (o servidor confere de novo em toda chamada). */
function Requer({ permitido, children }: { permitido: (caps: Capacidades) => boolean; children: ReactNode }) {
  const { caps } = useAuth()
  return permitido(caps) ? <>{children}</> : <AcessoNegado />
}

function NaoEncontrada() {
  return (
    <section className="painel">
      <div className="estado vazio">
        <strong>Página não encontrada</strong>
        <Link to="/" className="btn btn-secundario">
          Voltar ao início
        </Link>
      </div>
    </section>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/trocar-senha" element={<TrocarSenhaPage />} />

      <Route element={<RotaAutenticada />}>
        <Route element={<Layout />}>
          <Route index element={<InicioPage />} />

          <Route path="instituicoes" element={<Requer permitido={(c) => c.adminSistema}><InstituicoesPage /></Requer>} />
          <Route path="departamentos" element={<Requer permitido={(c) => c.administrador}><DepartamentosPage /></Requer>} />
          <Route path="usuarios" element={<Requer permitido={(c) => c.administrador}><UsuariosPage /></Requer>} />
          <Route path="auditoria" element={<Requer permitido={(c) => c.administrador}><AuditoriaPage /></Requer>} />

          <Route path="pacientes" element={<Requer permitido={(c) => c.recepcionista}><PacientesPage /></Requer>} />
          <Route path="solicitacoes" element={<Requer permitido={(c) => c.recepcionista}><SolicitacoesPage /></Requer>} />
          <Route
            path="atendimentos"
            element={<Requer permitido={(c) => c.recepcionista || c.profissional}><AtendimentosPage /></Requer>}
          />
          <Route path="atendimentos/:id" element={<Requer permitido={(c) => c.profissional}><AtendimentoPage /></Requer>} />
          <Route path="historico" element={<Requer permitido={(c) => c.profissional}><HistoricoPage /></Requer>} />
          <Route path="historico/:cpf" element={<Requer permitido={(c) => c.profissional}><HistoricoPage /></Requer>} />

          <Route path="meu/historico" element={<Requer permitido={(c) => c.paciente}><MeuHistoricoPage /></Requer>} />
          <Route path="meu/historico/:id" element={<Requer permitido={(c) => c.paciente}><MeuAtendimentoPage /></Requer>} />
          <Route path="meu/medicamentos" element={<Requer permitido={(c) => c.paciente}><MeusMedicamentosPage /></Requer>} />
          <Route path="meu/exames" element={<Requer permitido={(c) => c.paciente}><MeusExamesPage /></Requer>} />
          <Route path="meu/solicitacoes" element={<Requer permitido={(c) => c.paciente}><MinhasSolicitacoesPage /></Requer>} />
          <Route path="meu/acessos" element={<Requer permitido={(c) => c.paciente}><QuemAcessouPage /></Requer>} />

          <Route path="*" element={<NaoEncontrada />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
