package com.example.sistema.controller;

import com.example.sistema.model.Asistencia;
import com.example.sistema.model.Auditoria;
import com.example.sistema.model.Usuario;
import com.example.sistema.model.Empresa;
import com.example.sistema.repository.AsistenciaRepository;
import com.example.sistema.repository.AuditoriaRepository;
import com.example.sistema.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/operaciones")
public class AuditoriaController {

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario getUsuarioLogueado(Principal principal) {
        if (principal == null) throw new RuntimeException("No hay ninguna sesión activa.");
        
        if ("admin".equalsIgnoreCase(principal.getName())) {
            Usuario adminFicticio = new Usuario();
            adminFicticio.setUsername("admin");
            adminFicticio.setRol("JEFE");
            
            Empresa empresaSimulada = new Empresa();
            empresaSimulada.setId(1L);
            adminFicticio.setEmpresa(empresaSimulada);
            
            return adminFicticio;
        }

        return usuarioRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado en el sistema."));
    }

    @GetMapping("/auditoria")
    public String mostrarAuditoria(Principal principal, Model model) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            model.addAttribute("usuarioLogueado", logueado);

            String rol = logueado.getRol() != null ? logueado.getRol().toUpperCase() : "";
            if ("JEFE".equals(rol) || "GERENTE".equals(rol)) {
                
                List<Auditoria> logsFiscales;
                if (logueado.getEmpresa() == null) {
                    logsFiscales = auditoriaRepository.findAll(); // Fallback si no tiene empresa
                } else {
                    Long empresaId = logueado.getEmpresa().getId();
                    logsFiscales = auditoriaRepository.findByEmpresaIdOrderByFechaRegistroDesc(empresaId);
                }
                model.addAttribute("auditorias", logsFiscales);

                // ==================== CORRECCIÓN EN ASISTENCIAS ====================
                List<Asistencia> listaAsistencias = asistenciaRepository.findAll();
                
                // Si el usuario logueado tiene empresa, filtramos; si los usuarios no tienen empresa asignada, mostramos las asistencias globales.
                if (logueado.getEmpresa() != null) {
                    Long empresaId = logueado.getEmpresa().getId();
                    List<Asistencia> filtradas = listaAsistencias.stream()
                            .filter(a -> a.getUsuario() != null)
                            .filter(a -> a.getUsuario().getEmpresa() == null || a.getUsuario().getEmpresa().getId().equals(empresaId))
                            .collect(Collectors.toList());
                    
                    if (!filtradas.isEmpty()) {
                        listaAsistencias = filtradas;
                    }
                }
                
                model.addAttribute("asistencias", listaAsistencias);
                
                String nombreEmpresa = (logueado.getEmpresa() != null && logueado.getEmpresa().getRazonSocial() != null) 
                        ? logueado.getEmpresa().getRazonSocial() : "OFICINA FISCAL";
                model.addAttribute("empresaNombre", nombreEmpresa);
                
            } else {
                model.addAttribute("auditorias", new ArrayList<>());
                model.addAttribute("asistencias", new ArrayList<>());
            }

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("auditorias", new ArrayList<>());
            model.addAttribute("asistencias", new ArrayList<>());
        }

        return "auditoria";
    }
}