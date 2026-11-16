package com.droubicaciones.web;

import com.droubicaciones.model.ArticuloUbicacionRow;
import com.droubicaciones.model.ResultadoGuardado;
import com.droubicaciones.model.SesionUsuario;
import com.droubicaciones.model.UbicacionConflicto;
import com.droubicaciones.service.UbicacionService;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

public class UbicacionWebServer {

    private final UbicacionService service;
    private HttpServer server;
    private HttpsServer httpsServer;
    private int port;
    private final int httpsPort;

    public UbicacionWebServer(int port) {
        this.port = port;
        this.httpsPort = 8444;
        this.service = new UbicacionService();
        this.service.inicializarServicios();
    }

    public void start() throws IOException {
        int chosenPort = findAvailablePort(this.port);
        if (chosenPort != this.port) {
            System.out.println("⚠️ [Aviso] El puerto " + this.port
                    + " ya está en uso (ej. XAMPP/Apache). Usando puerto alternativo: " + chosenPort);
        }
        this.port = chosenPort;

        // 1. HTTP Server
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", this.port), 0);

        server.createContext("/api/articulo", new ArticuloHandler());
        server.createContext("/api/ubicaciones", new UbicacionesHandler());
        server.createContext("/api/ubicacion/check", new UbicacionCheckHandler());
        server.createContext("/api/conflictos/resolver", new ResolverConflictoHandler());
        server.createContext("/api/conflictos", new ConflictosHandler());
        server.createContext("/api/imagen/actualizar", new ActualizarImagenHandler());
        server.createContext("/api/imagen", new ImagenHandler());
        server.createContext("/api/info", new InfoHandler());
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        // 2. HTTPS Server (Puerto 8443 para Cámara en Vivo 60 FPS en Móviles)
        startHttps();

        System.out.println("\n=======================================================");
        System.out.println("🚀 [DroUbicaciones] Servidor Web Local Iniciado!");
        System.out.println("💻 Acceso Local HTTP:  http://localhost:" + this.port);
        System.out.println("🔒 Acceso HTTPS (Cámara en Vivo): https://localhost:" + httpsPort);
        printLocalIPs();
        System.out.println("=======================================================\n");
    }

    private int findAvailablePort(int preferredPort) {
        int[] candidates = { preferredPort, 8085, 8088, 8090, 8081, 9000 };
        for (int p : candidates) {
            try (java.net.ServerSocket ss = new java.net.ServerSocket()) {
                ss.setReuseAddress(true);
                ss.bind(new InetSocketAddress("0.0.0.0", p));
                return p;
            } catch (Exception ignored) {
            }
        }
        return preferredPort;
    }

