package com.example.sistema.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_auditoria")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String usuario;

    @Column(nullable = false, length = 100)
    private String accion;

    @Column(columnDefinition = "VARCHAR(MAX)")
    private String detalles;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    // ==================== NUEVO: RELACIÓN CON EMPRESA ====================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = true) // nullable = true por si hay admins globales sin empresa
    private Empresa empresa;

    // Constructor vacío obligatorio
    public Auditoria() {}

    // Constructor antiguo (lo mantenemos para no romper otras partes del código si se usa)
    public Auditoria(String usuario, String accion, String detalles) {
        this.usuario = usuario;
        this.accion = accion;
        this.detalles = detalles;
        this.fechaRegistro = LocalDateTime.now();
    }

    // NUEVO: Constructor práctico que recibe la empresa para el aislamiento directo
    public Auditoria(String usuario, String accion, String detalles, Empresa empresa) {
        this.usuario = usuario;
        this.accion = accion;
        this.detalles = detalles;
        this.empresa = empresa;
        this.fechaRegistro = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public String getDetalles() { return detalles; }
    public void setDetalles(String detalles) { this.detalles = detalles; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    // NUEVO: Getter y Setter para Empresa
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
}