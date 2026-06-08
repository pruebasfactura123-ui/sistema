package com.example.sistema.controller;

import com.example.sistema.model.Asistencia;
import com.example.sistema.model.Auditoria;
import com.example.sistema.model.Usuario;
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
        return usuarioRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado en el sistema."));
    }

    @GetMapping("/auditoria")
    public String mostrarAuditoria(Principal principal, Model model) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            model.addAttribute("usuarioLogueado", logueado);

            // Solo cargar datos costosos de bases de datos si el rol lo va a visualizar
            String rol = logueado.getRol() != null ? logueado.getRol().toUpperCase() : "";
            if ("JEFE".equals(rol) || "GERENTE".equals(rol)) {
                
                List<Auditoria> logsFiscales = auditoriaRepository.findAllByOrderByFechaRegistroDesc();
                model.addAttribute("auditorias", logsFiscales);

                List<Asistencia> listaAsistencias = asistenciaRepository.findAll().stream()
                        .filter(a -> a.getUsuario() != null && a.getUsuario().getEmpresa() != null)
                        .filter(a -> a.getUsuario().getEmpresa().getId().equals(logueado.getEmpresa().getId()))
                        .collect(Collectors.toList());
                model.addAttribute("asistencias", listaAsistencias);
            } else {
                // Listas vacías de seguridad para trabajadores corrientes
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