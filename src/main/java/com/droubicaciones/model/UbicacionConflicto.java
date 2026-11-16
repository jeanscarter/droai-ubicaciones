package com.droubicaciones.model;

import java.util.ArrayList;
import java.util.List;

public class UbicacionConflicto {
    private String ubicacion;
    private int cantidad;
    private boolean enExcel;
    private List<ArticuloUbicacionRow> articulos = new ArrayList<>();

    public UbicacionConflicto() {}

    public UbicacionConflicto(String ubicacion, int cantidad, boolean enExcel) {
        this.ubicacion = ubicacion;
        this.cantidad = cantidad;
        this.enExcel = enExcel;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public boolean isEnExcel() {
        return enExcel;
    }

    public void setEnExcel(boolean enExcel) {
        this.enExcel = enExcel;
    }

    public List<ArticuloUbicacionRow> getArticulos() {
        return articulos;
    }

    public void setArticulos(List<ArticuloUbicacionRow> articulos) {
        this.articulos = articulos;
    }
}

