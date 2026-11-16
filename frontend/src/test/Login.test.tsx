import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import { AuthProvider } from '../auth/AuthContext'
import { AvisosProvider } from '../components/Avisos'

function montar(rota = '/') {
  return render(
    <MemoryRouter initialEntries={[rota]}>
      <AvisosProvider>
        <AuthProvider>
          <App />
        </AuthProvider>
      </AvisosProvider>
    </MemoryRouter>,
  )
}

const usuarioBase = { id: 1, nome: 'Maria Souza', email: 'maria@x.com', cpf: null, roles: ['ROLE_ADMIN_SISTEMA'], instituicoes: [], deveTrocarSenha: false }

function jwtFalso(): string {
  const b64 = (o: object) => btoa(JSON.stringify(o)).replace(/=+$/, '')
  return `${b64({ alg: 'HS256' })}.${b64({ exp: Math.floor(Date.now() / 1000) + 3600 })}.x`
}

describe('login e protecao de rotas', () => {
  beforeEach(() => sessionStorage.clear())
  afterEach(() => vi.restoreAllMocks())

  it('sem sessao, qualquer rota leva ao login', async () => {
    montar('/usuarios')
    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
  })

  it('mostra a mensagem do servidor quando a senha esta errada', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ erro: 'Credenciais inválidas.', campos: {} }), { status: 401 }))
    montar('/login')
    await userEvent.type(screen.getByLabelText('E-mail ou CPF'), 'maria@x.com')
    await userEvent.type(screen.getByLabelText('Senha'), 'errada')
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Credenciais inválidas.')
  })

  it('entra e mostra o inicio com o menu do papel', async () => {
    const corpo = { token: jwtFalso(), tipo: 'Bearer', expiraEmSegundos: 3600, usuario: usuarioBase }
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (url) =>
      String(url).includes('/api/auth/me') ? new Response(JSON.stringify(usuarioBase), { status: 200 }) : new Response(JSON.stringify(corpo), { status: 200 }),
    )
    montar('/login')
    await userEvent.type(screen.getByLabelText('E-mail ou CPF'), 'maria@x.com')
    await userEvent.type(screen.getByLabelText('Senha'), 'Senha123')
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('heading', { level: 1, name: /Maria/ })).toBeInTheDocument()
    await waitFor(() => expect(screen.getAllByText('Instituições').length).toBeGreaterThan(0))
    expect(screen.queryByText('Pacientes')).not.toBeInTheDocument()
  })

  it('com senha temporaria so deixa ir para a troca de senha', async () => {
    const corpo = { token: jwtFalso(), tipo: 'Bearer', expiraEmSegundos: 3600, usuario: { ...usuarioBase, roles: [], deveTrocarSenha: true } }
    sessionStorage.setItem('prontuarios.sessao', JSON.stringify({ token: corpo.token, usuario: corpo.usuario }))
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify(corpo.usuario), { status: 200 }))
    montar('/usuarios')
    expect(await screen.findByRole('heading', { name: 'Defina a sua senha' })).toBeInTheDocument()
  })

  it('usuario sem permissao na tela ve o aviso de acesso negado', async () => {
    const corpo = { ...usuarioBase, roles: [], instituicoes: [{ cnpj: 'A', nomeInstituicao: 'Hospital A', papeis: ['RECEPCIONISTA'] }] }
    sessionStorage.setItem('prontuarios.sessao', JSON.stringify({ token: jwtFalso(), usuario: corpo }))
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify(corpo), { status: 200 }))
    montar('/instituicoes')
    expect(await screen.findByText('Você não tem acesso a esta tela')).toBeInTheDocument()
  })
})
