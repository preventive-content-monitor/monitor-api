# Documentação Completa — Guardian Backend (monitor-api)

> **Finalidade deste documento:** Descrever toda a estrutura, nomenclatura, endpoints, modelos de dados, regras de negócio e contratos de integração do backend Guardian para que o front-end saiba exatamente como se comunicar com a API.

---

## 1. Visão Geral

| Item | Valor |
|------|-------|
| Linguagem | Kotlin |
| Framework | Spring Boot |
| Banco de Dados | MySQL |
| Autenticação | JWT (Bearer Token) |
| Porta padrão | `8080` |
| Base URL | `http://localhost:8080` |
| Documentação Swagger | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

---

## 2. Estrutura de Pastas

```
src/main/kotlin/br/com/guardian/backend/
│
├── GuardianAplicacao.kt                        ← Classe principal (main)
│
├── adaptadores/
│   ├── entrada/
│   │   ├── dto/                                ← Objetos de transferência de dados (request/response)
│   │   │   ├── ItemIngestaoEventoDto.kt
│   │   │   ├── RequisicaoAtualizarPolitica.kt
│   │   │   ├── RequisicaoCriarDependente.kt
│   │   │   ├── RequisicaoIngestaoLoteEvento.kt
│   │   │   ├── RequisicaoLogin.kt
│   │   │   ├── RequisicaoRegistro.kt
│   │   │   ├── RequisicaoVinculacao.kt
│   │   │   ├── RespostaAutenticacao.kt
│   │   │   ├── RespostaDispositivo.kt
│   │   │   └── RespostaErro.kt
│   │   └── rest/                               ← Controllers REST
│   │       ├── AutenticacaoController.kt
│   │       ├── DependenteController.kt
│   │       ├── DispositivoController.kt
│   │       ├── IngestaoEventoController.kt
│   │       ├── PainelController.kt
│   │       └── PoliticaController.kt
│   └── saida/
│       ├── classificacao/
│       │   └── ClienteClassificador.kt         ← Classificador de conteúdo (heurística)
│       ├── json/
│       │   └── JacksonSerializadorJson.kt       ← Implementação de serialização JSON
│       ├── persistencia/                        ← Repositórios JPA
│       │   ├── ClassificacaoRepositorio.kt
│       │   ├── DependenteRepositorio.kt
│       │   ├── DispositivoRepositorio.kt
│       │   ├── EventoRepositorio.kt
│       │   ├── PoliticaRepositorio.kt
│       │   ├── UsuarioRepositorio.kt
│       │   └── VulnerabilidadeRepositorio.kt
│       └── seguranca/
│           └── JwtGeradorToken.kt              ← Geração de tokens JWT
│
├── aplicacao/
│   ├── porta/entrada/                          ← Interfaces (contratos) de serviço
│   │   ├── ServicoAutenticacao.kt
│   │   ├── ServicoPolitica.kt (interface)
│   │   └── ValidadorUsuario.kt
│   └── servico/                                ← Implementações dos serviços
│       ├── EstrategiaCalculoPoliticaPorIdade.kt
│       ├── ServicoAutenticacaoImpl.kt
│       ├── ServicoClassificacao.kt
│       ├── ServicoIngestaoEvento.kt
│       ├── ServicoMetricas.kt
│       ├── ServicoPoliticaImpl.kt
│       ├── ServicoVinculacaoDispositivo.kt
│       ├── ServicoVulnerabilidade.kt
│       └── ValidadorUsuarioImpl.kt
│
├── dominio/
│   ├── excecao/
│   │   └── Excecoes.kt                         ← Todas as exceções de domínio
│   ├── modelo/                                 ← Entidades JPA e enums
│   │   ├── Dependente.kt
│   │   ├── Dispositivo.kt
│   │   ├── Evento.kt
│   │   ├── JwtClaims.kt
│   │   ├── ModoPolitica.kt
│   │   ├── PapelUsuario.kt
│   │   ├── Politica.kt
│   │   ├── ResultadoClassificacao.kt
│   │   ├── TipoEvento.kt
│   │   ├── UsuarioGuardian.kt
│   │   └── VulnerabilidadeDiaria.kt
│   ├── porta/                                  ← Abstrações de infraestrutura
│   │   ├── GeradorToken.kt
│   │   └── SerializadorJson.kt
│   └── servico/
│       ├── CalculadorIdade.kt
│       └── EstrategiaCalculoPolitica.kt
│
└── infraestrutura/
    ├── configuracao/
    │   ├── ConfiguracaoCors.kt
    │   ├── ConfiguracaoJackson.kt
    │   ├── ConfiguracaoOpenApi.kt
    │   └── ConfiguracaoSeguranca.kt
    ├── excecao/
    │   └── TratadorGlobalExcecao.kt
    └── filtro/
        └── FiltroLogRequisicao.kt
```

