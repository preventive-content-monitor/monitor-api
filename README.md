# Guardian Backend

Sistema de controle parental que monitora e classifica a navegação de crianças e adolescentes. Este backend fornece APIs REST para:

- Autenticação de responsáveis (pais/tutores)
- Gerenciamento de dependentes (crianças monitoradas)
- Vinculação de dispositivos
- Ingestão de eventos de navegação
- Classificação automática de conteúdo
- Aplicação de políticas de bloqueio
- Métricas e painel de controle

---

## Stack Tecnológica

| Tecnologia | Versão |
|------------|--------|
| Kotlin | 1.9.24 |
| Spring Boot | 3.2.5 |
| Spring Security + OAuth2 Resource Server | JWT (Bearer Token) |
| Spring Data JPA | Hibernate 6 |
| Banco de Dados | MySQL (prod) |
| Documentação | OpenAPI 3 / Swagger UI |
| Build | Maven |

---

## Arquitetura Hexagonal (Ports & Adapters)

O projeto segue a **Arquitetura Hexagonal**, também conhecida como **Ports and Adapters**, que isola a lógica de negócio das tecnologias externas (banco de dados, HTTP, segurança, etc.).

### Diagrama Geral

```
                    ┌──────────────────────────────────────────────┐
                    │              INFRAESTRUTURA                   │
                    │  (Configurações Spring, Filtros, Segurança)   │
                    └──────────────┬───────────────────────────────┘
                                   │
        ┌──────────────────────────┼──────────────────────────┐
        │                          │                          │
  ┌─────▼──────┐           ┌──────▼───────┐          ┌───────▼──────┐
  │ ADAPTADORES │           │  APLICAÇÃO   │          │  ADAPTADORES │
  │  ENTRADA    │──────────▶│  (Serviços)  │◀─────────│    SAÍDA     │
  │  (REST API) │           │              │          │ (Repos, JWT) │
  └─────────────┘           └──────┬───────┘          └──────────────┘
                                   │
                            ┌──────▼───────┐
                            │   DOMÍNIO    │
                            │  (Modelos,   │
                            │   Portas,    │
                            │   Exceções)  │
                            └──────────────┘
```

### Estrutura de Pacotes

