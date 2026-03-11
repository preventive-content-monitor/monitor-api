# Guardian Backend - Documentação Completa para Integração

## Visão Geral

O **Guardian** é um sistema de controle parental que monitora e classifica a navegação de crianças e adolescentes. Este backend fornece APIs REST para:

- Autenticação de responsáveis (pais/tutores)
- Gerenciamento de dependentes (crianças monitoradas)
- Vinculação de dispositivos
- Ingestão de eventos de navegação
- Classificação automática de conteúdo
- Aplicação de políticas de bloqueio
- Métricas e dashboard

---

## Arquitetura

```
┌─────────────────────┐     ┌──────────────────────┐
│  Extensão Browser   │────▶│   Guardian Backend   │
│  (Chrome/Firefox)   │     │   (Spring Boot)      │
└─────────────────────┘     └──────────────────────┘
                                      │
                            ┌─────────┴─────────┐
                            ▼                   ▼
                    ┌──────────────┐    ┌──────────────┐
                    │   Database   │    │  Classifier  │
                    │     (H2)     │    │   (Mock/ML)  │
                    └──────────────┘    └──────────────┘
```

### Stack Tecnológica
- **Framework**: Spring Boot 3.2.5
- **Linguagem**: Kotlin 1.9
- **Autenticação**: JWT (Bearer Token)
- **Database**: H2 (dev) / MySQL (prod)
- **Documentação**: OpenAPI 3 (Swagger)

---

## Base URL

```
http://localhost:8080
```

---

## Autenticação

O sistema usa **JWT Bearer Tokens**. Após login, inclua o token em todas as requisições protegidas:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Endpoints Públicos (sem autenticação)
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/events/batch`
- `POST /api/devices/enroll`
- `GET /api/policy/current`

### Endpoints Protegidos (requer JWT)
- `GET /api/dependents`
- `POST /api/dependents`
- `GET /api/devices`
- `PUT /api/policy`
- `GET /api/dashboard/summary`

---

## Modelos de Dados

### GuardianUser (Responsável)
```json
{
  "id": "uuid",
  "email": "string",
  "role": "RESPONSAVEL",
  "createdAt": "2026-02-22T10:00:00Z"
}
```

### Dependent (Dependente/Criança)
```json
{
  "id": "uuid",
  "guardianUser": { "id": "uuid", "email": "string" },
  "nickname": "string",
  "birthYear": 2015,
  "createdAt": "2026-02-22T10:00:00Z"
}
```

### Device (Dispositivo)
```json
{
  "id": "uuid",
  "dependent": { "id": "uuid", "nickname": "string" },
  "deviceName": "Chrome - Windows",
  "enrolledAt": "2026-02-22T10:00:00Z",
  "lastSeenAt": "2026-02-22T15:30:00Z"
}
```

### Policy (Política de Controle)
```json
{
  "id": "uuid",
  "dependent": { "id": "uuid" },
  "mode": "BLOCK | WARN | EDUCATE",
  "riskThreshold": 30,
  "blockedDomains": "[\"site1.com\", \"site2.com\"]",
  "createdAt": "2026-02-22T10:00:00Z"
}
```

### Event (Evento de Navegação)
```json
{
  "id": "uuid",
  "device": { "id": "uuid" },
  "type": "NAVIGATION | BLOCK_ATTEMPT | PERMISSION_REQUEST",
  "urlHost": "youtube.com",
  "urlPathHash": "sha256hash",
  "title": "Título da página",
  "occurredAt": "2026-02-22T15:30:00Z",
  "metadata": { "key": "value" }
}
```

### EventType (Enum)
| Valor | Descrição |
|-------|-----------|
| `NAVIGATION` | Navegação normal para uma URL |
| `BLOCK_ATTEMPT` | Tentativa de acessar conteúdo bloqueado |
| `PERMISSION_REQUEST` | Solicitação de permissão (câmera, mic, etc) |

### PolicyMode (Enum)
| Valor | Descrição | Comportamento |
|-------|-----------|---------------|
| `BLOCK` | Bloqueio total | Bloqueia acesso a conteúdo acima do threshold |
| `WARN` | Aviso | Exibe aviso mas permite continuar |
| `EDUCATE` | Educativo | Apenas registra para análise posterior |

---

## Endpoints

### 1. Autenticação

#### POST /api/auth/register
Cadastra um novo responsável.

**Request:**
```json
{
  "email": "pai@familia.com",
  "password": "senhaSegura123"
}
```

**Response:** `201 Created`

**Erros:**
| Status | Descrição |
|--------|-----------|
| 409 | Email já cadastrado |
| 400 | Dados inválidos |

---

#### POST /api/auth/login
Autentica e retorna token JWT.

**Request:**
```json
{
  "email": "pai@familia.com",
  "password": "senhaSegura123"
}
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1dWlkLWRvLXVzdWFyaW8iLCJyb2xlIjoiUkVTUE9OU0FWRUwiLCJpYXQiOjE3MDg2MDAwMDAsImV4cCI6MTcwODY4NjQwMH0.signature"
}
```

**Erros:**
| Status | Descrição |
|--------|-----------|
| 401 | Credenciais inválidas |

---

### 2. Dependentes

#### POST /api/dependents
Cria um novo dependente. **Requer autenticação.**

**Headers:**
```http
Authorization: Bearer <token>
```

**Request:**
```json
{
  "nickname": "João",
  "birthYear": 2015
}
```

**Response:** `201 Created`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "guardianUser": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "email": "pai@familia.com"
  },
  "nickname": "João",
  "birthYear": 2015,
  "createdAt": "2026-02-22T10:00:00Z"
}
```

