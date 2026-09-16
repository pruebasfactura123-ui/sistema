package com.example.sistema.controller;

import com.example.sistema.model.Asistencia;
import com.example.sistema.model.Auditoria;
import com.example.sistema.model.Usuario;
import com.example.sistema.model.Empresa;
import com.example.sistema.repository.AsistenciaRepository;
import com.example.sistema.repository.AuditoriaRepository;
import com.example.sistema.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
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

    // =========================================================================
    // VISTA DE AUDITORÍA Y ASISTENCIAS AGRUPADAS POR SEMANAS
    // =========================================================================
    @GetMapping("/auditoria")
    public String mostrarAuditoria(Principal principal, Model model) {
        try {
            Usuario logueado = getUsuarioLogueado(principal);
            model.addAttribute("usuarioLogueado", logueado);

            String rol = logueado.getRol() != null ? logueado.getRol().toUpperCase() : "";
            if ("JEFE".equals(rol) || "GERENTE".equals(rol)) {
                
                // 1. Cargar Auditorías
                List<Auditoria> logsFiscales;
                if (logueado.getEmpresa() == null) {
                    logsFiscales = auditoriaRepository.findAll();
                } else {
                    Long empresaId = logueado.getEmpresa().getId();
                    logsFiscales = auditoriaRepository.findByEmpresaIdOrderByFechaRegistroDesc(empresaId);
                }
                model.addAttribute("auditorias", logsFiscales);

                // 2. Cargar Asistencias y Filtrar por Empresa
                List<Asistencia> listaAsistencias = asistenciaRepository.findAll();
                
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
                
                // 3. Agrupar Asistencias por Semana
                Map<String, List<Asistencia>> asistenciasPorSemana = listaAsistencias.stream()
                        .filter(a -> a.getFecha() != null)
                        .sorted(Comparator.comparing(Asistencia::getFecha).reversed())
                        .collect(Collectors.groupingBy(
                                Asistencia::getEtiquetaSemana,
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

    // =========================================================================
    // PASE DE LISTA GENERAL (Soporta /asistencia/pase-lista y /operaciones/asistencia/pase-lista)
    // =========================================================================
    @PostMapping(value = {"/asistencia/pase-lista", "/pase-lista", "/operaciones/asistencia/pase-lista"})
    public String registrarPaseLista(@RequestParam(value = "usuarioId", required = false) List<Long> usuarioIds,
                                     @RequestParam(value = "estado", required = false) List<String> estados,
                                     @RequestParam(value = "observaciones", required = false) List<String> observaciones,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes,
                                     HttpServletRequest request) {

        String paginaOrigen = request.getHeader("Referer");
        String redireccionDestino = (paginaOrigen != null && !paginaOrigen.contains("/pase-lista")) 
                ? "redirect:" + paginaOrigen 
                : "redirect:/usuarios";

        boolean esJefe = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("JEFE") || a.getAuthority().equals("ROLE_JEFE") 
                            || a.getAuthority().equals("GERENTE") || a.getAuthority().equals("ROLE_GERENTE")
                            || a.getAuthority().equals("ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));

        if (!esJefe) {
            redirectAttributes.addFlashAttribute("alertaAsistencia", "Acceso denegado: Solo el Jefe o Gerente puede registrar asistencia.");
            return redireccionDestino;
        }

        if (usuarioIds == null || usuarioIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("alertaAsistencia", "No hay empleados disponibles para pasar lista.");
            return redireccionDestino;
        }

        try {
            String usuarioLogueado = authentication.getName();
            int procesados = 0;

            for (int i = 0; i < usuarioIds.size(); i++) {
                Long uId = usuarioIds.get(i);
                String estadoStr = (estados != null && i < estados.size()) ? estados.get(i) : "ASISTENCIA";
                String obsStr = (observaciones != null && i < observaciones.size()) ? observaciones.get(i) : "";

                Usuario empleado = usuarioRepository.findById(uId).orElse(null);

                if (empleado == null || empleado.getUsername().equalsIgnoreCase(usuarioLogueado)) {
                    continue; 
                }

                LocalDate hoy = LocalDate.now();
                Optional<Asistencia> existencia = asistenciaRepository.findByUsuarioAndFecha(empleado, hoy);

                Asistencia asistencia = existencia.orElseGet(() -> {
                    Asistencia nueva = new Asistencia();
                    nueva.setUsuario(empleado);
                    nueva.setFecha(hoy);
                    nueva.setHoraEntrada(LocalTime.now());
                    return nueva;
                });

                asistencia.setEstado(estadoStr);
                asistencia.setObservaciones(obsStr);

                asistenciaRepository.save(asistencia);
                procesados++;
            }

            redirectAttributes.addFlashAttribute("exitoAsistencia", "¡Pase de lista guardado con éxito! Se registraron " + procesados + " asistencias.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("alertaAsistencia", "Error al procesar el pase de lista: " + e.getMessage());
        }

        return redireccionDestino;
    }
}