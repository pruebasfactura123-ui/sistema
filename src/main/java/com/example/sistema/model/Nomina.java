package com.example.sistema.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "nominas")
public class Nomina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación con el trabajador (Usuario)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario trabajador;

    private LocalDate fechaEmision;
    private String periodo; // Ej: "Primera Quincena - Junio 2026"
    
    private Double sueldoBase;       // Sueldo bruto pactado
    
    // === NUEVOS CAMPOS DE INCIDENCIAS ===
    private Integer diasTrabajados;
    private Integer horasExtra;
    private Integer faltas;
    private Integer retardos;
    // ====================================

    private Double percepciones;     // Bonos, horas extra, etc.
    private Double deducciones;      // Retenciones de ISR, IMSS, faltas
    private Double sueldoNeto;       // El total que se le deposita (Sueldo Base + Percepciones - Deducciones)

    private String estado; // "SIMULADO" o "PAGADO"

    // Constructor vacío requerido por JPA
    public Nomina() {}

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getTrabajador() { return trabajador; }
    public void setTrabajador(Usuario trabajador) { this.trabajador = trabajador; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }

    public Double getSueldoBase() { return sueldoBase; }
    public void setSueldoBase(Double sueldoBase) { this.sueldoBase = sueldoBase; }

    // === GETTERS Y SETTERS DE INCIDENCIAS ===
    public Integer getDiasTrabajados() { return diasTrabajados; }
    public void setDiasTrabajados(Integer diasTrabajados) { this.diasTrabajados = diasTrabajados; }

    public Integer getHorasExtra() { return horasExtra; }
    public void setHorasExtra(Integer horasExtra) { this.horasExtra = horasExtra; }

    public Integer getFaltas() { return faltas; }
    public void setFaltas(Integer faltas) { this.faltas = faltas; }

    public Integer getRetardos() { return retardos; }
    public void setRetardos(Integer retardos) { this.retardos = retardos; }
    // ========================================

    public Double getPercepciones() { return percepciones; }
    public void setPercepciones(Double percepciones) { this.percepciones = percepciones; }

    public Double getDeducciones() { return deducciones; }
    public void setDeducciones(Double deducciones) { this.deducciones = deducciones; }

    public Double getSueldoNeto() { return sueldoNeto; }
    public void setSueldoNeto(Double sueldoNeto) { this.sueldoNeto = sueldoNeto; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}