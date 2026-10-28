# Proyecto web: catálogo de videojuegos con MVC

Este es el proyecto Maven de `desarrolloWeb/proyecto-web`. Genera **target/proyecto-web.war** y se ejecuta en **http://localhost:8080/proyecto-web/** con Tomcat 11 y Java 17. Permite consultar, agregar, modificar y borrar videojuegos.

## Adaptación del ejemplo del profesor

Se tomó como referencia `../Servlets/uv/webpokes/controlador/srvAgregarUsuario.java` y `../Servlets/frmAgregarUsuario.jsp`.

El ejemplo recibe parámetros del formulario, construye un usuario, invoca su inserción y vuelve a una JSP. La adaptación conserva ese flujo para la tabla de videojuegos:

- `SrvAgregarVideojuego` recibe código, título, género y plataforma.
- El formulario usa POST; GET solo consulta y no modifica datos.
- El controlador llama al modelo para insertar, modificar y borrar con parámetros JDBC, y redirige al listado después de cada cambio para evitar repetirlo al actualizar.
- La JSP recibe atributos del controlador; no abre conexiones ni ejecuta SQL.
- Se validan campos y longitudes, se muestran errores de códigos repetidos o inexistentes y se conserva el formulario cuando falla el alta o la edición.
- Las operaciones de escritura requieren un formulario de la sesión y usan POST; borrar pide confirmación en el navegador.
- La entrada y la salida usan UTF-8 para los acentos.

## Archivos MVC

| Parte | Archivo |
| --- | --- |
| Modelo | src/main/java/com/ejemplo/proyectoweb/modelo/Videojuego.java |
| Persistencia del modelo | src/main/java/com/ejemplo/proyectoweb/modelo/VideojuegoDAO.java |
| Controlador | src/main/java/com/ejemplo/proyectoweb/controlador/SrvAgregarVideojuego.java |
| Vista: formulario y tabla | src/main/webapp/WEB-INF/vista/index.jsp |
| Entrada y redirección | src/main/webapp/index.jsp |
| Página de bienvenida configurada | src/main/webapp/WEB-INF/web.xml |

Rutas: GET `/videojuegos` muestra el catálogo y el formulario; GET `/videojuegos?editar=CODIGO` abre el formulario de edición; POST `/srvAgregarVideojuego` agrega, POST `/srvModificarVideojuego` modifica y POST `/srvBorrarVideojuego` borra. Todas las rutas son atendidas por el mismo controlador. Las JSP de WEB-INF solo se abren mediante el controlador.

## Usar la base de la práctica anterior

No hace falta recrear `videojuegos`. Se utiliza la misma tabla `public.Videojuego` con los registros existentes.

Entra desde PowerShell:

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -h localhost -U postgres -d videojuegos
```

Dentro de psql concede permisos a la cuenta de la aplicación:

```sql
GRANT CONNECT ON DATABASE videojuegos TO usrvideojuegos;
GRANT USAGE ON SCHEMA public TO usrvideojuegos;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.Videojuego TO usrvideojuegos;
```

También están en `sql/permisos-mvc.sql`. Sal con `\q`.

`sql/ejemplo.sql` crea la tabla y cinco registros iniciales para instalaciones nuevas; no es necesario volver a ejecutarlo si tu base ya está preparada.

## Compilar y ejecutar

Desde esta carpeta:

```powershell
cd 'C:\Users\rojas\OneDrive\Documentos\desarrolloWeb\proyecto-web'
mvn clean package
```

Detén la instancia anterior de Tomcat con Ctrl+C. Después, en la misma ventana de PowerShell, configura la contraseña REAL de `usrvideojuegos`, copia el WAR y arranca:

```powershell
$tomcat = 'C:\Users\rojas\Downloads\apache-tomcat-11.0.26\apache-tomcat-11.0.26'
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot'
$env:JRE_HOME = $env:JAVA_HOME
$env:CATALINA_HOME = $tomcat
$env:CATALINA_BASE = $tomcat
$env:DB_URL = 'jdbc:postgresql://localhost:5432/videojuegos?connectTimeout=5&socketTimeout=10'
$env:DB_USER = 'usrvideojuegos'
$env:DB_PASSWORD = 'TU_CONTRASENA_REAL'
Copy-Item '.\target\proyecto-web.war' "$tomcat\webapps\" -Force
& "$tomcat\bin\catalina.bat" run
```

Abre **http://localhost:8080/proyecto-web/**. La página muestra el login y, después de autenticarte, abre `/proyecto-web/videojuegos`.

## Crear el usuario para iniciar sesión

La autenticación usa una tabla nueva `usuarios_app` en la misma base `videojuegos`; no altera los videojuegos existentes. Ejecuta una vez:

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -h localhost -U postgres -d videojuegos -f '.\sql\usuarios-login.sql'
```

Después de `mvn clean package`, genera un INSERT con tu usuario y contraseña. El programa pide la contraseña en la terminal y solo imprime su hash:

```powershell
java -cp '.\target\classes' com.ejemplo.proyectoweb.modelo.CrearUsuario
```

Copia el `INSERT INTO usuarios_app ...` que aparece y ejecútalo dentro de `psql` conectado como `postgres` a `videojuegos`. Comprueba con:

```sql
SELECT clave, nombre, activo FROM usuarios_app;
```

La cuenta `usrvideojuegos` solo recibe permiso de lectura sobre `usuarios_app`. No incluyas la contraseña en el código ni en capturas.

Para verificar el acceso: entra con el usuario creado, comprueba que el título del navegador indica el nombre de la sesión, cierra sesión y prueba abrir `/proyecto-web/videojuegos`. Debe volver al login. Una contraseña incorrecta muestra un mensaje diferente al fallo de conexión con PostgreSQL.

## Probar

1. Agrega un código que no exista (por ejemplo VG011), título Pokémon Scarlet, género Rol y aventura, plataforma Nintendo Switch.
2. Comprueba el mensaje de éxito y la nueva fila.
3. En psql ejecuta `SELECT * FROM public.Videojuego WHERE codigo = 'VG011';`.
4. Actualiza el navegador: no debe insertarse otra vez.
5. Intenta guardar de nuevo el mismo código: debe mostrar un aviso y conservar los datos del formulario.
6. En la fila nueva, pulsa **Modificar**, cambia título, género o plataforma y guarda. El código queda fijo. Comprueba el cambio en la tabla y en PostgreSQL.
7. Pulsa **Borrar** en esa fila, acepta la confirmación y comprueba que ya no aparece en la tabla ni en `SELECT * FROM public.Videojuego WHERE codigo = 'VG011';`.

Las acciones de modificar y borrar afectan al registro que elijas. Para conservar un registro, cancela la edición o la confirmación de borrado.

`mvn test` prueba las rutas HTTP, la JSP real en Tomcat, las altas, modificaciones y bajas, acentos, duplicados, validación, escape HTML, token del formulario y fallos de persistencia. Usa H2 en modo PostgreSQL como base desechable; no conecta con tu base de datos real. Las dependencias de pruebas no se incluyen en el WAR.

Si hay un fallo de conexión o permisos, revisa la consola de Tomcat. La contraseña no está incluida en el código.
