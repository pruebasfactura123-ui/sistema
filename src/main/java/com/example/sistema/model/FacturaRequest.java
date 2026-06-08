package com.example.sistema.model;

import java.util.List;

public class FacturaRequest {
    private String clienteNombre;
    private String clienteRfc;
    private String clienteRegimenFiscal;
    private String clienteCodigoPostal;
    private String formaPago;
    private String metodoPago;
    private String usoCfdi;
    private List<ConceptoItem> conceptos;

    public FacturaRequest() {
    }

    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public String getClienteRfc() { return clienteRfc; }
    public void setClienteRfc(String clienteRfc) { this.clienteRfc = clienteRfc; }

    public String getClienteRegimenFiscal() { return clienteRegimenFiscal; }
    public void setClienteRegimenFiscal(String clienteRegimenFiscal) { this.clienteRegimenFiscal = clienteRegimenFiscal; }

    public String getClienteCodigoPostal() { return clienteCodigoPostal; }
    public void setClienteCodigoPostal(String clienteCodigoPostal) { this.clienteCodigoPostal = clienteCodigoPostal; }

    public String getFormaPago() { return formaPago; }
    public void setFormaPago(String formaPago) { this.formaPago = formaPago; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getUsoCfdi() { return usoCfdi; }
    public void setUsoCfdi(String usoCfdi) { this.usoCfdi = usoCfdi; }

    public List<ConceptoItem> getConceptos() { return conceptos; }
    public void setConceptos(List<ConceptoItem> conceptos) { this.conceptos = conceptos; }
}