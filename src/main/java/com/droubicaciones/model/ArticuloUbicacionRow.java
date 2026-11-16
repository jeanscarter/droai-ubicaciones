package com.droubicaciones.model;

/**
 * DTO para representar un artículo y sus ubicaciones (campo5 = Ubicación 1,
 * campo6 = Ubicación 2).
 */
public class ArticuloUbicacionRow {

    private String coArt;
    private String codigoBarra;
    private String descripcion;
    private String marca;
    private String linea;
    private double existencia; // Stock disponible para venta (Almacenes 01, 02)
    private double stockAveria; // Stock en Almacén AV (Avería / Merma)
    private double stockVencido; // Stock en Almacenes VP / VC (Vencimientos)
    private double stockTotal; // Stock total acumulado
    private String ubicacion1; // campo5
    private String ubicacion2; // campo6

    public ArticuloUbicacionRow() {
        this.coArt = "";
        this.codigoBarra = "";
        this.descripcion = "";
        this.marca = "";
        this.linea = "";
        this.existencia = 0.0;
        this.stockAveria = 0.0;
        this.stockVencido = 0.0;
        this.stockTotal = 0.0;
        this.ubicacion1 = "";
        this.ubicacion2 = "";
    }

    public String getCoArt() {
        return coArt;
    }

    public void setCoArt(String coArt) {
        this.coArt = coArt;
    }

    public String getCodigoBarra() {
        return codigoBarra;
    }

    public void setCodigoBarra(String codigoBarra) {
        this.codigoBarra = codigoBarra;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getLinea() {
        return linea;
    }

    public void setLinea(String linea) {
        this.linea = linea;
    }

    public double getExistencia() {
        return existencia;
    }

    public void setExistencia(double existencia) {
        this.existencia = existencia;
    }

    public double getStockAveria() {
        return stockAveria;
    }

    public void setStockAveria(double stockAveria) {
        this.stockAveria = stockAveria;
    }

    public double getStockVencido() {
        return stockVencido;
    }

    public void setStockVencido(double stockVencido) {
        this.stockVencido = stockVencido;
    }

    public double getStockTotal() {
        return stockTotal;
    }

    public void setStockTotal(double stockTotal) {
        this.stockTotal = stockTotal;
    }

    public String getUbicacion1() {
        return ubicacion1;
    }

    public void setUbicacion1(String ubicacion1) {
        this.ubicacion1 = ubicacion1 != null ? ubicacion1.trim() : "";
    }

    public String getUbicacion2() {
        return ubicacion2;
    }

    public void setUbicacion2(String ubicacion2) {
        this.ubicacion2 = ubicacion2 != null ? ubicacion2.trim() : "";
    }
}
