-- =============================================================
--  DATOS DE PRUEBA — Horse Reserved
--  Fecha de referencia: 2026-03-07
--
--  CONTRASEÑA DE TODOS LOS USUARIOS: Test1234!
--  Hash BCrypt factor 10 generado con pgcrypto y compatible
--  con Spring Security BCryptPasswordEncoder.
--
--  Para ejecutar contra el contenedor Docker:
--    docker exec -i cabalgatas-db \
--      psql -U cabalgatas_user -d cabalgatas_db \
--      < src/main/resources/db/test_data.sql
-- =============================================================

-- =============================================================
--  LIMPIAR TABLAS (orden inverso de dependencias)
-- =============================================================
TRUNCATE TABLE
    password_reset_tokens,
    participantes,
    reservaciones,
    salida_caballos,
    salida_guias,
    salidas,
    caballos,
    guias,
    rutas,
    usuarios
RESTART IDENTITY CASCADE;


-- =============================================================
--  USUARIOS
--  id | Nombre           | Rol           | Email
--   1 | Carlos Montoya   | ADMINISTRADOR | admin@horsereserved.com
--   2 | Laura Gómez      | OPERADOR      | operador1@horsereserved.com
--   3 | Diego Ramírez    | OPERADOR      | operador2@horsereserved.com
--   4 | María García     | CLIENTE       | maria.garcia@test.com
--   5 | Juan Pérez       | CLIENTE       | juan.perez@test.com
--   6 | Sofía Torres     | CLIENTE       | sofia.torres@test.com
--   7 | Andrés López     | CLIENTE       | andres.lopez@test.com
--   8 | Valentina Cruz   | CLIENTE inac. | valentina.cruz@test.com
-- =============================================================
INSERT INTO usuarios
    (primer_nombre, primer_apellido, tipo_documento, documento,
     email, password_hash, telefono, role, is_active,
     password_changed_at, habeas_data_consented, habeas_data_consented_at)
VALUES
-- ADMINISTRADOR
('Carlos',    'Montoya',  'CEDULA', '10000001',
 'admin@horsereserved.com',
 '$2b$10$em2Tf30PMD/kQFOoMEmkb.6SbcrWd5/dQH9QbVGcWw4KPMPy.8hDa',
 '3001110001', 'ADMINISTRADOR', TRUE,
 '1970-01-01 00:00:00+00', TRUE, '2025-01-10 08:00:00+00'),

-- OPERADORES
('Laura',     'Gómez',    'CEDULA', '20000001',
 'operador1@horsereserved.com',
 '$2b$10$em2Tf30PMD/kQFOoMEmkb.6SbcrWd5/dQH9QbVGcWw4KPMPy.8hDa',
 '3002220001', 'OPERADOR', TRUE,
 '1970-01-01 00:00:00+00', TRUE, '2025-01-10 08:30:00+00'),

('Diego',     'Ramírez',  'CEDULA', '20000002',
 'operador2@horsereserved.com',
 '$2b$10$em2Tf30PMD/kQFOoMEmkb.6SbcrWd5/dQH9QbVGcWw4KPMPy.8hDa',
 '3002220002', 'OPERADOR', TRUE,
 '1970-01-01 00:00:00+00', TRUE, '2025-01-10 09:00:00+00'),

-- CLIENTES ACTIVOS
('María',     'García',   'CEDULA', '30000001',
 'maria.garcia@test.com',
 '$2b$10$em2Tf30PMD/kQFOoMEmkb.6SbcrWd5/dQH9QbVGcWw4KPMPy.8hDa',
 '3003330001', 'CLIENTE', TRUE,
 '1970-01-01 00:00:00+00', TRUE, '2025-06-15 10:00:00+00'),

