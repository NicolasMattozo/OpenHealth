# OpenHealth — Sistema de Prontuários

O **OpenHealth** é um sistema web acadêmico para gerenciamento de prontuários e atendimentos em instituições de saúde. O projeto é dividido em uma API REST desenvolvida em Java com Spring Boot e uma aplicação web desenvolvida em React com TypeScript.

A aplicação contempla cadastro e administração de instituições e usuários, atendimento e triagem, registros clínicos, prescrições, requisições e resultados de exames, portal do paciente, solicitações de acesso ao prontuário e registros de auditoria.

> **Atenção:** este projeto está em desenvolvimento e deve ser utilizado somente em ambiente de desenvolvimento ou demonstração, com dados fictícios. Não utilize dados reais de pacientes nem o disponibilize publicamente antes de uma revisão de segurança, privacidade, integridade dos dados e validação clínica.

---

## 1. Tecnologias utilizadas

### Backend

- **Java 21**
- **Spring Boot 4.1.2**
- Spring Web MVC
- Spring Data JPA
- Spring Security e OAuth2 Resource Server para validação de JWT
- Bean Validation
- Flyway para migrações do banco de dados
- PostgreSQL JDBC Driver
- Lombok
- Maven Wrapper

### Frontend

- **React 19**
- **TypeScript 5.9**
- **Vite 6**
- **React Router 7**
- CSS próprio em `src/estilos.css`
- Vitest, Testing Library e jsdom para testes

### Banco de dados

- **PostgreSQL**
- Migrações SQL versionadas em `src/main/resources/db/migration/`

---

## 2. Funcionalidades contempladas

As funcionalidades abaixo estão representadas na implementação atual do projeto. Isso não significa que o sistema esteja validado para uso clínico ou produção.

### Autenticação e controle de acesso

- Autenticação por e-mail ou CPF, conforme o tipo de usuário.
- Emissão e validação de token JWT.
- Senhas armazenadas com hash BCrypt.
- Controle de acesso conforme o papel do usuário e a instituição selecionada.
- Troca de senha, incluindo o fluxo de senha temporária.

### Perfis de acesso

| Perfil | Escopo principal |
|---|---|
| Administrador do sistema | Administração global e cadastro de instituições. |
| Administrador da instituição | Administração local de usuários, papéis e departamentos. |
| Recepcionista | Cadastro de pacientes e abertura/acompanhamento de atendimentos. |
| Enfermeiro | Acesso às funcionalidades de triagem e registros relacionados. |
| Médico | Atendimento clínico, diagnósticos, prescrições e requisições de exames. |
| Paciente | Consulta ao próprio prontuário e gerenciamento de solicitações de acesso. |

Os papéis institucionais são definidos no código como `RECEPCIONISTA`, `MEDICO`, `ENFERMEIRO` e `ADMIN_INSTITUICAO`. O administrador global e o acesso do paciente possuem tratamento próprio na aplicação.

### Atendimento e prontuário clínico

- Cadastro e consulta de pacientes.
- Abertura e acompanhamento de atendimentos por instituição.
- Registro de triagem e classificação de risco.
- Registro de dados clínicos e diagnósticos.
- Registro de prescrições de medicamentos.
- Requisição de exames e inclusão de resultados.
- Consulta ao histórico clínico conforme as permissões aplicáveis.

Os estados de atendimento definidos no código são `ABERTO`, `EM_ANDAMENTO`, `FINALIZADO` e `CANCELADO`. O fluxo principal implementado utiliza a abertura, o início e a finalização do atendimento. As regras clínicas e de transição devem ser avaliadas e validadas antes de qualquer utilização real.

### Consentimento e acesso ao prontuário

- Solicitação de acesso ao prontuário por uma instituição.
- Aprovação, negativa e revogação de solicitações pelo paciente.
- Consulta do histórico, medicamentos, requisições e resultados disponibilizados ao paciente.

Os estados de solicitação definidos no código são `PENDENTE`, `APROVADA`, `NEGADA` e `REVOGADA`.

### Auditoria

O projeto possui mecanismo de auditoria por interceptor e anotação nos endpoints selecionados. Os registros incluem informações da operação e do contexto da requisição. A migração `V4__auditoria.sql` também cria uma proteção para impedir atualização e exclusão de registros da tabela `log_auditoria`.

A cobertura da auditoria deve ser verificada endpoint por endpoint. A implementação atual não deve ser descrita como auditoria automática de toda operação SQL do banco.

---

## 3. Organização do projeto

A estrutura principal do repositório é:

