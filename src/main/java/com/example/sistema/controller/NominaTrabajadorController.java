package com.example.sistema.controller;

import com.example.sistema.model.Nomina;
import com.example.sistema.model.ReciboNomina;
import com.example.sistema.model.Usuario;
import com.example.sistema.repository.NominaRepository;
import com.example.sistema.repository.ReciboNominaRepository;
import com.example.sistema.repository.UsuarioRepository;
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

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NominaRepository nominaRepository;

    // RUTA INTELIGENTE UNIFICADA (Resuelve el mapeo ambiguo)
    @GetMapping("/operaciones/nominas")
    public String verNominas(Model model, Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        String usernameActivo = authentication.getName();
        Usuario usuarioLogueado = usuarioRepository.findByUsername(usernameActivo).orElse(null);

        if (usuarioLogueado == null) {
            return "redirect:/login";
        }

        // Verificamos si el usuario tiene rol administrativo (JEFE o GERENTE)
        boolean esJefe = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_JEFE") || a.getAuthority().equals("ROLE_GERENTE") || usuarioLogueado.getRol().equals("JEFE") || usuarioLogueado.getRol().equals("GERENTE"));

        if (esJefe) {
            // ==========================================
            // 1. VISTA DEL JEFE (Lógica original de tu NominaController)
            // ==========================================
            Long empresaId = usuarioLogueado.getEmpresa().getId();
            List<Nomina> nominas = nominaRepository.findByTrabajadorEmpresaIdOrderByFechaEmisionDesc(empresaId);
            List<Usuario> trabajadores = usuarioRepository.findByEmpresaId(empresaId);

            model.addAttribute("nominas", nominas);
            model.addAttribute("trabajadores", trabajadores);
            
            String nombreEmpresa = "OFICINA FISCAL";
            if (usuarioLogueado.getEmpresa() != null && usuarioLogueado.getEmpresa().getRazonSocial() != null) {
                nombreEmpresa = usuarioLogueado.getEmpresa().getRazonSocial();
            }
            model.addAttribute("empresaNombre", nombreEmpresa);
            
            return "nominas"; // Muestra el panel administrativo (Tu foto 1)
            
        } else {
            // ==========================================
            // 2. VISTA DEL TRABAJADOR
            // ==========================================
            List<ReciboNomina> misRecibos = reciboNominaRepository.findByUsuarioUsernameOrderByFechaPagoDesc(usernameActivo);
            model.addAttribute("recibos", misRecibos);
            
            return "mis-nominas"; // Muestra la tabla sencilla de descargas (Tu foto 2)
        }
    }

    // Rutas de descarga para el empleado regular
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