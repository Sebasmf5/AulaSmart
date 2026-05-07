INSERT INTO usuarios (nombre, apellido, email, password, rol)
VALUES ('Sebastián', 'Morales', 'sebas@email.com', 'Sebas123456', 'Estudiante')
ON CONFLICT (email) DO UPDATE SET password = EXCLUDED.password;

INSERT INTO usuarios (nombre, apellido, email, password, rol)
VALUES ('Admin', 'Sistema', 'admin@uceva.edu.co', 'Admin123#', 'Administrativo')
ON CONFLICT (email) DO UPDATE SET password = EXCLUDED.password;