('Juan',      'Pérez',    'CEDULA', '30000002',
 'juan.perez@test.com',
 '$2b$10$em2Tf30PMD/kQFOoMEmkb.6SbcrWd5/dQH9QbVGcWw4KPMPy.8hDa',
 '3003330002', 'CLIENTE', TRUE,
 '1970-01-01 00:00:00+00', TRUE, '2025-07-20 14:00:00+00'),

('Sofía',     'Torres',   'CEDULA', '30000003',
 'sofia.torres@test.com',
 '$2b$10$em2Tf30PMD/kQFOoMEmkb.6SbcrWd5/dQH9QbVGcWw4KPMPy.8hDa',
 '3003330003', 'CLIENTE', TRUE,
 '1970-01-01 00:00:00+00', TRUE, '2025-08-10 09:00:00+00'),

('Andrés',    'López',    'CEDULA', '30000004',
 'andres.lopez@test.com',
 '$2b$10$em2Tf30PMD/kQFOoMEmkb.6SbcrWd5/dQH9QbVGcWw4KPMPy.8hDa',
 '3003330004', 'CLIENTE', TRUE,
 '1970-01-01 00:00:00+00', TRUE, '2025-09-05 11:00:00+00'),

-- CLIENTE INACTIVO — para probar bloqueo de acceso (is_active = FALSE)
('Valentina', 'Cruz',     'CEDULA', '30000005',
 'valentina.cruz@test.com',
 '$2b$10$em2Tf30PMD/kQFOoMEmkb.6SbcrWd5/dQH9QbVGcWw4KPMPy.8hDa',
 NULL, 'CLIENTE', FALSE,
 '1970-01-01 00:00:00+00', FALSE, NULL);


-- =============================================================
--  RUTAS
--  id | Nombre                    | Dificultad | Min  | Precio
--   1 | Ruta del Amanecer         | FACIL      |  90m | $50.000
--   2 | Sendero del Bosque        | MEDIA      | 150m | $85.000
--   3 | Expedición a las Cumbres  | DIFICIL    | 240m | $120.000
--   4 | Paseo Familiar Corto      | FACIL      |  60m | $35.000 (inactiva)
-- =============================================================
INSERT INTO rutas (nombre, descripcion, dificultad, duracion_minutos, image_url, is_active, precio)
VALUES
('Ruta del Amanecer',
 'Hermosa ruta matutina a través de praderas y quebradas. Ideal para principiantes y familias. No requiere experiencia previa en equitación.',
 'FACIL', 90, NULL, TRUE, 50000.00),

('Sendero del Bosque',
 'Recorrido intermedio por senderos boscosos con vistas panorámicas. Incluye paso por riachuelos y zonas de descanso. Se recomienda experiencia básica.',
 'MEDIA', 150, NULL, TRUE, 85000.00),

('Expedición a las Cumbres',
 'Aventura de alto rendimiento hacia los picos más altos de la región. Solo para jinetes experimentados. Terreno irregular con ascensos pronunciados.',
 'DIFICIL', 240, NULL, TRUE, 120000.00),

('Paseo Familiar Corto',
 'Paseo tranquilo de corta duración para toda la familia. Temporalmente fuera de servicio mientras se remodela el sendero principal.',
 'FACIL', 60, NULL, FALSE, 35000.00);


-- =============================================================
--  CABALLOS (10 activos)
-- =============================================================
INSERT INTO caballos (nombre, raza, is_active)
VALUES
('Relámpago', 'Pura Sangre Inglés',  TRUE),   -- id=1
('Tornado',   'Cuarto de Milla',     TRUE),   -- id=2
('Luna',      'Andaluz',             TRUE),   -- id=3
('Trueno',    'Criollo Colombiano',  TRUE),   -- id=4
('Estrella',  'Árabe',               TRUE),   -- id=5
('Canela',    'Paso Fino',           TRUE),   -- id=6
('Rocío',     'Appaloosa',           TRUE),   -- id=7
('Cometa',    'Pinto',               TRUE),   -- id=8
('Bravo',     'Percherón',           TRUE),   -- id=9
('Furia',     'Mustang',             TRUE);   -- id=10


