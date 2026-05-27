INSERT INTO usuarios (nombre, apellido, email, password, rol)
VALUES ('Sebastián', 'Morales', 'sebas@email.com', 'Sebas123456', 'Estudiante')
ON CONFLICT (email) DO NOTHING;

INSERT INTO usuarios (nombre, apellido, email, password, rol)
VALUES ('Admin', 'Sistema', 'admin@uceva.edu.co', 'Admin123#', 'Administrador')
ON CONFLICT (email) DO NOTHING;
