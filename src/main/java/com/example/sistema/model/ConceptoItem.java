package com.example.sistema.model;

public class ConceptoItem {
    private String descripcion;
    private int cantidad;
    private double precioUnitario;
    private String claveSatProduct; 
    private String claveUnidadSat;  

    public ConceptoItem() {
    }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }

    public String getClaveSatProduct() { return claveSatProduct; }
    public void setClaveSatProduct(String claveSatProduct) { this.claveSatProduct = claveSatProduct; }

    public String getClaveUnidadSat() { return claveUnidadSat; }
    public void setClaveUnidadSat(String claveUnidadSat) { this.claveUnidadSat = claveUnidadSat; }
}