-- Ejecutar como postgres en la base videojuegos, una sola vez.
CREATE TABLE IF NOT EXISTS usuarios_app (
    clave VARCHAR(60) PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    password_hash VARCHAR(200) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);
GRANT SELECT ON usuarios_app TO usrvideojuegos;
-- Después de crear la tabla, generar un INSERT con CrearUsuario y ejecutarlo como postgres.