---

## 3. Autenticação

Todas as rotas protegidas exigem o header:
```
Authorization: Bearer <token_jwt>
```

O token JWT é obtido no endpoint de login e contém:
- `sub` → UUID do usuário
- `role` → papel do usuário (`RESPONSAVEL`)

### Rotas públicas (sem autenticação)
- `POST /api/autenticacao/registrar`
- `POST /api/autenticacao/entrar`
- `POST /api/eventos/lote`
- `POST /api/dispositivos/vincular`
- `GET  /api/politica/atual`
- Swagger UI e `/v3/api-docs/**`

---

## 4. Endpoints da API

### 4.1 Autenticação — `/api/autenticacao`

#### `POST /api/autenticacao/registrar`
Cria um novo usuário responsável.

**Request Body:**
```json
{
  "email": "responsavel@email.com",
  "senha": "minhasenha123"
}
```

**Respostas:**
| Código | Descrição |
|--------|-----------|
| `201` | Usuário criado com sucesso (sem body) |
| `409` | Email já cadastrado |
| `400` | Dados inválidos |

---

#### `POST /api/autenticacao/entrar`
Autentica o usuário e retorna o token JWT.

**Request Body:**
```json
{
  "email": "responsavel@email.com",
  "senha": "minhasenha123"
}
```

**Response 200:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Respostas:**
| Código | Descrição |
|--------|-----------|
| `200` | Login realizado, token retornado |
| `401` | Credenciais inválidas |

---

### 4.2 Dependentes — `/api/dependentes` 🔒

#### `POST /api/dependentes`
Cria um dependente vinculado ao responsável autenticado.

**Request Body:**
```json
{
  "apelido": "João",
  "dataNascimento": "2015-09-21",
  "sexo": "MASCULINO"
}
```

**Response 201:** objeto `Dependente`
```json
{
  "id": "uuid",
  "apelido": "João",
  "anoNascimento": 2015,
  "dataNascimento": "2015-09-21",
  "sexo": "MASCULINO",
  "criadoEm": "2026-05-03T00:00:00Z"
}
```

---

#### `GET /api/dependentes`
Lista todos os dependentes do responsável autenticado.

**Response 200:** array de `Dependente`

---

#### `GET /api/dependentes/{id}`
Retorna um dependente específico pelo UUID.

**Path Param:** `id` (UUID do dependente)

**Respostas:**
| Código | Descrição |
|--------|-----------|
| `200` | Dependente encontrado |
| `404` | Dependente não encontrado ou não pertence ao usuário |

---

### 4.3 Dispositivos — `/api/dispositivos` 🔒

#### `GET /api/dispositivos`
Lista todos os dispositivos de todos os dependentes do usuário autenticado.

**Response 200:** array de `RespostaDispositivo`
```json
[
  {
    "id": "uuid",
    "nome": "Chrome - Windows",
    "plataforma": "Windows",
    "dependenteId": "uuid",
    "apelidoDependente": "João",
    "vinculadoEm": "2026-05-01T10:00:00Z",
    "ultimoAcessoEm": "2026-05-03T08:30:00Z"
  }
]
```