```
br.com.guardian.backend
│
├── dominio/                          # 🔵 NÚCLEO - Regras de negócio puras
│   ├── modelo/                       #    Entidades JPA e enums
│   │   ├── UsuarioGuardian.kt
│   │   ├── PapelUsuario.kt
│   │   ├── Dependente.kt
│   │   ├── Dispositivo.kt
│   │   ├── Evento.kt
│   │   ├── TipoEvento.kt
│   │   ├── Politica.kt
│   │   ├── ModoPolitica.kt
│   │   ├── ResultadoClassificacao.kt
│   │   ├── VulnerabilidadeDiaria.kt
│   │   └── JwtClaims.kt
│   ├── excecao/                      #    Exceções de domínio
│   │   └── Excecoes.kt
│   ├── porta/                        #    Portas de saída (interfaces)
│   │   ├── GeradorToken.kt
│   │   └── SerializadorJson.kt
│   └── servico/                      #    Serviços de domínio puros
│       ├── CalculadorIdade.kt
│       └── EstrategiaCalculoPolitica.kt
│
├── aplicacao/                        # 🟢 CASOS DE USO - Orquestração
│   ├── porta/
│   │   └── entrada/                  #    Interfaces dos casos de uso
│   │       ├── ServicoAutenticacao.kt
│   │       ├── ValidadorUsuario.kt
│   │       └── ServicoPoliticaInterface.kt
│   └── servico/                      #    Implementações dos casos de uso
│       ├── ServicoAutenticacaoImpl.kt
│       ├── ValidadorUsuarioImpl.kt
│       ├── ServicoPoliticaImpl.kt
│       ├── ServicoClassificacao.kt
│       ├── ServicoIngestaoEvento.kt
│       ├── ServicoMetricas.kt
│       ├── ServicoVinculacaoDispositivo.kt
│       ├── ServicoVulnerabilidade.kt
│       └── EstrategiaCalculoPoliticaPorIdade.kt
│
├── adaptadores/                      # 🟡 ADAPTADORES - Tecnologias externas
│   ├── entrada/
│   │   ├── rest/                     #    Controllers REST (driving adapters)
│   │   │   ├── AutenticacaoController.kt
│   │   │   ├── DependenteController.kt
│   │   │   ├── DispositivoController.kt
│   │   │   ├── IngestaoEventoController.kt
│   │   │   ├── PainelController.kt
│   │   │   └── PoliticaController.kt
│   │   └── dto/                      #    Objetos de transferência
│   │       ├── RequisicaoRegistro.kt
│   │       ├── RequisicaoLogin.kt
│   │       ├── RespostaAutenticacao.kt
│   │       ├── RequisicaoCriarDependente.kt
│   │       ├── RequisicaoVinculacao.kt
│   │       ├── RequisicaoIngestaoLoteEvento.kt
│   │       ├── ItemIngestaoEventoDto.kt
│   │       ├── RequisicaoAtualizarPolitica.kt
│   │       ├── RespostaDispositivo.kt
│   │       └── RespostaErro.kt
│   └── saida/
│       ├── persistencia/             #    Repositórios JPA (driven adapters)
│       │   ├── UsuarioRepositorio.kt
│       │   ├── DependenteRepositorio.kt
│       │   ├── DispositivoRepositorio.kt
│       │   ├── EventoRepositorio.kt
│       │   ├── PoliticaRepositorio.kt
│       │   ├── ClassificacaoRepositorio.kt
│       │   └── VulnerabilidadeRepositorio.kt
│       ├── classificacao/            #    Cliente de classificação
│       │   └── ClienteClassificador.kt
│       ├── json/                     #    Serialização JSON
│       │   └── JacksonSerializadorJson.kt
│       └── seguranca/                #    Geração de token JWT
│           └── JwtGeradorToken.kt
│
└── infraestrutura/                   # 🔴 INFRAESTRUTURA - Config do framework
    ├── configuracao/
    │   ├── ConfiguracaoSeguranca.kt
    │   ├── ConfiguracaoCors.kt
    │   ├── ConfiguracaoJackson.kt
    │   └── ConfiguracaoOpenApi.kt
    ├── excecao/
    │   └── TratadorGlobalExcecao.kt
    └── filtro/
        └── FiltroLogRequisicao.kt
```

### Fluxo de uma Requisição

```
HTTP Request
    │
    ▼
[FiltroLogRequisicao]       ← infraestrutura/filtro
    │
    ▼
[ConfiguracaoSeguranca]     ← infraestrutura/configuracao (validação JWT)
    │
    ▼
[Controller REST]           ← adaptadores/entrada/rest (converte DTO → domínio)
    │
    ▼
[Serviço de Aplicação]      ← aplicacao/servico (orquestra lógica)
    │
    ├──▶ [Repositório JPA]  ← adaptadores/saida/persistencia
    ├──▶ [GeradorToken]     ← adaptadores/saida/seguranca
    ├──▶ [Classificador]    ← adaptadores/saida/classificacao
    └──▶ [SerializadorJson] ← adaptadores/saida/json
    │
    ▼
[Modelo de Domínio]         ← dominio/modelo (entidades, enums)
```

### Princípios da Arquitetura

| Princípio | Como é aplicado |
|-----------|----------------|
| **Inversão de Dependência** | O domínio define interfaces (portas) — `GeradorToken`, `SerializadorJson`, `ServicoPolitica` — que os adaptadores implementam |
| **Isolamento do Domínio** | `dominio/` não importa nada de Spring; anotações JPA são a única exceção (pragmatismo) |
| **Adaptadores de Entrada** | Controllers REST recebem DTOs e delegam para serviços da aplicação |
| **Adaptadores de Saída** | Repositórios JPA, cliente de classificação e gerador JWT implementam as portas |
| **Infraestrutura** | Configurações do Spring (segurança, CORS, Jackson, OpenAPI) ficam isoladas |

