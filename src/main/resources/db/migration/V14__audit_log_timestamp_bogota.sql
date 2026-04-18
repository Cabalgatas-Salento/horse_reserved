ALTER TABLE audit_log
ALTER COLUMN ocurrido_en TYPE TIMESTAMP
USING (ocurrido_en AT TIME ZONE 'America/Bogota');

ALTER TABLE audit_log
ALTER COLUMN ocurrido_en SET DEFAULT (NOW() AT TIME ZONE 'America/Bogota');
