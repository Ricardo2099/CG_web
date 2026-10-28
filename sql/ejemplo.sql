-- PostgreSQL: ejecutar conectado a la base videojuegos como administrador.
-- Para una base existente usa solamente permisos-mvc.sql; no necesitas volver a cargar datos.

CREATE TABLE IF NOT EXISTS Videojuego (
    codigo VARCHAR(30) PRIMARY KEY,
    titulo VARCHAR(100) NOT NULL,
    genero VARCHAR(100) NOT NULL,
    plataforma VARCHAR(100) NOT NULL
);

-- Conserva los registros que ya tengan estos códigos.
INSERT INTO Videojuego (codigo, titulo, genero, plataforma) VALUES
('VG001', 'Minecraft', 'Supervivencia y construcción', 'PC'),
('VG002', 'Stardew Valley', 'Simulación y rol', 'PC'),
('VG003', 'Hollow Knight', 'Acción y aventura', 'PC'),
('VG004', 'Mario Kart 8 Deluxe', 'Carreras', 'Nintendo Switch'),
('VG005', 'The Legend of Zelda: Breath of the Wild', 'Aventura', 'Nintendo Switch')
ON CONFLICT (codigo) DO NOTHING;

-- Crea una cuenta de la aplicacion con tu contraseña, si aún no tienes una:
-- CREATE USER usrvideojuegos WITH PASSWORD 'TU_CONTRASENA';
-- GRANT CONNECT ON DATABASE videojuegos TO usrvideojuegos;
-- GRANT USAGE ON SCHEMA public TO usrvideojuegos;
-- GRANT SELECT, INSERT ON TABLE public.Videojuego TO usrvideojuegos;