---

---

## Deploy em Produção (AWS)

O backend é executado em uma EC2 `t3.micro` com Amazon Linux 2023, provisionada via Terraform.

### Infraestrutura

| Componente | Detalhes |
|------------|---------|
| **EC2** | t3.micro, Amazon Linux 2023, `LabInstanceProfile` |
| **nginx** | Porta 80 — serve o dashboard e faz proxy `/api/` → `:8080` |
| **Spring Boot** | Porta 8080 (interno), gerenciado pelo systemd (`guardian.service`) |
| **RDS** | MySQL 8.0, subnet privada, acesso apenas via VPC |
| **JAR** | Armazenado em `s3://guardian-logs-<account>/app/backend.jar` |

### URL de Produção

```
http://<EC2-IP>/api          ← Elastic IP atribuído pelo Terraform
```

Obtenha com: `terraform output backend_api_url`

### Variáveis de Ambiente (systemd)

| Variável | Fonte |
|----------|-------|
| `DB_URL` | JDBC URL do RDS MySQL (injetada pelo `ec2_userdata.sh`) |
| `DB_USERNAME` | Usuário master do RDS |
| `DB_PASSWORD` | Senha do RDS |
| `JWT` | Segredo para assinar tokens JWT |

### Build e Deploy

```powershell
# 1. Build do JAR
cd monitor-api
./mvnw clean package -DskipTests

# 2. Upload para S3
aws s3 cp target/backend-0.1.0.jar s3://guardian-logs-750476866422/app/backend.jar

# 3. Aplicar infraestrutura (EC2 baixa o JAR automaticamente via userdata)
cd ../terraform
terraform apply
```

### Integração com Lambda Classificadora

O backend **não classifica eventos diretamente**. A classificação de URLs é feita pela Lambda `guardian-classificador` (Python 3.12 + OpenAI gpt-4o-mini), que:
1. Consome eventos do SQS (`guardian-eventos`)
2. Classifica via OpenAI com cache 24h e pré-filtro local
3. Persiste o resultado diretamente no RDS MySQL via PyMySQL

O `ClienteClassificador.kt` no Spring Boot existe para casos de classificação síncrona, mas o fluxo principal é assíncrono via Lambda.

---

## Base URL

**Produção:** `http://<EC2-IP>/api` (ver `terraform output backend_api_url`)

**Desenvolvimento local:** `http://localhost:8080`

---

## Autenticação

O sistema usa **JWT Bearer Tokens**. Após login, inclua o token em todas as requisições protegidas:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Endpoints Públicos (sem autenticação)
- `POST /api/autenticacao/registrar`
- `POST /api/autenticacao/entrar`
- `POST /api/eventos/lote`
- `POST /api/dispositivos/vincular`
- `GET  /api/politica/atual`

### Endpoints Protegidos (requer JWT)
- `GET  /api/dependentes`
- `POST /api/dependentes`
- `GET  /api/dependentes/{id}`
- `GET  /api/dispositivos`
- `POST /api/dispositivos/gerar-codigo/{dependenteId}`
- `PUT  /api/politica`
- `GET  /api/painel/resumo`
- `GET  /api/painel/top-dominios`
- `GET  /api/painel/vulnerabilidade`

---

## Modelos de Dados

### UsuarioGuardian (Responsável)
```json
{
  "id": "uuid",
  "email": "string",
  "papel": "RESPONSAVEL",
  "criadoEm": "2026-02-22T10:00:00Z"
}
```

### Dependente (Criança/Adolescente)
```json
{
  "id": "uuid",
  "usuarioGuardian": { "id": "uuid", "email": "string" },
  "apelido": "string",
  "anoNascimento": 2015,
  "dataNascimento": "2015-09-21",
  "sexo": "MASCULINO | FEMININO",
  "criadoEm": "2026-02-22T10:00:00Z"
}
```

