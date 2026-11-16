package com.droubicaciones.service;

import com.droubicaciones.dao.UbicacionDAO;
import com.droubicaciones.model.ArticuloUbicacionRow;
import com.droubicaciones.model.ResultadoGuardado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Motor de Búsqueda e Índice en Memoria RAM para DroUbicaciones.
 * Mantiene los ~5.000 artículos indexados en memoria RAM para responder
 * consultas
 * de búsqueda, ubicaciones y códigos en menos de 2 milisegundos (< 2 ms).
 * 
 * Basado en la arquitectura recomendada para WMS/Inventarios de alta velocidad:
 * - Búsqueda O(1) en RAM para códigos y ubicaciones.
 * - Índice invertido por trigramas/palabras clave para nombres.
 * - Sincronización atómica inmediata en caliente al guardar ubicaciones.
 */
public class CatalogoMemoryService {

    private static final Logger logger = LoggerFactory.getLogger(CatalogoMemoryService.class);
    private static final CatalogoMemoryService INSTANCE = new CatalogoMemoryService();

    public static CatalogoMemoryService getInstance() {
        return INSTANCE;
    }

    private final UbicacionDAO dao = new UbicacionDAO();
    private final ImageCatalogService imageCatalog = ImageCatalogService.getInstance();

    // Estructuras en memoria RAM
    private final List<ArticuloUbicacionRow> allArticulos = new ArrayList<>();
    private final Map<String, ArticuloUbicacionRow> byCoArt = new ConcurrentHashMap<>();
    private final Map<String, ArticuloUbicacionRow> byBarcode = new ConcurrentHashMap<>();
    private final Map<String, List<ArticuloUbicacionRow>> byLocation = new ConcurrentHashMap<>();
    private final Map<String, Set<ArticuloUbicacionRow>> tokenIndex = new ConcurrentHashMap<>();