```text
OpenHealth/
├── .mvn/
│   └── wrapper/
├── src/
│   ├── main/
│   │   ├── java/com/example/sistema_prontuarios/
│   │   │   ├── audit/          # Anotações, interceptor e serviços de auditoria
│   │   │   ├── config/         # Configurações, segurança e administrador inicial
│   │   │   ├── domain/         # Entidades e enums do domínio
│   │   │   ├── repository/     # Acesso e consultas ao banco
│   │   │   ├── security/       # Serviços relacionados a JWT
│   │   │   ├── service/        # Regras de negócio
│   │   │   ├── util/           # Utilitários de CPF e CNPJ
│   │   │   └── web/            # Controllers e DTOs da API
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/   # Migrações Flyway
│   └── test/                  # Testes automatizados do backend
├── frontend/
│   ├── public/
│   └── src/
│       ├── api/               # Cliente HTTP, tipos e endpoints
│       ├── auth/              # Contexto de autenticação e permissões de menu
│       ├── components/        # Componentes reutilizáveis
│       ├── pages/             # Telas por perfil
│       ├── test/              # Configuração dos testes
│       └── utils/             # Formatação e funções auxiliares
├── testar-admin.ps1           # Teste manual da administração
├── testar-auditoria.ps1       # Teste manual da auditoria
├── testar-clinica.ps1         # Teste manual das funcionalidades clínicas
├── testar-paciente.ps1        # Teste manual do portal do paciente
├── testar-recepcao.ps1        # Teste manual da recepção
├── mvnw
├── mvnw.cmd
└── pom.xml
```

O backend está organizado em camadas: controllers (`web`), serviços (`service`), repositórios (`repository`) e entidades (`domain`). O frontend possui diretórios separados para API, autenticação, componentes, páginas e utilitários.

---

## 4. Pré-requisitos

Antes de executar o projeto, instale:

- **JDK 21** (com `java` e `javac` disponíveis no `PATH`).
- **Node.js 20.16 ou superior** e npm.
- **PostgreSQL** em execução.
- Acesso à internet na primeira execução do Maven Wrapper e na instalação das dependências npm.

O Maven Wrapper (`mvnw` / `mvnw.cmd`) baixa a distribuição Maven configurada no projeto, portanto não é necessário instalar o Maven separadamente.

Verifique as instalações:

```powershell
java -version
node --version
npm --version
psql --version
```

---

## 5. Configuração do banco de dados

A aplicação está configurada para utilizar um banco PostgreSQL chamado **`prontuarios`**, por padrão em `localhost:5432`.

Crie o banco antes de iniciar o backend. No terminal, com o PostgreSQL disponível:

```powershell
createdb -U postgres prontuarios
```

Como alternativa, crie um banco com o nome `prontuarios` pelo pgAdmin.

**Não é necessário executar um arquivo `prontuario.sql`.** O esquema é criado e atualizado pelo Flyway, utilizando as migrações versionadas em:

```text
src/main/resources/db/migration/
```

Atualmente, o projeto contém as migrações `V1__schema_inicial.sql`, `V2__ajustes_casos_de_uso.sql`, `V3__administracao.sql` e `V4__auditoria.sql`. Elas são aplicadas automaticamente quando o backend inicia, desde que a conexão com o banco esteja configurada corretamente.

> Se o banco já possuir tabelas ou dados de outra instalação, faça backup e verifique o estado das migrações antes de executar a aplicação. Não apague um banco existente sem confirmar que os dados podem ser descartados.

---

## 6. Variáveis de ambiente do backend

As configurações do backend ficam em `src/main/resources/application.properties`. Os valores abaixo podem ser fornecidos por variáveis de ambiente:

| Variável | Finalidade |
|---|---|
| `DB_USER` | Usuário do PostgreSQL. |
| `DB_PASSWORD` | Senha do usuário do PostgreSQL. |
| `JWT_SECRET` | Chave usada para assinar e validar tokens JWT. Deve ter pelo menos 32 caracteres. |
| `ADMIN_EMAIL` | E-mail do administrador inicial. |
| `ADMIN_SENHA` | Senha do administrador inicial. |

No **PowerShell**, defina as variáveis na mesma janela em que o backend será iniciado:

```powershell
$env:DB_USER = "postgres"
$env:DB_PASSWORD = "SUA_SENHA_DO_POSTGRES"
$env:JWT_SECRET = "COLOQUE_UMA_CHAVE_ALEATORIA_COM_NO_MINIMO_32_CARACTERES"
$env:ADMIN_EMAIL = "admin@seuambiente.local"
$env:ADMIN_SENHA = "DEFINA_UMA_SENHA_FORTE"
```

No Linux ou macOS, use `export`:

```bash
export DB_USER="postgres"
export DB_PASSWORD="SUA_SENHA_DO_POSTGRES"
export JWT_SECRET="COLOQUE_UMA_CHAVE_ALEATORIA_COM_NO_MINIMO_32_CARACTERES"
export ADMIN_EMAIL="admin@seuambiente.local"
export ADMIN_SENHA="DEFINA_UMA_SENHA_FORTE"
```

Na primeira inicialização, a aplicação cria o administrador global caso ainda não exista um usuário com o e-mail configurado em `ADMIN_EMAIL`. Se esse usuário já existir, a senha não é redefinida automaticamente.