-- =============================================================
--  GUÍAS (5 activos)
-- =============================================================
INSERT INTO guias (nombre, telefono, email, is_active)
VALUES
('Carlos Herrera',    '3001234567', 'carlos.herrera@horsereserved.com',  TRUE),  -- id=1
('Ana Morales',       '3017654321', 'ana.morales@horsereserved.com',     TRUE),  -- id=2
('Pedro Jiménez',     '3024567890', 'pedro.jimenez@horsereserved.com',   TRUE),  -- id=3
('Sandra Vásquez',    '3031234567', 'sandra.vasquez@horsereserved.com',  TRUE),  -- id=4
('Miguel Ángel Ruiz', '3048765432', 'miguel.ruiz@horsereserved.com',     TRUE);  -- id=5


-- =============================================================
--  SALIDAS
--  Regla de guías: ≤8 personas en salida → 1 guía | >8 → 2 guías
--
--  id | Ruta | Fecha      | Horario       | Estado
--   1 |  1   | 2026-02-14 | 08:00-09:30  | completado
--   2 |  2   | 2026-02-21 | 09:00-11:30  | completado
--   3 |  1   | 2026-03-01 | 07:00-08:30  | completado
--   4 |  3   | 2026-03-05 | 06:00-10:00  | completado
--   5 |  2   | 2026-03-07 | 08:00-10:30  | en_curso    ← HOY (9 personas)
--   6 |  1   | 2026-03-07 | 14:00-15:30  | programado  ← HOY tarde
--   7 |  1   | 2026-03-08 | 08:00-09:30  | cancelado
--   8 |  2   | 2026-03-10 | 09:00-11:30  | programado
--   9 |  3   | 2026-03-15 | 06:00-10:00  | programado
--  10 |  1   | 2026-03-20 | 08:00-09:30  | programado
-- =============================================================
INSERT INTO salidas (ruta_id, fecha_programada, tiempo_inicio, tiempo_fin, estado)
VALUES
(1, '2026-02-14', '08:00', '09:30', 'completado'),
(2, '2026-02-21', '09:00', '11:30', 'completado'),
(1, '2026-03-01', '07:00', '08:30', 'completado'),
(3, '2026-03-05', '06:00', '10:00', 'completado'),
(2, '2026-03-07', '08:00', '10:30', 'en_curso'),
(1, '2026-03-07', '14:00', '15:30', 'programado'),
(1, '2026-03-08', '08:00', '09:30', 'cancelado'),
(2, '2026-03-10', '09:00', '11:30', 'programado'),
(3, '2026-03-15', '06:00', '10:00', 'programado'),
(1, '2026-03-20', '08:00', '09:30', 'programado');


-- =============================================================
--  CABALLOS POR SALIDA
-- =============================================================
INSERT INTO salida_caballos (salida_id, horse_id)
VALUES
-- Salida 1 — completada, 3 personas → 3 caballos
(1,1), (1,2), (1,3),
-- Salida 2 — completada, 5 personas → 5 caballos
(2,1), (2,2), (2,3), (2,4), (2,5),
-- Salida 3 — completada, 3 personas → 3 caballos
(3,2), (3,3), (3,4),
-- Salida 4 — completada, 1 persona → 3 caballos disponibles (solo 1 usó)
(4,5), (4,6), (4,7),
-- Salida 5 — en curso HOY, 9 personas → 9 caballos
(5,1), (5,2), (5,3), (5,4), (5,6), (5,7), (5,8), (5,9), (5,10),
-- Salida 6 — programada HOY tarde, 3 personas → 3 caballos
(6,3), (6,4), (6,5),
-- Salida 7 — cancelada → caballos asignados antes de cancelar
(7,1), (7,2),
-- Salida 8 — futura, 5 personas → 5 caballos
(8,3), (8,4), (8,5), (8,6), (8,7),
-- Salida 9 — futura, 1 persona → 4 caballos disponibles
(9,7), (9,8), (9,9), (9,10),
-- Salida 10 — futura, 3 personas → 3 caballos
(10,1), (10,2), (10,3);


