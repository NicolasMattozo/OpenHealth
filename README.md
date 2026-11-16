# 🏥 OpenHealth — Prontuário Unificado (Rede Integrada de Saúde)

Plataforma web de **Prontuário Eletrônico Unificado e Federado** para redes integradas de saúde, projetada com foco em **interoperabilidade entre instituições**, **controle de consentimento pelo paciente (LGPD)**, **fluxo assistencial orientado ao Protocolo de Manchester** e **auditoria imutável em nível de banco de dados**.

---

## 📌 Visão Geral

O **OpenHealth** centraliza o histórico clínico do cidadão em um prontuário único, permitindo que diferentes unidades de saúde (Hospitais Gerais, UPAs, UBS e Clínicas de Diagnóstico) prestem cuidado contínuo mediante autorização explícita e auditável do titular dos dados.

### Principais Pilares
- **Privacidade e Consentimento (LGPD):** O atendimento e o acesso ao histórico clínico por uma instituição dependem de solicitação formal (`PENDENTE -> APROVADA | NEGADA -> REVOGADA`) controlada pelo próprio paciente.
- **Fluxo Assistencial Estrito:** Máquina de estados validada da recepção até a alta (`AGUARDANDO_TRIAGEM -> AGUARDANDO_MEDICO -> EM_CONSULTA -> FINALIZADO`), exigindo classificação de risco antes da consulta médica.
- **Prontuário Clínico Especializado:** Registros transacionais tipados (`TRIAGEM`, `DADO_CLINICO`, `CONDICAO_DIAGNOSTICO`, `PRESCRICAO_MEDICAMENTO` e `REQUISICAO_EXAME`) com versionamento imutável de laudos de exames (`versao 1..N`).
- **Rastreabilidade Total:** Auditoria automática de operações (`INSERT`, `UPDATE`, `DELETE`) vinculada ao contexto do usuário autenticado diretamente no banco de dados.

---

## 🛠️ Stack Tecnológica

### Backend
- **Linguagem:** Java 21 LTS
- **Framework:** Spring Boot 3.x (`Spring Web`, `Spring Data JPA` / `Spring JDBC`, `Spring Security`, `Bean Validation`)
- **Autenticação & Autorização:** JWT (Stateless) + Controle de Acesso Contextual por Instituição (RBAC Multi-tenant)
- **Migrações & Persistência:** Flyway + PostgreSQL Driver

### Banco de Dados
- **SGBD:** PostgreSQL 16+
- **Recursos Avançados:**
  - Tipos `ENUM` nativos (`papel_usuario`, `tipo_profissional`, `status_solicitacao`, `status_atendimento`, `tipo_registro_clinico`)
  - Constraint Triggers `DEFERRABLE INITIALLY DEFERRED` para especialização 1:1 estrita de registros clínicos
  - Triggers PL/pgSQL para máquina de estados, bloqueio de mutação de histórico e auditoria via `current_setting('app.id_usuario', true)`
  - Views dedicadas para visibilidade do paciente (`historico_clinico`, `registros_visiveis_ao_paciente`, `resultados_visiveis_ao_paciente`)

### Frontend
- **Biblioteca Base:** React 18+ com TypeScript (Vite)
- **Estilização & UI:** Tailwind CSS + Design System próprio orientado a componentes reutilizáveis
- **Roteamento & Estado Assíncrono:** React Router v6 + TanStack Query (React Query)

---

## 👥 Perfis de Acesso (RBAC)

| Perfil | Escopo | Principais Responsabilidades |
| :--- | :--- | :--- |
| **Recepcionista** | Institucional | Cadastro de pacientes, abertura de prontuário 1:1, solicitação de acesso (LGPD) e abertura/acompanhamento da fila de atendimentos. |
| **Enfermeiro** | Assistencial Inicial | Aferição de sinais vitais (PA, FC, Temp, SpO₂, Peso, Altura), classificação de risco pelo **Protocolo de Manchester**, consulta de histórico/alergias e leitura de prescrições. |
| **Médico** | Clínico Completo | Condução da consulta (`EM_CONSULTA`), anamnese/evolução clínica, registro de diagnósticos (`CID`), emissão de prescrições, requisição/laudo de exames e finalização de atendimento. |
| **Paciente** | Portal do Titular | Visualização do próprio histórico clínico, receitas ativas, resultados de exames e gestão de consentimento (`Autorizar`, `Recusar` ou `Revogar` acesso de instituições). |
| **Administrador da Instituição** | Gerencial Local | Gestão do quadro de profissionais da unidade (`Médicos`, `Enfermeiros`, `Recepcionistas`), vínculos institucionais, departamentos e matriz de permissões. |
| **Administrador Global** | Governança da Rede | Credenciamento de instituições de saúde (CNPJ), designação de administradores locais e monitoramento da rede federada. |

---

