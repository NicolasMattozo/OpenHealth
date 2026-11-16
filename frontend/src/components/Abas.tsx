export interface ItemAba<T extends string> {
  id: T
  rotulo: string
}

export function Abas<T extends string>({
  itens,
  ativa,
  aoMudar,
}: {
  itens: ItemAba<T>[]
  ativa: T
  aoMudar: (id: T) => void
}) {
  return (
    <div className="abas" role="tablist">
      {itens.map((item) => (
        <button
          key={item.id}
          type="button"
          role="tab"
          aria-selected={item.id === ativa}
          className={item.id === ativa ? 'aba ativa' : 'aba'}
          onClick={() => aoMudar(item.id)}
        >
          {item.rotulo}
        </button>
      ))}
    </div>
  )
}