-- =============================================================
--  GUÍAS POR SALIDA (≤8 personas → 1 guía | >8 → 2 guías)
-- =============================================================
INSERT INTO salida_guias (salida_id, guia_id)
VALUES
(1, 1),          -- Salida 1: 3 personas → 1 guía
(2, 1),          -- Salida 2: 5 personas → 1 guía
(3, 2),          -- Salida 3: 3 personas → 1 guía
(4, 3),          -- Salida 4: 1 persona  → 1 guía
(5, 4), (5, 5),  -- Salida 5: 9 personas → 2 guías (>8)
(6, 1),          -- Salida 6: 3 personas → 1 guía
(7, 2),          -- Salida 7: cancelada  → guía asignado antes de cancelar
(8, 3),          -- Salida 8: 5 personas → 1 guía
(9, 4),          -- Salida 9: 1 persona  → 1 guía
(10, 5);         -- Salida 10: 3 personas → 1 guía


-- =============================================================
--  RESERVACIONES
--  client_id: 4=María, 5=Juan, 6=Sofía, 7=Andrés
--  operator_id: 2=Laura, 3=Diego
--
--  id | Salida | Cliente | Operador | Personas | Estado     | Total
--   1 |   1    |   4     |   -      |    2     | completado | $100.000
--   2 |   1    |   5     |   -      |    1     | completado |  $50.000
--   3 |   2    |   6     |   2      |    3     | completado | $255.000
--   4 |   2    |   4     |   -      |    2     | completado | $170.000
--   5 |   3    |   7     |   -      |    3     | completado | $150.000
--   6 |   4    |   5     |   -      |    1     | completado | $120.000
--   7 |   5    |   6     |   -      |    2     | en_curso   | $170.000
--   8 |   5    |   7     |   3      |    3     | en_curso   | $255.000
--   9 |   5    |   4     |   -      |    4     | en_curso   | $340.000
--  10 |   6    |   5     |   -      |    2     | reservado  | $100.000
--  11 |   6    |   6     |   -      |    1     | cancelado  |  $50.000
--  12 |   7    |   4     |   -      |    1     | cancelado  |  $50.000
--  13 |   8    |   7     |   -      |    2     | reservado  | $170.000
--  14 |   8    |   5     |   2      |    3     | reservado  | $255.000
--  15 |   9    |   6     |   -      |    1     | reservado  | $120.000
--  16 |  10    |   4     |   -      |    3     | reservado  | $150.000
-- =============================================================
INSERT INTO reservaciones
    (salida_id, client_id, operator_id, num_people, estado, precio_unitario, total)
VALUES
-- === COMPLETADAS (salidas históricas 1-4) ===
(1, 4, NULL, 2, 'completado',  50000.00,  100000.00),
(1, 5, NULL, 1, 'completado',  50000.00,   50000.00),
(2, 6,    2, 3, 'completado',  85000.00,  255000.00),
(2, 4, NULL, 2, 'completado',  85000.00,  170000.00),
(3, 7, NULL, 3, 'completado',  50000.00,  150000.00),
(4, 5, NULL, 1, 'completado', 120000.00,  120000.00),
-- === EN CURSO (salida 5 — hoy, 9 personas en total) ===
(5, 6, NULL, 2, 'en_curso',    85000.00,  170000.00),
(5, 7,    3, 3, 'en_curso',    85000.00,  255000.00),
(5, 4, NULL, 4, 'en_curso',    85000.00,  340000.00),
-- === RESERVADAS / CANCELADAS (salida 6 — hoy tarde) ===
(6, 5, NULL, 2, 'reservado',   50000.00,  100000.00),
(6, 6, NULL, 1, 'cancelado',   50000.00,   50000.00),
-- === CANCELADA (salida 7) ===
(7, 4, NULL, 1, 'cancelado',   50000.00,   50000.00),
-- === FUTURAS (salidas 8-10) ===
(8, 7, NULL, 2, 'reservado',   85000.00,  170000.00),
(8, 5,    2, 3, 'reservado',   85000.00,  255000.00),
(9, 6, NULL, 1, 'reservado',  120000.00,  120000.00),
(10, 4, NULL, 3, 'reservado',  50000.00,  150000.00);


