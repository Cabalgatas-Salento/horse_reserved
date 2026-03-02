UPDATE usuarios
SET role = UPPER(role);

ALTER TABLE usuarios
DROP CONSTRAINT IF EXISTS chk_role;

ALTER TABLE usuarios
    ADD CONSTRAINT chk_role
        CHECK (role IN ('CLIENTE', 'OPERADOR', 'ADMINISTRADOR'));