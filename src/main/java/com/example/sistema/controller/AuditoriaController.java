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
import java.util.*;
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
                    logsFiscales = auditoriaRepository.findAll();
                } else {
                    Long empresaId = logueado.getEmpresa().getId();
                    logsFiscales = auditoriaRepository.findByEmpresaIdOrderByFechaRegistroDesc(empresaId);
                }
                model.addAttribute("auditorias", logsFiscales != null ? logsFiscales : new ArrayList<>());

                // Obtener todas las asistencias de forma segura
                List<Asistencia> listaAsistencias = asistenciaRepository.findAll();
                if (listaAsistencias == null) {
                    listaAsistencias = new ArrayList<>();
                }
                
                // Filtrar por empresa de forma segura evitando nulos
                if (logueado.getEmpresa() != null && logueado.getEmpresa().getId() != null) {
                    Long empresaId = logueado.getEmpresa().getId();
                    listaAsistencias = listaAsistencias.stream()
                            .filter(a -> a != null && a.getUsuario() != null)
                            .filter(a -> a.getUsuario().getEmpresa() == null || 
                                       (a.getUsuario().getEmpresa().getId() != null && 
                                        a.getUsuario().getEmpresa().getId().equals(empresaId)))
                            .collect(Collectors.toList());
                }
                
                // Agrupamiento seguro por semana
                Map<String, List<Asistencia>> asistenciasPorSemana = listaAsistencias.stream()
                        .filter(a -> a != null && a.getFecha() != null)
                        .sorted(Comparator.comparing(Asistencia::getFecha, Comparator.nullsLast(Comparator.reverseOrder())))
                        .collect(Collectors.groupingBy(
                                a -> {
                                    String semana = a.getEtiquetaSemana();
                                    return (semana != null && !semana.isEmpty()) ? semana : "Semana General";
                                },
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

                model.addAttribute("asistenciasPorSemana", asistenciasPorSemana);
                
                String nombreEmpresa = (logueado.getEmpresa() != null && logueado.getEmpresa().getRazonSocial() != null) 
                        ? logueado.getEmpresa().getRazonSocial() : "OFICINA FISCAL";
                model.addAttribute("empresaNombre", nombreEmpresa);
                
            } else {
                model.addAttribute("auditorias", new ArrayList<>());
                model.addAttribute("asistenciasPorSemana", new LinkedHashMap<>());
            }

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("auditorias", new ArrayList<>());
            model.addAttribute("asistenciasPorSemana", new LinkedHashMap<>());
        }

        return "auditoria";
    }
}