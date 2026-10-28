package com.ejemplo.proyectoweb.modelo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/** Modelo de persistencia: operaciones CRUD con JDBC, sin depender de la vista. */
public final class VideojuegoDAO {
    private final DataSource dataSource;

    public VideojuegoDAO() {
        this(null);
    }

    /** Permite un DataSource administrado o una base aislada de pruebas. */
    public VideojuegoDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Connection conectar() throws SQLException {
        if (dataSource != null) return dataSource.getConnection();
        String url = entorno("DB_URL", "jdbc:postgresql://localhost:5432/videojuegos?connectTimeout=5&socketTimeout=10");
        String usuario = entorno("DB_USER", "usrvideojuegos");
        String password = System.getenv("DB_PASSWORD");
        if (password == null) {
            throw new SQLException("Configura DB_PASSWORD antes de iniciar Tomcat.");
        }
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("No se encontró el controlador de PostgreSQL.", ex);
        }
        return DriverManager.getConnection(url, usuario, password);
    }

    public List<Videojuego> listar() throws SQLException {
        List<Videojuego> videojuegos = new ArrayList<>();
        String sql = "SELECT codigo, titulo, genero, plataforma FROM Videojuego ORDER BY codigo";
        try (Connection conexion = conectar();
             PreparedStatement consulta = conexion.prepareStatement(sql);
             ResultSet filas = consulta.executeQuery()) {
            while (filas.next()) {
                videojuegos.add(new Videojuego(filas.getString("codigo"), filas.getString("titulo"),
                        filas.getString("genero"), filas.getString("plataforma")));
            }
        }
        return videojuegos;
    }

    public void insertar(Videojuego videojuego) throws SQLException {
        String sql = "INSERT INTO Videojuego (codigo, titulo, genero, plataforma) VALUES (?, ?, ?, ?)";
        try (Connection conexion = conectar();
             PreparedStatement comando = conexion.prepareStatement(sql)) {
            comando.setString(1, videojuego.codigo());
            comando.setString(2, videojuego.titulo());
            comando.setString(3, videojuego.genero());
            comando.setString(4, videojuego.plataforma());
            comando.executeUpdate();
        }
    }

    /** Actualiza los datos de un código existente, que permanece como clave primaria. */
    public boolean actualizar(Videojuego videojuego) throws SQLException {
        String sql = "UPDATE Videojuego SET titulo = ?, genero = ?, plataforma = ? WHERE codigo = ?";
        try (Connection conexion = conectar();
             PreparedStatement comando = conexion.prepareStatement(sql)) {
            comando.setString(1, videojuego.titulo());
            comando.setString(2, videojuego.genero());
            comando.setString(3, videojuego.plataforma());
            comando.setString(4, videojuego.codigo());
            return comando.executeUpdate() == 1;
        }
    }

    /** Borra por clave primaria; devuelve false si el registro ya no existe. */
    public boolean borrar(String codigo) throws SQLException {
        String sql = "DELETE FROM Videojuego WHERE codigo = ?";
        try (Connection conexion = conectar();
             PreparedStatement comando = conexion.prepareStatement(sql)) {
            comando.setString(1, codigo);
            return comando.executeUpdate() == 1;
        }
    }

    private static String entorno(String nombre, String defecto) {
        String valor = System.getenv(nombre);
        return valor == null || valor.isBlank() ? defecto : valor;
    }
}