---

#### GET /api/dependents
Lista todos os dependentes do responsável autenticado.

**Headers:**
```http
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "nickname": "João",
    "birthYear": 2015,
    "createdAt": "2026-02-22T10:00:00Z"
  }
]
```

---

### 3. Dispositivos

#### POST /api/devices/generate-code/{dependentId}
Gera código de vinculação (válido por 5 minutos). **Requer autenticação.**

**Headers:**
```http
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
{
  "code": "a1b2c3"
}
```

---

#### POST /api/devices/enroll
Vincula dispositivo usando o código. **Endpoint público (sem autenticação) - usado pela extensão.**

**Request:**
```json
{
  "code": "a1b2c3",
  "deviceName": "Chrome - Windows 11"
}
```

**Response:** `201 Created`
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "dependent": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "nickname": "João"
  },
  "deviceName": "Chrome - Windows 11",
  "enrolledAt": "2026-02-22T10:05:00Z",
  "lastSeenAt": null
}
```

**Erros:**
| Status | Descrição |
|--------|-----------|
| 400 | Código inválido ou expirado |
| 404 | Dependente não encontrado |

---

### 4. Eventos (Ingestão)

#### POST /api/events/batch
**ENDPOINT PRINCIPAL PARA A EXTENSÃO**

Envia lote de eventos de navegação. **Não requer autenticação** (apenas deviceId válido).

**Request:**
```json
{
  "deviceId": "660e8400-e29b-41d4-a716-446655440001",
  "events": [
    {
      "type": "NAVIGATION",
      "url": "https://youtube.com/watch?v=abc123",
      "title": "Vídeo do YouTube",
      "occurredAt": "2026-02-22T15:30:00Z",
      "metadata": {
        "referrer": "https://google.com",
        "timeOnPage": 120
      }
    },
    {
      "type": "NAVIGATION",
      "url": "https://instagram.com/explore",
      "title": "Instagram Explorar",
      "occurredAt": "2026-02-22T15:32:00Z",
      "metadata": null
    }
  ]
}
```

**Response:** `200 OK`
```json
{
  "status": "ok",
  "ingested": 2
}
```

**Erros:**
| Status | Descrição |
|--------|-----------|
| 404 | Dispositivo não encontrado |
| 400 | Dados inválidos |

**Comportamento interno:**
1. Valida se o `deviceId` existe
2. Para cada evento:
   - Extrai host e hash do path da URL
   - Salva evento no banco
   - Classifica conteúdo (riskScore 0-100)
   - Verifica política de bloqueio
3. Atualiza `lastSeenAt` do dispositivo
4. Retorna contagem de eventos processados

---

### 5. Políticas

#### GET /api/policy/current
Obtém política ativa para um dispositivo. **Endpoint público (sem autenticação).**

**Query Parameters:**
| Param | Tipo | Descrição |
|-------|------|-----------|
| deviceId | UUID | ID do dispositivo |

**Request:**
```http
GET /api/policy/current?deviceId=660e8400-e29b-41d4-a716-446655440001
```

**Response:** `200 OK`
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440002",
  "mode": "BLOCK",
  "riskThreshold": 30,
  "blockedDomains": "[\"pornhub.com\", \"xvideos.com\"]",
  "createdAt": "2026-02-22T10:00:00Z"
}
```

**Criação automática de política:**
Se não existir política, uma é criada automaticamente baseada na idade:

| Idade | Mode | Threshold |
|-------|------|-----------|
| ≤ 10 anos | BLOCK | 30 |
| 11-13 anos | WARN | 50 |
| > 13 anos | EDUCATE | 70 |

#### PUT /api/policy
Atualiza a política de controle parental do dispositivo. **Requer autenticação.**

**Query Parameters:**
| Param | Tipo | Descrição |
|-------|------|-----------|
| deviceId | UUID | ID do dispositivo |