> A plataforma é inferida do nome do dispositivo: `Windows`, `macOS`, `Linux`, `Android`, `iOS`, ou `Desconhecido`.

---

#### `POST /api/dispositivos/gerar-codigo/{dependenteId}` 🔒
Gera um código de 6 caracteres válido por **5 minutos** para vincular um dispositivo.

**Path Param:** `dependenteId` (UUID)

**Response 200:**
```json
{
  "codigo": "a3f9b2"
}
```

---

#### `POST /api/dispositivos/vincular` *(público)*
Vincula um dispositivo ao dependente usando o código gerado.

**Request Body:**
```json
{
  "codigo": "a3f9b2",
  "nomeDispositivo": "Chrome - Windows 11"
}
```

**Response 201:** objeto `Dispositivo`
```json
{
  "id": "uuid",
  "nomeDispositivo": "Chrome - Windows 11",
  "vinculadoEm": "2026-05-03T10:00:00Z",
  "ultimoAcessoEm": null
}
```

**Respostas:**
| Código | Descrição |
|--------|-----------|
| `201` | Dispositivo vinculado |
| `400` | Código inválido ou expirado |
| `404` | Dependente não encontrado |

---

### 4.4 Eventos — `/api/eventos` *(público)*

#### `POST /api/eventos/lote`
Recebe um lote de eventos de navegação de um dispositivo. Cada evento é classificado automaticamente e verificado contra a política ativa.

**Request Body:**
```json
{
  "dispositivoId": "uuid-do-dispositivo",
  "eventos": [
    {
      "tipo": "NAVIGATION",
      "url": "https://youtube.com/watch?v=abc",
      "titulo": "Vídeo de música",
      "ocorridoEm": "2026-05-03T14:00:00Z",
      "metadados": {}
    }
  ]
}
```

**Tipos de evento (`TipoEvento`):**
| Valor | Descrição |
|-------|-----------|
| `NAVIGATION` | Navegação normal |
| `BLOCK_ATTEMPT` | Tentativa de acessar conteúdo bloqueado |
| `PERMISSION_REQUEST` | Solicitação de permissão |

**Response 200:**
```json
{
  "status": "ok",
  "ingeridos": 1
}
```

**O que acontece internamente:**
1. O evento é salvo no banco
2. É classificado pelo `ClienteClassificador` (heurística por palavras-chave no host/título)
3. A política do dispositivo é verificada — se deve bloquear, loga um aviso
4. O índice de vulnerabilidade diária do dependente é recalculado

---

### 4.5 Painel — `/api/painel` 🔒

#### `GET /api/painel/resumo`
Resumo de métricas de navegação de um dispositivo num período.

**Query Params:**
| Param | Tipo | Exemplo |
|-------|------|---------|
| `dispositivoId` | UUID | `?dispositivoId=uuid` |
| `from` | ISO 8601 | `?from=2026-05-01T00:00:00Z` |
| `to` | ISO 8601 | `?to=2026-05-03T23:59:59Z` |

**Response 200:**
```json
{
  "totalEventos": 120,
  "eventosSensiveis": 5,
  "tentativasBloqueio": 2
}
```

> **Evento sensível** = evento com `pontuacaoRisco >= 70`

---

#### `GET /api/painel/top-dominios`
Top domínios acessados por um dispositivo num período.

**Query Params:** mesmos de `/resumo`

**Response 200:** lista de arrays `[host, contagem]`
```json
[
  ["youtube.com", 45],
  ["google.com", 30]
]
```

---

#### `GET /api/painel/vulnerabilidade`
Histórico diário do índice de vulnerabilidade de um dependente.

**Query Params:**
| Param | Tipo | Exemplo |
|-------|------|---------|
| `dependenteId` | UUID | `?dependenteId=uuid` |
| `from` | `YYYY-MM-DD` | `?from=2026-05-01` |
| `to` | `YYYY-MM-DD` | `?to=2026-05-03` |