-- =============================================================
--  PARTICIPANTES
--  Restricción: UNIQUE (reservacion_id, tipo_documento, documento)
--  El mismo documento puede repetirse en distintas reservas.
-- =============================================================
INSERT INTO participantes
    (reservacion_id, primer_nombre, primer_apellido, tipo_documento, documento, edad, altura_cm, peso_kg)
VALUES
-- Reserva 1: María + hermano (2 personas) — completado
(1,  'María',     'García',    'CEDULA',            '10234567',  32, 165, 60.50),
(1,  'Roberto',   'García',    'CEDULA',            '10234568',  35, 175, 78.00),

-- Reserva 2: Juan solo (1 persona) — completado
(2,  'Juan',      'Pérez',     'CEDULA',            '20345678',  28, 180, 82.00),

-- Reserva 3: Sofía + grupo (3 personas) — completado, via operador
(3,  'Sofía',     'Torres',    'CEDULA',            '30456789',  25, 162, 55.00),
(3,  'Lucas',     'Torres',    'CEDULA',            '30456790',  27, 170, 70.00),
(3,  'Elena',     'Martínez',  'PASAPORTE',         'AB123456',  30, 158, 52.50),

-- Reserva 4: María + amiga (2 personas) — completado
(4,  'María',     'García',    'CEDULA',            '10234567',  32, 165, 60.50),
(4,  'Ana',       'Quintero',  'CEDULA',            '40567890',  29, 168, 63.00),

-- Reserva 5: Andrés + familia (3 personas, menor de edad) — completado
(5,  'Andrés',    'López',     'CEDULA',            '50678901',  35, 176, 85.00),
(5,  'Carmen',    'López',     'CEDULA',            '50678902',  33, 160, 58.00),
(5,  'Felipe',    'López',     'TARJETA_IDENTIDAD', '100234567', 16, 168, 65.00),

-- Reserva 6: Juan solo, ruta DIFICIL (1 persona) — completado
(6,  'Juan',      'Pérez',     'CEDULA',            '20345678',  28, 180, 82.00),

-- Reserva 7: Sofía + amiga (2 personas) — en curso HOY
(7,  'Sofía',     'Torres',    'CEDULA',            '30456789',  25, 162, 55.00),
(7,  'Isabela',   'Rojas',     'CEDULA',            '60789012',  26, 165, 57.50),

-- Reserva 8: Andrés + compañeros (3 personas) — en curso HOY, via operador
(8,  'Andrés',    'López',     'CEDULA',            '50678901',  35, 176, 85.00),
(8,  'Daniel',    'Castro',    'CEDULA',            '70890123',  31, 172, 75.00),
(8,  'Marcela',   'Vargas',    'CEDULA',            '70890124',  28, 160, 60.00),

-- Reserva 9: María + familia (4 personas, menor de edad) — en curso HOY
(9,  'María',     'García',    'CEDULA',            '10234567',  32, 165, 60.50),
(9,  'Pedro',     'García',    'CEDULA',            '10234569',  36, 178, 83.00),
(9,  'Laura',     'García',    'CEDULA',            '10234570',  30, 163, 59.00),
(9,  'Santiago',  'García',    'TARJETA_IDENTIDAD', '100456789', 15, 162, 60.00),