### Dispositivo
```json
{
  "id": "uuid",
  "dependente": { "id": "uuid", "apelido": "string" },
  "nomeDispositivo": "Chrome - Windows",
  "vinculadoEm": "2026-02-22T10:00:00Z",
  "ultimoAcessoEm": "2026-02-22T15:30:00Z"
}
```

### Politica (Controle Parental)
```json
{
  "id": "uuid",
  "dependente": { "id": "uuid" },
  "modo": "BLOCK | WARN | EDUCATE",
  "limiteRisco": 30,
  "dominiosBloqueados": "[\"site1.com\", \"site2.com\"]",
  "dominiosPermitidos": "[]",
  "modoEscolaAtivo": false,
  "escolaInicio": "07:00",
  "escolaFim": "17:00",
  "criadoEm": "2026-02-22T10:00:00Z"
}
```

### Evento (Navegação)
```json
{
  "id": "uuid",
  "dispositivo": { "id": "uuid" },
  "tipo": "NAVIGATION | BLOCK_ATTEMPT | PERMISSION_REQUEST",
  "urlHost": "youtube.com",
  "urlPathHash": "sha256hash",
  "titulo": "Título da página",
  "ocorridoEm": "2026-02-22T15:30:00Z",
  "metadados": "{ \"key\": \"value\" }"
}
```

### Enums

**TipoEvento:**
| Valor | Descrição |
|-------|-----------|
| `NAVIGATION` | Navegação normal para uma URL |
| `BLOCK_ATTEMPT` | Tentativa de acessar conteúdo bloqueado |
| `PERMISSION_REQUEST` | Solicitação de permissão (câmera, mic, etc.) |

**ModoPolitica:**
| Valor | Descrição | Comportamento |
|-------|-----------|---------------|
| `BLOCK` | Bloqueio total | Bloqueia acesso a conteúdo acima do limite de risco |
| `WARN` | Aviso | Exibe aviso mas permite continuar |
| `EDUCATE` | Educativo | Apenas registra para análise posterior |

---

## Endpoints

### 1. Autenticação

#### POST /api/autenticacao/registrar
Cadastra um novo responsável.

**Request:**
```json
{
  "email": "pai@familia.com",
  "senha": "senhaSegura123"
}
```

**Response:** `201 Created`

| Status | Descrição |
|--------|-----------|
| 201 | Usuário criado com sucesso |
| 409 | Email já cadastrado |
| 400 | Dados inválidos |

---

#### POST /api/autenticacao/entrar
Autentica e retorna token JWT (válido por 24 horas).

**Request:**
```json
{
  "email": "pai@familia.com",
  "senha": "senhaSegura123"
}
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs..."
}
```

| Status | Descrição |
|--------|-----------|
| 200 | Login realizado com sucesso |
| 401 | Credenciais inválidas |

---

### 2. Dependentes

#### POST /api/dependentes
Cria um novo dependente. **Requer autenticação.**

**Request:**
```json
{
  "apelido": "João",
  "anoNascimento": 2015
}
```

**Response:** `201 Created`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "usuarioGuardian": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "email": "pai@familia.com"
  },
  "apelido": "João",
  "anoNascimento": 2015,
  "criadoEm": "2026-02-22T10:00:00Z"
}
```

---

#### GET /api/dependentes
Lista todos os dependentes do responsável autenticado.

**Response:** `200 OK`
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "apelido": "João",
    "anoNascimento": 2015,
    "criadoEm": "2026-02-22T10:00:00Z"
  }
]
```

---

#### GET /api/dependentes/{id}
Retorna um dependente específico do responsável autenticado.

---

### 3. Dispositivos

#### POST /api/dispositivos/gerar-codigo/{dependenteId}
Gera código de vinculação (válido por 5 minutos). **Requer autenticação.**

**Response:** `200 OK`
```json
{
  "codigo": "a1b2c3"
}
```

---

#### POST /api/dispositivos/vincular
Vincula dispositivo usando o código. **Endpoint público** (usado pela extensão).

**Request:**
```json
{
  "codigo": "a1b2c3",
  "nomeDispositivo": "Chrome - Windows 11"
}
```

