package com.droubicaciones.service;

import com.droubicaciones.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de Indexación y Entrega Instantánea de Imágenes de Productos.
 * Mapea imágenes nombradas por código de barras, código interno o nombres de
 * producto
 * hacia los identificadores primarios (co_art y ref) para consulta O(1) en
 * memoria.
 */
public class ImageCatalogService {

    private static final Logger logger = LoggerFactory.getLogger(ImageCatalogService.class);
    private static final ImageCatalogService INSTANCE = new ImageCatalogService();

    public static ImageCatalogService getInstance() {
        return INSTANCE;
    }

    // Mapeos en memoria para respuesta en 0 ms
    private final Map<String, Path> imageByCode = new ConcurrentHashMap<>();
    private final Map<String, Path> imageByBarcode = new ConcurrentHashMap<>();

    private Path sourceDir;
    private Path localProjectDir;
    private boolean initialized = false;

    private ImageCatalogService() {
        this.localProjectDir = Paths.get("imagenes").toAbsolutePath();
        // Directorio fuente de origen
        String userHome = System.getProperty("user.home", "C:\\Users\\jeancarlos");
        this.sourceDir = Paths.get(userHome, "Documents", "Neyber", "IMAGENES");
        if (!Files.exists(sourceDir)) {
            this.sourceDir = localProjectDir;
        }
    }

    public synchronized void initialize() {
        if (initialized)
            return;

        long t0 = System.currentTimeMillis();
        logger.info("[ImageCatalog] 🚀 Iniciando indexación de imágenes de productos...");

        try {
            // Asegurar directorio local de imágenes
            if (!Files.exists(localProjectDir)) {
                Files.createDirectories(localProjectDir);
            }

            // 1. Obtener todos los artículos activos desde la BD
            List<ArticuloSimpleMeta> articulos = loadArticulosMeta();
            logger.info("[ImageCatalog] ✔ Artículos cargados desde SQL Server: {}", articulos.size());

            // 2. Escanear directorio de imágenes (primero en localProjectDir, luego
            // sourceDir)
            List<File> imageFiles = scanImageFiles();
            logger.info("[ImageCatalog] ✔ Archivos de imagen encontrados: {}", imageFiles.size());

            // 3. Indexar imágenes numéricas (código de barra / co_art directo)
            // y preparar estructuras para matching de nombres
            Map<String, File> filesByBarcode = new HashMap<>();
            List<ImageFileMeta> textImageFiles = new ArrayList<>();

            for (File f : imageFiles) {
                String baseName = getBaseName(f.getName());
                if (baseName.matches("^\\d+$")) {
                    filesByBarcode.put(baseName, f);
                    // Si tiene padding con ceros o sin ceros, registrar variaciones
                    String unpadded = baseName.replaceFirst("^0+", "");
                    if (!unpadded.isEmpty()) {
                        filesByBarcode.putIfAbsent(unpadded, f);
                    }
                } else {
                    String norm = normalize(baseName);
                    Set<String> tokens = extractTokens(norm);
                    textImageFiles.add(new ImageFileMeta(f, tokens));
                }
            }

            int matchBarcode = 0;
            int matchText = 0;

            // 4. Mapear cada artículo a su imagen correspondiente
            for (ArticuloSimpleMeta art : articulos) {
                File matchedFile = null;

                // A. Coincidencia por Código de Barras (ref)
                if (art.ref != null && !art.ref.isBlank()) {
                    matchedFile = filesByBarcode.get(art.ref);
                }

                // B. Coincidencia por Código Interno (co_art)
                if (matchedFile == null && art.coArt != null && !art.coArt.isBlank()) {
                    matchedFile = filesByBarcode.get(art.coArt);
                    if (matchedFile == null) {
                        String unpadded = art.coArt.replaceFirst("^0+", "");
                        if (!unpadded.isEmpty())
                            matchedFile = filesByBarcode.get(unpadded);
                    }
                }

                if (matchedFile != null) {
                    matchBarcode++;
                } else {
                    // C. Coincidencia Inteligente por Descripción (Token Overlap)
                    matchedFile = findBestMatch(art, textImageFiles);
                    if (matchedFile != null) {
                        matchText++;
                    }
                }

                // Si encontramos coincidencia, guardamos en los mapas en memoria
                if (matchedFile != null) {
                    Path p = matchedFile.toPath();
                    if (art.coArt != null && !art.coArt.isBlank()) {
                        imageByCode.put(art.coArt, p);
                    }
                    if (art.ref != null && !art.ref.isBlank()) {
                        imageByBarcode.put(art.ref, p);
                    }
                }
            }

            initialized = true;
            long elapsed = System.currentTimeMillis() - t0;
            logger.info(
                    "[ImageCatalog] ✅ Indexación completada en {} ms! Total con imagen: {} (Por Código/Barra: {}, Por Nombre: {})",
                    elapsed, imageByCode.size(), matchBarcode, matchText);

        } catch (Exception e) {
            logger.error("[ImageCatalog] ❌ Error durante la indexación de imágenes: {}", e.getMessage(), e);
        }
    }

