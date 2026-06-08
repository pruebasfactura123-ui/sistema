package com.example.sistema.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "declaraciones")
public class Declaracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer anio;
    private String mes; // Ej: "Enero", "Febrero"...
    private String tipo; // Ej: "Mensual IVA", "Mensual ISR", "Anual"
    private String estado; // Ej: "PENDIENTE", "PRESENTADA"
    private LocalDate fechaVencimiento;
    private LocalDate fechaPresentacion;
    private String rutaAcusePdf; // Ruta del archivo físico en el disco C:/

    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    // ==========================================
    // GETTERS Y SETTERS
    // ==========================================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }

    public String getMes() { return mes; }
    public void setMes(String mes) { this.mes = mes; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public LocalDate getFechaPresentacion() { return fechaPresentacion; }
    public void setFechaPresentacion(LocalDate fechaPresentacion) { this.fechaPresentacion = fechaPresentacion; }

    public String getRutaAcusePdf() { return rutaAcusePdf; }
    public void setRutaAcusePdf(String rutaAcusePdf) { this.rutaAcusePdf = rutaAcusePdf; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
}