    private void startHttps() {
        try {
            char[] passphrase = "droubicaciones123".toCharArray();
            KeyStore ks = KeyStore.getInstance("PKCS12");

            InputStream is = getClass().getResourceAsStream("/keystore.p12");
            if (is == null) {
                is = getClass().getResourceAsStream("/keystore.jks");
            }

            if (is != null) {
                try (InputStream stream = is) {
                    ks.load(stream, passphrase);
                }
            } else {
                System.err.println("⚠️ KeyStore SSL no encontrado. HTTPS omitido.");
                return;
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, passphrase);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);

            httpsServer = HttpsServer.create(new InetSocketAddress("0.0.0.0", httpsPort), 0);
            httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                @Override
                public void configure(HttpsParameters params) {
                    try {
                        SSLContext context = getSSLContext();
                        SSLParameters sslParams = context.getDefaultSSLParameters();
                        params.setSSLParameters(sslParams);
                    } catch (Exception ex) {
                        System.err.println("Failed to configure HTTPS parameters");
                    }
                }
            });

            httpsServer.createContext("/api/articulo", new ArticuloHandler());
            httpsServer.createContext("/api/ubicaciones", new UbicacionesHandler());
            httpsServer.createContext("/api/ubicacion/check", new UbicacionCheckHandler());
            httpsServer.createContext("/api/conflictos/resolver", new ResolverConflictoHandler());
            httpsServer.createContext("/api/conflictos", new ConflictosHandler());
            httpsServer.createContext("/api/imagen/actualizar", new ActualizarImagenHandler());
            httpsServer.createContext("/api/imagen", new ImagenHandler());
            httpsServer.createContext("/api/info", new InfoHandler());
            httpsServer.createContext("/", new StaticFileHandler());

            httpsServer.setExecutor(Executors.newCachedThreadPool());
            httpsServer.start();
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo iniciar el servidor HTTPS: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
        if (httpsServer != null) {
            httpsServer.stop(0);
        }
    }

    private void printLocalIPs() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp())
                    continue;

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.getHostAddress().contains(":"))
                        continue; // Ignorar IPv6
                    System.out.println("📱 Acceso HTTP:  http://" + addr.getHostAddress() + ":" + port);
                    System.out.println("🎥 Acceso HTTPS (Cámara en Vivo 60 FPS): https://" + addr.getHostAddress() + ":"
                            + httpsPort);
                }
            }
        } catch (Exception ignored) {
        }
    }

    // ── 1. BUSCADOR DE ARTÍCULOS EN PROFIT PLUS ──
    private class ArticuloHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                addCorsHeaders(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJsonResponse(exchange, 405, "{\"error\":\"Método no permitido\"}");
                    return;
                }

                String queryParam = getQueryParam(exchange.getRequestURI().getQuery(), "q");
                if (queryParam == null || queryParam.isBlank()) {
                    sendJsonResponse(exchange, 400, "{\"error\":\"Parámetro 'q' requerido\"}");
                    return;
                }

                System.out.println("[WebAPI] 🔍 Consultando producto/pasillo: " + queryParam);
                List<ArticuloUbicacionRow> lista = service.buscarArticulos(queryParam);
                if (!lista.isEmpty()) {
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < lista.size(); i++) {
                        ArticuloUbicacionRow art = lista.get(i);
                        if (i > 0)
                            json.append(",");

                        long imgVer = service.obtenerVersionImagen(art.getCoArt(), art.getCodigoBarra());
                        String imgUrl = (imgVer > 0)
                                ? String.format("/api/imagen?id=%s&v=%d", escapeJson(art.getCoArt()), imgVer)
                                : String.format("/api/imagen?id=%s", escapeJson(art.getCoArt()));

                        json.append(String.format(Locale.US, """
                                {
                                    "coArt": "%s",
                                    "codigoBarra": "%s",
                                    "descripcion": "%s",
                                    "marca": "%s",
                                    "linea": "%s",
                                    "existencia": %.2f,
                                    "stockAveria": %.2f,
                                    "stockVencido": %.2f,
                                    "stockTotal": %.2f,
                                    "ubicacion1": "%s",
                                    "ubicacion2": "%s",
                                    "imagenUrl": "%s"
                                }
                                """,
                                escapeJson(art.getCoArt()),
                                escapeJson(art.getCodigoBarra()),
                                escapeJson(art.getDescripcion()),
                                escapeJson(art.getMarca()),
                                escapeJson(art.getLinea()),
                                art.getExistencia(),
                                art.getStockAveria(),
                                art.getStockVencido(),
                                art.getStockTotal(),
                                escapeJson(art.getUbicacion1()),
                                escapeJson(art.getUbicacion2()),
                                imgUrl));
                    }
                    json.append("]");
                    sendJsonResponse(exchange, 200, json.toString());
                } else {
                    sendJsonResponse(exchange, 404,
                            "{\"error\":\"No se encontraron productos con el criterio de búsqueda\"}");
                }
            } catch (Throwable e) {
                if (isClientDisconnect(e)) {
                    System.out.println("[WebAPI] ℹ️ Conexión cancelada por el cliente (ArticuloHandler).");
                    return;
                }
                System.err.println("❌ Error en ArticuloHandler:");
                e.printStackTrace();
                String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(msg) + "\"}");
            }
        }
    }

    // ── 2. GUARDAR UBICACIONES ──
    private class UbicacionesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                addCorsHeaders(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJsonResponse(exchange, 405, "{\"error\":\"Método no permitido\"}");
                    return;
                }

                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String coArt = getJsonFieldValue(body, "coArt");
                String u1 = getJsonFieldValue(body, "ubicacion1");
                String u2 = getJsonFieldValue(body, "ubicacion2");

                if (coArt == null || coArt.isBlank()) {
                    sendJsonResponse(exchange, 400, "{\"error\":\"Código de artículo requerido\"}");
                    return;
                }

                System.out
                        .println("[WebAPI] 💾 Guardando ubicaciones para: " + coArt + " -> U1: " + u1 + " | U2: " + u2);
                ResultadoGuardado resultado = service.guardarUbicaciones(coArt, u1, u2);

                if (resultado.isSuccess()) {
                    StringBuilder json = new StringBuilder();
                    json.append("{\"success\":true,\"message\":\"").append(escapeJson(resultado.getMensaje()))
                            .append("\",\"desalojados\":[");
                    for (int i = 0; i < resultado.getDesalojados().size(); i++) {
                        ResultadoGuardado.ArticuloSimple a = resultado.getDesalojados().get(i);
                        if (i > 0)
                            json.append(",");
                        json.append(String.format("{\"coArt\":\"%s\",\"descripcion\":\"%s\"}",
                                escapeJson(a.getCoArt()), escapeJson(a.getDescripcion())));
                    }
                    json.append("]}");
                    sendJsonResponse(exchange, 200, json.toString());
                } else {
                    sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(resultado.getMensaje()) + "\"}");
                }
            } catch (Throwable e) {
                if (isClientDisconnect(e)) {
                    System.out.println("[WebAPI] ℹ️ Conexión cancelada por el cliente (UbicacionesHandler).");
                    return;
                }
                System.err.println("❌ Error en UbicacionesHandler:");
                e.printStackTrace();
                String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(msg) + "\"}");
            }
        }
    }

    // ── 3. VERIFICAR SI UNA UBICACIÓN YA ESTÁ OCUPADA ──
    private class UbicacionCheckHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                addCorsHeaders(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                String ubi = getQueryParam(exchange.getRequestURI().getQuery(), "ubi");
                String coArt = getQueryParam(exchange.getRequestURI().getQuery(), "coArt");

                if (ubi == null || ubi.isBlank()) {
                    sendJsonResponse(exchange, 200, "{\"ocupado\":false,\"articulos\":[]}");
                    return;
                }

                List<ResultadoGuardado.ArticuloSimple> ocupantes = service.verificarOcupacionUbicacion(ubi, coArt);
                boolean ocupado = !ocupantes.isEmpty();
                StringBuilder json = new StringBuilder();
                json.append("{\"ocupado\":").append(ocupado).append(",\"articulos\":[");
                for (int i = 0; i < ocupantes.size(); i++) {
                    ResultadoGuardado.ArticuloSimple a = ocupantes.get(i);
                    if (i > 0)
                        json.append(",");
                    json.append(String.format("{\"coArt\":\"%s\",\"descripcion\":\"%s\"}",
                            escapeJson(a.getCoArt()), escapeJson(a.getDescripcion())));
                }
                json.append("]}");
                sendJsonResponse(exchange, 200, json.toString());
            } catch (Throwable e) {
                if (isClientDisconnect(e))
                    return;
                System.err.println("❌ Error en UbicacionCheckHandler: " + e.getMessage());
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // ── 4. ENTREGA INSTANTÁNEA DE IMÁGENES CON CACHÉ HTTP ──
    private class ImagenHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                addCorsHeaders(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                String id = getQueryParam(exchange.getRequestURI().getQuery(), "id");
                if (id == null || id.isBlank()) {
                    String path = exchange.getRequestURI().getPath();
                    if (path.startsWith("/api/imagen/")) {
                        id = path.substring("/api/imagen/".length());
                    }
                }

                if (id == null || id.isBlank()) {
                    exchange.sendResponseHeaders(400, -1);
                    return;
                }

                java.nio.file.Path imgPath = service.obtenerImagenArticulo(id.trim());
                if (imgPath != null && java.nio.file.Files.exists(imgPath)) {
                    byte[] bytes = java.nio.file.Files.readAllBytes(imgPath);
                    String fileName = imgPath.getFileName().toString().toLowerCase();
                    String contentType = "image/png";
                    if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg"))
                        contentType = "image/jpeg";
                    else if (fileName.endsWith(".webp"))
                        contentType = "image/webp";
                    else if (fileName.endsWith(".jfif"))
                        contentType = "image/jpeg";
                    else if (fileName.endsWith(".gif"))
                        contentType = "image/gif";

                    long lastMod = java.nio.file.Files.getLastModifiedTime(imgPath).toMillis();
                    exchange.getResponseHeaders().set("Content-Type", contentType);
                    exchange.getResponseHeaders().set("Cache-Control", "no-cache, must-revalidate");
                    exchange.getResponseHeaders().set("ETag", "\"" + lastMod + "-" + bytes.length + "\"");
                    exchange.getResponseHeaders().set("Last-Modified", String.valueOf(lastMod));
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                } else {
                    // SVG placeholder elegante cuando el producto no tiene foto
                    String svg = """
                            <svg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round">
                                <rect x="3" y="3" width="18" height="18" rx="2" ry="2" fill="rgba(148, 163, 184, 0.08)"/>
                                <circle cx="8.5" cy="8.5" r="1.5"/>
                                <polyline points="21 15 16 10 5 21"/>
                            </svg>
                            """;
                    byte[] svgBytes = svg.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "image/svg+xml");
                    exchange.getResponseHeaders().set("Cache-Control", "public, max-age=86400");
                    exchange.sendResponseHeaders(200, svgBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(svgBytes);
                    }
                }
            } catch (Throwable e) {
                if (isClientDisconnect(e))
                    return;
                try {
                    exchange.sendResponseHeaders(500, -1);
                } catch (Exception ignored) {
                }
            }
        }
    }

    // ── 4B. ACTUALIZAR O CAMBIAR IMAGEN DE UN PRODUCTO (URL O ARCHIVO) ──
    private class ActualizarImagenHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                addCorsHeaders(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }

                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String coArt = getJsonFieldValue(body, "coArt");
                String codigoBarra = getJsonFieldValue(body, "codigoBarra");
                String url = getJsonFieldValue(body, "url");
                String base64 = getJsonFieldValue(body, "base64");

                if ((coArt == null || coArt.isBlank()) && (codigoBarra == null || codigoBarra.isBlank())) {
                    sendJsonResponse(exchange, 400,
                            "{\"success\":false,\"error\":\"Código de artículo o barra requerido\"}");
                    return;
                }

                java.nio.file.Path guardada = null;
                if (url != null && !url.isBlank()) {
                    guardada = service.guardarImagenDesdeUrl(coArt, codigoBarra, url);
                } else if (base64 != null && !base64.isBlank()) {
                    String cleanBase64 = base64;
                    String ext = ".jpg";
                    if (cleanBase64.contains(",")) {
                        String header = cleanBase64.substring(0, cleanBase64.indexOf(","));
                        if (header.contains("png"))
                            ext = ".png";
                        else if (header.contains("webp"))
                            ext = ".webp";
                        cleanBase64 = cleanBase64.substring(cleanBase64.indexOf(",") + 1);
                    }
                    byte[] bytes = java.util.Base64.getDecoder().decode(cleanBase64.replaceAll("\\s+", ""));
                    guardada = service.guardarImagenBytes(coArt, codigoBarra, bytes, ext);
                } else {
                    sendJsonResponse(exchange, 400,
                            "{\"success\":false,\"error\":\"Debe proporcionar una URL web o un archivo de imagen\"}");
                    return;
                }

                long v = System.currentTimeMillis();
                String targetId = (coArt != null && !coArt.isBlank()) ? coArt.trim() : codigoBarra.trim();
                String resp = String.format("""
                        {
                            "success": true,
                            "mensaje": "Foto actualizada correctamente en el catálogo.",
                            "archivo": "%s",
                            "imageUrl": "/api/imagen?id=%s&v=%d"
                        }
                        """, escapeJson(guardada.getFileName().toString()), escapeJson(targetId), v);
                sendJsonResponse(exchange, 200, resp);
            } catch (Throwable e) {
                if (isClientDisconnect(e))
                    return;
                System.err.println("❌ Error en ActualizarImagenHandler: " + e.getMessage());
                sendJsonResponse(exchange, 500, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // ── 5. INFORMACIÓN DE LA SESIÓN ──
    private class InfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                addCorsHeaders(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                String usuario = (SesionUsuario.current() != null && SesionUsuario.current().getCoUsuario() != null)
                        ? SesionUsuario.current().getCoUsuario().trim()
                        : "ADMIN";
                String nombre = (SesionUsuario.current() != null && SesionUsuario.current().getNombreUsuario() != null)
                        ? SesionUsuario.current().getNombreUsuario().trim()
                        : "Administrador";

                String host = "SRVDB0101";
                try {
                    host = InetAddress.getLocalHost().getHostName();
                } catch (Exception ignored) {
                }

                String localIp = "172.16.0.71";
                try {
                    for (java.util.Enumeration<java.net.NetworkInterface> en = java.net.NetworkInterface
                            .getNetworkInterfaces(); en.hasMoreElements();) {
                        java.net.NetworkInterface intf = en.nextElement();
                        if (intf.isLoopback() || !intf.isUp())
                            continue;
                        for (java.util.Enumeration<java.net.InetAddress> enumIp = intf.getInetAddresses(); enumIp
                                .hasMoreElements();) {
                            java.net.InetAddress addr = enumIp.nextElement();
                            if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                                String ipStr = addr.getHostAddress();
                                if (ipStr.startsWith("172.") || ipStr.startsWith("192.168.")
                                        || ipStr.startsWith("10.")) {
                                    localIp = ipStr;
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }

                String json = String.format("""
                        {
                            "usuario": "%s",
                            "nombreUsuario": "%s",
                            "maquina": "%s",
                            "baseDatos": "DROA_A",
                            "localIp": "%s",
                            "httpPort": %d,
                            "httpsPort": %d
                        }
                        """, escapeJson(usuario), escapeJson(nombre), escapeJson(host),
                        escapeJson(localIp), port, httpsPort);

                sendJsonResponse(exchange, 200, json);
            } catch (Throwable e) {
                if (isClientDisconnect(e))
                    return;
                System.err.println("❌ Error en InfoHandler: " + e.getMessage());
            }
        }
    }

    // ── 4. ARCHIVOS ESTÁTICOS DE LA WEB (HTML, CSS, JS) — CON CACHE + GZIP ──
    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                addCorsHeaders(exchange);
                String path = exchange.getRequestURI().getPath();

                if (path.equals("/") || path.isBlank()) {
                    path = "/index.html";
                }

                String resourcePath = "/web" + path;
                byte[] bytes = null;
                java.io.File diskFile = new java.io.File("src/main/resources" + resourcePath);
                if (diskFile.exists() && diskFile.isFile()) {
                    bytes = java.nio.file.Files.readAllBytes(diskFile.toPath());
                } else {
                    try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                        if (is != null) {
                            bytes = is.readAllBytes();
                        }
                    }
                }

                if (bytes == null) {
                    sendJsonResponse(exchange, 404, "{\"error\":\"Archivo no encontrado\"}");
                    return;
                }
                String contentType = getContentType(path);

                // ── Cache Headers ──
                if (path.endsWith(".html") || path.endsWith(".css") || path.endsWith(".js")) {
                    exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
                    exchange.getResponseHeaders().set("Pragma", "no-cache");
                    exchange.getResponseHeaders().set("Expires", "0");
                } else {
                    exchange.getResponseHeaders().set("Cache-Control", "public, max-age=604800"); // 7 días
                                                                                                  // (imágenes/iconos)
                }

                exchange.getResponseHeaders().set("Content-Type", contentType);

                // ── Gzip Compression para contenido de texto ──
                String acceptEncoding = exchange.getRequestHeaders().getFirst("Accept-Encoding");
                boolean supportsGzip = acceptEncoding != null && acceptEncoding.contains("gzip");
                boolean isText = contentType.startsWith("text/") || contentType.contains("javascript")
                        || contentType.contains("json") || contentType.contains("svg");

                if (supportsGzip && isText && bytes.length > 256) {
                    byte[] compressed = gzipCompress(bytes);
                    exchange.getResponseHeaders().set("Content-Encoding", "gzip");
                    exchange.getResponseHeaders().set("Vary", "Accept-Encoding");
                    exchange.sendResponseHeaders(200, compressed.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(compressed);
                    }
                } else {
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                }
            } catch (Throwable e) {
                if (isClientDisconnect(e))
                    return;
                System.err.println("⚠️ Error en StaticFileHandler: " + e.getMessage());
            }
        }
    }

    // ── 6. LISTADO DE CONFLICTOS DE UBICACIÓN (+1 PRODUCTO) ──
    private class ConflictosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                addCorsHeaders(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                List<UbicacionConflicto> conflictos = service.obtenerConflictos();
                long excelCount = conflictos.stream().filter(UbicacionConflicto::isEnExcel).count();

                StringBuilder sb = new StringBuilder();
                sb.append("{\"total\":").append(conflictos.size())
                        .append(",\"excelCount\":").append(excelCount)
                        .append(",\"conflictos\":[");

                for (int i = 0; i < conflictos.size(); i++) {
                    UbicacionConflicto c = conflictos.get(i);
                    if (i > 0)
                        sb.append(",");
                    sb.append("{")
                            .append("\"ubicacion\":\"").append(escapeJson(c.getUbicacion())).append("\",")
                            .append("\"cantidad\":").append(c.getCantidad()).append(",")
                            .append("\"enExcel\":").append(c.isEnExcel()).append(",")
                            .append("\"articulos\":[");

                    for (int j = 0; j < c.getArticulos().size(); j++) {
                        ArticuloUbicacionRow art = c.getArticulos().get(j);
                        if (j > 0)
                            sb.append(",");
                        sb.append("{")
                                .append("\"coArt\":\"").append(escapeJson(art.getCoArt())).append("\",")
                                .append("\"codigoBarra\":\"").append(escapeJson(art.getCodigoBarra())).append("\",")
                                .append("\"descripcion\":\"").append(escapeJson(art.getDescripcion())).append("\",")
                                .append("\"marca\":\"").append(escapeJson(art.getMarca())).append("\",")
                                .append("\"linea\":\"").append(escapeJson(art.getLinea())).append("\",")
                                .append("\"existencia\":").append(art.getExistencia()).append(",")
                                .append("\"stockAveria\":").append(art.getStockAveria()).append(",")
                                .append("\"stockVencido\":").append(art.getStockVencido()).append(",")
                                .append("\"stockTotal\":").append(art.getStockTotal()).append(",")
                                .append("\"ubicacion1\":\"").append(escapeJson(art.getUbicacion1())).append("\",")
                                .append("\"ubicacion2\":\"").append(escapeJson(art.getUbicacion2())).append("\",");

                        long imgVer = service.obtenerVersionImagen(art.getCoArt(), art.getCodigoBarra());
                        String imgUrl = (imgVer > 0)
                                ? "/api/imagen?id=" + escapeJson(art.getCoArt()) + "&v=" + imgVer
                                : "/api/imagen?id=" + escapeJson(art.getCoArt());

                        sb.append("\"imagenUrl\":\"").append(imgUrl).append("\"")
                                .append("}");
                    }
                    sb.append("]}");
                }
                sb.append("]}");

                sendJsonResponse(exchange, 200, sb.toString());
            } catch (Throwable e) {
                if (isClientDisconnect(e))
                    return;
                System.err.println("❌ Error en ConflictosHandler: " + e.getMessage());
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // ── 7. RESOLVER CONFLICTO (MANTENER, LIBERAR, REASIGNAR) ──
    private class ResolverConflictoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                addCorsHeaders(exchange);
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }

                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String ubi = getJsonFieldValue(body, "ubi");
                String coArt = getJsonFieldValue(body, "coArt");
                String accion = getJsonFieldValue(body, "accion");
                String nuevaUbi = getJsonFieldValue(body, "nuevaUbi");

                ResultadoGuardado res = service.resolverConflicto(ubi, coArt, accion, nuevaUbi);
                String respJson = String.format("{\"success\":%b,\"mensaje\":\"%s\"}",
                        res.isSuccess(), escapeJson(res.getMensaje()));
                sendJsonResponse(exchange, res.isSuccess() ? 200 : 400, respJson);
            } catch (Throwable e) {
                if (isClientDisconnect(e))
                    return;
                System.err.println("❌ Error en ResolverConflictoHandler: " + e.getMessage());
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) {
        try {
            byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } catch (IOException e) {
            if (isClientDisconnect(e)) {
                System.out.println("[WebAPI] ℹ️ Conexión cerrada por el cliente antes de recibir la respuesta.");
            } else {
                System.err.println("⚠️ Error al enviar respuesta JSON: " + e.getMessage());
            }
        }
    }

    private boolean isClientDisconnect(Throwable e) {
        if (e == null)
            return false;
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof IOException) {
                String msg = cause.getMessage();
                if (msg != null) {
                    String lower = msg.toLowerCase();
                    if (lower.contains("connection reset") ||
                            lower.contains("broken pipe") ||
                            lower.contains("aborted") ||
                            lower.contains("forcibly closed")) {
                        return true;
                    }
                }
            }
            cause = cause.getCause();
        }
        return false;
    }

    private String getContentType(String path) {
        if (path.endsWith(".html"))
            return "text/html; charset=UTF-8";
        if (path.endsWith(".css"))
            return "text/css; charset=UTF-8";
        if (path.endsWith(".js"))
            return "text/javascript; charset=UTF-8";

        if (path.endsWith(".png"))
            return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg"))
            return "image/jpeg";
        if (path.endsWith(".svg"))
            return "image/svg+xml";
        if (path.endsWith(".ico"))
            return "image/x-icon";
        return "text/plain; charset=UTF-8";
    }

    private String getQueryParam(String rawQuery, String paramName) {
        if (rawQuery == null || rawQuery.isBlank())
            return null;
        for (String pair : rawQuery.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2 && kv[0].equalsIgnoreCase(paramName)) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private String getJsonFieldValue(String json, String field) {
        if (json == null || field == null)
            return null;
        String pattern = "\"" + field + "\":\\s*\"(.*?)\"";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String escapeJson(String raw) {
        if (raw == null)
            return "";
        return raw.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private byte[] gzipCompress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length / 2);
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(data);
        }
        return baos.toByteArray();
    }
}