    public Path getImageForArticle(String coArt, String codigoBarra) {
        if (!initialized) {
            initialize();
        }

        if (coArt != null && !coArt.isBlank()) {
            Path p = imageByCode.get(coArt.trim());
            if (p != null)
                return p;
        }

        if (codigoBarra != null && !codigoBarra.isBlank()) {
            Path p = imageByBarcode.get(codigoBarra.trim());
            if (p != null)
                return p;
        }

        return null;
    }

    public Path getImageById(String id) {
        if (id == null || id.isBlank())
            return null;
        return getImageForArticle(id.trim(), id.trim());
    }

    public int getIndexedCount() {
        return imageByCode.size();
    }

    /**
     * Descarga y guarda una imagen desde una URL web para un artículo específico.
     */
    public Path guardarImagenDesdeUrl(String coArt, String codigoBarra, String urlStr) throws IOException {
        if (urlStr == null || urlStr.isBlank()) {
            throw new IllegalArgumentException("La URL de la imagen no puede estar vacía");
        }

        java.net.URI uri = java.net.URI.create(urlStr.trim());
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .followRedirects(java.net.http.HttpClient.Redirect.ALWAYS)
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(uri)
                .timeout(java.time.Duration.ofSeconds(15))
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                .GET()
                .build();

        try {
            java.net.http.HttpResponse<byte[]> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                byte[] bytes = response.body();
                String ext = detectImageExtension(bytes, urlStr);
                return guardarImagenBytes(coArt, codigoBarra, bytes, ext);
            } else {
                throw new IOException("El servidor de la imagen respondió con código HTTP " + response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Descarga cancelada o interrumpida", e);
        }
    }

    /**
     * Guarda bytes de imagen para un artículo y actualiza los índices en memoria.
     */
    public Path guardarImagenBytes(String coArt, String codigoBarra, byte[] bytes, String ext) throws IOException {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("No se recibieron datos de imagen");
        }

        if (!Files.exists(localProjectDir)) {
            Files.createDirectories(localProjectDir);
        }

        // Extensión por defecto
        if (ext == null || ext.isBlank()) {
            ext = ".jpg";
        }
        if (!ext.startsWith(".")) {
            ext = "." + ext;
        }

        // Limpiar archivos anteriores con distintas extensiones para evitar
        // ambigüedades
        String[] possibleExts = { ".jpg", ".jpeg", ".png", ".webp", ".jfif" };
        for (String oldExt : possibleExts) {
            if (!oldExt.equalsIgnoreCase(ext)) {
                if (coArt != null && !coArt.isBlank()) {
                    try {
                        Files.deleteIfExists(localProjectDir.resolve(coArt.trim() + oldExt));
                    } catch (Exception ignored) {
                    }
                }
                if (codigoBarra != null && !codigoBarra.isBlank()) {
                    try {
                        Files.deleteIfExists(localProjectDir.resolve(codigoBarra.trim() + oldExt));
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // Nombre de archivo preferido: Código de barra si existe, sino código interno
        String baseName = (codigoBarra != null && !codigoBarra.isBlank()) ? codigoBarra.trim() : coArt.trim();
        String fileName = baseName + ext;
        Path targetPath = localProjectDir.resolve(fileName);

        Files.write(targetPath, bytes);

        // Actualizar mapeos en memoria RAM inmediatamente (0 ms)
        if (coArt != null && !coArt.isBlank()) {
            imageByCode.put(coArt.trim(), targetPath);
        }
        if (codigoBarra != null && !codigoBarra.isBlank()) {
            imageByBarcode.put(codigoBarra.trim(), targetPath);
        }

        // También guardar copia con co_art si difiere de baseName
        if (coArt != null && !coArt.isBlank() && !coArt.trim().equalsIgnoreCase(baseName)) {
            Path altPath = localProjectDir.resolve(coArt.trim() + ext);
            Files.write(altPath, bytes);
            imageByCode.put(coArt.trim(), altPath);
        }

        logger.info("[ImageCatalog] ✔ Imagen actualizada con éxito para art {} / ref {}: {}", coArt, codigoBarra,
                targetPath.getFileName());
        return targetPath;
    }

    public long getImageVersion(String coArt, String codigoBarra) {
        Path p = getImageForArticle(coArt, codigoBarra);
        if (p != null && Files.exists(p)) {
            try {
                return Files.getLastModifiedTime(p).toMillis();
            } catch (Exception ignored) {
            }
        }
        return 0L;
    }

    private String detectImageExtension(byte[] bytes, String url) {
        if (bytes.length >= 3 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) {
            return ".jpg";
        }
        if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50 && bytes[2] == (byte) 0x4E
                && bytes[3] == (byte) 0x47) {
            return ".png";
        }
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' &&
                bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return ".webp";
        }
        if (url != null) {
            String lower = url.toLowerCase();
            if (lower.contains(".png"))
                return ".png";
            if (lower.contains(".webp"))
                return ".webp";
            if (lower.contains(".jpeg"))
                return ".jpeg";
        }
        return ".jpg";
    }

    // ── MÉTODOS DE BÚSQUEDA Y MATCHING ──

    private File findBestMatch(ArticuloSimpleMeta art, List<ImageFileMeta> candidates) {
        if (art.normDes.isEmpty() || art.tokens.isEmpty())
            return null;

        File bestFile = null;
        double bestScore = 0.0;

        for (ImageFileMeta cand : candidates) {
            if (cand.tokens.isEmpty())
                continue;

            // Contar tokens en común
            int overlap = 0;
            for (String t : cand.tokens) {
                if (art.tokens.contains(t)) {
                    overlap++;
                }
            }

            // Requiere al menos 2 tokens en común
            if (overlap >= 2) {
                double ratio = (double) overlap / cand.tokens.size();
                if (ratio >= 0.50) { // Al menos 50% de las palabras clave de la imagen coinciden
                    double score = overlap * 10.0 + ratio;
                    if (score > bestScore) {
                        bestScore = score;
                        bestFile = cand.file;
                    }
                }
            }
        }

        return bestFile;
    }

    private List<ArticuloSimpleMeta> loadArticulosMeta() {
        List<ArticuloSimpleMeta> list = new ArrayList<>();
        String sql = "SELECT RTRIM(co_art) AS co_art, RTRIM(art_des) AS art_des, RTRIM(ISNULL(ref, '')) AS ref FROM saArticulo WHERE anulado = 0";

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String co = rs.getString("co_art");
                String des = rs.getString("art_des");
                String rf = rs.getString("ref");
                String norm = normalize(des);
                Set<String> tokens = extractTokens(norm);
                list.add(new ArticuloSimpleMeta(co, rf, norm, tokens));
            }
        } catch (Exception e) {
            logger.error("[ImageCatalog] Error al cargar saArticulo: {}", e.getMessage());
        }
        return list;
    }

