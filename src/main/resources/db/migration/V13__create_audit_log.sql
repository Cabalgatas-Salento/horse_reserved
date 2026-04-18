CREATE TABLE audit_log (
    id            BIGSERIAL    PRIMARY KEY,
    ocurrido_en   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    usuario_id    BIGINT       REFERENCES usuarios(id) ON DELETE SET NULL,
    usuario_email VARCHAR(200),
    categoria     VARCHAR(50)  NOT NULL,
    accion        VARCHAR(100) NOT NULL,
    resultado     VARCHAR(20)  NOT NULL DEFAULT 'EXITO',
    detalle       TEXT,
    entidad_tipo  VARCHAR(50),
    entidad_id    BIGINT,
    ip_origen     VARCHAR(45),
    CONSTRAINT chk_audit_resultado CHECK (resultado IN ('EXITO', 'FALLO', 'ERROR_SISTEMA')),
    CONSTRAINT chk_audit_categoria CHECK (categoria IN (
        'AUTENTICACION', 'RESERVA', 'RECURSO_ADMIN', 'CUENTA', 'SISTEMA'))
);

CREATE INDEX idx_audit_ocurrido_en ON audit_log(ocurrido_en DESC);
CREATE INDEX idx_audit_usuario_id  ON audit_log(usuario_id);
CREATE INDEX idx_audit_categoria   ON audit_log(categoria);
CREATE INDEX idx_audit_resultado   ON audit_log(resultado);
