-- Constraint de no solapamiento de reservas (usa la extensión btree_gist creada en init.sql)
ALTER TABLE reservas ADD CONSTRAINT no_solapamiento_reservas EXCLUDE USING gist (codigo_aula WITH =, tsrange(hora_inicio, hora_fin) WITH &&) WHERE (estado != 'CANCELADA');
