# Liga Not FIFA 2026

Página deportiva con sistema de apuestas hecha con **Java 21, Spring Boot, Thymeleaf, Spring Security, JPA y PostgreSQL**.
Sigue una estructura MVC en español: `entidades`, `repositorios`, `servicios`, `recursos` (controllers), `dto`, `exception`, `seguridad`, `config`.

> **Cuotas:** las calcula el servidor con una fórmula estadística (Poisson) basada en el historial de los equipos, su nivel y, para el
> mercado Goleador, la posición y la media de cada jugador. Ver la sección 10.

## 1. Requisitos
- JDK 21
- Maven 3.9+ (o el `mvnw` incluido)
- PostgreSQL 14 o superior
- Un IDE (IntelliJ IDEA recomendado, con el plugin de Lombok activo)

## 2. Instalación
1. Clona o descomprime el proyecto.
2. Crea la base de datos (sección 3).
3. Ajusta la configuración si hace falta (sección 4).
4. Ejecuta `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`).

## 3. Configuración de PostgreSQL
Crea la base de datos vacía (Spring crea las tablas solo, con `ddl-auto=update`, que nunca borra datos existentes):

```sql
CREATE DATABASE liga_not_fifa_2026;
```

## 4. Configuración y variables de entorno
`src/main/resources/application.properties` lee estas variables (si no existen, usa los valores locales por defecto):

| Variable | Para qué sirve |
|---|---|
| `DATABASE_URL` | URL JDBC, ej. `jdbc:postgresql://localhost:5432/liga_not_fifa_2026` |
| `DATABASE_USERNAME` | Usuario de PostgreSQL |
| `DATABASE_PASSWORD` | Contraseña de PostgreSQL |
| `ADMIN_PASSWORD` | Contraseña inicial del administrador `Egoisu` |
| `ADMIN_PASSWORD_RESPALDO` | Segunda contraseña válida del administrador (respaldo) |
| `SPRING_PROFILES_ACTIVE` | `prod` en el hosting (activa `application-prod.properties`) |
| `APP_TIMEZONE` | Zona horaria de los partidos (por defecto `America/Bogota`) |

**Seguridad:** no subas contraseñas reales a Git. Define `DATABASE_PASSWORD` y `ADMIN_PASSWORD` como variables de entorno
o usa un archivo `application-local.properties` (ya está en `.gitignore`).

Reglas de apuestas configurables (mismo archivo): `apuestas.comision` (0.05), `apuestas.lineas-goles`, `apuestas.lineas-tiros`,
`apuestas.lineas-corners`, `apuestas.parley-maximo-selecciones` y `apuestas.parley-bonus-por-seleccion` (0 = sin bonus).

## 5. Ejecución
```bash
./mvnw spring-boot:run
```
Abre <http://localhost:8080>. Para generar el jar: `./mvnw clean package` y luego `java -jar target/*.jar`.

## 6. Usuario administrador
- Usuario único: **Egoisu** (rol `ROLE_ADMIN`). Se crea al arrancar si no existe.
- Contraseña: la de `ADMIN_PASSWORD`; si no la defines, se genera una **aleatoria y se muestra una sola vez en la consola**.
- Además hay una **contraseña de respaldo** (`ADMIN_PASSWORD_RESPALDO`): el administrador puede entrar con cualquiera de las dos. Se guarda cifrada y se sincroniza en cada arranque. En producción defínela como variable de entorno y cámbiala si el repositorio es público.
- Nadie puede registrarse con ese nombre ni obtener rol de administrador desde el formulario.
- El administrador gestiona equipos, jugadores (con posición y media 1-99), partidos y resultados, y consulta todas las apuestas (solo lectura). No puede apostar.

## 7. Usuarios normales
Se registran en `/registro` (usuario único, contraseña de 8 a 72 caracteres, guardada con BCrypt) y reciben `ROLE_USER`.
Pueden ver partidos y equipos, armar un boleto (simple o parley), ver la ganancia potencial y confirmar apuestas.
La ganancia potencial es `(apuesta - apuesta × 0.05) × cuota`. Ver sus apuestas en "Mis apuestas" y su resumen en "Perfil".

## 8. Tests
```bash
./mvnw test
```
Cubren: cálculo de ganancias y parley, liquidación de apuestas, registro de usuarios y del administrador, registro de resultados,
estadísticas de equipos, flujo de apuestas (con el servicio de cuotas simulado) y reglas de seguridad por URL.
También se prueban las funciones de probabilidad y el motor de cuotas (equipos más fuertes, historial, posición y media de jugadores, márgenes y límites).

## 9. Estructura del proyecto
```
src/main/java/Fifan/t/Egoisu
├── config/        ConfiguracionApuestas, DataInitializer (admin), MetodosSeguridadConfig
├── seguridad/     SeguridadConfig (reglas por URL, BCrypt, CSRF, login/logout)
├── entidades/     Usuario, Equipo, Jugador, Partido, EstadisticaPartido, Gol, Apuesta, SeleccionApuesta (+ enums)
├── repositorios/  Interfaces Spring Data JPA
├── dto/           Formularios y modelos de vista
├── servicios/     Lógica de negocio (apuestas, liquidación, cuotas, estadísticas...)
├── recursos/      Controllers MVC
└── exception/     Excepciones propias y manejador global
src/main/resources
├── templates/     Vistas Thymeleaf (+ fragments/layout y fragments/mercados)
└── static/        css/ (style, navbar, apuestas, admin, dark-mode), js/main.js, images/
src/test/java      Tests unitarios y de seguridad
```
Imágenes: coloca `logo.png`, `apuesta-confirmada.png` y las fotos de patrocinadores en `static/images/` (ver `static/images/README.md`).
Si faltan, la página muestra marcadores de posición.