**Response:** `201 Created`
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "dependente": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "apelido": "João"
  },
  "nomeDispositivo": "Chrome - Windows 11",
  "vinculadoEm": "2026-02-22T10:05:00Z",
  "ultimoAcessoEm": null
}
```

| Status | Descrição |
|--------|-----------|
| 201 | Dispositivo vinculado com sucesso |
| 400 | Código inválido ou expirado |
| 404 | Dependente não encontrado |

---

#### GET /api/dispositivos
Lista dispositivos do responsável autenticado.

**Response:** `200 OK`
```json
[
  {
    "id": "660e8400-...",
    "nome": "Chrome - Windows 11",
    "plataforma": "Windows",
    "dependenteId": "550e8400-...",
    "apelidoDependente": "João",
    "vinculadoEm": "2026-02-22T10:05:00Z",
    "ultimoAcessoEm": "2026-02-22T15:30:00Z"
  }
]
```

---

### 4. Eventos (Ingestão)

#### POST /api/eventos/lote
**ENDPOINT PRINCIPAL PARA A EXTENSÃO.** Envia lote de eventos de navegação. **Não requer autenticação** (apenas `dispositivoId` válido).

**Request:**
```json
{
  "dispositivoId": "660e8400-e29b-41d4-a716-446655440001",
  "eventos": [
    {
      "tipo": "NAVIGATION",
      "url": "https://youtube.com/watch?v=abc123",
      "titulo": "Vídeo do YouTube",
      "ocorridoEm": "2026-02-22T15:30:00Z",
      "metadados": {
        "referrer": "https://google.com",
        "timeOnPage": 120
      }
    },
    {
      "tipo": "NAVIGATION",
      "url": "https://instagram.com/explore",
      "titulo": "Instagram Explorar",
      "ocorridoEm": "2026-02-22T15:32:00Z",
      "metadados": null
    }
  ]
}
```

**Response:** `200 OK`
```json
{
  "status": "ok",
  "ingeridos": 2
}
```

**Comportamento interno:**
1. Valida se o `dispositivoId` existe
2. Para cada evento:
   - Extrai host e hash do path da URL
   - Salva evento no banco
   - Classifica conteúdo (pontuação de risco 0–100)
   - Verifica política de bloqueio
3. Atualiza `ultimoAcessoEm` do dispositivo
4. Recalcula índice de vulnerabilidade diário
5. Retorna contagem de eventos processados

| Status | Descrição |
|--------|-----------|
| 200 | Eventos processados com sucesso |
| 404 | Dispositivo não encontrado |
| 400 | Dados inválidos |

---

### 5. Políticas

#### GET /api/politica/atual
Obtém política ativa para um dispositivo. **Endpoint público.**

**Query Parameters:**
| Param | Tipo | Descrição |
|-------|------|-----------|
| dispositivoId | UUID | ID do dispositivo |

**Request:**
```http
GET /api/politica/atual?dispositivoId=660e8400-e29b-41d4-a716-446655440001
```

**Response:** `200 OK`
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440002",
  "modo": "BLOCK",
  "limiteRisco": 30,
  "dominiosBloqueados": "[]",
  "dominiosPermitidos": "[]",
  "modoEscolaAtivo": false,
  "escolaInicio": null,
  "escolaFim": null,
  "criadoEm": "2026-02-22T10:00:00Z"
}
```

**Criação automática de política:** Se não existir política, uma é criada automaticamente baseada na idade:

| Idade | Modo | Limite de Risco |
|-------|------|-----------------|
| ≤ 10 anos | BLOCK | 30 |
| 11–13 anos | WARN | 50 |
| > 13 anos | EDUCATE | 70 |

---

#### PUT /api/politica
Atualiza a política de controle parental. **Requer autenticação.**

**Query Parameters:**
| Param | Tipo | Descrição |
|-------|------|-----------|
| dispositivoId | UUID | ID do dispositivo |