**Response 200:** array de `VulnerabilidadeDiaria`
```json
[
  {
    "id": "uuid",
    "dia": "2026-05-01",
    "pontuacao": 35,
    "caracteristicas": "sensiveis=2,bloqueio=1,noturno=3"
  }
]
```

> **Score de vulnerabilidade (0–100):**
> - `0.5 × eventosSensíveis + 0.3 × tentativasBloqueio + 0.2 × usoNoturno`
> - Uso noturno = eventos entre 22h–06h (UTC)

---

### 4.6 Políticas — `/api/politica` 🔒*

#### `GET /api/politica/atual` *(público)*
Retorna a política ativa do dispositivo. Se não existir, cria uma política padrão baseada na idade do dependente.

**Query Param:** `?dispositivoId=uuid`

**Response 200:** objeto `Politica`
```json
{
  "id": "uuid",
  "modo": "WARN",
  "limiteRisco": 50,
  "dominiosBloqueados": "[\"facebook.com\"]",
  "dominiosPermitidos": "[]",
  "modoEscolaAtivo": false,
  "escolaInicio": null,
  "escolaFim": null,
  "criadoEm": "2026-05-01T00:00:00Z"
}
```

> **Atenção:** `dominiosBloqueados` e `dominiosPermitidos` são strings JSON — é necessário fazer `JSON.parse()` no front.

---

#### `PUT /api/politica` 🔒
Atualiza a política de controle parental do dispositivo.

**Query Param:** `?dispositivoId=uuid`

**Request Body:**
```json
{
  "modo": "BLOCK",
  "limiteRisco": 40,
  "dominiosBloqueados": ["tiktok.com", "facebook.com"],
  "dominiosPermitidos": ["khanacademy.org"],
  "modoEscolaAtivo": true,
  "escolaInicio": "07:00",
  "escolaFim": "17:00"
}
```

**Modos de política (`ModoPolitica`):**
| Valor | Comportamento |
|-------|---------------|
| `BLOCK` | Bloqueia o acesso quando o risco supera o limite |
| `WARN` | Avisa sem bloquear |
| `EDUCATE` | Modo educativo (menos restritivo) |

**Política padrão por idade:**
| Idade | Modo | Limite de Risco |
|-------|------|-----------------|
| ≤ 10 anos | `BLOCK` | 30 |
| 11–13 anos | `WARN` | 50 |
| ≥ 14 anos | `EDUCATE` | 70 |

---

## 5. Modelos de Dados

### `UsuarioGuardian` (tabela: `usuarios_guardian`)
| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | UUID | Identificador único |
| `email` | String | Email único do responsável |
| `senhaHash` | String | Hash BCrypt da senha |
| `papel` | `PapelUsuario` | Sempre `RESPONSAVEL` |
| `criadoEm` | Instant | Data de criação |

---

### `Dependente` (tabela: `dependentes`)
| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | UUID | Identificador único |
| `usuarioGuardian` | FK | Responsável dono |
| `apelido` | String | Nome/apelido da criança |
| `anoNascimento` | Int | Ano de nascimento (para calcular idade) |
| `criadoEm` | Instant | Data de criação |

---

### `Dispositivo` (tabela: `dispositivos`)
| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | UUID | Identificador único |
| `dependente` | FK | Dependente dono |
| `nomeDispositivo` | String | Nome livre do dispositivo |
| `vinculadoEm` | Instant | Data de vinculação |
| `ultimoAcessoEm` | Instant? | Último evento recebido |

---

### `Evento` (tabela: `eventos`)
| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | UUID | Identificador único |
| `dispositivo` | FK | Dispositivo que gerou |
| `tipo` | `TipoEvento` | `NAVIGATION`, `BLOCK_ATTEMPT`, `PERMISSION_REQUEST` |
| `urlHost` | String | Domínio da URL (ex: `youtube.com`) |
| `urlPathHash` | String? | SHA-256 do caminho da URL (privacidade) |
| `titulo` | String? | Título da página |
| `ocorridoEm` | Instant | Timestamp do evento |
| `metadados` | String? | JSON livre com dados extras |

