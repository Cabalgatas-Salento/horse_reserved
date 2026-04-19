-- Restaura las columnas de Mercado Pago eliminadas en V16
ALTER TABLE intentos_pago
    ADD COLUMN IF NOT EXISTS mp_preference_id  VARCHAR(120),
    ADD COLUMN IF NOT EXISTS mp_payment_id     VARCHAR(80),
    ADD COLUMN IF NOT EXISTS mp_payment_status VARCHAR(30);

CREATE INDEX IF NOT EXISTS idx_intentos_pago_mp_payment_id
    ON intentos_pago (mp_payment_id) WHERE mp_payment_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_intentos_pago_mp_preference_id
    ON intentos_pago (mp_preference_id) WHERE mp_preference_id IS NOT NULL;
