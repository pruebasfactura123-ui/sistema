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

    // Constructor vacío obligatorio
    public Auditoria() {}

    // Constructor práctico para guardar eventos
    public Auditoria(String usuario, String accion, String detalles) {
        this.usuario = usuario;
        this.accion = accion;
        this.detalles = detalles;
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
}