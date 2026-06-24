package com.example.sistema.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Validación: Solo letras, espacios, acentos y puntos (para empresas Tipo S.A. de C.V.)
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s\\.]+$", message = "El nombre solo debe contener letras, espacios y puntos.")
    private String nombre;

    // Validación: Formato oficial de RFC en México (3-4 letras, 6 números, 3 caracteres de homoclave)
    @Pattern(regexp = "^[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3}$", message = "El formato del RFC no es válido.")
    private String rfc;
    
    private String telefono;
    private String correo;

    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    // --- CONSTRUCTORES ---
    public Cliente() {
    }

    public Cliente(String nombre, String rfc, String telefono, String correo, Empresa empresa) {
        this.nombre = nombre;
        this.rfc = rfc;
        this.telefono = telefono;
        this.correo = correo;
        this.empresa = empresa;
    }

    // --- GETTERS Y SETTERS ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRfc() {
        return rfc;
    }

    public void setRfc(String rfc) {
        this.rfc = rfc;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }
}