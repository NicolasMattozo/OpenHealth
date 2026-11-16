import { meusDados } from '../../api/endpoints'
import { CabecalhoPagina } from '../../components/CabecalhoPagina'
import { Resultado, Vazio } from '../../components/Estados'
import { formatarDataHora } from '../../utils/format'
import { useAsync } from '../../utils/useAsync'

export function MeusMedicamentosPage() {
  const lista = useAsync(() => meusDados.medicamentos(), [])
  return (
    <>
      <CabecalhoPagina secao="Meu prontuário" titulo="Medicamentos prescritos" descricao="Tudo o que foi prescrito para você, do mais recente para o mais antigo." />
      <section className="painel">
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
      </section>
    </>
  )
}
