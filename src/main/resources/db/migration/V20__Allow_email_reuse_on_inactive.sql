-- Permite que un email dado de baja pueda volver a registrarse como cuenta nueva.
-- Elimina la restricción UNIQUE global sobre email y la reemplaza por un índice
-- parcial que solo aplica para usuarios activos (is_active = true).
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_email_key;

CREATE UNIQUE INDEX usuarios_email_active_unique
    ON usuarios (email)
    WHERE is_active = true;