**Request Body:**
```json
{
  "mode": "WARN",
  "riskThreshold": 50,
  "blockedDomains": ["facebook.com", "tiktok.com", "instagram.com"]
}
```

| Campo | Tipo | Descrição |
|-------|------|-----------|
| mode | string | Modo da política: `BLOCK`, `WARN` ou `EDUCATE` |
| riskThreshold | int | Limite de risco (0-100) para acionar a política |
| blockedDomains | string[] | Lista de domínios bloqueados manualmente |

**Request:**
```http
PUT /api/policy?deviceId=660e8400-e29b-41d4-a716-446655440001
Authorization: Bearer <token>
Content-Type: application/json

{
  "mode": "WARN",
  "riskThreshold": 50,
  "blockedDomains": ["facebook.com", "tiktok.com"]
}
```

**Response:** `200 OK`
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440002",
  "mode": "WARN",
  "riskThreshold": 50,
  "blockedDomains": "[\"facebook.com\", \"tiktok.com\"]",
  "createdAt": "2026-02-22T10:00:00Z"
}
```

**Errors:**
| HTTP | Descrição |
|------|-----------|
| 400 | Dados inválidos (riskThreshold fora de 0-100) |
| 401 | Token ausente ou inválido |
| 404 | Dispositivo ou política não encontrada |

---

### 6. Dashboard

#### GET /api/dashboard/summary
Resumo de métricas do dispositivo. **Requer autenticação.**

**Query Parameters:**
| Param | Tipo | Exemplo | Descrição |
|-------|------|---------|-----------|
| deviceId | UUID | `660e8400-...` | ID do dispositivo |
| from | ISO 8601 | `2026-02-01T00:00:00Z` | Data início |
| to | ISO 8601 | `2026-02-22T23:59:59Z` | Data fim |

**Response:** `200 OK`
```json
{
  "totalEvents": 1250,
  "sensitiveEvents": 23,
  "blockAttempts": 5
}
```

---

#### GET /api/dashboard/top-domains
Top domínios mais acessados. **Requer autenticação.**

**Query Parameters:** Mesmos do `/summary`

**Response:** `200 OK`
```json
[
  ["youtube.com", 450],
  ["instagram.com", 230],
  ["tiktok.com", 180],
  ["google.com", 120],
  ["whatsapp.com", 85]
]
```

---

#### GET /api/dashboard/vulnerability
Histórico de índice de vulnerabilidade. **Requer autenticação.**

**Query Parameters:**
| Param | Tipo | Exemplo | Descrição |
|-------|------|---------|-----------|
| dependentId | UUID | `550e8400-...` | ID do dependente |
| from | YYYY-MM-DD | `2026-02-01` | Data início |
| to | YYYY-MM-DD | `2026-02-22` | Data fim |

**Response:** `200 OK`
```json
[
  {
    "id": "uuid",
    "day": "2026-02-20",
    "score": 35,
    "features": "{\"sensitive\":5,\"blocks\":2,\"night\":10}"
  },
  {
    "id": "uuid",
    "day": "2026-02-21",
    "score": 28,
    "features": "{\"sensitive\":3,\"blocks\":1,\"night\":8}"
  }
]
```

---

## Formato de Erros

Todas as respostas de erro seguem o padrão:

```json
{
  "timestamp": "2026-02-22T15:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Dispositivo não encontrado",
  "path": "/api/events/batch",
  "details": null
}
```

Para erros de validação, `details` contém lista de campos:
```json
{
  "timestamp": "2026-02-22T15:30:00Z",
  "status": 400,
  "error": "Validation Error",
  "message": "Erro de validação nos dados enviados",
  "path": "/api/auth/register",
  "details": [
    "email: deve ser um email válido",
    "password: tamanho mínimo de 6 caracteres"
  ]
}
```

---

## Guia de Integração - Extensão de Navegador

### Fluxo de Vinculação

```
┌──────────────┐    ┌───────────────┐    ┌─────────────┐
│  App Móvel   │    │    Backend    │    │  Extensão   │
│  Responsável │    │               │    │  Browser    │
└──────┬───────┘    └───────┬───────┘    └──────┬──────┘
       │                    │                   │
       │ POST /api/devices/ │                   │
       │ generate-code/{id} │                   │
       │───────────────────▶│                   │
       │                    │                   │
       │    { code: "a1b2c3" }                  │
       │◀───────────────────│                   │
       │                    │                   │
       │      Mostra código para              │
       │      digitar na extensão               │
       │                    │                   │
       │                    │  POST /api/devices/enroll
       │                    │  { code, deviceName }
       │                    │◀──────────────────│
       │                    │                   │
       │                    │  Device vinculado │
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