    private volatile boolean ready = false;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "CatalogoMemory-SyncThread");
        t.setDaemon(true);
        return t;
    });

    private CatalogoMemoryService() {
    }

    public synchronized void initialize() {
        if (ready)
            return;

        // 1. Iniciar indexación de imágenes en paralelo
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ImageCatalog-Init");
            t.setDaemon(true);
            return t;
        }).execute(imageCatalog::initialize);

        // 2. Cargar catálogo completo en memoria
        recargarCatalogo();

        // 3. Programar refresco silencioso en segundo plano cada 60 minutos
        scheduler.scheduleWithFixedDelay(this::refrescarStockSilencioso, 60, 60, TimeUnit.MINUTES);
    }

    public synchronized void recargarCatalogo() {
        long t0 = System.currentTimeMillis();
        logger.info("[CatalogoMemory] 🚀 Cargando catálogo de artículos en memoria RAM...");

        try {
            // Carga completa e instantánea de todos los artículos activos (~4.936 artículos)
            List<ArticuloUbicacionRow> rawList = dao.obtenerTodosLosArticulos();
            indexarArticulos(rawList);
            ready = true;
            long elapsed = System.currentTimeMillis() - t0;
            logger.info("[CatalogoMemory] ✅ Catálogo indexado en RAM: {} artículos en {} ms!", byCoArt.size(), elapsed);

        } catch (Exception e) {
            logger.error("[CatalogoMemory] ❌ Error al cargar catálogo en memoria: {}", e.getMessage(), e);
        }
    }

    private void indexarArticulos(List<ArticuloUbicacionRow> articulos) {
        synchronized (allArticulos) {
            allArticulos.clear();
            byCoArt.clear();
            byBarcode.clear();
            byLocation.clear();
            tokenIndex.clear();

            for (ArticuloUbicacionRow a : articulos) {
                allArticulos.add(a);

                // Indexar por co_art
                String co = a.getCoArt() != null ? a.getCoArt().trim().toUpperCase() : "";
                if (!co.isEmpty()) {
                    byCoArt.put(co, a);
                    String unpadded = co.replaceFirst("^0+", "");
                    if (!unpadded.isEmpty())
                        byCoArt.putIfAbsent(unpadded, a);
                }

                // Indexar por código de barra (ref)
                String rf = a.getCodigoBarra() != null ? a.getCodigoBarra().trim().toUpperCase() : "";
                if (!rf.isEmpty()) {
                    byBarcode.put(rf, a);
                }

                // Indexar por ubicación actual (campo5)
                String ubi = a.getUbicacion1() != null ? a.getUbicacion1().trim().toUpperCase() : "";
                if (!ubi.isEmpty()) {
                    byLocation.computeIfAbsent(ubi, k -> new ArrayList<>()).add(a);
                }

                // Indexar palabras clave para búsqueda full-text instantánea
                String fullText = (co + " " + rf + " " + a.getDescripcion() + " " + a.getMarca() + " " + a.getLinea())
                        .toLowerCase();
                for (String token : tokenize(fullText)) {
                    tokenIndex.computeIfAbsent(token, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(a);
                }
            }
        }
    }

    /**
     * Búsqueda en RAM de ultra-alta velocidad (< 2 ms).
     */
    public List<ArticuloUbicacionRow> buscar(String query) {
        if (!ready || query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        long t0 = System.nanoTime();
        String cleanQuery = query.trim().toUpperCase();
        List<ArticuloUbicacionRow> results = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();

        // 1. Coincidencia EXACTA por Código de Barras (ref) o Código Interno (co_art) -> MÁXIMA PRIORIDAD
        ArticuloUbicacionRow barcodeArt = byBarcode.get(cleanQuery);
        if (barcodeArt != null && seenCodes.add(barcodeArt.getCoArt())) {
            results.add(barcodeArt);
        }

        ArticuloUbicacionRow directArt = byCoArt.get(cleanQuery);
        if (directArt != null && seenCodes.add(directArt.getCoArt())) {
            results.add(directArt);
        }

        // 2. Si parece código de ubicación o coincide con una ubicación existente:
        boolean isLocation = looksLikeLocationCode(cleanQuery) || byLocation.containsKey(cleanQuery);
        if (isLocation && results.size() < 50) {
            // A. Coincidencia exacta de ubicación
            List<ArticuloUbicacionRow> enUbi = byLocation.get(cleanQuery);
            if (enUbi != null) {
                for (ArticuloUbicacionRow art : enUbi) {
                    if (seenCodes.add(art.getCoArt())) {
                        results.add(art);
                    }
                }
            }

            // B. Coincidencia por prefijo de pasillo/estante (ej. "P5D")
            for (Map.Entry<String, List<ArticuloUbicacionRow>> entry : byLocation.entrySet()) {
                if (entry.getKey().startsWith(cleanQuery) && !entry.getKey().equals(cleanQuery)) {
                    for (ArticuloUbicacionRow art : entry.getValue()) {
                        if (seenCodes.add(art.getCoArt())) {
                            results.add(art);
                            if (results.size() >= 50)
                                break;
                        }
                    }
                }
                if (results.size() >= 50)
                    break;
            }
        }

        // 3. Búsqueda Full-Text por palabras clave usando el índice invertido en RAM
        if (results.size() < 50 && !isLocation) {
            List<String> tokens = tokenize(query.toLowerCase());
            if (!tokens.isEmpty()) {
                Set<ArticuloUbicacionRow> candidateSet = null;

                for (String t : tokens) {
                    Set<ArticuloUbicacionRow> matchingThisToken = new HashSet<>();
                    // Buscar coincidencia exacta de token o prefijo en el índice
                    for (Map.Entry<String, Set<ArticuloUbicacionRow>> entry : tokenIndex.entrySet()) {
                        if (entry.getKey().startsWith(t) || entry.getKey().contains(t)) {
                            matchingThisToken.addAll(entry.getValue());
                        }
                    }

                    if (candidateSet == null) {
                        candidateSet = matchingThisToken;
                    } else {
                        candidateSet.retainAll(matchingThisToken); // Intersección (AND)
                    }

                    if (candidateSet.isEmpty())
                        break;
                }

                if (candidateSet != null) {
                    for (ArticuloUbicacionRow art : candidateSet) {
                        if (seenCodes.add(art.getCoArt())) {
                            results.add(art);
                            if (results.size() >= 50)
                                break;
                        }
                    }
                }
            }
        }

        long nanos = System.nanoTime() - t0;
        double millis = nanos / 1_000_000.0;
        logger.debug("[CatalogoMemory] Búsqueda '{}' completada en {:.3f} ms con {} resultados.", query, millis,
                results.size());

        return results;
    }

    /**
     * Guarda en SQL Server y actualiza el caché en memoria en caliente sin recargas
     * pesadas.
     */
    public ResultadoGuardado guardarUbicaciones(String coArt, String ubi1, String ubi2) throws SQLException {
        // 1. Guardar atómicamente en SQL Server
        ResultadoGuardado resultado = dao.guardarUbicaciones(coArt, ubi1, ubi2);

        // 2. Si fue exitoso, sincronizar de inmediato en RAM
        if (resultado.isSuccess()) {
            actualizarEnMemoria(coArt, ubi1, ubi2, resultado.getDesalojados());
        }

        return resultado;
    }

    private void actualizarEnMemoria(String coArt, String ubi1, String ubi2,
            List<ResultadoGuardado.ArticuloSimple> desalojados) {
        String ubi1Clean = ubi1 != null ? ubi1.trim().toUpperCase() : "";
        String ubi2Clean = ubi2 != null ? ubi2.trim().toUpperCase() : "";

        // A. Actualizar artículos desalojados (quedan con campo5 = '')
        if (desalojados != null) {
            for (ResultadoGuardado.ArticuloSimple d : desalojados) {
                ArticuloUbicacionRow artD = byCoArt.get(d.getCoArt().trim().toUpperCase());
                if (artD != null) {
                    String oldUbi = artD.getUbicacion1();
                    artD.setUbicacion2(oldUbi);
                    artD.setUbicacion1("");

                    // Remover del índice de ubicaciones
                    if (oldUbi != null && !oldUbi.isEmpty()) {
                        List<ArticuloUbicacionRow> list = byLocation.get(oldUbi.toUpperCase());
                        if (list != null) {
                            list.removeIf(a -> a.getCoArt().equalsIgnoreCase(artD.getCoArt()));
                            if (list.isEmpty())
                                byLocation.remove(oldUbi.toUpperCase());
                        }
                    }
                }
            }
        }

        // B. Actualizar artículo destino
        ArticuloUbicacionRow art = byCoArt.get(coArt.trim().toUpperCase());
        if (art != null) {
            String oldUbi = art.getUbicacion1();
            art.setUbicacion1(ubi1Clean);
            art.setUbicacion2(ubi2Clean.isEmpty() ? oldUbi : ubi2Clean);

            // Actualizar mapa byLocation
            if (oldUbi != null && !oldUbi.isEmpty()) {
                List<ArticuloUbicacionRow> list = byLocation.get(oldUbi.toUpperCase());
                if (list != null) {
                    list.removeIf(a -> a.getCoArt().equalsIgnoreCase(art.getCoArt()));
                    if (list.isEmpty())
                        byLocation.remove(oldUbi.toUpperCase());
                }
            }

            if (!ubi1Clean.isEmpty()) {
                byLocation.computeIfAbsent(ubi1Clean, k -> new ArrayList<>()).add(art);
            }
        }

        logger.info("[CatalogoMemory] ✔ Sincronización en caliente completada para artículo {}", coArt);
    }

    private void refrescarStockSilencioso() {
        try {
            logger.info("[CatalogoMemory] 🔄 Sincronizando catálogo en segundo plano...");
            recargarCatalogo();
        } catch (Exception e) {
            logger.warn("[CatalogoMemory] Refresco en segundo plano no pudo completarse: {}", e.getMessage());
        }
    }

    public void triggerRefrescoAsync() {
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "CatalogoMemory-ManualSync");
            t.setDaemon(true);
            return t;
        }).submit(this::recargarCatalogo);
    }

    private boolean looksLikeLocationCode(String text) {
        if (text == null || text.length() < 2)
            return false;
        // Si contiene solo dígitos (código de barra EAN o código numérico), NUNCA es ubicación
        if (text.matches("^[0-9]+$"))
            return false;
        return text.matches("(?i)^[A-Z][0-9].*");
    }

    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null)
            return tokens;
        String[] parts = text.replaceAll("[^a-zA-Z0-9áéíóúüñÁÉÍÓÚÜÑ]", " ").split("\\s+");
        for (String p : parts) {
            if (p.length() >= 2) {
                tokens.add(p);
            }
        }
        return tokens;
    }
}
