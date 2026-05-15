-- ============================================================
-- Datos de prueba para el nuevo schema normalizado
-- Facultad → Bloque → Aula
-- ============================================================

-- Facultades (equivalente a "dependencias" del SIGA)
INSERT INTO facultades (codigo_dependencia, nombre) VALUES ('14', 'INFORMATICA') ON CONFLICT (codigo_dependencia) DO NOTHING;
INSERT INTO facultades (codigo_dependencia, nombre) VALUES ('2', 'INGENIERIA')   ON CONFLICT (codigo_dependencia) DO NOTHING;

-- Bloques (equivalente a "edificios" del SIGA)
INSERT INTO bloques (codigo_edificio, nombre, facultad_id) VALUES ('11', 'BLOQUE B - AVELLANOS', (SELECT id FROM facultades WHERE codigo_dependencia = '14')) ON CONFLICT (codigo_edificio) DO NOTHING;

-- Aulas
INSERT INTO aulas (codigo_aula, nombre_aula, capacidad, bloque_id, codigo_tipo_aula, nombre_tipo_aula, requiere_autorizacion)
VALUES (195, 'B101-PANTALLA (195)', 50, (SELECT id FROM bloques WHERE codigo_edificio = '11'), '79', 'AULA AUDIOVISUAL', true)
ON CONFLICT (codigo_aula) DO NOTHING;

INSERT INTO aulas (codigo_aula, nombre_aula, capacidad, bloque_id, codigo_tipo_aula, nombre_tipo_aula, requiere_autorizacion)
VALUES (196, 'B102-DIGITAL (196)', 50, (SELECT id FROM bloques WHERE codigo_edificio = '11'), '78', 'AULA INTERACTIVA', false)
ON CONFLICT (codigo_aula) DO NOTHING;

INSERT INTO aulas (codigo_aula, nombre_aula, capacidad, bloque_id, codigo_tipo_aula, nombre_tipo_aula, requiere_autorizacion)
VALUES (197, 'B103-DIGITAL (197)', 60, (SELECT id FROM bloques WHERE codigo_edificio = '11'), '78', 'AULA INTERACTIVA', false)
ON CONFLICT (codigo_aula) DO NOTHING;

INSERT INTO aulas (codigo_aula, nombre_aula, capacidad, bloque_id, codigo_tipo_aula, nombre_tipo_aula, requiere_autorizacion)
VALUES (198, 'B104-DIGITAL (198)', 40, (SELECT id FROM bloques WHERE codigo_edificio = '11'), '78', 'AULA INTERACTIVA', false)
ON CONFLICT (codigo_aula) DO NOTHING;

INSERT INTO aulas (codigo_aula, nombre_aula, capacidad, bloque_id, codigo_tipo_aula, nombre_tipo_aula, requiere_autorizacion)
VALUES (199, 'B105 PANTALLA (199)', 45, (SELECT id FROM bloques WHERE codigo_edificio = '11'), '79', 'AULA AUDIOVISUAL', true)
ON CONFLICT (codigo_aula) DO NOTHING;