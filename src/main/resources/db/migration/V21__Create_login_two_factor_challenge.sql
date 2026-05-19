-- ============================================================
-- Tabla de desafíos 2FA para el flujo de login en dos pasos
-- ============================================================

CREATE TABLE login_two_factor_challenges
(
    -- Identificador público no enumerable
    id            VARCHAR(36)  NOT NULL,

    -- Referencia al usuario; sin FK formal para evitar bloqueos en CASCADE
    usuario_id    BIGINT       NOT NULL,

    -- OTP almacenado como hash BCrypt; nunca en texto plano
    otp_hash      VARCHAR(255) NOT NULL,

    -- Ventana de validez del desafío (inmutable tras creación)
    expires_at    TIMESTAMP    NOT NULL,

    -- Momento en que se consumió exitosamente
    consumed_at   TIMESTAMP,

    -- Control de intentos de verificación
    attempt_count INT          NOT NULL DEFAULT 0,
    max_attempts  INT          NOT NULL DEFAULT 5,

    -- Control de reenvíos
    resend_count  INT          NOT NULL DEFAULT 0,
    max_resends   INT          NOT NULL DEFAULT 3,
    last_sent_at  TIMESTAMP,

    -- Trazabilidad
    created_at    TIMESTAMP    NOT NULL,
    created_ip    VARCHAR(50),
    verified_ip   VARCHAR(50),

    -- Ciclo de vida: PENDING → VERIFIED | EXPIRED | BLOCKED
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',

    CONSTRAINT pk_ltfc PRIMARY KEY (id),
    CONSTRAINT chk_ltfc_status CHECK (status IN ('PENDING', 'VERIFIED', 'EXPIRED', 'BLOCKED')),
    CONSTRAINT chk_ltfc_attempts CHECK (attempt_count >= 0),
    CONSTRAINT chk_ltfc_resends  CHECK (resend_count  >= 0)
);

-- Consultas frecuentes por usuario para invalidar challenges previos
CREATE INDEX idx_ltfc_usuario_id     ON login_two_factor_challenges (usuario_id);

-- Filtro por estado en invalidaciones y reportes
CREATE INDEX idx_ltfc_status         ON login_two_factor_challenges (status);

-- Limpieza de expirados por jobs o validación lazy
CREATE INDEX idx_ltfc_expires_at     ON login_two_factor_challenges (expires_at);

-- Combinado: el más usado en invalidatePendingByUsuarioId()
CREATE INDEX idx_ltfc_usuario_status ON login_two_factor_challenges (usuario_id, status);

COMMENT ON TABLE  login_two_factor_challenges              IS 'Desafíos de verificación en dos pasos para el flujo de inicio de sesión';
COMMENT ON COLUMN login_two_factor_challenges.otp_hash     IS 'BCrypt del OTP; nunca almacenar texto plano';
COMMENT ON COLUMN login_two_factor_challenges.status       IS 'PENDING=activo, VERIFIED=consumido, EXPIRED=TTL vencido, BLOCKED=intentos superados';
COMMENT ON COLUMN login_two_factor_challenges.expires_at   IS 'Inmutable tras creación; no se extiende en reenvíos';