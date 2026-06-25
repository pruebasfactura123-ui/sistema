package com.example.sistema.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "recibos_nomina")
public class ReciboNomina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_username", nullable = false)
    private String usuarioUsername;

    @Column(name = "periodo_pago", nullable = false)
    private String periodoPago;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    @Column(name = "sueldo_neto", nullable = false)
    private Double sueldoNeto;

    @Column(name = "dias_trabajados", nullable = false)
    private Integer diasTrabajados;

    @Column(name = "pdf_url")
    private String pdfUrl;

    @Column(name = "xml_url")
    private String xmlUrl;

    public ReciboNomina() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsuarioUsername() { return usuarioUsername; }
    public void setUsuarioUsername(String usuarioUsername) { this.usuarioUsername = usuarioUsername; }

    public String getPeriodoPago() { return periodoPago; }
    public void setPeriodoPago(String periodoPago) { this.periodoPago = periodoPago; }

    public LocalDate getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDate fechaPago) { this.fechaPago = fechaPago; }

    public Double getSueldoNeto() { return sueldoNeto; }
    public void setSueldoNeto(Double sueldoNeto) { this.sueldoNeto = sueldoNeto; }

    public Integer getDiasTrabajados() { return diasTrabajados; }
    public void setDiasTrabajados(Integer diasTrabajados) { this.diasTrabajados = diasTrabajados; }

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }

    public String getXmlUrl() { return xmlUrl; }
    public void setXmlUrl(String xmlUrl) { this.xmlUrl = xmlUrl; }
}