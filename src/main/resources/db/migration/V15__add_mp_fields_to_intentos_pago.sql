ALTER TABLE intentos_pago
    ADD COLUMN mp_preference_id  VARCHAR(120),
    ADD COLUMN mp_payment_id     VARCHAR(80),
    ADD COLUMN mp_payment_status VARCHAR(30);

CREATE INDEX idx_intentos_pago_mp_payment ON intentos_pago(mp_payment_id)
    WHERE mp_payment_id IS NOT NULL;

CREATE INDEX idx_intentos_pago_mp_preference ON intentos_pago(mp_preference_id)
    WHERE mp_preference_id IS NOT NULL;
