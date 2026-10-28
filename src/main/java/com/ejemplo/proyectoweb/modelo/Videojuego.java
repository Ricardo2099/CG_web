package com.ejemplo.proyectoweb.modelo;

/** Datos y reglas del modelo, independientes de HTTP y de la JSP. */
public record Videojuego(String codigo, String titulo, String genero, String plataforma) {
    public Videojuego {
        codigo = validar(codigo, "El código", 30);
        titulo = validar(titulo, "El título", 100);
        genero = validar(genero, "El género", 100);
        plataforma = validar(plataforma, "La plataforma", 100);
    }

    private static String validar(String valor, String campo, int limite) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " es obligatorio.");
        }
        String limpio = valor.strip();
        if (limpio.length() > limite) {
            throw new IllegalArgumentException(campo + " admite como máximo " + limite + " caracteres.");
        }
        return limpio;
    }
}
