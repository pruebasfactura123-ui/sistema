package com.example.sistema.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "factura")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String rfcEmisor;

    @Column(length = 15)
    private String rfcCliente; 

    @Column(name = "nombre_archivo")
    private String nombreArchivo; 

    private Double subtotal;
    private Double iva;
    private Double total;
    private LocalDate fecha;

    @Column(length = 50)
    private String tipo; // INGRESO o EGRESO

    @Column(columnDefinition = "TEXT")
    private String estado; // Para descripciones largas de XML u observaciones

    // --- RELACIÓN: Muchas facturas pertenecen a una Empresa ---
    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    // CONSTRUCTOR VACÍO OBLIGATORIO PARA JPA
    public Factura() {}

    // GETTERS Y SETTERS
    public Long getId() { 
        return id; 
    }
    
    public void setId(Long id) { 
        this.id = id; 
    }
    
    public String getRfcEmisor() { 
        return rfcEmisor; 
    }
    
    public void setRfcEmisor(String rfcEmisor) { 
        this.rfcEmisor = rfcEmisor; 
    }
    
    public String getRfcCliente() { 
        return rfcCliente; 
    }
    
    public void setRfcCliente(String rfcCliente) { 
        this.rfcCliente = rfcCliente; 
    }
    
    public String getNombreArchivo() { 
        return nombreArchivo; 
    }
    
    public void setNombreArchivo(String nombreArchivo) { 
        this.nombreArchivo = nombreArchivo; 
    }
    
    public Double getSubtotal() { 
        return subtotal; 
    }
    
    public void setSubtotal(Double subtotal) { 
        this.subtotal = subtotal; 
    }
    
    public Double getIva() { 
        return iva; 
    }
    
    public void setIva(Double iva) { 
        this.iva = iva; 
    }
    
    public Double getTotal() { 
        return total; 
    }
    
    public void setTotal(Double total) { 
        this.total = total; 
    }
    
    public LocalDate getFecha() { 
        return fecha; 
    }
    
    public void setFecha(LocalDate fecha) { 
        this.fecha = fecha; 
    }
    
    public String getTipo() { 
        return tipo; 
    }
    
    public void setTipo(String tipo) { 
        this.tipo = tipo; 
    }
    
    public String getEstado() { 
        return estado; 
    }
    
    public void setEstado(String estado) { 
        this.estado = estado; 
    }

    public Empresa getEmpresa() { 
        return empresa; 
    }
    
    public void setEmpresa(Empresa empresa) { 
        this.empresa = empresa; 
    }
}