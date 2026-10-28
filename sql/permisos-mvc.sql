-- Ejecutar como postgres, conectado a la base videojuegos.
-- Mantiene los registros de la práctica anterior.
GRANT CONNECT ON DATABASE videojuegos TO usrvideojuegos;
GRANT USAGE ON SCHEMA public TO usrvideojuegos;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.Videojuego TO usrvideojuegos;