// Buffer de eventos para envio em lote
let eventBuffer = [];

// Listener de navegação
chrome.webNavigation.onCompleted.addListener((details) => {
  if (details.frameId !== 0) return; // Apenas frame principal
  
  eventBuffer.push({
    type: 'NAVIGATION',
    url: details.url,
    title: '', // Será preenchido depois
    occurredAt: new Date().toISOString(),
    metadata: {
      tabId: details.tabId,
      transitionType: details.transitionType
    }
  });
});

// Obter título da página
chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
  if (changeInfo.title) {
    const event = eventBuffer.find(e => 
      e.metadata?.tabId === tabId && !e.title
    );
    if (event) {
      event.title = changeInfo.title;
    }
  }
});

// Envio periódico (a cada 30 segundos)
setInterval(async () => {
  if (eventBuffer.length === 0) return;
  
  const eventsToSend = [...eventBuffer];
  eventBuffer = [];
  
  try {
    const response = await fetch(`${API_URL}/api/events/batch`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        deviceId: DEVICE_ID,
        events: eventsToSend
      })
    });
    
    if (!response.ok) {
      // Recolocar eventos no buffer em caso de erro
      eventBuffer = [...eventsToSend, ...eventBuffer];
      console.error('Erro ao enviar eventos:', await response.json());
    }
  } catch (error) {
    eventBuffer = [...eventsToSend, ...eventBuffer];
    console.error('Erro de rede:', error);
  }
}, 30000);
```

### Fluxo de Verificação de Política

```javascript
// Verificar se deve bloquear antes de navegar

async function checkShouldBlock(url) {
  try {
    const response = await fetch(
      `${API_URL}/api/policy/current?deviceId=${DEVICE_ID}`,
      {
        headers: {
          'Authorization': `Bearer ${getStoredToken()}`
        }
      }
    );
    
    if (!response.ok) return false;
    
    const policy = await response.json();
    const blockedDomains = JSON.parse(policy.blockedDomains);
    const urlHost = new URL(url).hostname;
    
    // Verificar lista de bloqueio
    if (blockedDomains.includes(urlHost)) {
      return { blocked: true, reason: 'domain_blocked' };
    }
    
    // Para verificação de risco, o backend faz automaticamente
    // durante a ingestão de eventos
    return { blocked: false };
    
  } catch (error) {
    console.error('Erro ao verificar política:', error);
    return { blocked: false };
  }
}
```

### Registro de Tentativa de Bloqueio

```javascript
// Quando usuário tenta acessar conteúdo bloqueado

async function reportBlockAttempt(url, reason) {
  await fetch(`${API_URL}/api/events/batch`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      deviceId: DEVICE_ID,
      events: [{
        type: 'BLOCK_ATTEMPT',
        url: url,
        title: 'Tentativa de acesso bloqueado',
        occurredAt: new Date().toISOString(),
        metadata: {
          reason: reason,
          userAgent: navigator.userAgent
        }
      }]
    })
  });
}
```

---

## Constantes Importantes

### Event Types
```javascript
const EVENT_TYPES = {
  NAVIGATION: 'NAVIGATION',
  BLOCK_ATTEMPT: 'BLOCK_ATTEMPT', 
  PERMISSION_REQUEST: 'PERMISSION_REQUEST'
};
```

### Policy Modes
```javascript
const POLICY_MODES = {
  BLOCK: 'BLOCK',      // Bloqueia acesso
  WARN: 'WARN',        // Apenas avisa
  EDUCATE: 'EDUCATE'   // Registra silenciosamente
};
```

### Risk Score Thresholds
```javascript
const RISK_THRESHOLDS = {
  LOW: 30,      // Conteúdo seguro
  MEDIUM: 50,   // Atenção necessária
  HIGH: 70,     // Conteúdo sensível
  CRITICAL: 90  // Bloqueio imediato
};
```

---

## Swagger UI

Documentação interativa disponível em:
```
http://localhost:8080/swagger-ui/index.html
```

---

## Considerações de Performance

1. **Batch de eventos**: Envie eventos em lotes (recomendado: a cada 30s ou 50 eventos)
2. **Retry com backoff**: Em caso de falha, use exponential backoff
3. **Buffer local**: Mantenha buffer de eventos para offline
4. **Compressão**: Considere gzip para payloads grandes

---

## Segurança

1. **DeviceId**: Armazene de forma segura (chrome.storage.local)
2. **Não exponha tokens**: O endpoint de eventos não requer JWT
3. **Validação de URL**: Sempre valide URLs antes de enviar
4. **HTTPS**: Use HTTPS em produção

---

## Versão da API

- **Versão atual**: v1
- **Última atualização**: 2026-02-22
