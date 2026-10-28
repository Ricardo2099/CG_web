package com.ejemplo.proyectoweb.modelo;

import java.io.Console;

/** Genera el INSERT inicial sin guardar la contraseña en texto plano. */
public final class CrearUsuario {
    private CrearUsuario() { }

    public static void main(String[] args) {
        Console consola = System.console();
        if (consola == null) throw new IllegalStateException("Ejecuta java desde una terminal interactiva.");
        String clave = consola.readLine("Usuario: ").strip();
        String nombre = consola.readLine("Nombre a mostrar: ").strip();
        char[] password = consola.readPassword("Contraseña: ");
        if (clave.isBlank() || nombre.isBlank() || password.length < 8)
            throw new IllegalArgumentException("Usuario y nombre son obligatorios; contraseña de al menos 8 caracteres.");
        String hash = UsuarioDAO.hash(new String(password));
        java.util.Arrays.fill(password, '\0');
        System.out.println("INSERT INTO usuarios_app (clave, nombre, password_hash) VALUES ('"
                + sql(clave) + "', '" + sql(nombre) + "', '" + hash + "');");
    }

    private static String sql(String valor) { return valor.replace("'", "''"); }
}
