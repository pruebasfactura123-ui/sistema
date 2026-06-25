package com.example.sistema.controller;

import com.example.sistema.model.ReciboNomina;
import com.example.sistema.repository.ReciboNominaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

@Controller
public class NominaTrabajadorController {

    @Autowired
    private ReciboNominaRepository reciboNominaRepository;

    // RUTA INTELIGENTE UNIFICADA
    @GetMapping("/operaciones/nominas")
    public String verNominas(Model model, Authentication authentication) {
        String username = authentication.getName();
        
        // Verificamos si el usuario en sesión tiene rol de Jefe o Gerente
        boolean esJefe = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_JEFE") || a.getAuthority().equals("ROLE_GERENTE"));

        if (esJefe) {
            // 1. SI ES JEFE: Lo mandamos a tu archivo original 'nominas.html'
            // Nota: Aquí puedes agregar las listas que ya le pasabas a esa pantalla antes si las necesitas
            return "nominas"; 
        } else {
            // 2. SI ES TRABAJADOR: Lo mandamos al nuevo 'mis-nominas.html'
            List<ReciboNomina> misRecibos = reciboNominaRepository.findByUsuarioUsernameOrderByFechaPagoDesc(username);
            model.addAttribute("recibos", misRecibos);
            
            return "mis-nominas"; 
        }
    }

    // Ruta para descargar el PDF
    @GetMapping("/operaciones/nominas/descargar/pdf/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        ReciboNomina recibo = reciboNominaRepository.findById(id).orElse(null);
        String contenidoSimulado = "Contenido Binario del PDF del Recibo ID: " + id;
        byte[] documentoBytes = contenidoSimulado.getBytes();
        String nombreArchivo = "Recibo_" + (recibo != null ? recibo.getPeriodoPago().replace(" ", "_") : id) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(documentoBytes);
    }

    // Ruta para descargar el XML
    @GetMapping("/operaciones/nominas/descargar/xml/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> descargarXml(@PathVariable Long id) {
        ReciboNomina recibo = reciboNominaRepository.findById(id).orElse(null);
        String xmlSimulado = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><cfdi:Comprobante id=\"" + id + "\" total=\"" + (recibo != null ? recibo.getSueldoNeto() : 0) + "\"/>";
        byte[] xmlBytes = xmlSimulado.getBytes();
        String nombreArchivo = "Recibo_" + (recibo != null ? recibo.getPeriodoPago().replace(" ", "_") : id) + ".xml";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.APPLICATION_XML)
                .body(xmlBytes);
    }
}