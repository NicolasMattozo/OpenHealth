import { useState, type FormEvent } from 'react'
import { Navigate } from 'react-router-dom'
import { CHAVE_AVISO, useAuth } from '../auth/AuthContext'
import { Botao } from '../components/Botao'
import { CampoTexto } from '../components/Campos'
import { Icone } from '../components/Icone'
import { useEnvio } from '../utils/useEnvio'

export function LoginPage() {
  const { usuario, entrar } = useAuth()
  const [identificador, setIdentificador] = useState('')
  const [senha, setSenha] = useState('')
  const { enviando, erro, enviar } = useEnvio()
  const [aviso] = useState(() => {
    const texto = sessionStorage.getItem(CHAVE_AVISO)
    sessionStorage.removeItem(CHAVE_AVISO)
    return texto
  })

  if (usuario) return <Navigate to={usuario.deveTrocarSenha ? '/trocar-senha' : '/'} replace />

  const submeter = async (e: FormEvent) => {
    e.preventDefault()
    await enviar(() => entrar(identificador, senha))
  }

  return (
    <div className="pagina-entrada">
      <div className="entrada-apresentacao">
        <span className="marca-icone grande">
          <Icone nome="coracao" tamanho={28} />
        </span>
        <h1>Sistema de Prontuários</h1>
        <p>
          Prontuário eletrônico para uma rede de instituições de saúde, com o paciente no controle de quem pode ver os
          seus dados.
        </p>
        <ul>
          <li>
            <Icone nome="escudo" /> Acesso entre instituições só com a autorização do paciente
          </li>
          <li>
            <Icone nome="olho" /> Toda consulta fica registrada na auditoria
          </li>
          <li>
            <Icone nome="documento" /> Histórico clínico completo em um só lugar
          </li>
        </ul>
      </div>

      <form className="cartao-entrada" onSubmit={submeter}>
        <h2>Entrar</h2>
        <p className="texto-suave">Use o seu e-mail. Pacientes também podem entrar com o CPF.</p>

        {aviso && <div className="faixa faixa-aviso">{aviso}</div>}
        {erro && (
          <div className="faixa faixa-erro" role="alert">
            {erro}
          </div>
        )}

        <CampoTexto
          rotulo="E-mail ou CPF"
          value={identificador}
          onChange={(e) => setIdentificador(e.target.value)}
          autoComplete="username"
          autoFocus
          required
        />
        <CampoTexto
          rotulo="Senha"
          type="password"
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
          autoComplete="current-password"
          required
        />
        <Botao type="submit" carregando={enviando} className="btn-bloco">
          Entrar
        </Botao>
      </form>
    </div>
  )
}
