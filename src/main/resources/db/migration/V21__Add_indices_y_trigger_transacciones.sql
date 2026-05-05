-- Índices para optimizar las consultas de métricas por fecha y estado
CREATE INDEX idx_transacciones_fecha_estado
    ON transacciones (fecha_transaccion, estado);

CREATE INDEX idx_transacciones_fecha_estado_realizado
    ON transacciones (fecha_transaccion)
    WHERE estado = 'REALIZADO';

-- Trigger para actualizar updated_at automáticamente
CREATE OR REPLACE FUNCTION update_transacciones_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_transacciones_updated_at
    BEFORE UPDATE ON transacciones
    FOR EACH ROW
    EXECUTE FUNCTION update_transacciones_updated_at();

COMMENT ON TABLE transacciones IS
    'Registra todos los intentos de pago de una reservación. Una reservación puede tener múltiples transacciones (reintentos). Solo las transacciones con estado REALIZADO se contabilizan como ganancias efectivas.';

COMMENT ON COLUMN transacciones.estado IS
    'PENDIENTE: intento iniciado | REALIZADO: pago confirmado (cuenta como ganancia) | FALLIDO: pago rechazado | CANCELADO: cancelado por usuario/sistema';