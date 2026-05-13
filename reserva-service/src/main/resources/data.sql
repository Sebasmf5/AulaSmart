-- Constraint de no solapamiento de reservas (usa la extensión btree_gist creada en init.sql)
-- NOTA: Se usa 'aula_id' (PK del aula-service) en lugar de 'codigo_aula' (pasaporte SIGA)
-- para mantener integridad referencial real.
ALTER TABLE reservas DROP CONSTRAINT IF EXISTS no_solapamiento_reservas;
ALTER TABLE reservas ADD CONSTRAINT no_solapamiento_reservas EXCLUDE USING gist (aula_id WITH =, tsrange(hora_inicio, hora_fin) WITH &&) WHERE (estado != 'CANCELADA');
