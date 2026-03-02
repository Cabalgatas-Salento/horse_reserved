-- Migración para corregir los tipos de datos enum a VARCHAR para compatibilidad con JPA

-- Modificar la tabla usuarios para usar VARCHAR en lugar de enums
ALTER TABLE usuarios
    ALTER COLUMN tipo_documento TYPE VARCHAR(50),
    ALTER COLUMN role TYPE VARCHAR(50);

-- Modificar la tabla reservaciones para usar VARCHAR en lugar de enum
ALTER TABLE reservaciones
    ALTER COLUMN estado TYPE VARCHAR(50);

-- Modificar la tabla salidas para usar VARCHAR en lugar de enum
ALTER TABLE salidas
    ALTER COLUMN estado TYPE VARCHAR(50);

-- Modificar la tabla rutas para usar VARCHAR en lugar de enum
ALTER TABLE rutas
    ALTER COLUMN dificultad TYPE VARCHAR(50);

-- Modificar la tabla participantes para usar VARCHAR en lugar de enum
ALTER TABLE participantes
    ALTER COLUMN tipo_documento TYPE VARCHAR(50);

-- Eliminar los tipos enum que ya no se necesitan
DROP TYPE IF EXISTS rol_usuario CASCADE;
DROP TYPE IF EXISTS reserva_estado CASCADE;
DROP TYPE IF EXISTS salida_estado CASCADE;
DROP TYPE IF EXISTS nivel_dificultad CASCADE;
DROP TYPE IF EXISTS tipo_documento CASCADE;

-- Agregar constraints para validar los valores permitidos
ALTER TABLE usuarios
    ADD CONSTRAINT chk_tipo_documento_usuario
        CHECK (tipo_documento IN ('CEDULA', 'PASAPORTE', 'TARJETA_IDENTIDAD'));

ALTER TABLE usuarios
    ADD CONSTRAINT chk_role
        CHECK (role IN ('cliente', 'operador', 'administrador'));

ALTER TABLE reservaciones
    ADD CONSTRAINT chk_estado_reservacion
        CHECK (estado IN ('reservado', 'en_curso', 'cancelado', 'completado'));

ALTER TABLE salidas
    ADD CONSTRAINT chk_estado_salida
        CHECK (estado IN ('programado', 'en_curso', 'completado', 'cancelado'));

ALTER TABLE rutas
    ADD CONSTRAINT chk_dificultad
        CHECK (dificultad IN ('FACIL', 'MODERADO', 'DIFICIL'));

ALTER TABLE participantes
    ADD CONSTRAINT chk_tipo_documento_participante
        CHECK (tipo_documento IN ('CEDULA', 'PASAPORTE', 'TARJETA_IDENTIDAD'));