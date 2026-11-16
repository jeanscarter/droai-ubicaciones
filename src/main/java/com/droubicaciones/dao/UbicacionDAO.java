package com.droubicaciones.dao;

import com.droubicaciones.config.DatabaseConfig;
import com.droubicaciones.model.ArticuloUbicacionRow;
import com.droubicaciones.model.ResultadoGuardado;
import com.droubicaciones.model.SesionUsuario;
import com.droubicaciones.model.UbicacionConflicto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class UbicacionDAO {

    /**
     * Busca productos por código interno (co_art), código de barras (ref),
     * nombre/descripción (art_des) o pasillo/ubicación actual (campo5).
     * 
     * Optimización: Query unificada con OUTER APPLY (en vez de subconsultas
     * con full table scan) y prioridad de coincidencia exacta vs LIKE.
     * Una sola conexión al pool por búsqueda.
     */
    /**
     * Detecta si un texto tiene formato de código de ubicación de almacén.
     * Patrones válidos: P2A201, A3B501, C1D102, T2E301, I001, p5L104, 8xxxx
     */
    private boolean looksLikeLocationCode(String text) {
        if (text == null || text.length() < 2)
            return false;
        // Si contiene solo dígitos (código de barra EAN o código numérico), NUNCA es ubicación
        if (text.matches("^[0-9]+$"))
            return false;
        // Ubicaciones de almacén (ej: P4F401, E1C208, A3B501, etc.)
        return text.matches("(?i)^[A-Z][0-9].*");
    }

    public List<ArticuloUbicacionRow> obtenerTodosLosArticulos() throws SQLException {
        List<ArticuloUbicacionRow> lista = new ArrayList<>();
        String sql = """
                SELECT
                    a.co_art                        AS codigo,
                    ISNULL(a.ref, '')               AS codigoBarra,
                    ISNULL(a.art_des, '')           AS descripcion,
                    ISNULL(prov.prov_des, '')        AS marca,
                    ISNULL(l.lin_des, '')           AS linea,
                    ISNULL(stk.stockDisponible, 0)  AS existencia,
                    ISNULL(stk.stockAveria, 0)      AS stockAveria,
                    ISNULL(stk.stockVencido, 0)     AS stockVencido,
                    ISNULL(stk.totalStock, 0)       AS stockTotal,
                    ISNULL(a.campo5, '')            AS ubicacion1,
                    ISNULL(a.campo6, '')            AS ubicacion2
                FROM saArticulo a
                OUTER APPLY (
                    SELECT TOP 1 ap2.co_prov
                    FROM saArtProveedorReng ap2
                    WHERE ap2.co_art = a.co_art
                    ORDER BY ap2.reng_num
                ) ap
                LEFT JOIN saProveedor prov ON ap.co_prov = prov.co_prov
                LEFT JOIN saLineaArticulo l ON a.co_lin = l.co_lin
                OUTER APPLY (
                    SELECT
                        ISNULL(SUM(CASE WHEN s.co_alma NOT IN ('AV', 'VP', 'VC', '9999') THEN s.stock ELSE 0 END), 0) AS stockDisponible,
                        ISNULL(SUM(CASE WHEN s.co_alma = 'AV' THEN s.stock ELSE 0 END), 0) AS stockAveria,
                        ISNULL(SUM(CASE WHEN s.co_alma IN ('VP', 'VC') THEN s.stock ELSE 0 END), 0) AS stockVencido,
                        ISNULL(SUM(s.stock), 0) AS totalStock
                    FROM saStockAlmacen s
                    WHERE s.co_art = a.co_art
                      AND s.tipo = 'ACT'
                ) stk
                WHERE a.anulado = 0
                ORDER BY a.co_art
                """;

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapRow(rs));
            }
        }
        return lista;
    }

    public List<ArticuloUbicacionRow> buscarArticulos(String busqueda) throws SQLException {
        List<ArticuloUbicacionRow> lista = new ArrayList<>();
        if (busqueda == null || busqueda.isBlank()) {
            return lista;
        }

        String queryClean = busqueda.trim();
        String queryLike = "%" + queryClean + "%";
        boolean isLocationSearch = looksLikeLocationCode(queryClean);

        // Si el término parece una ubicación, buscar en campo5 (ubicación actual)
        // pero asegurando coincidencia si también coincide con ref o co_art.
        String sql;
        if (isLocationSearch) {
            sql = """
                    SELECT TOP 50
                        a.co_art                        AS codigo,
                        ISNULL(a.ref, '')               AS codigoBarra,
                        ISNULL(a.art_des, '')           AS descripcion,
                        ISNULL(prov.prov_des, '')        AS marca,
                        ISNULL(l.lin_des, '')           AS linea,
                        ISNULL(stk.stockDisponible, 0)  AS existencia,
                        ISNULL(stk.stockAveria, 0)      AS stockAveria,
                        ISNULL(stk.stockVencido, 0)     AS stockVencido,
                        ISNULL(stk.totalStock, 0)       AS stockTotal,
                        ISNULL(a.campo5, '')            AS ubicacion1,
                        ISNULL(a.campo6, '')            AS ubicacion2,
                        CASE
                            WHEN a.co_art = ? OR a.ref = ? THEN 0
                            WHEN a.campo5 = ? THEN 1
                            ELSE 2
                        END AS prioridad
                    FROM saArticulo a
                    OUTER APPLY (
                        SELECT TOP 1 ap2.co_prov
                        FROM saArtProveedorReng ap2
                        WHERE ap2.co_art = a.co_art
                        ORDER BY ap2.reng_num
                    ) ap
                    LEFT JOIN saProveedor prov ON ap.co_prov = prov.co_prov
                    LEFT JOIN saLineaArticulo l ON a.co_lin = l.co_lin
                    OUTER APPLY (
                        SELECT
                            ISNULL(SUM(CASE WHEN s.co_alma NOT IN ('AV', 'VP', 'VC', '9999') THEN s.stock ELSE 0 END), 0) AS stockDisponible,
                            ISNULL(SUM(CASE WHEN s.co_alma = 'AV' THEN s.stock ELSE 0 END), 0) AS stockAveria,
                            ISNULL(SUM(CASE WHEN s.co_alma IN ('VP', 'VC') THEN s.stock ELSE 0 END), 0) AS stockVencido,
                            ISNULL(SUM(s.stock), 0) AS totalStock
                        FROM saStockAlmacen s
                        WHERE s.co_art = a.co_art
                          AND s.tipo = 'ACT'
                    ) stk
                    WHERE a.anulado = 0
                      AND (a.campo5 = ? OR a.campo5 LIKE ? OR a.ref = ? OR a.co_art = ?)
                    ORDER BY prioridad, a.co_art
                    """;
        } else {
            sql = """
                    SELECT TOP 50
                        a.co_art                        AS codigo,
                        ISNULL(a.ref, '')               AS codigoBarra,
                        ISNULL(a.art_des, '')           AS descripcion,
                        ISNULL(prov.prov_des, '')        AS marca,
                        ISNULL(l.lin_des, '')           AS linea,
                        ISNULL(stk.stockDisponible, 0)  AS existencia,
                        ISNULL(stk.stockAveria, 0)      AS stockAveria,
                        ISNULL(stk.stockVencido, 0)     AS stockVencido,
                        ISNULL(stk.totalStock, 0)       AS stockTotal,
                        ISNULL(a.campo5, '')            AS ubicacion1,
                        ISNULL(a.campo6, '')            AS ubicacion2,
                        CASE
                            WHEN a.co_art = ? OR a.ref = ? THEN 0
                            WHEN a.campo5 = ? THEN 1
                            ELSE 2
                        END AS prioridad
                    FROM saArticulo a
                    OUTER APPLY (
                        SELECT TOP 1 ap2.co_prov
                        FROM saArtProveedorReng ap2
                        WHERE ap2.co_art = a.co_art
                        ORDER BY ap2.reng_num
                    ) ap
                    LEFT JOIN saProveedor prov ON ap.co_prov = prov.co_prov
                    LEFT JOIN saLineaArticulo l ON a.co_lin = l.co_lin
                    OUTER APPLY (
                        SELECT
                            ISNULL(SUM(CASE WHEN s.co_alma NOT IN ('AV', 'VP', 'VC', '9999') THEN s.stock ELSE 0 END), 0) AS stockDisponible,
                            ISNULL(SUM(CASE WHEN s.co_alma = 'AV' THEN s.stock ELSE 0 END), 0) AS stockAveria,
                            ISNULL(SUM(CASE WHEN s.co_alma IN ('VP', 'VC') THEN s.stock ELSE 0 END), 0) AS stockVencido,
                            ISNULL(SUM(s.stock), 0) AS totalStock
                        FROM saStockAlmacen s
                        WHERE s.co_art = a.co_art
                          AND s.tipo = 'ACT'
                    ) stk
                    WHERE a.anulado = 0
                      AND (a.co_art = ? OR a.ref = ?
                           OR a.co_art LIKE ? OR a.ref LIKE ?
                           OR a.art_des LIKE ?
                           OR a.campo5 LIKE ?)
                    ORDER BY prioridad, a.co_art
                    """;
        }

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            if (isLocationSearch) {
                // Parámetros para CASE (prioridad)
                ps.setString(1, queryClean);
                ps.setString(2, queryClean);
                ps.setString(3, queryClean);
                // Parámetros para WHERE: campo5 = ? OR campo5 LIKE ? OR ref = ? OR co_art = ?
                ps.setString(4, queryClean);
                ps.setString(5, queryLike);
                ps.setString(6, queryClean);
                ps.setString(7, queryClean);
            } else {
                // Parámetros para CASE (prioridad)
                ps.setString(1, queryClean);
                ps.setString(2, queryClean);
                ps.setString(3, queryClean);
                // Parámetros para WHERE (exacto)
                ps.setString(4, queryClean);
                ps.setString(5, queryClean);
                // Parámetros para WHERE (LIKE)
                ps.setString(6, queryLike);
                ps.setString(7, queryLike);
                ps.setString(8, queryLike);
                ps.setString(9, queryLike);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapRow(rs));
                }
            }
        }

        return lista;
    }

    public ArticuloUbicacionRow buscarArticulo(String busqueda) throws SQLException {
        List<ArticuloUbicacionRow> res = buscarArticulos(busqueda);
        return res.isEmpty() ? null : res.get(0);
    }

    /**
     * Consulta si una ubicación ya está asignada a otro producto activo.
     */
    public List<ResultadoGuardado.ArticuloSimple> verificarOcupacionUbicacion(String ubicacion, String coArtExcluir)
            throws SQLException {
        List<ResultadoGuardado.ArticuloSimple> lista = new ArrayList<>();
        if (ubicacion == null || ubicacion.isBlank()) {
            return lista;
        }

        String sql = """
                SELECT a.co_art, a.art_des,
                       ISNULL((SELECT SUM(stock) FROM saStockAlmacen s WHERE s.co_art = a.co_art AND s.tipo = 'ACT'), 0) as stock
                FROM saArticulo a
                WHERE a.campo5 = ? AND a.co_art <> ? AND a.anulado = 0
                """;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, ubicacion.trim());
            ps.setString(2, coArtExcluir != null ? coArtExcluir.trim() : "");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double st = rs.getDouble("stock");
                    String desc = rs.getString("art_des").trim();
                    if (st > 0) {
                        desc += String.format(Locale.US, " (Stock: %.0f und)", st);
                    } else {
                        desc += " (Sin Stock - Agotado)";
                    }
                    lista.add(new ResultadoGuardado.ArticuloSimple(
                            rs.getString("co_art").trim(),
                            desc));
                }
            }
        }
        return lista;
    }

    /**
     * Guarda la Ubicación Actual (campo5) y Ubicación Anterior (campo6) en
     * saArticulo.
     * 
     * Reglas aplicadas (según requerimiento de Almacén/Inventario):
     * 1. Eliminación total de duplicidad en estantes: Si otro artículo ocupaba esta
     * misma ubicación
     * física (campo5), se desaloja INMEDIATAMENTE (campo6 = campo5, campo5 = ''),
     * quedando sin ubicación.
     * Esto evita que coexistan dos o más artículos en el mismo estante físico para
     * Picking.
     * 2. Si no se especifica ubicación anterior (ubicacion2), se preserva la
     * ubicación previa
     * que tenía este mismo artículo como historial en campo6.
     * 3. Control de redundancia: Si ubicación actual y anterior son idénticas, se
     * limpia la anterior.
     * 4. Se ejecuta de forma atómica en una sola transacción SQL.
     */
    public ResultadoGuardado guardarUbicaciones(String coArt, String ubicacion1, String ubicacion2)
            throws SQLException {
        if (coArt == null || coArt.isBlank()) {
            return new ResultadoGuardado(false, "Código de artículo requerido");
        }

        String ubi1 = ubicacion1 != null ? ubicacion1.trim().toUpperCase() : "";
        String ubi2 = ubicacion2 != null ? ubicacion2.trim().toUpperCase() : "";

        // Regla: Si la ubicación actual y anterior son iguales, vaciar la anterior
        if (ubi1.equalsIgnoreCase(ubi2)) {
            ubi2 = "";
        }

        String usuario = (SesionUsuario.current() != null && SesionUsuario.current().getCoUsuario() != null)
                ? SesionUsuario.current().getCoUsuario().trim()
                : "ADMIN";

        ResultadoGuardado resultado = new ResultadoGuardado();

        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Si ubi2 viene vacía, obtener la ubicación actual previa de este artículo para
                // guardarla en campo6 (historial)
                if (ubi2.isBlank()) {
                    String sqlPrev = "SELECT ISNULL(campo5, '') AS ubi_ant FROM saArticulo WHERE co_art = ?";
                    try (PreparedStatement psPrev = conn.prepareStatement(sqlPrev)) {
                        psPrev.setString(1, coArt.trim());
                        try (ResultSet rsPrev = psPrev.executeQuery()) {
                            if (rsPrev.next()) {
                                String anterior = rsPrev.getString("ubi_ant").trim().toUpperCase();
                                if (!anterior.isBlank() && !anterior.equalsIgnoreCase(ubi1)) {
                                    ubi2 = anterior;
                                }
                            }
                        }
                    }
                }

                // 1. Si se asigna una ubicación actual (no vacía), verificar y desalojar a
                // CUALQUIER otro producto en este estante
                if (!ubi1.isBlank()) {
                    String sqlCheck = """
                            SELECT a.co_art, a.art_des,
                                   ISNULL((SELECT SUM(stock) FROM saStockAlmacen s WHERE s.co_art = a.co_art AND s.tipo = 'ACT'), 0) as stock
                            FROM saArticulo a
                            WHERE a.campo5 = ? AND a.co_art <> ? AND a.anulado = 0
                            """;
                    try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
                        psCheck.setString(1, ubi1);
                        psCheck.setString(2, coArt.trim());
                        try (ResultSet rs = psCheck.executeQuery()) {
                            while (rs.next()) {
                                double st = rs.getDouble("stock");
                                String desc = rs.getString("art_des").trim();
                                if (st > 0) {
                                    desc += String.format(Locale.US, " (Stock: %.0f und)", st);
                                }
                                resultado.getDesalojados().add(new ResultadoGuardado.ArticuloSimple(
                                        rs.getString("co_art").trim(),
                                        desc));
                            }
                        }
                    }

                    // Desalojo total: cualquier artículo anterior en esa ubicación queda sin
                    // ubicación (campo5 = '')
                    // y su estante anterior se preserva en campo6
                    if (!resultado.getDesalojados().isEmpty()) {
                        String sqlDesalojo = """
                                UPDATE saArticulo
                                SET campo6     = campo5,
                                    campo5     = '',
                                    co_us_mo   = ?,
                                    co_sucu_mo = '01',
                                    fe_us_mo   = GETDATE()
                                WHERE campo5 = ?
                                  AND co_art <> ?
                                  AND anulado = 0
                                """;
                        try (PreparedStatement psDesalojo = conn.prepareStatement(sqlDesalojo)) {
                            psDesalojo.setString(1, usuario);
                            psDesalojo.setString(2, ubi1);
                            psDesalojo.setString(3, coArt.trim());
                            psDesalojo.executeUpdate();
                        }
                    }
                }

                // 2. Asignar la nueva ubicación al artículo actual
                String sqlUpdate = """
                        UPDATE saArticulo
                        SET campo6     = ?,
                            campo5     = ?,
                            co_us_mo   = ?,
                            co_sucu_mo = '01',
                            fe_us_mo   = GETDATE()
                        WHERE co_art = ?
                        """;
                try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
                    psUpdate.setString(1, ubi2);
                    psUpdate.setString(2, ubi1);
                    psUpdate.setString(3, usuario);
                    psUpdate.setString(4, coArt.trim());
                    int updated = psUpdate.executeUpdate();

                    if (updated > 0) {
                        conn.commit();
                        resultado.setSuccess(true);
                        if (!resultado.getDesalojados().isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Ubicación ").append(ubi1)
                                    .append(" guardada en Profit Plus. Se liberó el estante del producto anterior: ");
                            for (int i = 0; i < resultado.getDesalojados().size(); i++) {
                                if (i > 0)
                                    sb.append(", ");
                                sb.append("[").append(resultado.getDesalojados().get(i).getCoArt()).append("] ")
                                        .append(resultado.getDesalojados().get(i).getDescripcion());
                            }
                            resultado.setMensaje(sb.toString());
                        } else {
                            resultado.setMensaje("Ubicación " + (ubi1.isBlank() ? "eliminada" : ubi1)
                                    + " guardada correctamente en Profit Plus.");
                        }
                    } else {
                        conn.rollback();
                        resultado.setSuccess(false);
                        resultado.setMensaje("No se encontró el artículo para actualizar en saArticulo.");
                    }
                }
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }

        return resultado;
    }

    public List<UbicacionConflicto> obtenerUbicacionesEnConflicto(Set<String> excelUbis) throws SQLException {
        Map<String, UbicacionConflicto> mapa = new LinkedHashMap<>();
        String sql = """
                SELECT RTRIM(a.campo5) as ubi,
                       RTRIM(a.co_art) as codigo,
                       ISNULL(RTRIM(a.ref), '') as codigoBarra,
                       ISNULL(RTRIM(a.art_des), '') as descripcion,
                       ISNULL(prov.prov_des, '') as marca,
                       ISNULL(l.lin_des, '') as linea,
                       ISNULL(stk.stockDisponible, 0) as existencia,
                       ISNULL(stk.stockAveria, 0) as stockAveria,
                       ISNULL(stk.stockVencido, 0) as stockVencido,
                       ISNULL(stk.totalStock, 0) as stockTotal,
                       ISNULL(RTRIM(a.campo5), '') as ubicacion1,
                       ISNULL(RTRIM(a.campo6), '') as ubicacion2
                FROM saArticulo a
                OUTER APPLY (
                    SELECT TOP 1 ap2.co_prov
                    FROM saArtProveedorReng ap2
                    WHERE ap2.co_art = a.co_art
                    ORDER BY ap2.reng_num
                ) ap
                LEFT JOIN saProveedor prov ON ap.co_prov = prov.co_prov
                LEFT JOIN saLineaArticulo l ON a.co_lin = l.co_lin
                OUTER APPLY (
                    SELECT
                        ISNULL(SUM(CASE WHEN s.co_alma NOT IN ('AV', 'VP', 'VC', '9999') THEN s.stock ELSE 0 END), 0) AS stockDisponible,
                        ISNULL(SUM(CASE WHEN s.co_alma = 'AV' THEN s.stock ELSE 0 END), 0) AS stockAveria,
                        ISNULL(SUM(CASE WHEN s.co_alma IN ('VP', 'VC') THEN s.stock ELSE 0 END), 0) AS stockVencido,
                        ISNULL(SUM(s.stock), 0) AS totalStock
                    FROM saStockAlmacen s
                    WHERE s.co_art = a.co_art AND s.tipo = 'ACT'
                ) stk
                WHERE a.campo5 IN (
                    SELECT RTRIM(campo5) FROM saArticulo
                    WHERE campo5 IS NOT NULL AND RTRIM(campo5) <> '' AND anulado = 0
                    GROUP BY RTRIM(campo5) HAVING COUNT(*) > 1
                ) AND a.anulado = 0
                ORDER BY a.campo5 ASC, stk.totalStock DESC, a.co_art ASC
                """;

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String ubi = rs.getString("ubi");
                if (ubi == null || ubi.isBlank())
                    continue;
                ubi = ubi.trim();

                ArticuloUbicacionRow art = mapRow(rs);
                UbicacionConflicto conf = mapa.get(ubi);
                if (conf == null) {
                    boolean enExcel = excelUbis != null && excelUbis.contains(ubi);
                    conf = new UbicacionConflicto(ubi, 0, enExcel);
                    mapa.put(ubi, conf);
                }
                conf.getArticulos().add(art);
                conf.setCantidad(conf.getArticulos().size());
            }
        }

        List<UbicacionConflicto> lista = new ArrayList<>(mapa.values());
        lista.sort((a, b) -> {
            if (a.isEnExcel() != b.isEnExcel()) {
                return a.isEnExcel() ? -1 : 1;
            }
            return Integer.compare(b.getCantidad(), a.getCantidad());
        });
        return lista;
    }

    public ResultadoGuardado resolverConflicto(String ubi, String coArt, String accion, String nuevaUbi)
            throws SQLException {
        ResultadoGuardado resultado = new ResultadoGuardado();
        String usuario = (SesionUsuario.current() != null && SesionUsuario.current().getCoUsuario() != null)
                ? SesionUsuario.current().getCoUsuario().trim()
                : "ADMIN";

        if (ubi == null || ubi.isBlank()) {
            return new ResultadoGuardado(false, "Ubicación requerida");
        }
        if (coArt == null || coArt.isBlank()) {
            return new ResultadoGuardado(false, "Código de artículo requerido");
        }

        String ubiClean = ubi.trim().toUpperCase();
        String coArtClean = coArt.trim();
        String nuevaUbiClean = (nuevaUbi != null) ? nuevaUbi.trim().toUpperCase() : "";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if ("mantener".equalsIgnoreCase(accion)) {
                    String sqlDesalojo = """
                            UPDATE saArticulo
                            SET campo6 = campo5,
                                campo5 = '',
                                co_us_mo = ?,
                                co_sucu_mo = '01',
                                fe_us_mo = GETDATE()
                            WHERE campo5 = ? AND co_art <> ? AND anulado = 0
                            """;
                    int desalojados;
                    try (PreparedStatement ps = conn.prepareStatement(sqlDesalojo)) {
                        ps.setString(1, usuario);
                        ps.setString(2, ubiClean);
                        ps.setString(3, coArtClean);
                        desalojados = ps.executeUpdate();
                    }
                    conn.commit();
                    resultado.setSuccess(true);
                    resultado.setMensaje(String.format(
                            "Artículo [%s] confirmado en estante [%s]. Se liberaron %d producto(s) conflictivos.",
                            coArtClean, ubiClean, desalojados));
                } else if ("liberar".equalsIgnoreCase(accion)) {
                    String sqlLiberar = """
                            UPDATE saArticulo
                            SET campo6 = campo5,
                                campo5 = '',
                                co_us_mo = ?,
                                co_sucu_mo = '01',
                                fe_us_mo = GETDATE()
                            WHERE co_art = ? AND anulado = 0
                            """;
                    int count;
                    try (PreparedStatement ps = conn.prepareStatement(sqlLiberar)) {
                        ps.setString(1, usuario);
                        ps.setString(2, coArtClean);
                        count = ps.executeUpdate();
                    }
                    conn.commit();
                    resultado.setSuccess(count > 0);
                    resultado.setMensaje(String.format(
                            "Artículo [%s] liberado del estante [%s]. Estante previo guardado en historial.",
                            coArtClean, ubiClean));
                } else if ("reasignar".equalsIgnoreCase(accion)) {
                    if (nuevaUbiClean.isBlank()) {
                        conn.rollback();
                        return new ResultadoGuardado(false, "Debe especificar la nueva ubicación a asignar");
                    }
                    String sqlReasignar = """
                            UPDATE saArticulo
                            SET campo6 = campo5,
                                campo5 = ?,
                                co_us_mo = ?,
                                co_sucu_mo = '01',
                                fe_us_mo = GETDATE()
                            WHERE co_art = ? AND anulado = 0
                            """;
                    int count;
                    try (PreparedStatement ps = conn.prepareStatement(sqlReasignar)) {
                        ps.setString(1, nuevaUbiClean);
                        ps.setString(2, usuario);
                        ps.setString(3, coArtClean);
                        count = ps.executeUpdate();
                    }
                    conn.commit();
                    resultado.setSuccess(count > 0);
                    resultado.setMensaje(String.format("Artículo [%s] reasignado exitosamente del estante [%s] a [%s].",
                            coArtClean, ubiClean, nuevaUbiClean));
                } else {
                    conn.rollback();
                    return new ResultadoGuardado(false, "Acción desconocida: " + accion);
                }
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return resultado;
    }

    private ArticuloUbicacionRow mapRow(ResultSet rs) throws SQLException {
        ArticuloUbicacionRow row = new ArticuloUbicacionRow();
        row.setCoArt(rs.getString("codigo").trim());
        row.setCodigoBarra(rs.getString("codigoBarra").trim());
        row.setDescripcion(rs.getString("descripcion").trim());
        row.setMarca(rs.getString("marca").trim());
        row.setLinea(rs.getString("linea").trim());
        row.setExistencia(rs.getDouble("existencia"));

        try {
            row.setStockAveria(rs.getDouble("stockAveria"));
        } catch (SQLException ignored) {}
        try {
            row.setStockVencido(rs.getDouble("stockVencido"));
        } catch (SQLException ignored) {}
        try {
            row.setStockTotal(rs.getDouble("stockTotal"));
        } catch (SQLException ignored) {
            row.setStockTotal(row.getExistencia() + row.getStockAveria() + row.getStockVencido());
        }

        row.setUbicacion1(rs.getString("ubicacion1").trim());
        row.setUbicacion2(rs.getString("ubicacion2").trim());
        return row;
    }
}
