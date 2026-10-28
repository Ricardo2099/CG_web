<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.UUID,com.ejemplo.proyectoweb.modelo.UsuarioDAO" %>
<%!
    private String html(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    UsuarioDAO.Usuario usuario = session.getAttribute("usuario") instanceof UsuarioDAO.Usuario u ? u : null;
    String csrf = (String) session.getAttribute("csrfLogin");
    if (csrf == null) {
        csrf = UUID.randomUUID().toString();
        session.setAttribute("csrfLogin", csrf);
    }
    String estado = request.getParameter("estado");
    String mensaje = switch (estado == null ? "" : estado) {
        case "usuario-invalido" -> "Usuario o contraseña incorrectos.";
        case "sin-base" -> "No se encontró la base de datos o no se pudo establecer la conexión. Revisa PostgreSQL y la configuración.";
        case "sin-sesion" -> "Inicia sesión para acceder al formulario y la tabla.";
        case "sesion-cerrada" -> "La sesión se cerró correctamente.";
        default -> null;
    };
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= usuario == null ? "Iniciar sesión" : "Sesión de " + html(usuario.nombre()) %> - Videojuegos</title>
    <style>
        body { font-family: system-ui, sans-serif; background:#f3f5f8; color:#172b4d; margin:0; padding:32px 16px; }
        main { max-width:520px; margin:auto; background:white; padding:24px; border-radius:12px; }
        label { display:grid; gap:6px; margin:14px 0; font-weight:600; }
        input { padding:10px; font:inherit; border:1px solid #aab8ca; border-radius:6px; }
        button { padding:12px; color:white; background:#164fa3; border:0; border-radius:6px; cursor:pointer; }
        .mensaje { padding:12px; background:#eaf0f8; }
    </style>
</head>
<body><main>
    <h1><%= usuario == null ? "Iniciar sesión" : "Bienvenido, " + html(usuario.nombre()) %></h1>
    <% if (mensaje != null) { %><p class="mensaje" role="status"><%= html(mensaje) %></p><% } %>
    <% if (usuario == null) { %>
        <form method="post" action="<%= html(request.getContextPath() + "/srvValidarSesion") %>" accept-charset="UTF-8">
            <input type="hidden" name="csrf" value="<%= html(csrf) %>">
            <label for="clave">Usuario<input id="clave" name="clave" maxlength="60" required autocomplete="username"></label>
            <label for="password">Contraseña<input id="password" name="password" type="password" required autocomplete="current-password"></label>
            <button type="submit">Entrar</button>
        </form>
    <% } else { %>
        <p><a href="<%= html(request.getContextPath() + "/videojuegos") %>">Ver tabla y formulario</a></p>
        <p><a href="<%= html(request.getContextPath() + "/srvCerrarSesion") %>">Cerrar sesión</a></p>
    <% } %>
</main></body></html>
