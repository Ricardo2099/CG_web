package com.ejemplo.proyectoweb.controlador;

import com.ejemplo.proyectoweb.modelo.UsuarioDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/srvValidarSesion")
public final class SrvValidarSesion extends HttpServlet {
    private final UsuarioDAO dao;
    public SrvValidarSesion() { this(new UsuarioDAO()); }
    public SrvValidarSesion(UsuarioDAO dao) { this.dao = dao; }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession sesion = request.getSession(false);
        if (sesion == null || !(sesion.getAttribute("csrfLogin") instanceof String token)
                || !token.equals(request.getParameter("csrf"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Formulario de acceso no válido.");
            return;
        }
        String clave = request.getParameter("clave");
        String password = request.getParameter("password");
        try {
            UsuarioDAO.Usuario usuario = dao.validar(clave, password);
            if (usuario == null) {
                redirigir(request, response, "usuario-invalido", "/index.jsp");
                return;
            }
            request.changeSessionId();
            sesion.setAttribute("usuario", usuario);
            sesion.removeAttribute("csrfLogin");
            redirigir(request, response, null, "/videojuegos");
        } catch (SQLException ex) {
            getServletContext().log("No se pudo validar el usuario en PostgreSQL", ex);
            redirigir(request, response, "sin-base", "/index.jsp");
        }
    }

    private static void redirigir(HttpServletRequest request, HttpServletResponse response, String estado, String ruta) {
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setHeader("Location", response.encodeRedirectURL(request.getContextPath() + ruta
                + (estado == null ? "" : "?estado=" + estado)));
    }
}
