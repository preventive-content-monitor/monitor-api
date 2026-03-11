create table guardian_users
(
    id            uuid primary key,
    email         varchar(320) unique not null,
    password_hash varchar(255)        not null,
    role          varchar(32)         not null,
    created_at    timestamp           not null
);

create table dependents
(
    id               uuid primary key,
    guardian_user_id uuid         not null,
    nickname         varchar(100) not null,
    birth_year       int          not null,
    created_at       timestamp    not null
);

create table devices
(
    id           uuid primary key,
    dependent_id uuid         not null,
    device_name  varchar(150) not null,
    enrolled_at  timestamp    not null,
    last_seen_at timestamp
);

create table events
(
    id            uuid primary key,
    device_id     uuid         not null,
    type          varchar(40)  not null,
    url_host      varchar(255) not null,
    url_path_hash varchar(64),
    title         varchar(512),
    occurred_at   timestamp    not null,
    metadata      clob
);

create table policies
(
    id                  uuid primary key,
    dependent_id        uuid        not null unique,
    mode                varchar(32) not null,
    risk_threshold      int         not null,
    blocked_domains     clob        not null,
    allowed_domains     clob        default '[]',
    school_mode_enabled boolean     default false,
    school_start        varchar(5)  default null,
    school_end          varchar(5)  default null,
    created_at          timestamp   not null
);

create table classifications
(
    id         uuid primary key,
    event_id   uuid        not null unique,
    model      varchar(80) not null,
    label      varchar(80) not null,
    risk_score int         not null,
    rationale  clob,
    created_at timestamp   not null
);

create table vulnerability_daily
(
    id           uuid primary key,
    dependent_id uuid not null,
    day_ref      date not null,
    score        int  not null,
    features     clob
);

-- ==========================================
-- DADOS MOCK PARA DESENVOLVIMENTO
-- ==========================================

-- Usuário responsável: ruan@gmail.com / 123456
-- Senha hash BCrypt de "123456"
INSERT INTO guardian_users (id, email, password_hash, role, created_at)
VALUES ('11111111-1111-1111-1111-111111111111', 'ruan@gmail.com', 
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/JvJ0aXJhO0e7hW1a3v6tK', 
        'RESPONSAVEL', '2026-02-20T10:00:00');

-- Dependente: Sisi (ano nascimento 2015 = 11 anos)
INSERT INTO dependents (id, guardian_user_id, nickname, birth_year, created_at)
VALUES ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111',
        'Sisi', 2015, '2026-02-20T10:00:00');

-- Dispositivo de teste
INSERT INTO devices (id, dependent_id, device_name, enrolled_at, last_seen_at)
VALUES ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222',
        'Chrome - Windows', '2026-02-20T10:00:00', '2026-02-22T12:00:00');

-- Política padrão para Sisi (11 anos = WARN, threshold 50)
INSERT INTO policies (id, dependent_id, mode, risk_threshold, blocked_domains, allowed_domains, school_mode_enabled, school_start, school_end, created_at)
VALUES ('44444444-4444-4444-4444-444444444444', '22222222-2222-2222-2222-222222222222',
        'WARN', 50, '["pornhub.com", "xvideos.com"]', '["khanacademy.org"]', false, null, null, '2026-02-20T10:00:00');