import type { NomeIcone } from '../auth/permissoes'

export type NomeIconeUi =
  | NomeIcone
  | 'sair'
  | 'mais'
  | 'busca'
  | 'ok'
  | 'x'
  | 'menu'
  | 'copiar'
  | 'alerta'
  | 'seta'
  | 'relogio'
  | 'atividade'
  | 'editar'
  | 'cadeado'

const CAMINHOS: Record<NomeIconeUi, string> = {
  inicio: 'M3 11l9-8 9 8M5 10v10h5v-6h4v6h5V10',
  predio: 'M4 21V5l8-3 8 3v16M9 21v-4h6v4M8 9h.01M12 9h.01M16 9h.01M8 13h.01M12 13h.01M16 13h.01',
  camadas: 'M12 3l9 5-9 5-9-5 9-5zM3 13l9 5 9-5M3 17l9 5 9-5',
  usuarios:
    'M16 19v-1a4 4 0 00-4-4H7a4 4 0 00-4 4v1M9.5 11a3.5 3.5 0 100-7 3.5 3.5 0 000 7zM21 19v-1a4 4 0 00-3-3.9M16 4.2a3.5 3.5 0 010 6.6',
  escudo: 'M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6l8-3zM9 12l2 2 4-4',
  paciente: 'M15 19v-1a4 4 0 00-4-4H7a4 4 0 00-4 4v1M9 11a3.5 3.5 0 100-7 3.5 3.5 0 000 7zM19 8v6M22 11h-6',
  prancheta:
    'M9 4h6a1 1 0 011 1v1H8V5a1 1 0 011-1zM8 6H6a1 1 0 00-1 1v13a1 1 0 001 1h12a1 1 0 001-1V7a1 1 0 00-1-1h-2M9 12h6M9 16h4',
  chave: 'M2.586 17.414A2 2 0 002 18.828V21a1 1 0 001 1h3a1 1 0 001-1v-1a1 1 0 011-1h1a1 1 0 001-1v-1a1 1 0 011-1h.172a2 2 0 001.414-.586l.814-.814a6.5 6.5 0 10-4-4zM16.5 7.5h.01',
  documento: 'M7 3h7l5 5v13H7zM14 3v5h5M10 13h6M10 17h6',
  pilula: 'M10.5 20.5l-7-7a4.9 4.9 0 017-7l7 7a4.9 4.9 0 01-7 7zM8.5 8.5l7 7',
  frasco: 'M9 3h6M10 3v6l-5 9a2 2 0 001.8 3h10.4A2 2 0 0019 18l-5-9V3M8 15h8',
  olho: 'M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12zM12 15a3 3 0 100-6 3 3 0 000 6z',
  coracao: 'M12 20s-7-4.5-9-9a5 5 0 019-3 5 5 0 019 3c-2 4.5-9 9-9 9z',
  sair: 'M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4M16 17l5-5-5-5M21 12H9',
  mais: 'M12 5v14M5 12h14',
  busca: 'M11 19a8 8 0 100-16 8 8 0 000 16zM21 21l-4.3-4.3',
  ok: 'M5 13l4 4L19 7',
  x: 'M6 6l12 12M18 6L6 18',
  menu: 'M4 6h16M4 12h16M4 18h16',
  copiar: 'M9 9h10v12H9zM5 15V3h10',
  alerta:
    'M12 9v4M12 17h.01M10.3 3.9L2.4 18a2 2 0 001.7 3h15.8a2 2 0 001.7-3L13.7 3.9a2 2 0 00-3.4 0z',
  seta: 'M9 6l6 6-6 6',
  relogio: 'M12 7v5l3 2M12 21a9 9 0 100-18 9 9 0 000 18z',
  atividade: 'M3 12h4l3-8 4 16 3-8h4',
  editar: 'M4 20h4L19 9l-4-4L4 16v4zM13.5 6.5l4 4',
  cadeado: 'M6 11h12v10H6zM8 11V8a4 4 0 018 0v3',
}

export function Icone({ nome, tamanho = 18 }: { nome: NomeIconeUi; tamanho?: number }) {
  return (
    <svg
      width={tamanho}
      height={tamanho}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      <path d={CAMINHOS[nome]} />
    </svg>
  )
}
