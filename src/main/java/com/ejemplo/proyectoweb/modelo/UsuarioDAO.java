package com.ejemplo.proyectoweb.modelo;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.sql.DataSource;

/** Consulta usuarios de la base; nunca conserva la contraseña en la sesión. */
public final class UsuarioDAO {
    private static final int ITERACIONES = 210_000;
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private final DataSource dataSource;

    public UsuarioDAO() { this(null); }
    public UsuarioDAO(DataSource dataSource) { this.dataSource = dataSource; }

    public record Usuario(String clave, String nombre) { }

    public Usuario validar(String clave, String password) throws SQLException {
        if (clave == null || clave.isBlank() || password == null) return null;
        String sql = "SELECT clave, nombre, password_hash FROM usuarios_app WHERE clave = ? AND activo = TRUE";
        try (Connection conexion = conectar(); PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setString(1, clave.strip());
            try (ResultSet fila = consulta.executeQuery()) {
                if (!fila.next() || !verificar(password, fila.getString("password_hash"))) return null;
                return new Usuario(fila.getString("clave"), fila.getString("nombre"));
            }
        }
    }

    private Connection conectar() throws SQLException {
        if (dataSource != null) return dataSource.getConnection();
        String url = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/videojuegos?connectTimeout=5&socketTimeout=10");
        String usuario = System.getenv().getOrDefault("DB_USER", "usrvideojuegos");
        String password = System.getenv("DB_PASSWORD");
        if (password == null) throw new SQLException("Configura DB_PASSWORD antes de iniciar Tomcat.");
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("No se encontró el controlador de PostgreSQL.", ex);
        }
        return DriverManager.getConnection(url, usuario, password);
    }

    public static String hash(String password) {
        byte[] sal = new byte[16];
        ALEATORIO.nextBytes(sal);
        return "pbkdf2-sha256$" + ITERACIONES + "$" + Base64.getEncoder().encodeToString(sal)
                + "$" + Base64.getEncoder().encodeToString(derivar(password, sal, ITERACIONES));
    }

    private static boolean verificar(String password, String almacenado) {
        try {
            String[] partes = almacenado.split("\\$", -1);
            if (partes.length != 4 || !"pbkdf2-sha256".equals(partes[0])) return false;
            int iteraciones = Integer.parseInt(partes[1]);
            if (iteraciones < 100_000 || iteraciones > 2_000_000) return false;
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            if (sal.length != 16 || esperado.length != 32) return false;
            return MessageDigest.isEqual(esperado, derivar(password, sal, iteraciones));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static byte[] derivar(String password, byte[] sal, int iteraciones) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), sal, iteraciones, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("PBKDF2 no está disponible", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
