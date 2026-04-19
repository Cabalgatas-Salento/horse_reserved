CREATE TABLE intentos_pago (
                               id                      BIGSERIAL PRIMARY KEY,
                               reservacion_id          BIGINT NOT NULL REFERENCES reservaciones(id) ON DELETE CASCADE,
                               estado                  VARCHAR(20) NOT NULL,
                               metodo_pago             VARCHAR(20) NOT NULL,
                               monto                   NUMERIC(12,2) NOT NULL CHECK (monto > 0),
                               moneda                  VARCHAR(3)  NOT NULL DEFAULT 'COP' CHECK (moneda = 'COP'),
                               pagado_por_usuario_id   BIGINT REFERENCES usuarios(id),
                               pagado_por_operador_id  BIGINT REFERENCES usuarios(id),
                               referencia_simulada     VARCHAR(120) NOT NULL UNIQUE,
                               fecha_intento           TIMESTAMP NOT NULL DEFAULT NOW(),
                               created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
                               updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),

                               CONSTRAINT chk_intento_estado
                                   CHECK (estado IN ('PENDIENTE','CANCELADO','REALIZADO','REEMBOLSADO')),
                               CONSTRAINT chk_metodo_pago
                                   CHECK (metodo_pago IN ('EFECTIVO','TARJETA')),
                               CONSTRAINT chk_pagador_unico
                                   CHECK (
                                       (pagado_por_usuario_id IS NOT NULL AND pagado_por_operador_id IS NULL)
                                           OR
                                       (pagado_por_usuario_id IS NULL AND pagado_por_operador_id IS NOT NULL)
                                       )
);

CREATE INDEX idx_intentos_pago_reservacion  ON intentos_pago(reservacion_id);
CREATE INDEX idx_intentos_pago_estado       ON intentos_pago(estado);
CREATE INDEX idx_intentos_pago_fecha        ON intentos_pago(fecha_intento);

-- Restricción crítica: solo un intento REALIZADO por reserva
CREATE UNIQUE INDEX uq_intento_realizado_por_reserva
    ON intentos_pago(reservacion_id)
    WHERE estado = 'REALIZADO';

-- ─────────────────────────────────────────────────────────────

CREATE TABLE transacciones (
                               id                  BIGSERIAL PRIMARY KEY,
                               intento_pago_id     BIGINT NOT NULL REFERENCES intentos_pago(id) ON DELETE CASCADE,
                               tipo_movimiento     VARCHAR(20) NOT NULL,
                               estado              VARCHAR(20) NOT NULL,
                               monto               NUMERIC(12,2) NOT NULL CHECK (monto > 0),
                               moneda              VARCHAR(3)  NOT NULL DEFAULT 'COP' CHECK (moneda = 'COP'),
                               detalle             VARCHAR(255),
                               fecha_transaccion   TIMESTAMP NOT NULL DEFAULT NOW(),
                               created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                               updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),

                               CONSTRAINT chk_transaccion_tipo
                                   CHECK (tipo_movimiento IN ('PAGO','REEMBOLSO')),
                               CONSTRAINT chk_transaccion_estado
                                   CHECK (estado IN ('PENDIENTE','CANCELADO','REALIZADO','REEMBOLSADO'))
);

CREATE INDEX idx_transacciones_intento ON transacciones(intento_pago_id);
CREATE INDEX idx_transacciones_estado  ON transacciones(estado);
CREATE INDEX idx_transacciones_fecha   ON transacciones(fecha_transaccion);