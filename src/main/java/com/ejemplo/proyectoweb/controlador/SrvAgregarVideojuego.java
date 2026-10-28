package com.ejemplo.proyectoweb.controlador;

import com.ejemplo.proyectoweb.modelo.Videojuego;
import com.ejemplo.proyectoweb.modelo.VideojuegoDAO;
import com.ejemplo.proyectoweb.modelo.UsuarioDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Controlador MVC: las operaciones de escritura solo aceptan POST. */
@WebServlet(name = "srvAgregarVideojuego", urlPatterns = {
        "/videojuegos", "/srvAgregarVideojuego", "/srvModificarVideojuego", "/srvBorrarVideojuego"
})
public class SrvAgregarVideojuego extends HttpServlet {
    private static final String TOKEN_SESION = "csrfVideojuegos";
    private final VideojuegoDAO dao;

    public SrvAgregarVideojuego() {
        this(new VideojuegoDAO());
    }

    public SrvAgregarVideojuego(VideojuegoDAO dao) {
        this.dao = dao;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!sesionValida(request, response)) return;
        switch (request.getParameter("resultado") == null ? "" : request.getParameter("resultado")) {
            case "creado" -> request.setAttribute("mensaje", "Videojuego agregado correctamente.");
            case "modificado" -> request.setAttribute("mensaje", "Videojuego modificado correctamente.");
            case "borrado" -> request.setAttribute("mensaje", "Videojuego borrado correctamente.");
            default -> { }
        }
        mostrar(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!sesionValida(request, response)) return;
        request.setCharacterEncoding("UTF-8");
        String ruta = request.getServletPath();
        if (!ruta.equals("/srvAgregarVideojuego")
                && !ruta.equals("/srvModificarVideojuego")
                && !ruta.equals("/srvBorrarVideojuego")) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        HttpSession sesion = request.getSession(false);
        if (sesion == null || !(sesion.getAttribute(TOKEN_SESION) instanceof String token)
                || !token.equals(request.getParameter("csrf"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Formulario no válido. Vuelve a cargar la página.");
            return;
        }

        boolean borrar = ruta.equals("/srvBorrarVideojuego");
        boolean modificar = ruta.equals("/srvModificarVideojuego");
        Map<String, String> formulario = null;
        if (!borrar) {
            formulario = Map.of(
                    "codigo", parametro(request, "codigo"), "titulo", parametro(request, "titulo"),
                    "genero", parametro(request, "genero"), "plataforma", parametro(request, "plataforma"));
            request.setAttribute("formulario", formulario);
            request.setAttribute("modoEditar", modificar);
        }
        try {
            if (borrar) {
                String codigo = parametro(request, "codigo").strip();
                if (codigo.isEmpty() || codigo.length() > 30) {
                    throw new IllegalArgumentException("Indica un código válido para borrar.");
                }
                if (!dao.borrar(codigo)) {
                    request.setAttribute("errorFormulario", "El videojuego ya no existe.");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                } else {
                    redirigir(request, response, "borrado");
                    return;
                }
            } else {
                Videojuego videojuego = new Videojuego(formulario.get("codigo"), formulario.get("titulo"),
                        formulario.get("genero"), formulario.get("plataforma"));
                if (modificar) {
                    if (!dao.actualizar(videojuego)) {
                        request.setAttribute("errorFormulario", "El videojuego ya no existe.");
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    } else {
                        redirigir(request, response, "modificado");
                        return;
                    }
                } else {
                    dao.insertar(videojuego);
                    redirigir(request, response, "creado");
                    return;
                }
            }
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("errorFormulario", ex.getMessage());
        } catch (SQLException ex) {
            if (!borrar && !modificar && "23505".equals(ex.getSQLState())) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                request.setAttribute("errorFormulario", "Ese código ya existe. Usa otro código.");
            } else {
                getServletContext().log("No se pudo procesar el videojuego", ex);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                request.setAttribute("errorFormulario", "No se pudo completar la operación. Revisa la conexión y los permisos de PostgreSQL.");
            }
        }
        mostrar(request, response);
    }

    private void redirigir(HttpServletRequest request, HttpServletResponse response, String resultado) {
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setHeader("Location", response.encodeRedirectURL(
                request.getContextPath() + "/videojuegos?resultado=" + resultado));
    }

    private void mostrar(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");
        HttpSession sesion = request.getSession(true);
        String token = (String) sesion.getAttribute(TOKEN_SESION);
        if (token == null) {
            token = UUID.randomUUID().toString();
            sesion.setAttribute(TOKEN_SESION, token);
        }
        request.setAttribute("csrf", token);
        try {
            List<Videojuego> videojuegos = dao.listar();
            request.setAttribute("videojuegos", videojuegos);
            if (request.getAttribute("formulario") == null) {
                String codigoEditar = request.getParameter("editar");
                if (codigoEditar != null) {
                    Videojuego elegido = videojuegos.stream().filter(v -> v.codigo().equals(codigoEditar))
                            .findFirst().orElse(null);
                    if (elegido == null) {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        request.setAttribute("errorFormulario", "El videojuego ya no existe.");
                    } else {
                        request.setAttribute("modoEditar", true);
                        request.setAttribute("formulario", Map.of(
                                "codigo", elegido.codigo(), "titulo", elegido.titulo(),
                                "genero", elegido.genero(), "plataforma", elegido.plataforma()));
                    }
                }
            }
        } catch (SQLException | IllegalArgumentException ex) {
            getServletContext().log("No se pudo consultar la tabla Videojuego", ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("videojuegos", List.of());
            request.setAttribute("errorListado", "No se pudieron cargar los videojuegos. Revisa la conexión con PostgreSQL.");
        }
        if (request.getAttribute("formulario") == null) {
            request.setAttribute("formulario",
                    Map.of("codigo", "", "titulo", "", "genero", "", "plataforma", ""));
        }
        request.getRequestDispatcher("/WEB-INF/vista/index.jsp").forward(request, response);
    }

    private static String parametro(HttpServletRequest request, String nombre) {
        String valor = request.getParameter(nombre);
        return valor == null ? "" : valor;
    }

    private static boolean sesionValida(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession sesion = request.getSession(false);
        if (sesion != null && sesion.getAttribute("usuario") instanceof UsuarioDAO.Usuario) return true;
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setHeader("Location", response.encodeRedirectURL(request.getContextPath() + "/index.jsp?estado=sin-sesion"));
        return false;
    }
}
