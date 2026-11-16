package com.droubicaciones.service;

import com.droubicaciones.dao.UbicacionDAO;
import com.droubicaciones.model.ArticuloUbicacionRow;
import com.droubicaciones.model.ResultadoGuardado;
import com.droubicaciones.model.UbicacionConflicto;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UbicacionService {

    private final UbicacionDAO dao;
    private final CatalogoMemoryService memoryService;
    private final ImageCatalogService imageCatalog;
    private final Set<String> excelConflictUbis = new HashSet<>();

    public UbicacionService() {
        this.dao = new UbicacionDAO();
        this.memoryService = CatalogoMemoryService.getInstance();
        this.imageCatalog = ImageCatalogService.getInstance();
        cargarUbisExcel();
    }

    private void cargarUbisExcel() {
        try (InputStream is = getClass().getResourceAsStream("/conflictos_excel.json")) {
            if (is != null) {
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Matcher m = Pattern.compile("\"ubicacion\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
                while (m.find()) {
                    excelConflictUbis.add(m.group(1).trim().toUpperCase());
                }
            }
        } catch (Exception ignored) {}
    }

    public void inicializarServicios() {
        memoryService.initialize();
    }

    public ArticuloUbicacionRow buscarArticulo(String busqueda) throws SQLException {
        if (busqueda == null || busqueda.isBlank()) {
            return null;
        }
        List<ArticuloUbicacionRow> lista = buscarArticulos(busqueda);
        return lista.isEmpty() ? null : lista.get(0);
    }

    public List<ArticuloUbicacionRow> buscarArticulos(String busqueda) throws SQLException {
        if (busqueda == null || busqueda.isBlank()) {
            return List.of();
        }

        // 1. Intentar consulta en memoria RAM (< 2 ms)
        List<ArticuloUbicacionRow> memList = memoryService.buscar(busqueda);
        if (!memList.isEmpty()) {
            return memList;
        }

        // 2. Fallback a base de datos relacional si el caché aún no está listo o no hubo coincidencias
        return dao.buscarArticulos(busqueda);
    }

    public ResultadoGuardado guardarUbicaciones(String coArt, String ubicacion1, String ubicacion2) throws SQLException {
        if (coArt == null || coArt.isBlank()) {
            return new ResultadoGuardado(false, "Código de artículo requerido");
        }
        return memoryService.guardarUbicaciones(coArt, ubicacion1, ubicacion2);
    }

    public List<ResultadoGuardado.ArticuloSimple> verificarOcupacionUbicacion(String ubicacion, String coArtExcluir) throws SQLException {
        if (ubicacion == null || ubicacion.isBlank()) {
            return List.of();
        }
        return dao.verificarOcupacionUbicacion(ubicacion, coArtExcluir);
    }

    public Path obtenerImagenArticulo(String id) {
        return imageCatalog.getImageById(id);
    }

    public Path guardarImagenDesdeUrl(String coArt, String codigoBarra, String url) throws java.io.IOException {
        return imageCatalog.guardarImagenDesdeUrl(coArt, codigoBarra, url);
    }

    public Path guardarImagenBytes(String coArt, String codigoBarra, byte[] bytes, String ext) throws java.io.IOException {
        return imageCatalog.guardarImagenBytes(coArt, codigoBarra, bytes, ext);
    }

    public long obtenerVersionImagen(String coArt, String codigoBarra) {
        return imageCatalog.getImageVersion(coArt, codigoBarra);
    }

    public List<UbicacionConflicto> obtenerConflictos() throws SQLException {
        if (excelConflictUbis.isEmpty()) {
            cargarUbisExcel();
        }
        return dao.obtenerUbicacionesEnConflicto(excelConflictUbis);
    }

    public ResultadoGuardado resolverConflicto(String ubi, String coArt, String accion, String nuevaUbi) throws SQLException {
        ResultadoGuardado res = dao.resolverConflicto(ubi, coArt, accion, nuevaUbi);
        if (res.isSuccess()) {
            memoryService.triggerRefrescoAsync();
        }
        return res;
    }
}
