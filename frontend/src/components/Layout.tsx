import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { montarMenu, type ItemMenu } from '../auth/permissoes'
import { ROTULO_PAPEL } from '../utils/rotulos'
import { Botao } from './Botao'
import { Icone } from './Icone'

function agrupar(itens: ItemMenu[]): [string, ItemMenu[]][] {
  const grupos = new Map<string, ItemMenu[]>()
  for (const item of itens) grupos.set(item.secao, [...(grupos.get(item.secao) ?? []), item])
  return [...grupos.entries()]
}

/** Resumo do que o usuario e na instituicao ativa (ex.: "Médico · Hospital A"). */
function resumoDoPerfil(
  papeis: string[],
  adminSistema: boolean,
  paciente: boolean,
  instituicao: string | null,
): string {
  const rotulos: string[] = []
  if (adminSistema) rotulos.push('Administrador do sistema')
  for (const p of papeis) rotulos.push(ROTULO_PAPEL[p as keyof typeof ROTULO_PAPEL] ?? p)
  if (paciente && rotulos.length === 0) rotulos.push('Paciente')
  const texto = rotulos.join(', ')
  return instituicao && papeis.length > 0 ? `${texto} · ${instituicao}` : texto
}

export function Layout() {
  const { usuario, caps, instituicaoAtiva, nomeInstituicaoAtiva, selecionarInstituicao, sair } = useAuth()
  const [menuAberto, setMenuAberto] = useState(false)
  const local = useLocation()

  useEffect(() => setMenuAberto(false), [local.pathname])

  const grupos = agrupar(montarMenu(caps))
  const vinculos = usuario?.instituicoes ?? []

  return (
    <div className="app">
      <aside className={`lateral ${menuAberto ? 'aberta' : ''}`} aria-label="Menu principal">
        <div className="marca">
          <span className="marca-icone">
            <Icone nome="coracao" tamanho={20} />
          </span>
          <div>
            <strong>Prontuários</strong>
            <small>Rede unificada de saúde</small>
          </div>
        </div>
        <nav>
          {grupos.map(([secao, itens]) => (
            <div key={secao} className="menu-grupo">
              <span className="menu-secao">{secao}</span>
              {itens.map((item) => (
                <NavLink key={item.caminho} to={item.caminho} end={item.caminho === '/'} className="menu-item">
                  <Icone nome={item.icone} />
                  <span>{item.rotulo}</span>
                </NavLink>
              ))}
            </div>
          ))}
        </nav>
        <div className="menu-rodape">
          <NavLink to="/trocar-senha" className="menu-item">
            <Icone nome="cadeado" />
            <span>Trocar senha</span>
          </NavLink>
        </div>
      </aside>
      {menuAberto && <div className="lateral-fundo" onClick={() => setMenuAberto(false)} aria-hidden="true" />}

      <div className="principal">
        <header className="topo">
          <button type="button" className="btn-icone botao-menu" onClick={() => setMenuAberto(true)} aria-label="Abrir menu">
            <Icone nome="menu" tamanho={22} />
          </button>

          {vinculos.length > 1 ? (
            <label className="topo-instituicao">
              <span>Atuando em</span>
              <select className="entrada" value={instituicaoAtiva ?? ''} onChange={(e) => selecionarInstituicao(e.target.value)}>
                {vinculos.map((v) => (
                  <option key={v.cnpj} value={v.cnpj}>
                    {v.nomeInstituicao}
                  </option>
                ))}
              </select>
            </label>
          ) : (
            nomeInstituicaoAtiva && (
              <div className="topo-instituicao">
                <span>Atuando em</span>
                <strong>{nomeInstituicaoAtiva}</strong>
              </div>
            )
          )}

          <div className="topo-espaco" />

          <div className="usuario-chip">
            <span className="avatar" aria-hidden="true">
              {(usuario?.nome ?? '?').trim().charAt(0).toUpperCase()}
            </span>
            <div>
              <strong>{usuario?.nome}</strong>
              <small>{resumoDoPerfil(caps.papeis, caps.adminSistema, caps.paciente, nomeInstituicaoAtiva)}</small>
            </div>
          </div>
          <Botao variante="fantasma" icone="sair" onClick={sair}>
            Sair
          </Botao>
        </header>

        <main className="conteudo">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
