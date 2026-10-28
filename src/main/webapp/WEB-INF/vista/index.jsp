<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="java.util.List,java.util.Map,java.net.URLEncoder,java.nio.charset.StandardCharsets,com.ejemplo.proyectoweb.modelo.Videojuego,com.ejemplo.proyectoweb.modelo.UsuarioDAO" %>
<%!
    private String html(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    List<Videojuego> videojuegos = (List<Videojuego>) request.getAttribute("videojuegos");
    Map<String, String> formulario = (Map<String, String>) request.getAttribute("formulario");
    String errorFormulario = (String) request.getAttribute("errorFormulario");
    String errorListado = (String) request.getAttribute("errorListado");
    String mensaje = (String) request.getAttribute("mensaje");
    String ruta = request.getContextPath() + "/videojuegos";
    String csrf = (String) request.getAttribute("csrf");
    boolean modoEditar = Boolean.TRUE.equals(request.getAttribute("modoEditar"));
    UsuarioDAO.Usuario usuario = (UsuarioDAO.Usuario) request.getSession(false).getAttribute("usuario");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Catálogo de videojuegos - Sesión de <%= html(usuario.nombre()) %></title>
    <style>
        body { font-family: system-ui, sans-serif; background: #f3f5f8; color: #172b4d; margin: 0; padding: 32px 16px; }
        main { max-width: 900px; margin: auto; background: white; padding: 24px; border-radius: 12px; }
        h1 { margin-top: 0; } .tabla { overflow-x: auto; }
        table { width: 100%; border-collapse: collapse; }
        caption { text-align: left; padding-bottom: 16px; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #dde3eb; }
        th { background: #eaf0f8; } .error { padding: 16px; background: #fff0ef; color: #8c2424; }
        a { color: #164fa3; }
        form.editor { display: grid; gap: 14px; margin-bottom: 28px; }
        label { display: grid; gap: 6px; font-weight: 600; }
        input { padding: 10px; border: 1px solid #aab8ca; border-radius: 6px; font: inherit; min-width: 0; }
        button { padding: 12px; background: #164fa3; color: white; border: 0; border-radius: 6px; font: inherit; cursor: pointer; }
        .exito { padding: 16px; background: #e8f6ed; color: #17592f; }
        .acciones { white-space: nowrap; }
        .acciones form { display: inline; margin-left: 8px; }
        .acciones button { background: #9d2828; padding: 6px 10px; }
    </style>
</head>
<body>
<main>
    <h1>Catálogo de videojuegos</h1>
    <p>Sesión de <strong><%= html(usuario.nombre()) %></strong> · <a href="<%= html(request.getContextPath() + "/srvCerrarSesion") %>">Cerrar sesión</a></p>
    <% if (mensaje != null) { %>
        <p class="exito" role="status"><%= html(mensaje) %></p>
    <% } %>
    <h2><%= modoEditar ? "Modificar videojuego" : "Agregar videojuego" %></h2>
    <% if (errorFormulario != null) { %>
        <p class="error" role="alert"><%= html(errorFormulario) %></p>
    <% } %>
    <form class="editor" action="<%= html(request.getContextPath() + (modoEditar ? "/srvModificarVideojuego" : "/srvAgregarVideojuego")) %>" method="post" accept-charset="UTF-8">
        <input type="hidden" name="csrf" value="<%= html(csrf) %>">
        <% if (modoEditar) { %>
            <input type="hidden" name="codigo" value="<%= html(formulario.get("codigo")) %>">
            <p>Código: <strong><%= html(formulario.get("codigo")) %></strong></p>
        <% } else { %>
        <label for="codigo">Código
            <input id="codigo" name="codigo" maxlength="30" required value="<%= html(formulario.get("codigo")) %>" placeholder="VG011">
        </label>
        <% } %>
        <label for="titulo">Título
            <input id="titulo" name="titulo" maxlength="100" required value="<%= html(formulario.get("titulo")) %>">
        </label>
        <label for="genero">Género
            <input id="genero" name="genero" maxlength="100" required value="<%= html(formulario.get("genero")) %>">
        </label>
        <label for="plataforma">Plataforma
            <input id="plataforma" name="plataforma" maxlength="100" required value="<%= html(formulario.get("plataforma")) %>">
        </label>
        <button type="submit"><%= modoEditar ? "Guardar cambios" : "Guardar videojuego" %></button>
        <% if (modoEditar) { %><a href="<%= html(ruta) %>">Cancelar edición</a><% } %>
    </form>
    <% if (errorListado != null) { %>
        <p class="error" role="alert"><%= html(errorListado) %></p>
    <% } else { %>
        <div class="tabla">
            <table>
                <caption>Videojuegos registrados: <%= videojuegos.size() %></caption>
                <thead><tr><th scope="col">Código</th><th scope="col">Título</th><th scope="col">Género</th><th scope="col">Plataforma</th><th scope="col">Acciones</th></tr></thead>
                <tbody>
                <% for (Videojuego videojuego : videojuegos) { %>
                    <tr>
                        <td><%= html(videojuego.codigo()) %></td>
                        <td><%= html(videojuego.titulo()) %></td>
                        <td><%= html(videojuego.genero()) %></td>
                        <td><%= html(videojuego.plataforma()) %></td>
                        <td class="acciones">
                            <a href="<%= html(ruta + "?editar=" + URLEncoder.encode(videojuego.codigo(), StandardCharsets.UTF_8)) %>">Modificar</a>
                            <form action="<%= html(request.getContextPath() + "/srvBorrarVideojuego") %>" method="post" onsubmit="return confirm('¿Borrar este videojuego?');">
                                <input type="hidden" name="csrf" value="<%= html(csrf) %>">
                                <input type="hidden" name="codigo" value="<%= html(videojuego.codigo()) %>">
                                <button type="submit" aria-label="Borrar <%= html(videojuego.codigo()) %>">Borrar</button>
                            </form>
                        </td>
                    </tr>
                <% } %>
                <% if (videojuegos.isEmpty()) { %>
                    <tr><td colspan="5">No hay videojuegos registrados.</td></tr>
                <% } %>
                </tbody>
            </table>
        </div>
    <% } %>
    <p><a href="<%= html(ruta) %>">Actualizar listado</a></p>
</main>
</body>
</html>
