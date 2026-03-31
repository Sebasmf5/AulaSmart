CREATE EXTENSION IF NOT EXISTS btree_gist;
ALTER TABLE reservas ADD CONSTRAINT no_solapamiento_reservas EXCLUDE USING gist (codigo_aula WITH =, tsrange(hora_inicio, hora_fin) WITH &&) WHERE (estado != 'CANCELADA');

INSERT INTO reservas (codigo_aula, hora_inicio, hora_fin, estado, id_solicitante, rol_solicitante, codigo_programa, grupo, version) VALUES (195, '2026-04-10 08:00:00', '2026-04-10 10:00:00', 'CONFIRMADA', 12345, 'DOCENTE', '101', 'G1', 0);
INSERT INTO reservas (codigo_aula, hora_inicio, hora_fin, estado, id_solicitante, rol_solicitante, codigo_programa, grupo, version) VALUES (196, '2026-04-10 10:00:00', '2026-04-10 12:00:00', 'PENDIENTE', 54321, 'ESTUDIANTE', '102', 'G2', 0);
INSERT INTO reservas (codigo_aula, hora_inicio, hora_fin, estado, id_solicitante, rol_solicitante, codigo_programa, grupo, version) VALUES (197, '2026-04-11 14:00:00', '2026-04-11 16:00:00', 'CONFIRMADA', 98765, 'ADMINISTRATIVO', '103', 'G1', 0);
INSERT INTO reservas (codigo_aula, hora_inicio, hora_fin, estado, id_solicitante, rol_solicitante, codigo_programa, grupo, version) VALUES (198, '2026-04-12 08:00:00', '2026-04-12 12:00:00', 'FINALIZADA', 11223, 'DOCENTE', '101', 'G3', 0);
INSERT INTO reservas (codigo_aula, hora_inicio, hora_fin, estado, id_solicitante, rol_solicitante, codigo_programa, grupo, version) VALUES (199, '2026-04-13 18:00:00', '2026-04-13 20:00:00', 'CANCELADA', 33445, 'ESTUDIANTE', '104', 'G4', 0);