## 10. Funcionamiento
- **Seguridad:** dos barreras. Spring Security protege las URLs (`/admin/**` solo ADMIN) y los servicios vuelven a exigir el rol con `@PreAuthorize`. CSRF activo en todos los formularios.
- **Apuestas:** el cliente solo envía *qué* apuesta y *cuánto*. La cuota, la comisión y la ganancia las calcula siempre el servidor.
  Al confirmar se guarda una copia inmutable (cuota, textos, monto y comisión) para que cambios futuros no afecten apuestas hechas.
- **Cierre de apuestas:** al llegar la hora del partido, un proceso cada minuto lo pasa de `PROGRAMADO` a `EN_JUEGO`.
- **Resultados:** al registrar el resultado (marcador, estadísticas y goleadores) el partido queda `FINALIZADO` (ya no se puede cambiar) y las apuestas se liquidan en la misma transacción.
- **Parley:** una selección por partido; pierde si una selección pierde, gana si todas ganan. Bonus configurable.
- **Partido cancelado:** las selecciones quedan `CANCELADA` (la apuesta se cancela, salvo que otra selección ya esté perdida).

### Supuestos tomados (confírmalos)
- El administrador no puede apostar y no tiene saldo/wallet: no hay saldo que descontar.
- Inicio, partidos y equipos son públicos (solo consulta); apostar exige iniciar sesión.
- El goleador gana si marca al menos un gol; no hay autogoles. Los tiros deben ser mayores o iguales a los goles.
- Líneas de mercados propuestas: goles 1.5/2.5/3.5, tiros 4.5/6.5/8.5, corners 2.5/3.5/4.5.

### Cómo se calculan las cuotas (`ServicioCuotasPoisson`)
Todo con `BigDecimal`. Los parámetros están en `application.properties` (prefijo `cuotas.`).

1. **Goles esperados de cada equipo (Poisson).** El ataque de un equipo mezcla su media de **goles** y sus **tiros** de partidos anteriores; la debilidad del rival mezcla los goles y tiros que ha **recibido**:
   `goles_local = media_liga_local × ataque_local × debilidad_visitante` (y viceversa para el visitante).
2. **Con historial:** 50% rendimiento general + 50% como local/visitante, suavizado hacia el promedio de la liga con peso `n/(n+3)` (n = partidos jugados).
3. **Sin partidos (primer partido):** solo el **nivel** del equipo: `ataque = 0.5 + nivel/10` y `debilidad = 1.5 − nivel/10`.
4. **Mercados:** resultado 1X2 (matriz de Poisson), más/menos goles (Poisson del total), tiros y corners (Poisson por equipo).
5. **Goleador:** los goles esperados del equipo se reparten entre sus jugadores activos según `peso = peso_posición × (media/70)³ × (1 + goles_previos/partidos)`
   (portero 0.02, defensa 0.25, mediocampista 0.60, delantero 1.00). `P(marca) = 1 − e^(−goles_del_jugador)`.
6. **Cuota final:** `1 / (p × (1 + 8%))`, entre 1.05 y 50.00, con 2 decimales. La comisión del 5% sobre el dinero apostado se aplica aparte.

Los jugadores creados antes de existir posición y media se tratan como mediocampista con media 60 hasta que el administrador los edite.

## 11. Despliegue en internet (gratis)
Render no ejecuta Java de forma nativa, por eso el proyecto incluye `Dockerfile`, `.dockerignore` y `render.yaml`.
1. Sube el proyecto a GitHub **sin contraseñas reales** en `application.properties` (usa variables de entorno).
2. Crea la base de datos PostgreSQL en la nube y copia su URL en formato **JDBC**: `jdbc:postgresql://HOST:5432/BD?sslmode=require`.
3. En Render: *New > Blueprint* y elige el repositorio (o *New > Web Service > Docker*). Plan **Free**.
4. Escribe en el panel las variables: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `ADMIN_PASSWORD`, `ADMIN_PASSWORD_RESPALDO`.
5. Cuando termine el despliegue, entra a la URL `https://TU-SERVICIO.onrender.com`.

Para copiar los datos de tu base local a la nube:
```bash
pg_dump -Fc --no-owner -U postgres -d liga_not_fifa_2026 -f liga.dump
pg_restore --no-owner -d "postgresql://USUARIO:CLAVE@HOST:5432/BD?sslmode=require" liga.dump
```
Límites del plan gratuito de Render: el servicio se duerme tras 15 minutos sin visitas (la primera visita tarda cerca de 1 minuto),
tiene ~512 MB de RAM y el disco se borra en cada reinicio (por eso los datos van en la base de datos externa).
Alternativa sin Docker: plataformas con buildpack de Java (p. ej. Koyeb) usan `system.properties` y `Procfile`, incluidos.