## 🧩 Arquitetura e Componentização

O projeto adota uma arquitetura de **Monólito Modular (Package-by-Feature)** no Spring Boot combinada com **Design System Component-Driven** no React:

```text
OpenHealth/
├── backend/                          # API RESTful em Spring Boot 3 (Java 21)
│   └── src/main/java/.../openhealth/
│       ├── shared/                   # Security JWT, AuditUserContextInterceptor, GlobalExceptionHandler
│       ├── auth/                     # M01: Login multi-perfil, sessão e troca de contexto
│       ├── governance/               # M02: Instituições, usuários, vínculos, papéis e conselhos (CRM/COREN)
│       ├── patient/                  # M03: Pacientes, prontuário 1:1 e solicitações de acesso (LGPD)
│       ├── attendance/               # M04: Recepção, fila em tempo real, KPIs e máquina de estados
│       ├── clinical/                 # M05-M07: Triagem Manchester, dados clínicos, CID, prescrições e exames
│       └── audit/                    # M08: Consulta de logs de auditoria e views do portal do paciente
│
└── frontend/                         # SPA em React 18 + TypeScript + Tailwind CSS
    └── src/
        ├── components/
        │   ├── layout/               # <AppShell />, <Sidebar />, <SidebarContextCard />, <TopHeader />
        │   ├── ui/                   # <KpiStatCard />, <DataTable />, <FilterPills />, <RiskBadge />, <BaseModal />
        │   └── domain/               # <AttendanceQueueView />, <TriageSplitWorkspace />, <PatientClinicalBanner />,
        │                             # <ClinicalTimeline />, <ClinicalDataGrid />, <PrescriptionWorkspace />, <ExamWorkspace />
        ├── pages/                    # Visões compostas por perfil (Recepção, Enfermagem, Médico, Paciente, Admins)
        ├── services/                 # Clientes HTTP e hooks do TanStack Query
        └── context/                  # Contexto de autenticação e escopo institucional ativo
```

---

## 🗺️ Roadmap de Sprints

- [ ] **Sprint 1 — Fundação, Auth & UI Shell (`🎯 Meta 40%`)**
  - Setup Spring Boot 3 + PostgreSQL 16 + Interceptor `SET LOCAL app.id_usuario` + Seeds
  - Autenticação JWT com seleção dos 6 perfis
  - `<AppShell />` dinâmico por perfil e biblioteca de componentes base (`<KpiStatCard />`, `<DataTable />`, `<RiskBadge />`, `<BaseModal />`)
- [ ] **Sprint 2 — Recepção, Consentimento LGPD & Triagem (`🎯 Meta 40%`)**
  - Gestão de Pacientes e criação automática de Prontuário 1:1
  - Fluxo de Solicitação e Aprovação/Revogação de Acesso (`solicitacao_acesso`)
  - Painel da Recepção, Fila de Atendimentos e Abertura de Atendimento (`AGUARDANDO_TRIAGEM`)
  - Fila de Triagem da Enfermagem, Sinais Vitais e Classificação de Manchester (`AGUARDANDO_MEDICO`)
- [ ] **Sprint 3 — Prontuário Unificado & Consulta Médica (`🚀 Escopo 100%`)**
  - Banner clínico do paciente, Linha do Tempo multi-institucional e Dados Clínicos (Alergias, Comorbidades, Uso Contínuo)
  - Assunção de consulta (`EM_CONSULTA`), Anamnese/Evolução e Diagnóstico com código CID
- [ ] **Sprint 4 — Prescrições, Exames & Desfecho (`🚀 Escopo 100%`)**
  - Emissão e leitura de Prescrições Médicas
  - Requisição de Exames e Laudos Versionados Imutáveis (`resultado_exame`)
  - Finalização do Atendimento (`FINALIZADO`)
- [ ] **Sprint 5 — Portal do Paciente, Governança & Auditoria (`🚀 Escopo 100%`)**
  - Portal do Paciente integrado às views de visibilidade
  - Painéis de Administrador da Instituição, Administrador Global e Logs de Auditoria (`log_auditoria`)

---

## 🚀 Como Executar o Projeto (Em Desenvolvimento)

### Pré-requisitos
- **Java 21+** e **Maven 3.9+**
- **Node.js 20+** e **npm**
- **PostgreSQL 16+** (local ou via Docker)

### 1. Banco de Dados (PostgreSQL 16)
```bash
# Crie o banco e aplique o schema base (localmente ignorado pelo git)
createdb prontuario
psql -v ON_ERROR_STOP=1 -d prontuario -f prontuario.sql
```

### 2. Backend (Spring Boot)
```bash
cd backend
./mvnw spring-boot:run
```

### 3. Frontend (React + Vite)
```bash
cd frontend
npm install
npm run dev
```

---

## 📄 Licença

Projeto acadêmico e de engenharia de software — **UFPel**.
