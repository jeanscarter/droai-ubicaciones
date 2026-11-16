# DroUbicaciones

Sistema web y móvil para la gestión, auditoría y reubicación de productos en almacén con sincronización directa a Profit Plus Administrativo.

---

## 🚀 Características Principales

* **Gestión de Ubicaciones en Almacén:**
  * Asignación y actualización en tiempo real de Ubicación Primaria (`co_ubicacion`) y Secundaria (`co_ubicacion2`) en la base de datos de Profit Plus.
  * Resolución de conflictos de estantes con múltiples productos (desalojo o reasignación).
* **Discriminación Inteligente de Stock:**
  * Visualización desglosada por producto:
    * 🟢 **Disponible:** Mercancía apta para venta (almacenes operativos `01`, `02`).
    * 🟠 **Avería:** Existencias en almacén `AV` (con alertas visuales destacadas).
    * 🔴 **Vencido:** Existencias en almacenes `VP` y `VC`.
    * 📦 **Total:** Existencia total de la empresa.
* **Catálogo e Indexación de Imágenes:**
  * Búsqueda e indexación inteligente por código de barras, código de artículo y coincidencia de nombres.
  * Actualización de fotos de productos mediante URL web o carga directa de fotos desde el teléfono móvil (con invalidación de caché automática).
* **Experiencia Mobile First:**
  * Interfaz táctil adaptada para teléfonos inteligentes y colectores de datos (pantallas alargadas, modales bottom-sheet, botones accesibles).

---

## 🛠️ Tecnologías Utilizadas

* **Backend:** Java 21 (LTS)
* **Servidor HTTP:** Java Core `com.sun.net.httpserver.HttpServer` (ligero, sin necesidad de Tomcat/Jetty externo)
* **Base de Datos:** Microsoft SQL Server (conexión con `mssql-jdbc` y pool de conexiones de alto rendimiento con `HikariCP`)
* **Construcción:** Apache Maven (`maven-shade-plugin` para empaquetado en Uber-JAR)
* **Frontend:** HTML5, CSS3 Moderno (Variables CSS, Flexbox/Grid responsive), JavaScript Vanilla ES6+

---

## ⚙️ Configuración y Despliegue

### 1. Clonar el repositorio
```bash
git clone https://github.com/jeanscarter/dro-ubicaciones.git
cd dro-ubicaciones
```

### 2. Configurar credenciales
Copia el archivo de ejemplo `db.properties.example` a `db.properties` y ajusta los parámetros de conexión:
```properties
DB_SERVER=servidor_sql
DB_PORT=1433
DB_DATABASE=DROA_A
DB_USER=usuario_sql
DB_PASSWORD=clave_sql
WEB_PORT=8085
```

### 3. Compilar el proyecto
Utiliza el Maven Wrapper incluido:
```bash
# En Windows
.\mvnw.cmd clean package -DskipTests

# En Linux / macOS
./mvnw clean package -DskipTests
```

### 4. Ejecutar el servidor
Puedes iniciar la aplicación directamente con Java o usando el script `iniciar_servidor.bat`:
```bash
java -Xms64m -Xmx256m -jar target/dro-ubicaciones-1.0-SNAPSHOT.jar
```
La aplicación web estará disponible en `http://localhost:8085` (o el puerto configurado).

---

## 📄 Licencia
Privado / Uso interno corporativo.