---

### `ResultadoClassificacao` (tabela: `classificacoes`)
| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | UUID | Identificador único |
| `evento` | FK (1:1) | Evento classificado |
| `modelo` | String | Versão do classificador (`mock-v1`) |
| `rotulo` | String | `SAFE`, `EXPLICIT`, `GROOMING_RISK` |
| `pontuacaoRisco` | Int | Score de risco (0–100) |
| `justificativa` | String? | Texto explicativo |
| `criadoEm` | Instant | Data da classificação |

---

### `Politica` (tabela: `politicas`)
| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | UUID | Identificador único |
| `dependente` | FK (1:1) | Dependente que a política protege |
| `modo` | `ModoPolitica` | `BLOCK`, `WARN`, `EDUCATE` |
| `limiteRisco` | Int | Threshold (0–100) para acionar a política |
| `dominiosBloqueados` | String | JSON array de domínios bloqueados |
| `dominiosPermitidos` | String | JSON array de domínios permitidos (whitelist) |
| `modoEscolaAtivo` | Boolean | Se restrições escolares estão ativas |
| `escolaInicio` | String? | Horário início escola (`HH:mm`) |
| `escolaFim` | String? | Horário fim escola (`HH:mm`) |
| `criadoEm` | Instant | Data de criação |

---

### `VulnerabilidadeDiaria` (tabela: `vulnerabilidade_diaria`)
| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | UUID | Identificador único |
| `dependente` | FK | Dependente avaliado |
| `dia` | LocalDate | Dia da avaliação (`YYYY-MM-DD`) |
| `pontuacao` | Int | Score 0–100 |
| `caracteristicas` | String? | Detalhes: `sensiveis=N,bloqueio=N,noturno=N` |

---

## 6. Enums

### `TipoEvento`
```
NAVIGATION          — Navegação normal no browser
BLOCK_ATTEMPT       — Tentativa de acesso a conteúdo bloqueado
PERMISSION_REQUEST  — Solicitação de permissão do sistema
```

### `ModoPolitica`
```
BLOCK    — Bloqueio automático ao atingir o limite de risco
WARN     — Apenas aviso, sem bloquear
EDUCATE  — Modo educativo, menos restritivo
```

### `PapelUsuario`
```
RESPONSAVEL  — Único papel disponível no sistema
```

---

## 7. Classificação de Conteúdo

O `ClienteClassificador` é uma heurística simples (a ser substituída por modelo real) que analisa o `titulo` e o `host` da URL:

| Palavra-chave no texto | Rótulo | Score |
|------------------------|--------|-------|
| `porn`, `xxx` | `EXPLICIT` | 95 |
| `chat`, `anon` | `GROOMING_RISK` | 75 |
| `game` | `SAFE` | 10 |
| Outros | `SAFE` | 5–40 (aleatório) |

A política de bloqueio decide bloquear quando:
- O domínio está na lista de bloqueados, **OU**
- O score ≥ `limiteRisco` E o modo é `BLOCK`

---

## 8. Estrutura de Erros

Todos os erros retornam no formato `RespostaErro`:

```json
{
  "timestamp": "2026-05-03T14:00:00Z",
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Dependente não encontrado",
  "caminho": "/api/dependentes/uuid-invalido",
  "detalhes": null
}
```

### Mapeamento de exceções → HTTP