**Request:**
```json
{
  "modo": "WARN",
  "limiteRisco": 50,
  "dominiosBloqueados": ["facebook.com", "tiktok.com"],
  "dominiosPermitidos": ["khanacademy.org"],
  "modoEscolaAtivo": true,
  "escolaInicio": "07:00",
  "escolaFim": "17:00"
}
```

| Campo | Tipo | Descrição |
|-------|------|-----------|
| modo | string | `BLOCK`, `WARN` ou `EDUCATE` |
| limiteRisco | int | Limite de risco (0–100) |
| dominiosBloqueados | string[] | Domínios bloqueados manualmente |
| dominiosPermitidos | string[] | Domínios sempre permitidos (whitelist) |
| modoEscolaAtivo | boolean | Ativa restrições em horário escolar |
| escolaInicio | string? | Horário de início (HH:mm) |
| escolaFim | string? | Horário de fim (HH:mm) |

**Response:** `200 OK` — retorna a política atualizada.

| Status | Descrição |
|--------|-----------|
| 200 | Política atualizada |
| 400 | Dados inválidos |
| 401 | Token ausente ou inválido |
| 404 | Dispositivo ou política não encontrada |

---

### 6. Painel (Dashboard)

#### GET /api/painel/resumo
Resumo de métricas do dispositivo. **Requer autenticação.**

**Query Parameters:**
| Param | Tipo | Exemplo |
|-------|------|---------|
| dispositivoId | UUID | `660e8400-...` |
| from | ISO 8601 | `2026-02-01T00:00:00Z` |
| to | ISO 8601 | `2026-02-22T23:59:59Z` |

**Response:** `200 OK`
```json
{
  "totalEventos": 1250,
  "eventosSensiveis": 23,
  "tentativasBloqueio": 5
}
```

---

#### GET /api/painel/top-dominios
Top domínios mais acessados. **Requer autenticação.**

**Query Parameters:** Mesmos do `/resumo`

**Response:** `200 OK`
```json
[
  ["youtube.com", 450],
  ["instagram.com", 230],
  ["tiktok.com", 180]
]
```

---

#### GET /api/painel/vulnerabilidade
Histórico de índice de vulnerabilidade. **Requer autenticação.**

**Query Parameters:**
| Param | Tipo | Exemplo |
|-------|------|---------|
| dependenteId | UUID | `550e8400-...` |
| from | YYYY-MM-DD | `2026-02-01` |
| to | YYYY-MM-DD | `2026-02-22` |

**Response:** `200 OK`
```json
[
  {
    "id": "uuid",
    "dia": "2026-02-20",
    "pontuacao": 35,
    "caracteristicas": "sensiveis=5,bloqueio=2,noturno=10"
  },
  {
    "id": "uuid",
    "dia": "2026-02-21",
    "pontuacao": 28,
    "caracteristicas": "sensiveis=3,bloqueio=1,noturno=8"
  }
]
```

O score de vulnerabilidade (0–100) é calculado com base em:
- **50%** — acessos a conteúdo sensível (risco ≥ 70)
- **30%** — tentativas de bloqueio
- **20%** — uso noturno (22h–6h)

---

## Formato de Erros

Todas as respostas de erro seguem o padrão:

```json
{
  "timestamp": "2026-02-22T15:30:00Z",
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Dispositivo não encontrado",
  "caminho": "/api/eventos/lote",
  "detalhes": null
}
```

Para erros de validação, `detalhes` contém a lista de campos:
```json
{
  "timestamp": "2026-02-22T15:30:00Z",
  "status": 400,
  "erro": "Validation Error",
  "mensagem": "Erro de validação nos dados enviados",
  "caminho": "/api/autenticacao/registrar",
  "detalhes": [
    "email: deve ser um email válido",
    "senha: tamanho mínimo de 6 caracteres"
  ]
}
```

---

## Guia de Integração — Extensão de Navegador

### Fluxo de Vinculação