-- Reserva 10: Juan + amigo (2 personas) — reservado tarde hoy
(10, 'Juan',      'Pérez',     'CEDULA',            '20345678',  28, 180, 82.00),
(10, 'Carlos',    'Mendoza',   'PASAPORTE',         'CD789012',  32, 175, 79.00),

-- Reserva 11: Sofía sola — CANCELADA (datos previos a la cancelación)
(11, 'Sofía',     'Torres',    'CEDULA',            '30456789',  25, 162, 55.00),

-- Reserva 12: María sola — CANCELADA (salida cancelada)
(12, 'María',     'García',    'CEDULA',            '10234567',  32, 165, 60.50),

-- Reserva 13: Andrés + pareja (2 personas) — reservado futuro
(13, 'Andrés',    'López',     'CEDULA',            '50678901',  35, 176, 85.00),
(13, 'Carmen',    'López',     'CEDULA',            '50678902',  33, 160, 58.00),

-- Reserva 14: Juan + compañeros (3 personas) — reservado futuro, via operador
(14, 'Juan',      'Pérez',     'CEDULA',            '20345678',  28, 180, 82.00),
(14, 'Paula',     'Sánchez',   'CEDULA',            '80901234',  24, 159, 54.00),
(14, 'Mario',     'Sánchez',   'CEDULA',            '80901235',  26, 173, 71.00),

-- Reserva 15: Sofía sola, ruta DIFICIL (1 persona) — reservado futuro
(15, 'Sofía',     'Torres',    'CEDULA',            '30456789',  25, 162, 55.00),

-- Reserva 16: María + familia (3 personas, adulto mayor) — reservado futuro
(16, 'María',     'García',    'CEDULA',            '10234567',  32, 165, 60.50),
(16, 'Roberto',   'García',    'CEDULA',            '10234568',  35, 175, 78.00),
(16, 'Luis',      'García',    'CEDULA',            '10234571',  60, 170, 88.00);


-- =============================================================
--  TOKENS DE RESTABLECIMIENTO DE CONTRASEÑA
--  (para probar el flujo de reset-password)
-- =============================================================
INSERT INTO password_reset_tokens (token, usuario_id, expires_at, used, created_at)
VALUES
-- Token VÁLIDO para María (cliente 4) — expira hoy a medianoche
('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 4, '2026-03-07 23:59:59', FALSE, '2026-03-07 10:00:00'),
-- Token YA USADO para Juan (cliente 5)
('b2c3d4e5-f6a7-8901-bcde-f12345678901', 5, '2026-03-07 12:00:00', TRUE,  '2026-03-07 08:00:00'),
-- Token EXPIRADO para Sofía (cliente 6) — venció ayer
('c3d4e5f6-a7b8-9012-cdef-123456789012', 6, '2026-03-06 23:59:59', FALSE, '2026-03-06 10:00:00');


-- =============================================================
--  VERIFICACIÓN RÁPIDA
-- =============================================================
SELECT 'usuarios'              AS tabla, COUNT(*) AS filas FROM usuarios
UNION ALL
SELECT 'rutas',                           COUNT(*) FROM rutas
UNION ALL
SELECT 'caballos',                        COUNT(*) FROM caballos
UNION ALL
SELECT 'guias',                           COUNT(*) FROM guias
UNION ALL
SELECT 'salidas',                         COUNT(*) FROM salidas
UNION ALL
SELECT 'salida_caballos',                 COUNT(*) FROM salida_caballos
UNION ALL
SELECT 'salida_guias',                    COUNT(*) FROM salida_guias
UNION ALL
SELECT 'reservaciones',                   COUNT(*) FROM reservaciones
UNION ALL
SELECT 'participantes',                   COUNT(*) FROM participantes
UNION ALL
SELECT 'password_reset_tokens',           COUNT(*) FROM password_reset_tokens
ORDER BY tabla;