**Importante sobre segurança:** o `application.properties` atualmente contém valores alternativos de desenvolvimento para algumas configurações. Defina explicitamente as variáveis acima e, antes de publicar o sistema, remova os valores padrão sensíveis do arquivo. Nunca versione senhas, tokens, chaves JWT ou arquivos locais com credenciais.

A configuração CORS do backend está em `app.cors.origens` no `application.properties`. Se o frontend for executado em outra origem, ajuste essa propriedade para incluir somente as origens autorizadas.

---

## 7. Como executar o backend

Na raiz do repositório, depois de criar o banco e configurar as variáveis de ambiente:

### Windows (PowerShell)

```powershell
.\mvnw.cmd spring-boot:run
```

### Linux ou macOS

```bash
./mvnw spring-boot:run
```

O backend inicia, por padrão, em:

```text
http://localhost:8080
```

A API utiliza o prefixo `/api`. Por exemplo, o endpoint de login é:

```text
POST http://localhost:8080/api/auth/login
```

Mantenha o terminal do backend aberto enquanto utilizar o frontend.

---

## 8. Como executar o frontend

Abra outro terminal e entre na pasta `frontend`:

```powershell
cd frontend
npm ci
npm run dev
```

No Linux ou macOS, os mesmos comandos podem ser executados em um terminal shell.

A aplicação ficará disponível em:

```text
http://localhost:5173
```

Durante o desenvolvimento, o Vite encaminha as requisições iniciadas por `/api` para `http://localhost:8080`. Esse endereço é o padrão configurado em `vite.config.ts`.

Para apontar o proxy para outro endereço de backend durante o desenvolvimento, defina a variável no mesmo terminal antes de iniciar o Vite.

No PowerShell:

```powershell
$env:VITE_BACKEND_URL = "http://localhost:8081"
npm run dev
```

No Linux ou macOS:

```bash
export VITE_BACKEND_URL="http://localhost:8081"
npm run dev
```

A configuração atual de `vite.config.ts` lê `VITE_BACKEND_URL` de `process.env`; por isso, defina-a no ambiente do terminal. Já `VITE_API_URL` define um prefixo para as chamadas feitas pelo cliente HTTP do frontend. No cenário local com proxy, deixe `VITE_API_URL` vazia. Se o frontend e a API forem publicados em origens diferentes, configure `VITE_API_URL` no ambiente do Vite e ajuste `app.cors.origens` no `application.properties` do backend para permitir a origem do frontend.

---

## 9. Testes e comandos úteis

### Backend

Na raiz do projeto:

```powershell
.\mvnw.cmd test
```

Ou, no Linux/macOS:

```bash
./mvnw test
```

### Frontend

Dentro da pasta `frontend`:

```bash
npm test
```

Para verificar os tipos e gerar a versão de produção:

```bash
npm run build
```

Para visualizar localmente a versão gerada:

```bash
npm run preview
```

### Testes manuais da API no Windows

O repositório também contém scripts PowerShell de teste de integração. Eles devem ser executados com o backend ligado e apontando para um banco de desenvolvimento/teste, pois podem criar usuários, pacientes e outros registros.

```powershell
powershell -ExecutionPolicy Bypass -File .\testar-admin.ps1
powershell -ExecutionPolicy Bypass -File .\testar-recepcao.ps1
powershell -ExecutionPolicy Bypass -File .\testar-clinica.ps1
powershell -ExecutionPolicy Bypass -File .\testar-paciente.ps1
powershell -ExecutionPolicy Bypass -File .\testar-auditoria.ps1
```

Os scripts exercitam fluxos da administração, recepção, área clínica, portal do paciente e auditoria. Eles são testes manuais de integração e não substituem testes automatizados abrangentes.

---

## 10. Configuração e segurança: observações importantes

- Não utilize dados reais de pacientes em ambiente de desenvolvimento ou demonstração.
- Não publique o backend com credenciais padrão, chave JWT de exemplo ou senha administrativa conhecida.
- Não envie arquivos como `local.properties`, `.env` ou `.env.local` com credenciais para o Git ou para terceiros.
- A autorização deve ser aplicada no backend; ocultar uma opção no frontend não é uma barreira de segurança suficiente.
- A auditoria atual é acionada por anotações nos endpoints selecionados. Revise a cobertura para garantir o registro de todos os acessos e operações que o projeto exigir.
- Valide as regras de transição dos atendimentos e a obrigatoriedade das etapas clínicas antes de considerar o fluxo pronto para uso.
- Faça backup do banco antes de aplicar alterações em ambientes que contenham dados que precisem ser preservados.

---

## 11. Licença e contexto

Projeto acadêmico desenvolvido no contexto da **Universidade Federal de Pelotas (UFPel)**. Este repositório não especifica uma licença de software; portanto, não presuma permissões de redistribuição ou uso comercial sem a definição correspondente pelos responsáveis pelo projeto.