```
┌──────────────┐    ┌───────────────┐    ┌─────────────┐
│  App/Web     │    │    Backend    │    │  Extensão   │
│  Responsável │    │               │    │  Browser    │
└──────┬───────┘    └───────┬───────┘    └──────┬──────┘
       │                    │                   │
       │ POST /api/         │                   │
       │ dispositivos/      │                   │
       │ gerar-codigo/{id}  │                   │
       │───────────────────▶│                   │
       │                    │                   │
       │  { codigo: "a1b2c3" }                  │
       │◀───────────────────│                   │
       │                    │                   │
       │   Mostra código    │                   │
       │   para digitar     │                   │
       │   na extensão      │                   │
       │                    │                   │
       │                    │ POST /api/dispositivos/vincular
       │                    │ { codigo, nomeDispositivo }
       │                    │◀──────────────────│
       │                    │                   │
       │                    │ Dispositivo       │
       │                    │ vinculado         │
       │                    │──────────────────▶│
       │                    │                   │
       │                    │  Salva deviceId   │
       │                    │  localmente       │
       └────────────────────┴───────────────────┘
```

### Fluxo de Monitoramento

```javascript
// background.js da extensão

const DEVICE_ID = localStorage.getItem('guardian_device_id');
const API_URL = 'http://localhost:8080';

let eventBuffer = [];

// Listener de navegação
chrome.webNavigation.onCompleted.addListener((details) => {
  if (details.frameId !== 0) return;
  
  eventBuffer.push({
    tipo: 'NAVIGATION',
    url: details.url,
    titulo: '',
    ocorridoEm: new Date().toISOString(),
    metadados: {
      tabId: details.tabId,
      transitionType: details.transitionType
    }
  });
});

// Envio periódico (a cada 30 segundos)
setInterval(async () => {
  if (eventBuffer.length === 0) return;
  
  const eventsToSend = [...eventBuffer];
  eventBuffer = [];
  
  try {
    const response = await fetch(`${API_URL}/api/eventos/lote`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        dispositivoId: DEVICE_ID,
        eventos: eventsToSend
      })
    });
    
    if (!response.ok) {
      eventBuffer = [...eventsToSend, ...eventBuffer];
    }
  } catch (error) {
    eventBuffer = [...eventsToSend, ...eventBuffer];
  }
}, 30000);
```

### Verificação de Política

```javascript
async function verificarBloqueio(url) {
  const response = await fetch(
    `${API_URL}/api/politica/atual?dispositivoId=${DEVICE_ID}`
  );
  
  if (!response.ok) return { bloqueado: false };
  
  const politica = await response.json();
  const dominiosBloqueados = JSON.parse(politica.dominiosBloqueados);
  const host = new URL(url).hostname;
  
  if (dominiosBloqueados.includes(host)) {
    return { bloqueado: true, motivo: 'dominio_bloqueado' };
  }
  
  return { bloqueado: false };
}
```

### Registro de Tentativa de Bloqueio

```javascript
async function reportarBloqueio(url, motivo) {
  await fetch(`${API_URL}/api/eventos/lote`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      dispositivoId: DEVICE_ID,
      eventos: [{
        tipo: 'BLOCK_ATTEMPT',
        url: url,
        titulo: 'Tentativa de acesso bloqueado',
        ocorridoEm: new Date().toISOString(),
        metadados: { motivo: motivo }
      }]
    })
  });
}
```

---

## Swagger UI

Documentação interativa disponível em:
```
http://localhost:8080/swagger-ui/index.html
```

---

## Como Executar

### Pré-requisitos
- Java 17+
- MySQL rodando na porta 3306

### Configuração do Banco
```sql
CREATE DATABASE guardian;
```

### Variáveis de Ambiente (opcionais)
| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `DB_URL` | `jdbc:mysql://localhost:3306/guardian?...` | URL do banco |
| `DB_USERNAME` | `root` | Usuário do banco |
| `DB_PASSWORD` | `urubu100` | Senha do banco |
| `JWT` | `guardian-super-secret-key-2026-secure` | Segredo JWT |

### Build e execução
```bash
./mvnw clean compile
./mvnw spring-boot:run
```

---

## Versão da API

- **Versão atual**: v1
- **Última atualização**: 2026-04-11
