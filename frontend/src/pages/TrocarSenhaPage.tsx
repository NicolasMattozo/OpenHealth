import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Botao } from '../components/Botao'
import { CampoTexto } from '../components/Campos'
import { Icone } from '../components/Icone'
import { useAvisos } from '../components/Avisos'
import { useEnvio } from '../utils/useEnvio'

export function TrocarSenhaPage() {
  const { usuario, trocarSenha, sair } = useAuth()
  const avisos = useAvisos()
  const navegar = useNavigate()
  const [atual, setAtual] = useState('')
  const [nova, setNova] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const { enviando, erro, campos, enviar } = useEnvio()
  const [erroLocal, setErroLocal] = useState<string | null>(null)

  if (!usuario) return <Navigate to="/login" replace />
  const obrigatoria = usuario.deveTrocarSenha

  const submeter = async (e: FormEvent) => {
    e.preventDefault()
    setErroLocal(null)
    if (nova.length < 8) return setErroLocal('A nova senha precisa ter pelo menos 8 caracteres.')
    if (nova !== confirmacao) return setErroLocal('A confirmação não é igual à nova senha.')
    const resultado = await enviar(() => trocarSenha(atual, nova))
    if (resultado) {
      avisos.sucesso('Senha alterada com sucesso.')
      navegar('/', { replace: true })
    }
  }

  return (
    <div className="pagina-entrada pagina-simples">
      <form className="cartao-entrada" onSubmit={submeter}>
        <span className="marca-icone grande">
          <Icone nome="cadeado" tamanho={26} />
        </span>
        <h2>{obrigatoria ? 'Defina a sua senha' : 'Trocar senha'}</h2>
        <p className="texto-suave">
          {obrigatoria
            ? `Olá, ${usuario.nome.split(' ')[0]}. Você entrou com uma senha temporária. Escolha uma senha nova para continuar.`
            : 'Informe a senha atual e escolha uma nova.'}
        </p>

        {(erroLocal || erro) && (
          <div className="faixa faixa-erro" role="alert">
            {erroLocal ?? erro}
          </div>
        )}

        <CampoTexto
          rotulo={obrigatoria ? 'Senha temporária' : 'Senha atual'}
          type="password"
          value={atual}
          onChange={(e) => setAtual(e.target.value)}
          autoComplete="current-password"
          autoFocus
          required
        />
        <CampoTexto
          rotulo="Nova senha"
          type="password"
          value={nova}
          onChange={(e) => setNova(e.target.value)}
          autoComplete="new-password"
          dica="Mínimo de 8 caracteres."
          erro={campos.novaSenha}
          required
        />
        <CampoTexto
          rotulo="Repita a nova senha"
          type="password"
          value={confirmacao}
          onChange={(e) => setConfirmacao(e.target.value)}
          autoComplete="new-password"
          required
        />
        <Botao type="submit" carregando={enviando} className="btn-bloco">
          Salvar nova senha
        </Botao>
        {obrigatoria ? (
          <Botao variante="fantasma" className="btn-bloco" onClick={sair}>
            Sair
          </Botao>
        ) : (
          <Link to="/" className="link-voltar">
            Cancelar
          </Link>
        )}
      </form>
    </div>
  )
}
