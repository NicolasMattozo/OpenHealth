import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { clinica } from '../../api/endpoints'
import { useAuth } from '../../auth/AuthContext'
import { Abas } from '../../components/Abas'
import { Botao } from '../../components/Botao'
import { CabecalhoPagina } from '../../components/CabecalhoPagina'
import { Resultado, Vazio } from '../../components/Estados'
import { Icone } from '../../components/Icone'
import { LinhaDoTempo } from '../../components/LinhaDoTempo'
import { Selo, SeloAtendimento } from '../../components/Selo'
import { formatarCpf, formatarDataHora, mascaraCpf, somenteDigitos } from '../../utils/format'
import { useAsync } from '../../utils/useAsync'

type IdAba = 'atendimentos' | 'medicamentos' | 'requisicoes' | 'exames'

function Atendimentos({ cnpj, cpf }: { cnpj: string; cpf: string }) {
  const lista = useAsync(() => clinica.historico(cnpj, cpf), [cnpj, cpf])
  return (
    <Resultado {...lista}>
      {(dados) =>
        dados.length === 0 ? (
          <Vazio icone="documento" titulo="Nenhum atendimento visível para esta instituição">
            Atendimentos de outras instituições só aparecem se o paciente tiver aprovado o acesso.
          </Vazio>
        ) : (
          <div className="tabela-rolagem">
            <table className="tabela">
              <thead>
                <tr>
                  <th>Data</th>
                  <th>Instituição</th>
                  <th>Motivo</th>
                  <th>Situação</th>
                  <th>Médico</th>
                  <th aria-label="Ações" />
                </tr>
              </thead>
              <tbody>
                {dados.map((a) => (
                  <tr key={a.id}>
                    <td>{formatarDataHora(a.dataHora)}</td>
                    <td>
                      {a.nomeInstituicao}
                      {a.cnpjInstituicao !== cnpj && (
                        <>
                          {' '}
                          <Selo tom="roxo">Outra instituição</Selo>
                        </>
                      )}
                    </td>
                    <td>{a.motivo}</td>
                    <td>
                      <SeloAtendimento status={a.status} />
                    </td>
                    <td>{a.nomeMedico ?? '—'}</td>
                    <td className="acoes">
                      <Link to={`/atendimentos/${a.id}`} className="btn btn-secundario btn-pequeno">
                        Abrir prontuário
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      }
    </Resultado>
  )
}

function Medicamentos({ cnpj, cpf }: { cnpj: string; cpf: string }) {
  const lista = useAsync(() => clinica.medicamentos(cnpj, cpf), [cnpj, cpf])
  return (
    <Resultado {...lista}>
      {(dados) =>
        dados.length === 0 ? (
          <Vazio icone="pilula" titulo="Nenhum medicamento prescrito" />
        ) : (
          <div className="tabela-rolagem">
            <table className="tabela">
              <thead>
                <tr>
                  <th>Medicamento</th>
                  <th>Dosagem</th>
                  <th>Via</th>
                  <th>Frequência</th>
                  <th>Prescrito por</th>
                  <th>Data</th>
                </tr>
              </thead>
              <tbody>
                {dados.map((m) => (
                  <tr key={m.id}>
                    <td>
                      <strong>{m.medicamento}</strong>
                    </td>
                    <td>{m.dosagem}</td>
                    <td>{m.via ?? '—'}</td>
                    <td>{m.frequencia ?? '—'}</td>
                    <td>{m.autor}</td>
                    <td>{formatarDataHora(m.dataRegistro)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      }
    </Resultado>
  )
}

function Requisicoes({ cnpj, cpf, somenteComResultado }: { cnpj: string; cpf: string; somenteComResultado: boolean }) {
  const lista = useAsync(() => (somenteComResultado ? clinica.exames(cnpj, cpf) : clinica.requisicoes(cnpj, cpf)), [cnpj, cpf, somenteComResultado])
  return (
    <Resultado {...lista}>
      {(dados) =>
        dados.length === 0 ? (
          <Vazio icone="frasco" titulo={somenteComResultado ? 'Nenhum exame com resultado' : 'Nenhuma requisição de exame'} />
        ) : (
          <LinhaDoTempo registros={dados} />
        )
      }
    </Resultado>
  )
}

/** Historico clinico de um paciente, consultado pelo CPF (casos de uso 10, 12, 13 e 14). */
export function HistoricoPage() {
  const { cpf: cpfDaUrl } = useParams()
  const navegar = useNavigate()
  const { instituicaoAtiva, caps } = useAuth()
  const cnpj = instituicaoAtiva as string
  const cpf = cpfDaUrl ? somenteDigitos(cpfDaUrl) : null
  const [texto, setTexto] = useState(cpf ? mascaraCpf(cpf) : '')
  const [aba, setAba] = useState<IdAba>('atendimentos')
  const [erro, setErro] = useState<string | null>(null)

  const consultar = (e: FormEvent) => {
    e.preventDefault()
    const digitos = somenteDigitos(texto)
    if (digitos.length !== 11) return setErro('Informe o CPF completo, com 11 números.')
    setErro(null)
    setAba('atendimentos')
    navegar(`/historico/${digitos}`)
  }

  const abas: { id: IdAba; rotulo: string }[] = [{ id: 'atendimentos', rotulo: 'Atendimentos' }]
  if (caps.enfermeiro) abas.push({ id: 'medicamentos', rotulo: 'Medicamentos' })
  abas.push({ id: 'requisicoes', rotulo: 'Requisições de exames' })
  if (caps.medico) abas.push({ id: 'exames', rotulo: 'Exames com resultado' })

  return (
    <>
      <CabecalhoPagina
        secao="Clínica"
        titulo="Histórico do paciente"
        descricao="Consulte o histórico clínico pelo CPF. Cada consulta fica registrada na auditoria."
      />

      <section className="painel">
        <form className="filtros" onSubmit={consultar}>
          <div className="campo-busca">
            <Icone nome="busca" />
            <input
              placeholder="CPF do paciente: 000.000.000-00"
              value={texto}
              onChange={(e) => setTexto(mascaraCpf(e.target.value))}
              aria-label="CPF do paciente"
              inputMode="numeric"
              autoFocus={!cpf}
            />
          </div>
          <Botao type="submit">Consultar</Botao>
        </form>
        {erro && <p className="erro-formulario">{erro}</p>}

        {!cpf ? (
          <Vazio icone="documento" titulo="Informe o CPF para ver o histórico">
            Você vê o que foi registrado nesta instituição e, se o paciente autorizou, também em outras.
          </Vazio>
        ) : (
          <>
            <div className="faixa faixa-info">
              <Icone nome="escudo" />
              <span>
                Paciente com CPF <strong>{formatarCpf(cpf)}</strong>. Registros de outras instituições só aparecem com acesso aprovado pelo
                paciente.
              </span>
            </div>
            <Abas itens={abas} ativa={abas.some((x) => x.id === aba) ? aba : 'atendimentos'} aoMudar={setAba} />
            {aba === 'atendimentos' && <Atendimentos cnpj={cnpj} cpf={cpf} />}
            {aba === 'medicamentos' && <Medicamentos cnpj={cnpj} cpf={cpf} />}
            {aba === 'requisicoes' && <Requisicoes cnpj={cnpj} cpf={cpf} somenteComResultado={false} />}
            {aba === 'exames' && <Requisicoes cnpj={cnpj} cpf={cpf} somenteComResultado />}
          </>
        )}
      </section>
    </>
  )
}
