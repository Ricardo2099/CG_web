package com.ejemplo.proyectoweb;

import java.net.URI;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.ejemplo.proyectoweb.controlador.SrvAgregarVideojuego;
import com.ejemplo.proyectoweb.controlador.SrvValidarSesion;
import com.ejemplo.proyectoweb.controlador.SrvCerrarSesion;
import com.ejemplo.proyectoweb.modelo.UsuarioDAO;
import com.ejemplo.proyectoweb.modelo.VideojuegoDAO;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** HTTP real + JSP compilada en Tomcat + JDBC a una base desechable, sin datos del usuario. */
class VideojuegoMvcTest {
    @TempDir static Path temp;
    static Tomcat tomcat;
    static JdbcDataSource dataSource;
    static VideojuegoDAO dao;
    static String base;
    static HttpClient client;

    @BeforeAll
    static void iniciar() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dao = new VideojuegoDAO(dataSource);
        tomcat = new Tomcat();
        tomcat.setBaseDir(temp.toString());
        tomcat.setPort(0);
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        Context context = tomcat.addWebapp("/proyecto-web", Path.of("src/main/webapp").toAbsolutePath().toString());
        context.setParentClassLoader(VideojuegoMvcTest.class.getClassLoader());
        context.setIgnoreAnnotations(true);
        Tomcat.addServlet(context, "videojuegos-test", new SrvAgregarVideojuego(dao));
        Tomcat.addServlet(context, "login-test", new SrvValidarSesion(new UsuarioDAO(dataSource)));
        Tomcat.addServlet(context, "logout-test", new SrvCerrarSesion());
        context.addServletMappingDecoded("/srvValidarSesion", "login-test");
        context.addServletMappingDecoded("/srvCerrarSesion", "logout-test");
        context.addServletMappingDecoded("/videojuegos", "videojuegos-test");
        context.addServletMappingDecoded("/srvAgregarVideojuego", "videojuegos-test");
        context.addServletMappingDecoded("/srvModificarVideojuego", "videojuegos-test");
        context.addServletMappingDecoded("/srvBorrarVideojuego", "videojuegos-test");
        tomcat.start();
        base = "http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + "/proyecto-web";
        client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(10)).build();
    }

    @BeforeEach
    void prepararTabla() throws Exception {
        sql("CREATE TABLE IF NOT EXISTS usuarios_app (clave VARCHAR(60) PRIMARY KEY, nombre VARCHAR(100) NOT NULL, password_hash VARCHAR(200) NOT NULL, activo BOOLEAN NOT NULL DEFAULT TRUE)");
        sql("DELETE FROM usuarios_app");
        try (var c = dataSource.getConnection(); var p = c.prepareStatement("INSERT INTO usuarios_app (clave, nombre, password_hash) VALUES (?, ?, ?)")) {
            p.setString(1, "alumno"); p.setString(2, "Alumno de prueba"); p.setString(3, UsuarioDAO.hash("clave-segura")); p.executeUpdate();
        }
        sql("CREATE TABLE IF NOT EXISTS Videojuego (codigo VARCHAR(30) PRIMARY KEY, titulo VARCHAR(100) NOT NULL, genero VARCHAR(100) NOT NULL, plataforma VARCHAR(100) NOT NULL)");
        sql("DELETE FROM Videojuego");
        get("/srvCerrarSesion");
        login("alumno", "clave-segura");
    }

    @AfterAll
    static void detener() throws Exception {
        if (tomcat != null) { tomcat.stop(); tomcat.destroy(); }
        if (dataSource != null) sql("SHUTDOWN");
    }

    @Test
    void entradaAnteriorRedirigeYVistaEsPrivada() throws Exception {
        assertEquals(200, get("/index.jsp").statusCode());
        assertTrue(get("/").body().contains("Cerrar sesión"));
        assertEquals(404, get("/WEB-INF/vista/index.jsp").statusCode());
        var pagina = get("/videojuegos");
        assertEquals(200, pagina.statusCode());
        assertTrue(pagina.body().contains("Guardar videojuego"));
        assertTrue(pagina.body().contains("action=\"/proyecto-web/srvAgregarVideojuego\""));
        assertTrue(pagina.body().contains("No hay videojuegos registrados"));
        assertEquals(200, get("/srvAgregarVideojuego?codigo=VG099&titulo=NoInsertar&genero=Rol&plataforma=PC").statusCode());
        assertTrue(dao.listar().isEmpty());
    }

    @Test
    void loginSesionYLogoutProtegenTodosLosEndpoints() throws Exception {
        assertTrue(get("/index.jsp").body().contains("Sesión de Alumno de prueba"));
        assertFalse(get("/index.jsp").body().contains("name=\"password\""));
        get("/srvCerrarSesion");
        assertTrue(get("/index.jsp").body().contains("name=\"password\""));
        for (String ruta : new String[]{"/videojuegos", "/srvAgregarVideojuego", "/srvModificarVideojuego", "/srvBorrarVideojuego"}) {
            assertTrue(get(ruta).headers().firstValue("Location").orElseThrow().endsWith("estado=sin-sesion"));
        }
        assertEquals(303, sendPost("/srvAgregarVideojuego", datos("VG999", "Sin acceso")).statusCode());
        assertTrue(dao.listar().isEmpty());
        assertTrue(get("/index.jsp?estado=sin-sesion").body().contains("Inicia sesión"));
    }

    @Test
    void usuarioIncorrectoYBaseAusenteMuestranMensajesDistintos() throws Exception {
        get("/srvCerrarSesion");
        String token = token(get("/index.jsp").body());
        var invalido = sendPost("/srvValidarSesion", Map.of("clave", "alumno", "password", "incorrecta", "csrf", token));
        assertTrue(invalido.headers().firstValue("Location").orElseThrow().endsWith("estado=usuario-invalido"));
        assertTrue(get("/index.jsp?estado=usuario-invalido").body().contains("Usuario o contraseña incorrectos"));
        sql("DROP TABLE usuarios_app");
        var sinBase = sendPost("/srvValidarSesion", Map.of("clave", "alumno", "password", "clave-segura", "csrf", token));
        assertTrue(sinBase.headers().firstValue("Location").orElseThrow().endsWith("estado=sin-base"));
        assertTrue(get("/index.jsp?estado=sin-base").body().contains("No se encontró la base de datos"));
    }

    @Test
    void altaConAcentosComillasYHtmlPersisteYNoSeRepiteAlActualizar() throws Exception {
        String titulo = "Pokémon O'Reilly <script>alert(1)</script>";
        var alta = post(Map.of("codigo", "VG011", "titulo", titulo, "genero", "Acción", "plataforma", "PC"));
        assertEquals(303, alta.statusCode());
        assertTrue(alta.headers().firstValue("Location").orElseThrow().endsWith("/videojuegos?resultado=creado"));
        assertEquals(titulo, dao.listar().get(0).titulo());
        var pagina = get("/videojuegos?resultado=creado");
        assertEquals(200, pagina.statusCode());
        assertTrue(pagina.headers().firstValue("Content-Type").orElseThrow().toLowerCase().contains("utf-8"));
        assertTrue(pagina.body().contains("Pokémon O&#39;Reilly &lt;script&gt;"));
        assertTrue(pagina.body().contains("Acción"));
        assertFalse(pagina.body().contains("<script>alert(1)</script>"));
        get("/videojuegos?resultado=creado");
        assertEquals(1, dao.listar().size());
    }

    @Test
    void codigoDuplicadoConservaFormularioYRegistroOriginal() throws Exception {
        assertEquals(303, post(datos("VG011", "Original")).statusCode());
        var duplicado = post(datos("VG011", "Otro título"));
        assertEquals(409, duplicado.statusCode());
        assertTrue(duplicado.body().contains("Ese código ya existe"));
        assertTrue(duplicado.body().contains("value=\"Otro título\""));
        assertEquals("Original", dao.listar().get(0).titulo());
        assertEquals(1, dao.listar().size());
    }

    @Test
    void validaCamposEnServidorAunqueSeOmitaValidacionDelNavegador() throws Exception {
        assertEquals(400, post(Map.of("codigo", "VG011")).statusCode());
        assertEquals(400, post(datos(" ", "Título")).statusCode());
        assertEquals(400, post(datos("x".repeat(31), "Título")).statusCode());
        assertEquals(400, post(datos("VG011", "x".repeat(101))).statusCode());
        assertTrue(dao.listar().isEmpty());
    }

    @Test
    void sqlEnElFormularioSeGuardaComoTexto() throws Exception {
        String titulo = "Juego'); DROP TABLE Videojuego; --";
        assertEquals(303, post(datos("VG011", titulo)).statusCode());
        assertEquals(titulo, dao.listar().get(0).titulo());
        assertEquals(200, get("/videojuegos").statusCode());
    }

    @Test
    void falloDeBaseNoInformaExitoNiFiltraSql() throws Exception {
        sql("DROP TABLE Videojuego");
        var fallo = post(datos("VG011", "Título"));
        assertEquals(500, fallo.statusCode());
        assertTrue(fallo.body().contains("No se pudo completar"));
        assertFalse(fallo.body().contains("agregado correctamente"));
        assertFalse(fallo.body().contains("INSERT INTO"));
        assertTrue(fallo.body().contains("value=\"Título\""));
    }

    @Test
    void modificaLosCamposSinCambiarCodigoNiDuplicarAlActualizar() throws Exception {
        assertEquals(303, post(datos("VG011", "Original")).statusCode());
        var editor = get("/videojuegos?editar=VG011");
        assertEquals(200, editor.statusCode());
        assertTrue(editor.body().contains("Modificar videojuego"));
        assertTrue(editor.body().contains("action=\"/proyecto-web/srvModificarVideojuego\""));
        assertTrue(editor.body().contains("value=\"Original\""));
        var cambio = post("/srvModificarVideojuego", Map.of("codigo", "VG011", "titulo", "Acción y rol",
                "genero", "Aventura", "plataforma", "Nintendo Switch"));
        assertEquals(303, cambio.statusCode());
        assertTrue(cambio.headers().firstValue("Location").orElseThrow().endsWith("resultado=modificado"));
        assertEquals("Acción y rol", dao.listar().get(0).titulo());
        assertEquals("Nintendo Switch", dao.listar().get(0).plataforma());
        get("/videojuegos?resultado=modificado");
        assertEquals(1, dao.listar().size());
    }

    @Test
    void editarDatoInvalidoOInexistenteNoModificaOtrosRegistros() throws Exception {
        post(datos("VG011", "Original"));
        var invalido = post("/srvModificarVideojuego", Map.of("codigo", "VG011", "titulo", " ",
                "genero", "Rol", "plataforma", "PC"));
        assertEquals(400, invalido.statusCode());
        assertEquals("Original", dao.listar().get(0).titulo());
        var inexistente = post("/srvModificarVideojuego", datos("NO_EXISTE", "Cambio"));
        assertEquals(404, inexistente.statusCode());
        assertEquals("Original", dao.listar().get(0).titulo());
        assertEquals(404, get("/videojuegos?editar=NO_EXISTE").statusCode());
    }

    @Test
    void borrarSoloPorPostYNoRepetirAlActualizar() throws Exception {
        post(datos("VG011", "Primero"));
        post(datos("VG012", "Segundo"));
        var vista = get("/videojuegos");
        assertTrue(vista.body().contains("action=\"/proyecto-web/srvBorrarVideojuego\""));
        assertTrue(vista.body().contains("¿Borrar este videojuego?"));
        assertEquals(200, get("/srvBorrarVideojuego?codigo=VG011").statusCode());
        assertEquals(2, dao.listar().size());
        var borrado = post("/srvBorrarVideojuego", Map.of("codigo", "VG011"));
        assertEquals(303, borrado.statusCode());
        assertTrue(borrado.headers().firstValue("Location").orElseThrow().endsWith("resultado=borrado"));
        assertEquals(1, dao.listar().size());
        assertEquals("VG012", dao.listar().get(0).codigo());
        assertEquals(200, get("/videojuegos?resultado=borrado").statusCode());
        assertEquals(404, post("/srvBorrarVideojuego", Map.of("codigo", "VG011")).statusCode());
    }

    @Test
    void rechazaEscriturasSinTokenDelFormulario() throws Exception {
        post(datos("VG011", "Conservado"));
        var sinToken = sendPost("/srvBorrarVideojuego", Map.of("codigo", "VG011"));
        assertEquals(403, sinToken.statusCode());
        var falso = sendPost("/srvModificarVideojuego", Map.of("codigo", "VG011", "titulo", "Otro",
                "genero", "Rol", "plataforma", "PC", "csrf", "falso"));
        assertEquals(403, falso.statusCode());
        assertEquals("Conservado", dao.listar().get(0).titulo());
    }

    @Test
    void codigoManipuladoEnEditarOBorrarNoAfectaOtrosJuegos() throws Exception {
        post(datos("VG011", "Conservado"));
        String ataque = "VG011' OR '1'='1";
        assertEquals(404, post("/srvModificarVideojuego", datos(ataque, "Otro")).statusCode());
        assertEquals(404, post("/srvBorrarVideojuego", Map.of("codigo", ataque)).statusCode());
        assertEquals(1, dao.listar().size());
        assertEquals("Conservado", dao.listar().get(0).titulo());
    }

    static Map<String, String> datos(String codigo, String titulo) {
        return Map.of("codigo", codigo, "titulo", titulo, "genero", "Aventura", "plataforma", "PC");
    }

    static void sql(String consulta) throws Exception {
        try (var conexion = dataSource.getConnection(); var comando = conexion.createStatement()) {
            comando.execute(consulta);
        }
    }

    static HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(30)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    static HttpResponse<String> post(Map<String, String> datos) throws Exception {
        return post("/srvAgregarVideojuego", datos);
    }

    static HttpResponse<String> post(String ruta, Map<String, String> datos) throws Exception {
        String html = get("/videojuegos").body();
        String token = token(html);
        var conToken = new java.util.HashMap<>(datos);
        conToken.put("csrf", token);
        return sendPost(ruta, conToken);
    }

    static void login(String usuario, String password) throws Exception {
        String csrf = token(get("/index.jsp").body());
        assertEquals(303, sendPost("/srvValidarSesion", Map.of("clave", usuario, "password", password, "csrf", csrf)).statusCode());
    }

    static String token(String html) {
        String marker = "name=\"csrf\" value=\"";
        int inicio = html.indexOf(marker);
        assertTrue(inicio >= 0, "El formulario debe contener el token");
        inicio += marker.length();
        return html.substring(inicio, html.indexOf('"', inicio));
    }

    static HttpResponse<String> sendPost(String ruta, Map<String, String> datos) throws Exception {
        String body = datos.entrySet().stream().map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
        return client.send(HttpRequest.newBuilder(URI.create(base + ruta))
                .timeout(Duration.ofSeconds(30)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
