-- Usuario de prueba para tests de carga k6 (perfil test).
-- No ejecutar en producción.
INSERT INTO usuarios (primer_nombre, primer_apellido, tipo_documento, documento, email, password_hash, telefono, role, is_active, password_changed_at, habeas_data_consented, habeas_data_consented_at)
VALUES (
    'Cliente',
    'Test',
    'CEDULA',
    '0000000000',
    'cliente@test.com',
    '$2a$12$afG9XkkB98KnCX9pR3LtRuIlFBLQ1c2FH.gQlsqy0lFD7UxosiRRK',
    '3000000000',
    'CLIENTE',
    TRUE,
    NOW(),
    TRUE,
    NOW()
)
ON CONFLICT (email) DO NOTHING;
