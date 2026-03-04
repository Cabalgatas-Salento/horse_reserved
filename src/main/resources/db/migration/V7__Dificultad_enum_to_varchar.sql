-- La columna dificultad ya es VARCHAR pero tiene un CHECK constraint con MODERADO.
-- Este script adapta la BD al enum Java: FACIL, MEDIA, DIFICIL.

-- 1. Eliminar el CHECK constraint que usa MODERADO
ALTER TABLE rutas DROP CONSTRAINT chk_dificultad;

-- 2. Renombrar MODERADO → MEDIA en datos existentes
UPDATE rutas SET dificultad = 'MEDIA' WHERE dificultad = 'MODERADO';

-- 3. Agregar nuevo CHECK constraint con los valores del enum Java
ALTER TABLE rutas
    ADD CONSTRAINT chk_dificultad CHECK (dificultad IN ('FACIL', 'MEDIA', 'DIFICIL'));

-- 4. Eliminar el tipo ENUM de PostgreSQL si aún existe
DROP TYPE IF EXISTS nivel_dificultad;