    private List<File> scanImageFiles() {
        List<File> list = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();

        // 1. Escanear directorio local del proyecto
        if (Files.exists(localProjectDir)) {
            File[] localFiles = localProjectDir.toFile().listFiles((dir, name) -> isImageFile(name));
            if (localFiles != null) {
                for (File f : localFiles) {
                    if (seenNames.add(f.getName().toLowerCase())) {
                        list.add(f);
                    }
                }
            }
        }

        // 2. Escanear directorio fuente externo
        if (Files.exists(sourceDir) && !sourceDir.equals(localProjectDir)) {
            File[] srcFiles = sourceDir.toFile().listFiles((dir, name) -> isImageFile(name));
            if (srcFiles != null) {
                for (File f : srcFiles) {
                    if (seenNames.add(f.getName().toLowerCase())) {
                        list.add(f);
                    }
                }
            }
        }

        return list;
    }

    private boolean isImageFile(String name) {
        String n = name.toLowerCase();
        return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".jfif")
                || n.endsWith(".webp");
    }

    private String getBaseName(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private String normalize(String s) {
        if (s == null)
            return "";
        String norm = s.toLowerCase();
        norm = norm.replaceAll("[áàäâ]", "a")
                .replaceAll("[éèëê]", "e")
                .replaceAll("[íìïî]", "i")
                .replaceAll("[óòöô]", "o")
                .replaceAll("[úùüû]", "u")
                .replaceAll("[ñ]", "n")
                .replaceAll("[^a-z0-9]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return norm;
    }

    private Set<String> extractTokens(String norm) {
        Set<String> tokens = new HashSet<>();
        for (String w : norm.split(" ")) {
            if (w.length() >= 3) {
                tokens.add(w);
            }
        }
        return tokens;
    }

    // Estructuras auxiliares
    private static class ArticuloSimpleMeta {
        final String coArt;
        final String ref;
        final String normDes;
        final Set<String> tokens;

        ArticuloSimpleMeta(String coArt, String ref, String normDes, Set<String> tokens) {
            this.coArt = coArt;
            this.ref = ref;
            this.normDes = normDes;
            this.tokens = tokens;
        }
    }

    private static class ImageFileMeta {
        final File file;
        final Set<String> tokens;

        ImageFileMeta(File file, Set<String> tokens) {
            this.file = file;
            this.tokens = tokens;
        }
    }
}
