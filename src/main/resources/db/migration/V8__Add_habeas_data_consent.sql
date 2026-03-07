ALTER TABLE usuarios
    ADD COLUMN habeas_data_consented     BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN habeas_data_consented_at  TIMESTAMPTZ;
