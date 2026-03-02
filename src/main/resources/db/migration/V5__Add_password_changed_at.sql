-- Marca de tiempo del último cambio de contraseña.
-- Tokens JWT emitidos ANTES de este timestamp se consideran inválidos.
-- El valor por defecto (epoch) implica "nunca cambiada": todos los tokens existentes siguen siendo válidos.
ALTER TABLE usuarios
    ADD COLUMN password_changed_at TIMESTAMPTZ NOT NULL DEFAULT '2025-12-30 00:00:00+00';

COMMENT ON COLUMN usuarios.password_changed_at
    IS 'Marca de tiempo del último cambio de contraseña. JWTs emitidos antes de esta fecha son rechazados.';
