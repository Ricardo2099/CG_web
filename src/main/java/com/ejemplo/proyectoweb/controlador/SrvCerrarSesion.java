package com.ejemplo.proyectoweb.controlador;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/srvCerrarSesion")
public final class SrvCerrarSesion extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession sesion = request.getSession(false);
        if (sesion != null) sesion.invalidate();
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setHeader("Location", response.encodeRedirectURL(request.getContextPath() + "/index.jsp?estado=sesion-cerrada"));
    }
}
