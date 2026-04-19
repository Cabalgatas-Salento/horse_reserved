-- Campos de auditoría para calcular métricas de calidad mensuales
ALTER TABLE reservaciones
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP;

-- Backfill para datos existentes
UPDATE reservaciones
SET confirmed_at = COALESCE(confirmed_at, created_at)
WHERE estado IN ('reservado', 'en_curso', 'completado');

UPDATE reservaciones
SET cancelled_at = COALESCE(cancelled_at, created_at)
WHERE estado = 'cancelado';

-- Trigger para mantener confirmed_at/cancelled_at coherentes con cambios de estado
CREATE OR REPLACE FUNCTION fn_reservacion_estado_timestamps()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.estado IN ('reservado', 'en_curso', 'completado') AND NEW.confirmed_at IS NULL THEN
            NEW.confirmed_at := NEW.created_at;
END IF;

        IF NEW.estado = 'cancelado' AND NEW.cancelled_at IS NULL THEN
            NEW.cancelled_at := NEW.created_at;
END IF;
RETURN NEW;
END IF;

    IF TG_OP = 'UPDATE' AND NEW.estado IS DISTINCT FROM OLD.estado THEN
        IF NEW.estado IN ('reservado', 'en_curso', 'completado') AND NEW.confirmed_at IS NULL THEN
            NEW.confirmed_at := NOW();
END IF;

        IF NEW.estado = 'cancelado' THEN
            NEW.cancelled_at := NOW();
END IF;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_reservacion_estado_timestamps ON reservaciones;

CREATE TRIGGER tr_reservacion_estado_timestamps
    BEFORE INSERT OR UPDATE ON reservaciones
                         FOR EACH ROW
                         EXECUTE FUNCTION fn_reservacion_estado_timestamps();