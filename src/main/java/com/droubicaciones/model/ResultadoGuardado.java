package com.droubicaciones.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa el resultado de una operación de guardado de ubicación,
 * incluyendo si se desocupó a otros artículos que ocupaban el mismo estante.
 */
public class ResultadoGuardado {

    private boolean success;
    private String mensaje;
    private List<ArticuloSimple> desalojados;

    public ResultadoGuardado() {
        this.success = false;
        this.mensaje = "";
        this.desalojados = new ArrayList<>();
    }

    public ResultadoGuardado(boolean success, String mensaje) {
        this.success = success;
        this.mensaje = mensaje;
        this.desalojados = new ArrayList<>();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public List<ArticuloSimple> getDesalojados() {
        return desalojados;
    }

    public void setDesalojados(List<ArticuloSimple> desalojados) {
        this.desalojados = desalojados;
    }

    public static class ArticuloSimple {
        private final String coArt;
        private final String descripcion;

        public ArticuloSimple(String coArt, String descripcion) {
            this.coArt = coArt != null ? coArt.trim() : "";
            this.descripcion = descripcion != null ? descripcion.trim() : "";
        }

        public String getCoArt() {
            return coArt;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }
}

