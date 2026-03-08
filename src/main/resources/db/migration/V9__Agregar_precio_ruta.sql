ALTER TABLE rutas
ADD COLUMN precio NUMERIC(10,2) NOT NULL CHECK (precio >= 0);

ALTER TABLE reservaciones
ADD COLUMN precio_unitario NUMERIC(10,2) NOT NULL CHECK (precio_unitario >= 0);

ALTER TABLE reservaciones
    ADD COLUMN total NUMERIC(20,2) NOT NULL CHECK (precio_unitario >= 0);