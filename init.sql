CREATE DATABASE usuariosdb;
CREATE DATABASE reservadb;
CREATE DATABASE auladb;
CREATE DATABASE incidenciadb;

-- Habilitar extensión btree_gist en reservadb (necesaria para el constraint de solapamiento)
\c reservadb;
CREATE EXTENSION IF NOT EXISTS btree_gist;
