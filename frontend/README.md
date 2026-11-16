# Frontend — Sistema de Prontuários

React + TypeScript + Vite. Fala com o backend Spring pela API REST (`/api/...`).

## Como rodar

Pré-requisito: Node.js 20.16 ou superior (testado com a 20.16.0 e com a 22) e o backend ligado na porta 8080.

```bash
cd frontend
npm install
npm run dev
```

Abra http://localhost:5173. Em desenvolvimento, o Vite repassa tudo que começa com `/api` para
`http://localhost:8080`, então não precisa configurar CORS. Se o backend estiver em outro endereço, copie
`.env.example` para `.env.local` e ajuste `VITE_BACKEND_URL`.

## Se der erro na instalação

- **"Cannot find native binding"** (ou erro parecido sobre `esbuild`/`rollup`): é um bug conhecido do npm com dependências
  opcionais por sistema operacional. Apague `node_modules` e `package-lock.json` e rode `npm install` de novo.
  No PowerShell: `Remove-Item -Recurse -Force node_modules, package-lock.json`.
- O `package-lock.json` é criado na primeira instalação. Pode versionar no Git; se alguém do grupo usar outro sistema
  operacional e tiver o erro acima, é só repetir o passo anterior.

## Comandos

| Comando | O que faz |
|---|---|
| `npm run dev` | servidor de desenvolvimento com recarga automática |
| `npm run build` | confere os tipos (`tsc`) e gera a versão de produção em `dist/` |
| `npm run preview` | serve a versão de `dist/` (também com o proxy para o backend) |
| `npm test` | roda os testes (formatação, token, menu por papel, cliente HTTP, login e rotas) |

## Como o código está organizado

```
src/
  api/        types.ts (espelha os DTOs do backend), client.ts (HTTP + token), endpoints.ts (uma função por endpoint)
  auth/       AuthContext (sessão), permissoes.ts (o que cada papel vê no menu)
  components/ Layout, Modal, Campos, Selo, Avisos (toasts), LinhaDoTempo, ...
  pages/      admin/ recepcao/ clinica/ paciente/ + login e troca de senha
  utils/      máscaras e formatação, useAsync, useEnvio, rótulos
  estilos.css todo o visual (cores em variáveis no topo)
```

## Pontos de atenção

- **A segurança de verdade está no servidor.** O frontend esconde o que o papel não pode usar, mas o backend confere
  toda chamada de novo.
- **Sessão:** o token fica em `sessionStorage` (some ao fechar a aba) e vence em 1 hora; ao vencer, volta para o login.
- **Senha temporária:** quem entra com uma senha temporária só consegue acessar a tela de troca de senha.
- **Instituição ativa:** quem atua em mais de uma instituição escolhe, no topo, onde está trabalhando agora; o menu e as
  telas seguem os papéis dela.