| Exceção | HTTP |
|---------|------|
| `RecursoNaoEncontradoExcecao` | `404` |
| `RequisicaoInvalidaExcecao` | `400` |
| `NaoAutorizadoExcecao` | `401` |
| `CredenciaisInvalidasExcecao` | `401` |
| `ConflitoExcecao` | `409` |
| `EmailJaExisteExcecao` | `409` |
| `DispositivoNaoEncontradoExcecao` | `404` |
| `DependenteNaoEncontradoExcecao` | `404` |
| `UsuarioNaoEncontradoExcecao` | `404` |
| `PoliticaNaoEncontradaExcecao` | `404` |
| `CodigoVinculacaoInvalidoExcecao` | `400` |
| `MethodArgumentNotValidException` | `400` (com campo `detalhes`) |
| `AccessDeniedException` | `403` |
| `AuthenticationException` | `401` |

---

## 9. Segurança e CORS

- CORS liberado para `http://localhost:*`, `http://127.0.0.1:*`, `chrome-extension://*`, `moz-extension://*`
- Métodos permitidos: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`
- `allowCredentials = true`
- Sessão: **stateless** (sem cookies de sessão)
- Senha: hash **BCrypt**
- Token JWT: algoritmo **HS256**, expiração **1 hora** (configurável via `guardian.security.jwt.expiration`)

---

## 10. Configuração (`application.yml`)

```yaml
server:
  port: 8080

spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/guardian?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:urubu100}
  jpa:
    hibernate:
      ddl-auto: update   # Cria/atualiza tabelas automaticamente

guardian:
  security:
    jwt:
      secret: ${JWT:guardian-super-secret-key-2026-secure}
      expiration: 3600000   # 1 hora em ms
```

**Variáveis de ambiente suportadas:**
| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `DB_URL` | `jdbc:mysql://localhost:3306/guardian...` | URL do banco |
| `DB_USERNAME` | `root` | Usuário do banco |
| `DB_PASSWORD` | `urubu100` | Senha do banco |
| `JWT` | `guardian-super-secret-key-2026-secure` | Segredo JWT |

---

## 11. Fluxo Completo de Uso (Guia Rápido)

```
1. Responsável se registra       → POST /api/autenticacao/registrar
2. Responsável faz login         → POST /api/autenticacao/entrar  ← obtém token JWT
3. Cria um dependente            → POST /api/dependentes           (com Bearer token)
4. Gera código de vinculação     → POST /api/dispositivos/gerar-codigo/{dependenteId}
5. Extensão do browser vincula   → POST /api/dispositivos/vincular  (sem token)
6. Extensão envia eventos        → POST /api/eventos/lote           (sem token)
7. Responsável vê painel         → GET  /api/painel/resumo          (com Bearer token)
8. Responsável ajusta política   → PUT  /api/politica               (com Bearer token)
9. Extensão consulta política    → GET  /api/politica/atual         (sem token)
```

---

## 12. Serviços Internos (resumo)

| Serviço | Responsabilidade |
|---------|-----------------|
| `ServicoAutenticacaoImpl` | Registrar usuário e autenticar (gerar JWT) |
| `ValidadorUsuarioImpl` | Verificar se email já existe; validar credenciais de login |
| `ServicoVinculacaoDispositivo` | Gerar e validar códigos de vinculação em memória (TTL 5min) |
| `ServicoIngestaoEvento` | Receber lote de eventos, classificar, verificar política, atualizar vulnerabilidade |
| `ServicoClassificacao` | Chamar o classificador e persistir o resultado |
| `ServicoPoliticaImpl` | CRUD de política; criar política padrão por idade; decidir se deve bloquear |
| `ServicoMetricas` | Calcular resumo e top domínios para o painel |
| `ServicoVulnerabilidade` | Calcular e persistir score de vulnerabilidade diária |
| `EstrategiaCalculoPoliticaPorIdade` | Retornar modo/limite de risco baseado na faixa etária |
| `CalculadorIdade` | Calcular idade a partir do `anoNascimento` |
| `JwtGeradorToken` | Gerar token JWT assinado com HS256 |
| `JacksonSerializadorJson` | Serializar/desserializar listas JSON (para campos de política) |
| `ClienteClassificador` | Heurística de classificação de conteúdo por palavras-chave |
